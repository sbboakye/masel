package com.sbboakye.masel.persistence.codec

import com.sbboakye.masel.core.domain.{
  ChallengeDifficulty,
  ChallengeId,
  ChallengeStatus,
  NonEmptyString,
  PositiveInt,
  Score,
  SubmissionId,
}
import io.github.iltotore.iron.autoRefine
import java.util.UUID
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import skunk.data.Encoded

/** Direct unit tests for the named codecs in [[SkunkCodec]]. */
class SkunkCodecTests extends AnyFreeSpec with Matchers:

  extension (encoded: List[Option[Encoded]]) private def asStrings: List[Option[String]] = encoded.map(_.map(_.value))

  "challengeId codec" - {
    "round-trips a UUID via encode/decode" in {
      val id = ChallengeId(UUID.randomUUID())
      val encoded = SkunkCodec.challengeId.encode(id).asStrings
      encoded shouldBe List(Some(id.value.toString))
      SkunkCodec.challengeId.decode(0, encoded) shouldBe Right(id)
    }

    "fails to decode a string that is not a valid UUID" in {
      SkunkCodec.challengeId.decode(0, List(Some("not-a-uuid"))).isLeft shouldBe true
    }
  }

  "submissionId codec" - {
    "round-trips a UUID via encode/decode" in {
      val id = SubmissionId(UUID.randomUUID())
      val encoded = SkunkCodec.submissionId.encode(id).asStrings
      encoded shouldBe List(Some(id.value.toString))
      SkunkCodec.submissionId.decode(0, encoded) shouldBe Right(id)
    }
  }

  "challengeStatus enum codec" - {
    "encodes each value as a lowercase string" in {
      SkunkCodec.challengeStatus.encode(ChallengeStatus.Draft).asStrings shouldBe List(Some("draft"))
      SkunkCodec.challengeStatus.encode(ChallengeStatus.Validated).asStrings shouldBe List(Some("validated"))
      SkunkCodec.challengeStatus.encode(ChallengeStatus.Active).asStrings shouldBe List(Some("active"))
      SkunkCodec.challengeStatus.encode(ChallengeStatus.Archived).asStrings shouldBe List(Some("archived"))
    }

    "decodes every value, case-insensitively (via fromString)" in {
      SkunkCodec.challengeStatus.decode(0, List(Some("draft"))) shouldBe Right(ChallengeStatus.Draft)
      SkunkCodec.challengeStatus.decode(0, List(Some("DRAFT"))) shouldBe Right(ChallengeStatus.Draft)
      SkunkCodec.challengeStatus.decode(0, List(Some("ArChIvEd"))) shouldBe Right(ChallengeStatus.Archived)
    }

    "round-trips every enum value" in
      ChallengeStatus.values.foreach { s =>
        SkunkCodec.challengeStatus.decode(0, SkunkCodec.challengeStatus.encode(s).asStrings) shouldBe Right(s)
      }

    "fails to decode an unknown enum literal" in {
      SkunkCodec.challengeStatus.decode(0, List(Some("retired"))).isLeft shouldBe true
    }
  }

  "challengeDifficulty enum codec" - {
    "encodes each value as a lowercase string" in {
      SkunkCodec.challengeDifficulty.encode(ChallengeDifficulty.Easy).asStrings shouldBe List(Some("easy"))
      SkunkCodec.challengeDifficulty.encode(ChallengeDifficulty.Medium).asStrings shouldBe List(Some("medium"))
      SkunkCodec.challengeDifficulty.encode(ChallengeDifficulty.Hard).asStrings shouldBe List(Some("hard"))
    }

    "decodes case-insensitively via fromString" in {
      SkunkCodec.challengeDifficulty.decode(0, List(Some("EASY"))) shouldBe Right(ChallengeDifficulty.Easy)
      SkunkCodec.challengeDifficulty.decode(0, List(Some("medium"))) shouldBe Right(ChallengeDifficulty.Medium)
    }

    "round-trips every enum value" in
      ChallengeDifficulty.values.foreach { d =>
        SkunkCodec.challengeDifficulty.decode(0, SkunkCodec.challengeDifficulty.encode(d).asStrings) shouldBe Right(d)
      }

    "fails to decode an unknown enum literal" in {
      SkunkCodec.challengeDifficulty.decode(0, List(Some("epic"))).isLeft shouldBe true
    }
  }

  "domainVarchar codec (NonEmptyString on varchar) is wired up" in {
    val input: NonEmptyString = "hello"
    SkunkCodec.domainVarchar.decode(0, SkunkCodec.domainVarchar.encode(input).asStrings) shouldBe Right(input)
    SkunkCodec.domainVarchar.decode(0, List(Some(""))).isLeft shouldBe true
  }

  "domainText codec (NonEmptyString on text) is wired up" in {
    val input: NonEmptyString = "instructions go here"
    SkunkCodec.domainText.decode(0, SkunkCodec.domainText.encode(input).asStrings) shouldBe Right(input)
    SkunkCodec.domainText.decode(0, List(Some(""))).isLeft shouldBe true
  }

  "domainPositiveInt codec (PositiveInt on int4) is wired up" in {
    val input: PositiveInt = 900
    SkunkCodec.domainPositiveInt.decode(0, SkunkCodec.domainPositiveInt.encode(input).asStrings) shouldBe Right(input)
    SkunkCodec.domainPositiveInt.decode(0, List(Some("0"))).isLeft shouldBe true
    SkunkCodec.domainPositiveInt.decode(0, List(Some("-1"))).isLeft shouldBe true
  }

  "submissionScore codec (Score on int4) is wired up" in {
    val input: Score = 73
    SkunkCodec.submissionScore.decode(0, SkunkCodec.submissionScore.encode(input).asStrings) shouldBe Right(input)
    SkunkCodec.submissionScore.decode(0, List(Some("101"))).isLeft shouldBe true
    SkunkCodec.submissionScore.decode(0, List(Some("-1"))).isLeft shouldBe true
  }
