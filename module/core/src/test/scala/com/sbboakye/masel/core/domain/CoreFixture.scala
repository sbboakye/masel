package com.sbboakye.masel.core.domain

import cats.*
import cats.effect.IO
import cats.effect.syntax.all.*
import cats.syntax.all.*
import com.sbboakye.masel.core.domain.ChallengeDifficulty.Easy
import com.sbboakye.masel.core.domain.ChallengeStatus.Draft
import io.github.iltotore.iron.autoRefine
import java.time.OffsetDateTime

trait CoreFixture:

  val challengeIO: IO[Challenge] = for {
    id <- ChallengeId.generate[IO]
    challenge = Challenge(
      id = id,
      title = "Sum of two squares",
      instructions = "Just sum them up",
      status = Draft,
      expectedSolution = "2 * 2 =",
      output = None,
      allottedTime = 900,
      difficulty = Easy,
      createdAt = OffsetDateTime.now,
      updatedAt = OffsetDateTime.now,
    )
  } yield challenge

  val submissionIO: IO[Submission] = for {
    submissionId <- SubmissionId.generate[IO]
    challengeId <- ChallengeId.generate[IO]
    submission = Submission(
      id = submissionId,
      challengeId = challengeId,
      candidateSolution = "x + x = y",
      output = None,
      score = None,
      createdAt = OffsetDateTime.now(),
      updatedAt = OffsetDateTime.now(),
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
