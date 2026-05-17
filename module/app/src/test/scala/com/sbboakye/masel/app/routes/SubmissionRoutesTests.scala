package com.sbboakye.masel.app.routes

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.sbboakye.masel.app.services.{AppServiceFixture, FailingAppDb, InMemoryAppDb, SubmissionService}
import com.sbboakye.masel.core.domain.{ChallengeId, Submission, SubmissionId}
import io.circe.Json
import io.circe.syntax.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.headers.`Content-Type`
import org.http4s.implicits.*
import org.http4s.{HttpRoutes, MediaType, Method, Request, Status}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

class SubmissionRoutesTests extends AsyncFreeSpec with AsyncIOSpec with Matchers with AppServiceFixture:

  private def setup: IO[(InMemoryAppDb[IO], HttpRoutes[IO])] =
    InMemoryAppDb.make[IO].map { db =>
      val service = new SubmissionService[IO](db)
      (db, new SubmissionRoutes[IO](service).routes)
    }

  private def failingRoutes(error: Throwable): HttpRoutes[IO] = {
    val service = new SubmissionService[IO](FailingAppDb[IO](error))
    new SubmissionRoutes[IO](service).routes
  }

  "GET /api/v1/submissions" - {
    "responds 200 with the empty list when nothing is stored" in
      setup.flatMap { case (_, routes) =>
        val req = Request[IO](Method.GET, uri"/api/v1/submissions")
        for {
          resp <- routes.orNotFound.run(req)
          body <- resp.as[List[Submission]]
        } yield {
          resp.status shouldBe Status.Ok
          body shouldBe empty
        }
      }

    "responds 200 with all seeded submissions" in
      setup.flatMap { case (db, routes) =>
        for {
          challenge <- seededChallenge()
          _ <- db.seedChallenge(challenge)
          s1 <- seededSubmission(challenge.id)
          s2 <- seededSubmission(challenge.id)
          _ <- db.seedSubmission(s1)
          _ <- db.seedSubmission(s2)
          req = Request[IO](Method.GET, uri"/api/v1/submissions")
          resp <- routes.orNotFound.run(req)
          body <- resp.as[List[Submission]]
        } yield {
          resp.status shouldBe Status.Ok
          body should have size 2
        }
      }

    "honours the limit and offset query parameters" in
      setup.flatMap { case (db, routes) =>
        for {
          challenge <- seededChallenge()
          _ <- db.seedChallenge(challenge)
          s1 <- seededSubmission(challenge.id)
          s2 <- seededSubmission(challenge.id)
          s3 <- seededSubmission(challenge.id)
          _ <- db.seedSubmission(s1)
          _ <- db.seedSubmission(s2)
          _ <- db.seedSubmission(s3)
          req = Request[IO](Method.GET, uri"/api/v1/submissions?limit=2&offset=1")
          resp <- routes.orNotFound.run(req)
          body <- resp.as[List[Submission]]
        } yield {
          resp.status shouldBe Status.Ok
          body should have size 2
        }
      }

    "returns 500 when the underlying repository fails unexpectedly" in {
      val req = Request[IO](Method.GET, uri"/api/v1/submissions")
      failingRoutes(new RuntimeException("conn lost")).orNotFound
        .run(req)
        .asserting(_.status shouldBe Status.InternalServerError)
    }
  }

  "GET /api/v1/submissions/{id}" - {
    "responds 200 with the submission when it exists" in
      setup.flatMap { case (db, routes) =>
        for {
          challenge <- seededChallenge()
          _ <- db.seedChallenge(challenge)
          seeded <- seededSubmission(challenge.id, withScore = true)
          _ <- db.seedSubmission(seeded)
          req = Request[IO](
            Method.GET,
            uri"/api/v1/submissions".addSegment(seeded.id.value.toString),
          )
          resp <- routes.orNotFound.run(req)
          body <- resp.as[Submission]
        } yield {
          resp.status shouldBe Status.Ok
          body shouldBe seeded
        }
      }

    "responds 404 when the submission is absent" in
      setup.flatMap { case (_, routes) =>
        for {
          missing <- SubmissionId.generate[IO]
          req = Request[IO](
            Method.GET,
            uri"/api/v1/submissions".addSegment(missing.value.toString),
          )
          resp <- routes.orNotFound.run(req)
          json <- resp.as[Json]
        } yield {
          resp.status shouldBe Status.NotFound
          json.hcursor.get[String]("message").toOption shouldBe Some(
            s"Submission with id ${missing.value} not found",
          )
        }
      }

    "falls through (route does not match) when the id segment is not a UUID" in
      setup.flatMap { case (_, routes) =>
        val req = Request[IO](Method.GET, uri"/api/v1/submissions/not-a-uuid")
        routes.run(req).value.asserting(_ shouldBe None)
      }
  }

  "POST /api/v1/submissions" - {
    "responds 201 and stores the submission when the referenced challenge exists" in
      setup.flatMap { case (db, routes) =>
        for {
          challenge <- seededChallenge()
          _ <- db.seedChallenge(challenge)
          body = createSubmissionRequestFor(challenge.id)
          req = Request[IO](Method.POST, uri"/api/v1/submissions").withEntity(body)
          resp <- routes.orNotFound.run(req)
          created <- resp.as[Submission]
          stored <- db.submissionsRef.get.map(_.get(created.id))
        } yield {
          resp.status shouldBe Status.Created
          created.challengeId shouldBe challenge.id
          created.candidateSolution shouldBe body.candidateSolution
          stored shouldBe Some(created)
        }
      }

    "responds 404 when the referenced challenge does not exist" in
      setup.flatMap { case (_, routes) =>
        for {
          missingChallenge <- ChallengeId.generate[IO]
          body = createSubmissionRequestFor(missingChallenge)
          req = Request[IO](Method.POST, uri"/api/v1/submissions").withEntity(body)
          resp <- routes.orNotFound.run(req)
          json <- resp.as[Json]
        } yield {
          resp.status shouldBe Status.NotFound
          json.hcursor.get[String]("message").toOption shouldBe Some(
            s"Challenge with id ${missingChallenge.value} not found",
          )
        }
      }

    // http4s's circe decoder surfaces every decode failure as InvalidMessageBodyFailure
    // (-> 422), regardless of whether the body was unparseable JSON or the wrong shape.
    "returns 422 (UnprocessableEntity) when the body is not valid JSON" in
      setup.flatMap { case (_, routes) =>
        val req = Request[IO](Method.POST, uri"/api/v1/submissions")
          .withEntity("garbage")
          .withContentType(`Content-Type`(MediaType.application.json))
        routes.orNotFound.run(req).asserting(_.status shouldBe Status.UnprocessableEntity)
      }

    "returns 422 (UnprocessableEntity) when required fields are missing from the body" in
      setup.flatMap { case (_, routes) =>
        val incomplete = Json.obj("candidateSolution" -> Json.fromString("SELECT 1"))
        val req = Request[IO](Method.POST, uri"/api/v1/submissions").withEntity(incomplete)
        routes.orNotFound.run(req).asserting(_.status shouldBe Status.UnprocessableEntity)
      }

    "returns 500 when the repository fails unexpectedly" in {
      for {
        challengeId <- ChallengeId.generate[IO]
        body = createSubmissionRequestFor(challengeId)
        req = Request[IO](Method.POST, uri"/api/v1/submissions").withEntity(body)
        resp <- failingRoutes(new RuntimeException("write failed")).orNotFound.run(req)
      } yield resp.status shouldBe Status.InternalServerError
    }
  }

  "PUT /api/v1/submissions/{id}" - {
    "responds 200 with the updated submission when it exists" in
      setup.flatMap { case (db, routes) =>
        for {
          challenge <- seededChallenge()
          _ <- db.seedChallenge(challenge)
          seeded <- seededSubmission(challenge.id)
          _ <- db.seedSubmission(seeded)
          req = Request[IO](
            Method.PUT,
            uri"/api/v1/submissions".addSegment(seeded.id.value.toString),
          ).withEntity(fullUpdateSubmissionRequest.asJson)
          resp <- routes.orNotFound.run(req)
          updated <- resp.as[Submission]
        } yield {
          resp.status shouldBe Status.Ok
          updated.id shouldBe seeded.id
          updated.candidateSolution shouldBe fullUpdateSubmissionRequest.candidateSolution.get
          updated.score shouldBe fullUpdateSubmissionRequest.score
        }
      }

    "responds 404 when the submission does not exist" in
      setup.flatMap { case (_, routes) =>
        for {
          missing <- SubmissionId.generate[IO]
          req = Request[IO](
            Method.PUT,
            uri"/api/v1/submissions".addSegment(missing.value.toString),
          ).withEntity(fullUpdateSubmissionRequest.asJson)
          resp <- routes.orNotFound.run(req)
        } yield resp.status shouldBe Status.NotFound
      }

    "accepts an empty JSON object as a no-op merge" in
      setup.flatMap { case (db, routes) =>
        for {
          challenge <- seededChallenge()
          _ <- db.seedChallenge(challenge)
          seeded <- seededSubmission(challenge.id, withScore = true)
          _ <- db.seedSubmission(seeded)
          req = Request[IO](
            Method.PUT,
            uri"/api/v1/submissions".addSegment(seeded.id.value.toString),
          ).withEntity(Json.obj())
          resp <- routes.orNotFound.run(req)
          updated <- resp.as[Submission]
        } yield {
          resp.status shouldBe Status.Ok
          updated.candidateSolution shouldBe seeded.candidateSolution
          updated.score shouldBe seeded.score
        }
      }
  }

  "DELETE /api/v1/submissions/{id}" - {
    "responds 204 and removes the submission when it exists" in
      setup.flatMap { case (db, routes) =>
        for {
          challenge <- seededChallenge()
          _ <- db.seedChallenge(challenge)
          seeded <- seededSubmission(challenge.id)
          _ <- db.seedSubmission(seeded)
          req = Request[IO](
            Method.DELETE,
            uri"/api/v1/submissions".addSegment(seeded.id.value.toString),
          )
          resp <- routes.orNotFound.run(req)
          after <- db.submissionsRef.get
        } yield {
          resp.status shouldBe Status.NoContent
          after.get(seeded.id) shouldBe None
        }
      }

    "responds 404 when the submission does not exist" in
      setup.flatMap { case (_, routes) =>
        for {
          missing <- SubmissionId.generate[IO]
          req = Request[IO](
            Method.DELETE,
            uri"/api/v1/submissions".addSegment(missing.value.toString),
          )
          resp <- routes.orNotFound.run(req)
        } yield resp.status shouldBe Status.NotFound
      }
  }

  "method routing" - {
    "PATCH on /api/v1/submissions falls through (route is not declared)" in
      setup.flatMap { case (_, routes) =>
        val req = Request[IO](Method.PATCH, uri"/api/v1/submissions")
        routes.run(req).value.asserting(_ shouldBe None)
      }
  }
