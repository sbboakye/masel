package com.sbboakye.masel.core.domain

import io.circe.syntax.*
import io.circe.{Decoder, Json}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class EnumCodecTests extends AnyFreeSpec with Matchers:

  "ChallengeStatus.fromString" - {
    "matches every defined value case-insensitively" in {
      ChallengeStatus.fromString("draft") shouldBe Some(ChallengeStatus.Draft)
      ChallengeStatus.fromString("DRAFT") shouldBe Some(ChallengeStatus.Draft)
      ChallengeStatus.fromString("DrAfT") shouldBe Some(ChallengeStatus.Draft)
      ChallengeStatus.fromString("validated") shouldBe Some(ChallengeStatus.Validated)
      ChallengeStatus.fromString("active") shouldBe Some(ChallengeStatus.Active)
      ChallengeStatus.fromString("archived") shouldBe Some(ChallengeStatus.Archived)
    }

    "returns None for unknown values" in {
      ChallengeStatus.fromString("nope") shouldBe None
      ChallengeStatus.fromString("") shouldBe None
    }
  }

  "ChallengeStatus JSON codec" - {
    "encodes as a lowercase string" in {
      ChallengeStatus.values.toList.map(_.asJson.noSpaces) shouldBe List(
        "\"draft\"",
        "\"validated\"",
        "\"active\"",
        "\"archived\"",
      )
    }

    "decodes case-insensitively" in {
      Json.fromString("DRAFT").as[ChallengeStatus] shouldBe Right(ChallengeStatus.Draft)
      Json.fromString("active").as[ChallengeStatus] shouldBe Right(ChallengeStatus.Active)
    }

    "round-trips through encode/decode for every enum value" in
      ChallengeStatus.values.foreach(s => s.asJson.as[ChallengeStatus] shouldBe Right(s))

    "fails to decode an unknown status string with a useful error message" in {
      val result = Json.fromString("unknown").as[ChallengeStatus]
      result.isLeft shouldBe true
      result.left.toOption.get.getMessage should include("Unknown challenge status: unknown")
    }

    "fails to decode a non-string JSON value" in {
      val result = Decoder[ChallengeStatus].decodeJson(Json.fromInt(1))
      result.isLeft shouldBe true
    }
  }

  "ChallengeDifficulty.fromString" - {
    "matches every defined value case-insensitively" in {
      ChallengeDifficulty.fromString("easy") shouldBe Some(ChallengeDifficulty.Easy)
      ChallengeDifficulty.fromString("MEDIUM") shouldBe Some(ChallengeDifficulty.Medium)
      ChallengeDifficulty.fromString("Hard") shouldBe Some(ChallengeDifficulty.Hard)
    }

    "returns None for unknown values" in {
      ChallengeDifficulty.fromString("epic") shouldBe None
      ChallengeDifficulty.fromString("") shouldBe None
    }
  }

  "ChallengeDifficulty JSON codec" - {
    "encodes as a lowercase string" in {
      ChallengeDifficulty.values.toList.map(_.asJson.noSpaces) shouldBe List(
        "\"easy\"",
        "\"medium\"",
        "\"hard\"",
      )
    }

    "decodes case-insensitively" in {
      Json.fromString("EASY").as[ChallengeDifficulty] shouldBe Right(ChallengeDifficulty.Easy)
    }

    "round-trips through encode/decode for every enum value" in
      ChallengeDifficulty.values.foreach(d => d.asJson.as[ChallengeDifficulty] shouldBe Right(d))

    "fails to decode an unknown difficulty with a useful error message" in {
      val result = Json.fromString("impossible").as[ChallengeDifficulty]
      result.isLeft shouldBe true
      result.left.toOption.get.getMessage should include("Unknown difficulty: impossible")
    }
  }
