package com.sbboakye.masel.app.routes

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.sbboakye.masel.app.services.{AppServiceFixture, ChallengeService, FailingAppDb, InMemoryAppDb}
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId}
import io.circe.Json
import io.circe.syntax.*
import io.github.iltotore.iron.autoRefine
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.headers.`Content-Type`
import org.http4s.implicits.*
import org.http4s.{HttpRoutes, MediaType, Method, Request, Status}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

class ChallengeRoutesTests extends AsyncFreeSpec with AsyncIOSpec with Matchers with AppServiceFixture:

  private def setup: IO[(InMemoryAppDb[IO], HttpRoutes[IO])] =
    InMemoryAppDb.make[IO].map { db =>
      val service = new ChallengeService[IO](db)
      (db, new ChallengeRoutes[IO](service).routes)
    }

  private def failingRoutes(error: Throwable): HttpRoutes[IO] = {
    val service = new ChallengeService[IO](FailingAppDb[IO](error))
    new ChallengeRoutes[IO](service).routes
  }

  "GET /api/v1/challenges" - {
    "responds 200 with the empty list when nothing is stored" in
      setup.flatMap { case (_, routes) =>
        val req = Request[IO](Method.GET, uri"/api/v1/challenges")
        for {
          resp <- routes.orNotFound.run(req)
          body <- resp.as[List[Challenge]]
        } yield {
          resp.status shouldBe Status.Ok
          body shouldBe empty
        }
      }

    "responds 200 with every seeded challenge under the default page size" in
      setup.flatMap { case (db, routes) =>
        val req = Request[IO](Method.GET, uri"/api/v1/challenges")
        for {
          c1 <- seededChallenge(title = "a")
          c2 <- seededChallenge(title = "b")
          _ <- db.seedChallenge(c1)
          _ <- db.seedChallenge(c2)
          resp <- routes.orNotFound.run(req)
          body <- resp.as[List[Challenge]]
        } yield {
          resp.status shouldBe Status.Ok
          body should have size 2
        }
      }

    "honours the limit and offset query parameters" in
      setup.flatMap { case (db, routes) =>
        for {
          c1 <- seededChallenge(title = "a")
          c2 <- seededChallenge(title = "b")
          c3 <- seededChallenge(title = "c")
          _ <- db.seedChallenge(c1)
          _ <- db.seedChallenge(c2)
          _ <- db.seedChallenge(c3)
          req = Request[IO](Method.GET, uri"/api/v1/challenges?limit=1&offset=1")
          resp <- routes.orNotFound.run(req)
          body <- resp.as[List[Challenge]]
        } yield {
          resp.status shouldBe Status.Ok
          body should have size 1
        }
      }

    "returns 500 when the underlying repository fails unexpectedly" in {
      val req = Request[IO](Method.GET, uri"/api/v1/challenges")
      failingRoutes(new RuntimeException("conn lost")).orNotFound
        .run(req)
        .asserting(_.status shouldBe Status.InternalServerError)
    }
  }

  "GET /api/v1/challenges/{id}" - {
    "responds 200 with the challenge when it exists" in
      setup.flatMap { case (db, routes) =>
        for {
          seeded <- seededChallenge()
          _ <- db.seedChallenge(seeded)
          req = Request[IO](
            Method.GET,
            uri"/api/v1/challenges".addSegment(seeded.id.value.toString),
          )
          resp <- routes.orNotFound.run(req)
          body <- resp.as[Challenge]
        } yield {
          resp.status shouldBe Status.Ok
          body shouldBe seeded
        }
      }

    "responds 404 when the challenge is absent" in
      setup.flatMap { case (_, routes) =>
        for {
          missing <- ChallengeId.generate[IO]
          req = Request[IO](
            Method.GET,
            uri"/api/v1/challenges".addSegment(missing.value.toString),
          )
          resp <- routes.orNotFound.run(req)
          json <- resp.as[Json]
        } yield {
          resp.status shouldBe Status.NotFound
          json.hcursor.get[String]("message").toOption shouldBe Some(
            s"Challenge with id ${missing.value} not found",
          )
        }
      }

    "falls through (route does not match) when the id segment is not a UUID" in
      setup.flatMap { case (_, routes) =>
        val req = Request[IO](Method.GET, uri"/api/v1/challenges/not-a-uuid")
        routes.run(req).value.asserting(_ shouldBe None)
      }
  }

  "POST /api/v1/challenges" - {
    "responds 201 and stores the new challenge" in
      setup.flatMap { case (db, routes) =>
        val req = Request[IO](Method.POST, uri"/api/v1/challenges").withEntity(createChallengeRequest)
        for {
          resp <- routes.orNotFound.run(req)
          created <- resp.as[Challenge]
          stored <- db.challengesRef.get.map(_.get(created.id))
        } yield {
          resp.status shouldBe Status.Created
          created.title shouldBe createChallengeRequest.title
          stored shouldBe Some(created)
        }
      }

    // http4s's circe entity decoder raises InvalidMessageBodyFailure for any decode
    // failure — both unparseable JSON and well-formed JSON of the wrong shape — so both
    // surface as 422 at the route layer. (See ErrorHandlingTests for the direct
    // MalformedMessageBodyFailure -> 400 case raised by other producers.)
    "returns 422 (UnprocessableEntity) when the body is not valid JSON" in
      setup.flatMap { case (_, routes) =>
        val req = Request[IO](Method.POST, uri"/api/v1/challenges")
          .withEntity("not json")
          .withContentType(`Content-Type`(MediaType.application.json))
        routes.orNotFound.run(req).asserting(_.status shouldBe Status.UnprocessableEntity)
      }

    "returns 422 (UnprocessableEntity) when the body is valid JSON but is missing required fields" in
      setup.flatMap { case (_, routes) =>
        val incomplete = Json.obj("title" -> Json.fromString("only a title"))
        val req = Request[IO](Method.POST, uri"/api/v1/challenges").withEntity(incomplete)
        routes.orNotFound.run(req).asserting(_.status shouldBe Status.UnprocessableEntity)
      }

    "returns 500 when the repository fails unexpectedly" in {
      val req = Request[IO](Method.POST, uri"/api/v1/challenges").withEntity(createChallengeRequest)
      failingRoutes(new RuntimeException("write failed")).orNotFound
        .run(req)
        .asserting(_.status shouldBe Status.InternalServerError)
    }
  }

  "PUT /api/v1/challenges/{id}" - {
    "responds 200 with the updated challenge when it exists" in
      setup.flatMap { case (db, routes) =>
        for {
          seeded <- seededChallenge()
          _ <- db.seedChallenge(seeded)
          req = Request[IO](
            Method.PUT,
            uri"/api/v1/challenges".addSegment(seeded.id.value.toString),
          ).withEntity(fullUpdateChallengeRequest.asJson)
          resp <- routes.orNotFound.run(req)
          updated <- resp.as[Challenge]
        } yield {
          resp.status shouldBe Status.Ok
          updated.id shouldBe seeded.id
          updated.title shouldBe fullUpdateChallengeRequest.title.get
        }
      }

    "responds 404 when the challenge does not exist" in
      setup.flatMap { case (_, routes) =>
        for {
          missing <- ChallengeId.generate[IO]
          req = Request[IO](
            Method.PUT,
            uri"/api/v1/challenges".addSegment(missing.value.toString),
          ).withEntity(fullUpdateChallengeRequest.asJson)
          resp <- routes.orNotFound.run(req)
        } yield resp.status shouldBe Status.NotFound
      }

    "accepts an empty JSON object as an update (no-op merge)" in
      setup.flatMap { case (db, routes) =>
        for {
          seeded <- seededChallenge()
          _ <- db.seedChallenge(seeded)
          req = Request[IO](
            Method.PUT,
            uri"/api/v1/challenges".addSegment(seeded.id.value.toString),
          ).withEntity(Json.obj())
          resp <- routes.orNotFound.run(req)
          updated <- resp.as[Challenge]
        } yield {
          resp.status shouldBe Status.Ok
          updated.title shouldBe seeded.title
          updated.instructions shouldBe seeded.instructions
        }
      }
  }

  "DELETE /api/v1/challenges/{id}" - {
    "responds 204 and removes the challenge when it exists" in
      setup.flatMap { case (db, routes) =>
        for {
          seeded <- seededChallenge()
          _ <- db.seedChallenge(seeded)
          req = Request[IO](
            Method.DELETE,
            uri"/api/v1/challenges".addSegment(seeded.id.value.toString),
          )
          resp <- routes.orNotFound.run(req)
          after <- db.challengesRef.get
        } yield {
          resp.status shouldBe Status.NoContent
          after.get(seeded.id) shouldBe None
        }
      }

    "responds 404 when the challenge does not exist" in
      setup.flatMap { case (_, routes) =>
        for {
          missing <- ChallengeId.generate[IO]
          req = Request[IO](
            Method.DELETE,
            uri"/api/v1/challenges".addSegment(missing.value.toString),
          )
          resp <- routes.orNotFound.run(req)
        } yield resp.status shouldBe Status.NotFound
      }
  }

  "method routing" - {
    "PATCH on /api/v1/challenges falls through (the route is not declared)" in
      setup.flatMap { case (_, routes) =>
        val req = Request[IO](Method.PATCH, uri"/api/v1/challenges")
        routes.run(req).value.asserting(_ shouldBe None)
      }

    "an entirely different path (e.g. /api/v1/foo) falls through" in
      setup.flatMap { case (_, routes) =>
        val req = Request[IO](Method.GET, uri"/api/v1/foo")
        routes.run(req).value.asserting(_ shouldBe None)
      }
  }
