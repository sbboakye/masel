package com.sbboakye.masel.persistence

import cats.*
import cats.effect.{Clock, IO}
import com.sbboakye.masel.core.domain.ChallengeDifficulty.Easy
import com.sbboakye.masel.core.domain.ChallengeStatus.Draft
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId, Submission, SubmissionId}
import io.github.iltotore.iron.autoRefine
import java.time.{OffsetDateTime, ZoneOffset}

trait CoreFixture:

  val challengeIOOne: IO[Challenge] = for {
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

  val challengeIOTwo: IO[Challenge] = for {
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
