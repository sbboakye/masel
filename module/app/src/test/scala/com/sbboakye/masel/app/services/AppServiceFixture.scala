package com.sbboakye.masel.app.services

import cats.effect.{Clock, IO}
import com.sbboakye.masel.app.requests.{
  CreateChallengeRequest,
  CreateSubmissionRequest,
  UpdateChallengeRequest,
  UpdateSubmissionRequest,
}
import com.sbboakye.masel.core.domain.ChallengeDifficulty.{Easy, Hard, Medium}
import com.sbboakye.masel.core.domain.ChallengeStatus.{Active, Draft, Validated}
import com.sbboakye.masel.core.domain.{
  Challenge,
  ChallengeId,
  ChallengeStatus,
  NonEmptyString,
  Submission,
  SubmissionId,
}
import io.circe.Json
import io.github.iltotore.iron.autoRefine
import java.time.ZoneOffset
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

trait AppServiceFixture:

  given LoggerFactory[IO] = Slf4jFactory.create[IO]

  val createChallengeRequest: CreateChallengeRequest = CreateChallengeRequest(
    title = "Sum of two squares",
    instructions = "Sum two squares using SQL",
    expectedSolution = "SELECT 1 + 4",
    allottedTime = 900,
    difficulty = Easy,
  )

  val emptyUpdateChallengeRequest: UpdateChallengeRequest = UpdateChallengeRequest(
    title = None,
    instructions = None,
    status = None,
    expectedSolution = None,
    output = None,
    allottedTime = None,
    difficulty = None,
  )

  val fullUpdateChallengeRequest: UpdateChallengeRequest = UpdateChallengeRequest(
    title = Some("Updated title"),
    instructions = Some("Updated instructions"),
    status = Some(Active),
    expectedSolution = Some("SELECT 42"),
    output = Some(Json.obj("col" -> Json.fromString("v"))),
    allottedTime = Some(1200),
    difficulty = Some(Hard),
  )

  def seededChallenge(
      title: NonEmptyString = "Seeded challenge",
      status: ChallengeStatus = Draft,
  ): IO[Challenge] =
    for {
      id <- ChallengeId.generate[IO]
      now <- Clock[IO].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
    } yield Challenge(
      id = id,
      title = title,
      instructions = "Some instructions",
      status = status,
      expectedSolution = "SELECT 1",
      output = None,
      allottedTime = 600,
      difficulty = Medium,
      createdAt = now,
      updatedAt = now,
    )

  def createSubmissionRequestFor(challengeId: ChallengeId): CreateSubmissionRequest =
    CreateSubmissionRequest(
      challengeId = challengeId,
      candidateSolution = "SELECT 1 + 4",
    )

  val emptyUpdateSubmissionRequest: UpdateSubmissionRequest = UpdateSubmissionRequest(
    candidateSolution = None,
    output = None,
    score = None,
  )

  val fullUpdateSubmissionRequest: UpdateSubmissionRequest = UpdateSubmissionRequest(
    candidateSolution = Some("SELECT 42 AS answer"),
    output = Some(Json.obj("answer" -> Json.fromInt(42))),
    score = Some(85),
  )

  def seededSubmission(
      challengeId: ChallengeId,
      withScore: Boolean = false,
  ): IO[Submission] =
    for {
      id <- SubmissionId.generate[IO]
      now <- Clock[IO].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
    } yield Submission(
      id = id,
      challengeId = challengeId,
      candidateSolution = "SELECT 1",
      output = None,
      score = if withScore then Some(50) else None,
      createdAt = now,
      updatedAt = now,
    )

  // Convenience so tests can refer to Validated without importing inside each spec
  val validatedStatus: ChallengeStatus = Validated
