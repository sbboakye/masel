package com.sbboakye.masel.app.routes

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.sbboakye.masel.app.services.{AppServiceFixture, ChallengeService, InMemoryAppDb, SubmissionService}
import com.sbboakye.masel.core.domain.{ChallengeId, SubmissionId}
import io.circe.syntax.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.implicits.*
import org.http4s.{HttpRoutes, Method, Request}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

/**
 * Regression tests for the `BaseEndpoint.basePath` qualification fix in the route patterns.
 *
 * Prior to the fix, `basePath` was an unqualified lowercase identifier in pattern position and Scala bound it as a
 * fresh pattern variable. That made `/challenges`, `/foo/challenges`, `/api/v1/challenges`, etc. all match. After
 * qualifying with `BaseEndpoint.basePath`, only `/api/v1/...` matches.
 */
class BasePathEnforcementTests extends AsyncFreeSpec with AsyncIOSpec with Matchers with AppServiceFixture:

  private def challengeRoutes: IO[HttpRoutes[IO]] =
    InMemoryAppDb.make[IO].map(db => new ChallengeRoutes[IO](new ChallengeService[IO](db)).routes)

  private def submissionRoutes: IO[HttpRoutes[IO]] =
    InMemoryAppDb.make[IO].map(db => new SubmissionRoutes[IO](new SubmissionService[IO](db)).routes)

  "ChallengeRoutes enforces the /api/v1 prefix" - {
    "GET /challenges (no prefix) does not match" in
      challengeRoutes.flatMap(r => r.run(Request[IO](Method.GET, uri"/challenges")).value.asserting(_ shouldBe None))

    "GET /foo/challenges (wrong prefix) does not match" in
      challengeRoutes.flatMap { r =>
        r.run(Request[IO](Method.GET, uri"/foo/challenges")).value.asserting(_ shouldBe None)
      }

    "GET /challenges/{uuid} (no prefix) does not match" in
      challengeRoutes.flatMap { r =>
        for {
          id <- ChallengeId.generate[IO]
          uri = uri"/challenges".addSegment(id.value.toString)
          out <- r.run(Request[IO](Method.GET, uri)).value
        } yield out shouldBe None
      }

    "PUT /challenges/{uuid} (no prefix) does not match" in
      challengeRoutes.flatMap { r =>
        for {
          id <- ChallengeId.generate[IO]
          uri = uri"/challenges".addSegment(id.value.toString)
          out <- r
            .run(Request[IO](Method.PUT, uri).withEntity(fullUpdateChallengeRequest.asJson))
            .value
        } yield out shouldBe None
      }

    "DELETE /challenges/{uuid} (no prefix) does not match" in
      challengeRoutes.flatMap { r =>
        for {
          id <- ChallengeId.generate[IO]
          uri = uri"/challenges".addSegment(id.value.toString)
          out <- r.run(Request[IO](Method.DELETE, uri)).value
        } yield out shouldBe None
      }

    "POST /challenges (no prefix) does not match" in
      challengeRoutes.flatMap { r =>
        val req = Request[IO](Method.POST, uri"/challenges").withEntity(createChallengeRequest)
        r.run(req).value.asserting(_ shouldBe None)
      }
  }

  "SubmissionRoutes enforces the /api/v1 prefix on every method" - {
    "GET /submissions does not match" in
      submissionRoutes.flatMap(r => r.run(Request[IO](Method.GET, uri"/submissions")).value.asserting(_ shouldBe None))

    "GET /submissions/{uuid} does not match" in
      submissionRoutes.flatMap { r =>
        for {
          id <- SubmissionId.generate[IO]
          uri = uri"/submissions".addSegment(id.value.toString)
          out <- r.run(Request[IO](Method.GET, uri)).value
        } yield out shouldBe None
      }

    "POST /submissions does not match" in
      submissionRoutes.flatMap { r =>
        for {
          challengeId <- ChallengeId.generate[IO]
          body = createSubmissionRequestFor(challengeId)
          out <- r
            .run(Request[IO](Method.POST, uri"/submissions").withEntity(body))
            .value
        } yield out shouldBe None
      }

    "PUT /submissions/{uuid} does not match" in
      submissionRoutes.flatMap { r =>
        for {
          id <- SubmissionId.generate[IO]
          uri = uri"/submissions".addSegment(id.value.toString)
          out <- r
            .run(Request[IO](Method.PUT, uri).withEntity(fullUpdateSubmissionRequest.asJson))
            .value
        } yield out shouldBe None
      }

    "DELETE /submissions/{uuid} does not match" in
      submissionRoutes.flatMap { r =>
        for {
          id <- SubmissionId.generate[IO]
          uri = uri"/submissions".addSegment(id.value.toString)
          out <- r.run(Request[IO](Method.DELETE, uri)).value
        } yield out shouldBe None
      }
  }
