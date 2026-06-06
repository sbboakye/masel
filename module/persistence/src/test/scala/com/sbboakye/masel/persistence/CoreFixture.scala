package com.sbboakye.masel.persistence

import cats.*
import cats.effect.{Clock, IO}
import com.sbboakye.masel.core.domain.ChallengeDifficulty.Easy
import com.sbboakye.masel.core.domain.ChallengeStatus.Draft
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId, QuerySql, SetupSql, Submission, SubmissionId}
import io.github.iltotore.iron.autoRefine
import java.time.ZoneOffset

trait CoreFixture:

  val challengeIOOne: IO[Challenge] = for {
    id <- ChallengeId.generate[IO]
    now <- Clock[IO].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
    challenge = Challenge(
      id = id,
      title = "Sum of two squares",
      instructions = "Just sum them up",
      setupSql = SetupSql("CREATE TABLE t (n INT);"),
      status = Draft,
      expectedSolution = QuerySql("2 * 2 ="),
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
      setupSql = SetupSql("CREATE TABLE t (n INT);"),
      status = Draft,
      expectedSolution = QuerySql("2 * 2 ="),
      output = None,
      allottedTime = 900,
      difficulty = Easy,
      createdAt = now,
      updatedAt = now,
    )
  } yield challenge

  def submissionIO(challengeId: ChallengeId): IO[Submission] = for {
    submissionId <- SubmissionId.generate[IO]
    now <- Clock[IO].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
    submission = Submission(
      id = submissionId,
      challengeId = challengeId,
      candidateSolution = QuerySql("x + x = y"),
      output = None,
      score = None,
      createdAt = now,
      updatedAt = now,
    )
  } yield submission

  def submissionWithScoreIO(challengeId: ChallengeId): IO[Submission] = for {
    submissionId <- SubmissionId.generate[IO]
    now <- Clock[IO].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
    submission = Submission(
      id = submissionId,
      challengeId = challengeId,
      candidateSolution = QuerySql("x + x = y"),
      output = None,
      score = Some(90),
      createdAt = now,
      updatedAt = now,
    )
  } yield submission
