package com.sbboakye.masel.core.domain

import cats.*
import cats.effect.{Clock, IO}
import com.sbboakye.masel.core.domain.ChallengeDifficulty.Easy
import com.sbboakye.masel.core.domain.ChallengeStatus.Draft
import io.github.iltotore.iron.autoRefine
import io.github.iltotore.iron.constraint.numeric.{GreaterEqual, LessEqual}
import java.time.{OffsetDateTime, ZoneOffset}
import org.scalacheck.Gen

trait CoreFixture:

  val challengeIO: IO[Challenge] = for {
    id <- ChallengeId.generate[IO]
    now <- Clock[IO].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
    challenge = Challenge(
      id = id,
      title = "Sum of two squares",
      instructions = "Just sum them up",
      status = Draft,
      expectedSolution = "2 * 2 =",
      output = None,
      allottedTime = 900,
      difficulty = Easy,
      createdAt = now,
      updatedAt = now,
    )
  } yield challenge

  val submissionIO: IO[Submission] = for {
    submissionId <- SubmissionId.generate[IO]
    challengeId <- ChallengeId.generate[IO]
    now <- Clock[IO].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
    submission = Submission(
      id = submissionId,
      challengeId = challengeId,
      candidateSolution = "x + x = y",
      output = None,
      score = None,
      createdAt = now,
      updatedAt = now,
    )
  } yield submission

  val submissionWithScoreIO: IO[Submission] = for {
    submissionId <- SubmissionId.generate[IO]
    challengeId <- ChallengeId.generate[IO]
    submission = Submission(
      id = submissionId,
      challengeId = challengeId,
      candidateSolution = "x + x = y",
      output = None,
      score = Some(90),
      createdAt = OffsetDateTime.now(),
      updatedAt = OffsetDateTime.now(),
    )
  } yield submission

  type ScoreConstraint = GreaterEqual[0] & LessEqual[100]

  val validScores: Gen[Int] = Gen.chooseNum(0, 100)

  val invalidScores: Gen[Int] = Gen.oneOf(
    Gen.choose(Int.MinValue, -1),
    Gen.choose(101, Int.MaxValue),
  )

  val positiveIntegers: Gen[Int] = Gen.posNum[Int]
  val negativeIntegers: Gen[Int] = Gen.negNum[Int]

  val nonEmptyStrings: Gen[String] = Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString)
  val emptyStrings: Gen[String] = Gen.const("")

  val validMinLengthStrings: Gen[String] = Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString).suchThat(_.length >= 8)
  val invalidMinLengthStrings: Gen[String] = Gen.choose(1, 7).flatMap(n => Gen.stringOfN(n, Gen.alphaChar))
