package com.sbboakye.masel.core.domain

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.{Empty, MinLength}
import io.github.iltotore.iron.constraint.numeric.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.*

class CustomIronTypeTests extends AnyPropSpec with Matchers with CoreFixture:

  property("valid score always refine") {
    forAll(validScores) { n =>
      val result = n.refineEither[ScoreConstraint]
      result shouldBe Right(n)
    }
  }

  property("invalid scores never refine") {
    forAll(invalidScores) { n =>
      val result = n.refineEither[ScoreConstraint]
      result.isLeft shouldBe true
    }
  }

  property("valid positive integer") {
    forAll(positiveIntegers) { n =>
      val result = n.refineEither[Positive]
      result shouldBe Right(n)
    }
  }

  property("invalid negative integer") {
    forAll(negativeIntegers) { n =>
      val result = n.refineEither[Positive]
      result.isLeft shouldBe true
    }
  }

  property("valid non empty string") {
    forAll(nonEmptyStrings) { s =>
      val result = s.refineEither[Not[Empty]]
      result shouldBe Right(s)
    }
  }

  property("invalid empty strings") {
    forAll(emptyStrings) { s =>
      val result = s.refineEither[Not[Empty]]
      result.isLeft shouldBe true
    }
  }

  property("valid minimum length string") {
    forAll(validMinLengthStrings) { s =>
      val result = s.refineEither[MinLength[8]]
      result shouldBe Right(s)
    }
  }

  property("invalid minimum length string") {
    forAll(invalidMinLengthStrings) { s =>
      val result = s.refineEither[MinLength[8]]
      result.isLeft shouldBe true
    }
  }
