package com.sbboakye.masel.core.domain

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class ChallengeCreationTest extends AnyFreeSpec with Matchers with CoreFixture:
  "Challenge creation logic" - {
    "create" - {
      "should create a valid challenge" in
        challengeIO.map { challenge =>
          challenge.id shouldBe a[ChallengeId]
          challenge.output shouldBe None
          challenge.status shouldBe ChallengeStatus.Draft
          challenge.difficulty shouldBe ChallengeDifficulty.Easy
          challenge.allottedTime >= 0
        }
    }
  }
