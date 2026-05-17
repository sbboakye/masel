package com.sbboakye.masel.core.domain

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import io.circe.Json
import io.circe.syntax.*
import java.util.UUID
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

class IdsTests extends AsyncFreeSpec with AsyncIOSpec with Matchers:

  "ChallengeId" - {
    "apply wraps a UUID and value extracts it" in {
      val uuid = UUID.randomUUID()
      val id = ChallengeId(uuid)
      id.value shouldBe uuid
    }

    "generate produces a distinct id on each call" in
      (for {
        a <- ChallengeId.generate[IO]
        b <- ChallengeId.generate[IO]
      } yield a should not equal b)

    "JSON round-trips: encode -> decode yields the same id" in {
      val uuid = UUID.randomUUID()
      val id = ChallengeId(uuid)
      id.asJson.noSpaces shouldBe s""""${uuid.toString}""""
      id.asJson.as[ChallengeId] shouldBe Right(id)
    }

    "fails to decode a string that is not a valid UUID" in {
      val result = Json.fromString("not-a-uuid").as[ChallengeId]
      result.isLeft shouldBe true
    }
  }

  "SubmissionId" - {
    "apply wraps a UUID and value extracts it" in {
      val uuid = UUID.randomUUID()
      val id = SubmissionId(uuid)
      id.value shouldBe uuid
    }

    "generate produces a distinct id on each call" in
      (for {
        a <- SubmissionId.generate[IO]
        b <- SubmissionId.generate[IO]
      } yield a should not equal b)

    "JSON round-trips: encode -> decode yields the same id" in {
      val uuid = UUID.randomUUID()
      val id = SubmissionId(uuid)
      id.asJson.noSpaces shouldBe s""""${uuid.toString}""""
      id.asJson.as[SubmissionId] shouldBe Right(id)
    }

    "fails to decode a string that is not a valid UUID" in {
      val result = Json.fromString("oops").as[SubmissionId]
      result.isLeft shouldBe true
    }
  }

  "ChallengeId and SubmissionId are distinct opaque types" - {
    "the same UUID wrapped as each compares equal when extracted" in {
      val uuid = UUID.randomUUID()
      ChallengeId(uuid).value shouldBe uuid
      SubmissionId(uuid).value shouldBe uuid
    }
  }
