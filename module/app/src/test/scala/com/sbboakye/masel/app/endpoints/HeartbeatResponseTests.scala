package com.sbboakye.masel.app.endpoints

import io.circe.parser.parse
import io.circe.syntax.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class HeartbeatResponseTests extends AnyFreeSpec with Matchers:

  "HealthResponse" - {
    "encodes to JSON with the supplied status" in {
      HealthResponse("ok").asJson.noSpaces shouldBe """{"status":"ok"}"""
    }

    "decodes from a JSON string with a status field" in {
      val json = """{"status":"degraded"}"""
      parse(json).flatMap(_.as[HealthResponse]) shouldBe Right(HealthResponse("degraded"))
    }

    "fails to decode when the status field is missing" in {
      val json = """{}"""
      parse(json).flatMap(_.as[HealthResponse]).isLeft shouldBe true
    }
  }

  "ReadinessResponse" - {
    "encodes to JSON with the supplied status" in {
      ReadinessResponse("not ready").asJson.noSpaces shouldBe """{"status":"not ready"}"""
    }

    "round-trips through encode/decode" in {
      val original = ReadinessResponse("ok")
      val roundTripped = parse(original.asJson.noSpaces).flatMap(_.as[ReadinessResponse])
      roundTripped shouldBe Right(original)
    }
  }
