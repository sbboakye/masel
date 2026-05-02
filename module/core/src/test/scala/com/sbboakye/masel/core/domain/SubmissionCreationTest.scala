package com.sbboakye.masel.core.domain

import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

class SubmissionCreationTest extends AsyncFreeSpec with AsyncIOSpec with Matchers with CoreFixture:
  "Submission creation logic" - {
    "create" - {
      "should create a valid submission" in
        submissionIO.map { submission =>
          submission.id shouldBe a[SubmissionId]
          submission.challengeId shouldBe a[ChallengeId]
          submission.id shouldNot be(submission.challengeId)
          submission.output shouldBe None
          submission.score shouldBe None
        }

      "should create a submission with valid score" in
        submissionWithScoreIO.map(submission => submission.score.get >= 0 && submission.score.get <= 100 shouldBe true)
    }
  }
