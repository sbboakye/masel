package com.sbboakye.masel.core.domain

import cats.effect.testing.scalatest.AsyncIOSpec
import io.circe.Json
import io.circe.syntax.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

class JsonCodecTests extends AsyncFreeSpec with AsyncIOSpec with Matchers with CoreFixture:

  "Challenge JSON codec" - {
    "round-trips a fully populated challenge (output = None)" in
      challengeIO.map(c => c.asJson.as[Challenge] shouldBe Right(c))

    "round-trips a challenge that has an output JSON payload" in
      challengeIO.map { base =>
        val withOutput = base.copy(output = Some(Json.obj("rows" -> Json.fromInt(7))))
        withOutput.asJson.as[Challenge] shouldBe Right(withOutput)
      }

    "encodes status and difficulty as lowercase strings" in
      challengeIO.map { c =>
        val json = c.asJson
        json.hcursor.get[String]("status") shouldBe Right("draft")
        json.hcursor.get[String]("difficulty") shouldBe Right("easy")
      }
  }

  "Submission JSON codec" - {
    "round-trips a submission with no output and no score" in
      submissionIO.map(s => s.asJson.as[Submission] shouldBe Right(s))

    "round-trips a submission with a score" in
      submissionWithScoreIO.map(s => s.asJson.as[Submission] shouldBe Right(s))

    "round-trips a submission with an output JSON payload" in
      submissionIO.map { base =>
        val withOutput = base.copy(output = Some(Json.obj("col" -> Json.fromString("v"))))
        withOutput.asJson.as[Submission] shouldBe Right(withOutput)
      }

    "decodes score back as an Iron-refined value when the input is in range" in
      submissionWithScoreIO.map { s =>
        val decoded = s.asJson.as[Submission]
        decoded.toOption.flatMap(_.score) shouldBe s.score
      }
  }
