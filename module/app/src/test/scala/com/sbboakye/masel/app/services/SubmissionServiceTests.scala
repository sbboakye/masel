package com.sbboakye.masel.app.services

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.sbboakye.masel.app.requests.UpdateSubmissionRequest
import com.sbboakye.masel.core.domain.{ChallengeId, SubmissionId}
import com.sbboakye.masel.core.errors.AppError
import io.circe.Json
import io.github.iltotore.iron.autoRefine
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

class SubmissionServiceTests extends AsyncFreeSpec with AsyncIOSpec with Matchers with AppServiceFixture:

  private def newService: IO[(InMemoryAppDb[IO], SubmissionService[IO])] =
    InMemoryAppDb.make[IO].map(db => (db, new SubmissionService[IO](db)))

  "SubmissionService" - {

    "listSubmissions" - {
      "returns an empty list when nothing is stored" in
        newService.flatMap { case (_, service) =>
          service.listSubmissions(10, 0).asserting(_ shouldBe empty)
        }

      "returns the seeded submissions (respecting limit and offset)" in
        newService.flatMap { case (db, service) =>
          for {
            challenge <- seededChallenge()
            _ <- db.seedChallenge(challenge)
            s1 <- seededSubmission(challenge.id)
            s2 <- seededSubmission(challenge.id)
            s3 <- seededSubmission(challenge.id)
            _ <- db.seedSubmission(s1)
            _ <- db.seedSubmission(s2)
            _ <- db.seedSubmission(s3)
            all <- service.listSubmissions(10, 0)
            page <- service.listSubmissions(2, 1)
          } yield {
            all should have size 3
            page should have size 2
          }
        }

      "wraps unexpected repository errors as InternalError" in {
        val boom = new RuntimeException("conn lost")
        val service = new SubmissionService[IO](FailingAppDb[IO](boom))
        service.listSubmissions(10, 0).attempt.asserting {
          case Left(AppError.InternalError(_, cause)) => cause shouldBe Some(boom)
          case other => fail(s"Expected InternalError, got $other")
        }
      }
    }

    "getSubmission" - {
      "returns the submission when it exists" in
        newService.flatMap { case (db, service) =>
          for {
            challenge <- seededChallenge()
            _ <- db.seedChallenge(challenge)
            seeded <- seededSubmission(challenge.id, withScore = true)
            _ <- db.seedSubmission(seeded)
            found <- service.getSubmission(seeded.id)
          } yield found shouldBe seeded
        }

      "raises NotFound when the submission is absent" in
        newService.flatMap { case (_, service) =>
          for {
            missing <- SubmissionId.generate[IO]
            result <- service.getSubmission(missing).attempt
          } yield result match {
            case Left(AppError.NotFound("Submission", id)) =>
              id shouldBe missing.value.toString
            case other => fail(s"Expected NotFound(Submission, _), got $other")
          }
        }

      "wraps unexpected repository errors as InternalError" in {
        val service = new SubmissionService[IO](FailingAppDb[IO](new RuntimeException("nope")))
        for {
          id <- SubmissionId.generate[IO]
          result <- service.getSubmission(id).attempt
        } yield result match {
          case Left(_: AppError.InternalError) => succeed
          case other => fail(s"Expected InternalError, got $other")
        }
      }
    }

    "createSubmission" - {
      "persists a new submission tied to the supplied challenge, with output=None and score=None" in
        newService.flatMap { case (db, service) =>
          for {
            challenge <- seededChallenge()
            _ <- db.seedChallenge(challenge)
            request = createSubmissionRequestFor(challenge.id)
            created <- service.createSubmission(request)
            stored <- db.submissionsRef.get.map(_.get(created.id))
          } yield {
            created.challengeId shouldBe challenge.id
            created.candidateSolution shouldBe request.candidateSolution
            created.output shouldBe None
            created.score shouldBe None
            created.createdAt shouldBe created.updatedAt
            stored shouldBe Some(created)
          }
        }

      "each invocation produces a distinct id" in
        newService.flatMap { case (db, service) =>
          for {
            challenge <- seededChallenge()
            _ <- db.seedChallenge(challenge)
            request = createSubmissionRequestFor(challenge.id)
            a <- service.createSubmission(request)
            b <- service.createSubmission(request)
          } yield a.id should not equal b.id
        }

      "raises NotFound when the referenced challenge does not exist" in
        newService.flatMap { case (_, service) =>
          for {
            missingChallenge <- ChallengeId.generate[IO]
            request = createSubmissionRequestFor(missingChallenge)
            result <- service.createSubmission(request).attempt
          } yield result match {
            case Left(AppError.NotFound("Challenge", id)) =>
              id shouldBe missingChallenge.value.toString
            case other => fail(s"Expected NotFound(Challenge, _), got $other")
          }
        }

      "wraps unexpected repository errors as InternalError" in {
        val service = new SubmissionService[IO](FailingAppDb[IO](new RuntimeException("write failed")))
        for {
          id <- ChallengeId.generate[IO]
          result <- service.createSubmission(createSubmissionRequestFor(id)).attempt
        } yield result match {
          case Left(_: AppError.InternalError) => succeed
          case other => fail(s"Expected InternalError, got $other")
        }
      }
    }

    "updateSubmission" - {
      "applies all supplied fields and bumps updatedAt" in
        newService.flatMap { case (db, service) =>
          for {
            challenge <- seededChallenge()
            _ <- db.seedChallenge(challenge)
            seeded <- seededSubmission(challenge.id)
            _ <- db.seedSubmission(seeded)
            updated <- service.updateSubmission(seeded.id, fullUpdateSubmissionRequest)
          } yield {
            updated.id shouldBe seeded.id
            updated.challengeId shouldBe seeded.challengeId
            updated.candidateSolution shouldBe fullUpdateSubmissionRequest.candidateSolution.get
            updated.output shouldBe fullUpdateSubmissionRequest.output
            updated.score shouldBe fullUpdateSubmissionRequest.score
            updated.createdAt shouldBe seeded.createdAt
          }
        }

      "an empty update is a no-op for content fields" in
        newService.flatMap { case (db, service) =>
          for {
            challenge <- seededChallenge()
            _ <- db.seedChallenge(challenge)
            seeded <- seededSubmission(challenge.id, withScore = true)
            _ <- db.seedSubmission(seeded)
            updated <- service.updateSubmission(seeded.id, emptyUpdateSubmissionRequest)
          } yield {
            updated.id shouldBe seeded.id
            updated.candidateSolution shouldBe seeded.candidateSolution
            updated.output shouldBe seeded.output
            updated.score shouldBe seeded.score
          }
        }

      "preserves existing output/score when the update omits them (orElse semantics)" in
        newService.flatMap { case (db, service) =>
          val existingOutput = Json.obj("rows" -> Json.fromInt(3))
          for {
            challenge <- seededChallenge()
            _ <- db.seedChallenge(challenge)
            base <- seededSubmission(challenge.id)
            seeded = base.copy(output = Some(existingOutput), score = Some(72))
            _ <- db.seedSubmission(seeded)
            partial = UpdateSubmissionRequest(
              candidateSolution = Some("SELECT 99"),
              output = None,
              score = None,
            )
            updated <- service.updateSubmission(seeded.id, partial)
          } yield {
            updated.candidateSolution shouldBe partial.candidateSolution.get
            updated.output shouldBe Some(existingOutput)
            updated.score shouldBe Some(72)
          }
        }

      "raises NotFound when the submission does not exist" in
        newService.flatMap { case (_, service) =>
          for {
            missing <- SubmissionId.generate[IO]
            result <- service.updateSubmission(missing, fullUpdateSubmissionRequest).attempt
          } yield result match {
            case Left(AppError.NotFound("Submission", id)) =>
              id shouldBe missing.value.toString
            case other => fail(s"Expected NotFound(Submission, _), got $other")
          }
        }

      "wraps unexpected repository errors as InternalError" in {
        val service = new SubmissionService[IO](FailingAppDb[IO](new RuntimeException("xa fail")))
        for {
          id <- SubmissionId.generate[IO]
          result <- service.updateSubmission(id, fullUpdateSubmissionRequest).attempt
        } yield result match {
          case Left(_: AppError.InternalError) => succeed
          case other => fail(s"Expected InternalError, got $other")
        }
      }
    }

    "deleteSubmission" - {
      "removes the submission when it exists" in
        newService.flatMap { case (db, service) =>
          for {
            challenge <- seededChallenge()
            _ <- db.seedChallenge(challenge)
            seeded <- seededSubmission(challenge.id)
            _ <- db.seedSubmission(seeded)
            _ <- service.deleteSubmission(seeded.id)
            after <- db.submissionsRef.get
          } yield after.get(seeded.id) shouldBe None
        }

      "raises NotFound when the submission does not exist" in
        newService.flatMap { case (_, service) =>
          for {
            missing <- SubmissionId.generate[IO]
            result <- service.deleteSubmission(missing).attempt
          } yield result match {
            case Left(AppError.NotFound("Submission", id)) =>
              id shouldBe missing.value.toString
            case other => fail(s"Expected NotFound(Submission, _), got $other")
          }
        }

      "wraps unexpected repository errors as InternalError" in {
        val service = new SubmissionService[IO](FailingAppDb[IO](new RuntimeException("delete failed")))
        for {
          id <- SubmissionId.generate[IO]
          result <- service.deleteSubmission(id).attempt
        } yield result match {
          case Left(_: AppError.InternalError) => succeed
          case other => fail(s"Expected InternalError, got $other")
        }
      }
    }
  }
