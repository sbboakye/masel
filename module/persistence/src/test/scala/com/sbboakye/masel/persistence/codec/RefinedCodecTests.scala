package com.sbboakye.masel.persistence.codec

import com.sbboakye.masel.core.domain.{NonEmptyString, PositiveInt, Score}
import io.github.iltotore.iron.autoRefine
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import skunk.Codec
import skunk.codec.all.{int4, varchar}
import skunk.data.Encoded

/**
 * Direct unit tests for the `refined[T]` Skunk codec adapter.
 *
 * These tests exercise the codec at the wire-format boundary, without going through Postgres. They lock in: the encode
 * is a passthrough to the base codec, the decode round-trips valid values, and the decode fails for any value that
 * doesn't satisfy the Iron constraint.
 */
class RefinedCodecTests extends AnyFreeSpec with Matchers:

  /** Skunk's encode returns wrapped [[Encoded]] strings; decode wants raw strings. */
  extension (encoded: List[Option[Encoded]]) private def asStrings: List[Option[String]] = encoded.map(_.map(_.value))

  "refined[NonEmptyString](varchar)" - {
    val codec: Codec[NonEmptyString] = refined[NonEmptyString](varchar)

    "encodes a non-empty string as the base wire value" in {
      val input: NonEmptyString = "hello"
      codec.encode(input).asStrings shouldBe List(Some("hello"))
    }

    "round-trips a non-empty string" in {
      val input: NonEmptyString = "hello"
      codec.decode(0, codec.encode(input).asStrings) shouldBe Right(input)
    }

    "fails to decode an empty string (violates Not[Empty])" in {
      codec.decode(0, List(Some(""))).isLeft shouldBe true
    }
  }

  "refined[PositiveInt](int4)" - {
    val codec: Codec[PositiveInt] = refined[PositiveInt](int4)

    "encodes and round-trips a positive integer" in {
      val input: PositiveInt = 42
      codec.encode(input).asStrings shouldBe List(Some("42"))
      codec.decode(0, codec.encode(input).asStrings) shouldBe Right(input)
    }

    "fails to decode zero (Positive requires > 0)" in {
      codec.decode(0, List(Some("0"))).isLeft shouldBe true
    }

    "fails to decode a negative integer" in {
      codec.decode(0, List(Some("-5"))).isLeft shouldBe true
    }
  }

  "refined[Score](int4)" - {
    val codec: Codec[Score] = refined[Score](int4)

    "round-trips the lower bound (0)" in {
      val input: Score = 0
      codec.decode(0, codec.encode(input).asStrings) shouldBe Right(input)
    }

    "round-trips the upper bound (100)" in {
      val input: Score = 100
      codec.decode(0, codec.encode(input).asStrings) shouldBe Right(input)
    }

    "round-trips a mid-range value" in {
      val input: Score = 50
      codec.decode(0, codec.encode(input).asStrings) shouldBe Right(input)
    }

    "fails to decode a value below the lower bound" in {
      codec.decode(0, List(Some("-1"))).isLeft shouldBe true
    }

    "fails to decode a value above the upper bound" in {
      codec.decode(0, List(Some("101"))).isLeft shouldBe true
    }
  }
