package com.sbboakye.masel.app.services

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.sbboakye.masel.app.requests.UpdateChallengeRequest
import com.sbboakye.masel.core.domain.ChallengeDifficulty.Hard
import com.sbboakye.masel.core.domain.ChallengeStatus.{Active, Draft}
import com.sbboakye.masel.core.domain.{ChallengeId, ChallengeStatus}
import com.sbboakye.masel.core.errors.AppError
import io.circe.Json
import io.github.iltotore.iron.autoRefine
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

class ChallengeServiceTests extends AsyncFreeSpec with AsyncIOSpec with Matchers with AppServiceFixture:

  private def newService: IO[(InMemoryAppDb[IO], ChallengeService[IO])] =
    InMemoryAppDb.make[IO].map(db => (db, new ChallengeService[IO](db)))

  "ChallengeService" - {

    "listChallenges" - {
      "returns an empty list when nothing is stored" in
        newService.flatMap { case (_, service) =>
          service.listChallenges(10, 0).asserting(_ shouldBe empty)
        }

      "returns the seeded challenges (respecting limit and offset)" in
        newService.flatMap { case (db, service) =>
          for {
            c1 <- seededChallenge(title = "first")
            c2 <- seededChallenge(title = "second")
            c3 <- seededChallenge(title = "third")
            _ <- db.seedChallenge(c1)
            _ <- db.seedChallenge(c2)
            _ <- db.seedChallenge(c3)
            all <- service.listChallenges(10, 0)
            page <- service.listChallenges(1, 1)
          } yield {
            all should have size 3
            all.map(_.title.toString).toSet shouldBe Set("first", "second", "third")
            page should have size 1
          }
        }

      "wraps unexpected repository errors as InternalError" in {
        val boom = new RuntimeException("conn lost")
        val service = new ChallengeService[IO](FailingAppDb[IO](boom))
        service
          .listChallenges(10, 0)
          .attempt
          .asserting {
            case Left(AppError.InternalError(_, cause)) => cause shouldBe Some(boom)
            case other => fail(s"Expected InternalError, got $other")
          }
      }
    }

    "getChallenge" - {
      "returns the challenge when it exists" in
        newService.flatMap { case (db, service) =>
          for {
            seeded <- seededChallenge()
            _ <- db.seedChallenge(seeded)
            found <- service.getChallenge(seeded.id)
          } yield found shouldBe seeded
        }

      "raises NotFound when the challenge is absent" in
        newService.flatMap { case (_, service) =>
          for {
            missing <- ChallengeId.generate[IO]
            result <- service.getChallenge(missing).attempt
          } yield result match {
            case Left(AppError.NotFound(entity, id)) =>
              entity shouldBe "Challenge"
              id shouldBe missing.value.toString
            case other => fail(s"Expected NotFound, got $other")
          }
        }

      "wraps unexpected repository errors as InternalError" in {
        val service = new ChallengeService[IO](FailingAppDb[IO](new RuntimeException("nope")))
        for {
          id <- ChallengeId.generate[IO]
          result <- service.getChallenge(id).attempt
        } yield result match {
          case Left(_: AppError.InternalError) => succeed
          case other => fail(s"Expected InternalError, got $other")
        }
      }
    }

    "createChallenge" - {
      "persists a new challenge with status=Draft, output=None, and generated id/timestamps" in
        newService.flatMap { case (db, service) =>
          for {
            created <- service.createChallenge(createChallengeRequest)
            stored <- db.challengesRef.get.map(_.get(created.id))
          } yield {
            created.title shouldBe createChallengeRequest.title
            created.instructions shouldBe createChallengeRequest.instructions
            created.expectedSolution shouldBe createChallengeRequest.expectedSolution
            created.allottedTime shouldBe createChallengeRequest.allottedTime
            created.difficulty shouldBe createChallengeRequest.difficulty
            created.status shouldBe Draft
            created.output shouldBe None
            created.createdAt shouldBe created.updatedAt
            stored shouldBe Some(created)
          }
        }

      "each invocation produces a distinct id" in
        newService.flatMap { case (_, service) =>
          for {
            first <- service.createChallenge(createChallengeRequest)
            second <- service.createChallenge(createChallengeRequest)
          } yield first.id should not equal second.id
        }

      "wraps unexpected repository errors as InternalError" in {
        val service = new ChallengeService[IO](FailingAppDb[IO](new RuntimeException("write failed")))
        service.createChallenge(createChallengeRequest).attempt.asserting {
          case Left(_: AppError.InternalError) => succeed
          case other => fail(s"Expected InternalError, got $other")
        }
      }
    }

    "updateChallenge" - {
      "applies all supplied fields and bumps updatedAt" in
        newService.flatMap { case (db, service) =>
          for {
            seeded <- seededChallenge()
            _ <- db.seedChallenge(seeded)
            updated <- service.updateChallenge(seeded.id, fullUpdateChallengeRequest)
          } yield {
            updated.id shouldBe seeded.id
            updated.title shouldBe fullUpdateChallengeRequest.title.get
            updated.instructions shouldBe fullUpdateChallengeRequest.instructions.get
            updated.status shouldBe Active
            updated.expectedSolution shouldBe fullUpdateChallengeRequest.expectedSolution.get
            updated.output shouldBe fullUpdateChallengeRequest.output
            updated.allottedTime shouldBe fullUpdateChallengeRequest.allottedTime.get
            updated.difficulty shouldBe Hard
            updated.createdAt shouldBe seeded.createdAt
            // updatedAt is bumped from now()
            updated.updatedAt.isAfter(seeded.updatedAt) || updated.updatedAt
              .isEqual(seeded.updatedAt) shouldBe true
          }
        }

      "an empty update is a no-op for content fields (only updatedAt may change)" in
        newService.flatMap { case (db, service) =>
          for {
            seeded <- seededChallenge()
            _ <- db.seedChallenge(seeded)
            updated <- service.updateChallenge(seeded.id, emptyUpdateChallengeRequest)
          } yield {
            updated.id shouldBe seeded.id
            updated.title shouldBe seeded.title
            updated.instructions shouldBe seeded.instructions
            updated.status shouldBe seeded.status
            updated.expectedSolution shouldBe seeded.expectedSolution
            updated.output shouldBe seeded.output
            updated.allottedTime shouldBe seeded.allottedTime
            updated.difficulty shouldBe seeded.difficulty
            updated.createdAt shouldBe seeded.createdAt
          }
        }

      "merges only the provided fields, leaving the rest intact" in
        newService.flatMap { case (db, service) =>
          val partial = UpdateChallengeRequest(
            title = Some("only the title changed"),
            instructions = None,
            setupSql = None,
            status = Some(validatedStatus),
            expectedSolution = None,
            output = None,
            allottedTime = None,
            difficulty = None,
          )
          for {
            seeded <- seededChallenge(title = "original title", status = Draft)
            _ <- db.seedChallenge(seeded)
            updated <- service.updateChallenge(seeded.id, partial)
          } yield {
            updated.title shouldBe partial.title.get
            updated.status shouldBe validatedStatus
            updated.instructions shouldBe seeded.instructions
            updated.expectedSolution shouldBe seeded.expectedSolution
            updated.allottedTime shouldBe seeded.allottedTime
            updated.difficulty shouldBe seeded.difficulty
            updated.output shouldBe seeded.output
          }
        }

      "leaves existing output untouched when the update omits it (output uses orElse)" in
        newService.flatMap { case (db, service) =>
          val existingOutput = Json.obj("k" -> Json.fromString("v"))
          for {
            base <- seededChallenge()
            seeded = base.copy(output = Some(existingOutput))
            _ <- db.seedChallenge(seeded)
            updated <- service.updateChallenge(seeded.id, emptyUpdateChallengeRequest)
          } yield updated.output shouldBe Some(existingOutput)
        }

      "raises NotFound when the challenge does not exist" in
        newService.flatMap { case (_, service) =>
          for {
            missing <- ChallengeId.generate[IO]
            result <- service.updateChallenge(missing, fullUpdateChallengeRequest).attempt
          } yield result match {
            case Left(AppError.NotFound("Challenge", id)) =>
              id shouldBe missing.value.toString
            case other => fail(s"Expected NotFound(Challenge, _), got $other")
          }
        }

      "wraps unexpected repository errors as InternalError" in {
        val service = new ChallengeService[IO](FailingAppDb[IO](new RuntimeException("xa fail")))
        for {
          id <- ChallengeId.generate[IO]
          result <- service.updateChallenge(id, fullUpdateChallengeRequest).attempt
        } yield result match {
          case Left(_: AppError.InternalError) => succeed
          case other => fail(s"Expected InternalError, got $other")
        }
      }
    }

    "deleteChallenge" - {
      "removes the challenge when it exists" in
        newService.flatMap { case (db, service) =>
          for {
            seeded <- seededChallenge()
            _ <- db.seedChallenge(seeded)
            _ <- service.deleteChallenge(seeded.id)
            after <- db.challengesRef.get
          } yield after.get(seeded.id) shouldBe None
        }

      "raises NotFound when the challenge does not exist" in
        newService.flatMap { case (_, service) =>
          for {
            missing <- ChallengeId.generate[IO]
            result <- service.deleteChallenge(missing).attempt
          } yield result match {
            case Left(AppError.NotFound("Challenge", id)) =>
              id shouldBe missing.value.toString
            case other => fail(s"Expected NotFound(Challenge, _), got $other")
          }
        }

      "wraps unexpected repository errors as InternalError" in {
        val service = new ChallengeService[IO](FailingAppDb[IO](new RuntimeException("delete failed")))
        for {
          id <- ChallengeId.generate[IO]
          result <- service.deleteChallenge(id).attempt
        } yield result match {
          case Left(_: AppError.InternalError) => succeed
          case other => fail(s"Expected InternalError, got $other")
        }
      }
    }
  }
