package com.sbboakye.masel.app.requests

import com.sbboakye.masel.core.domain.{
  ChallengeDifficulty,
  ChallengeStatus,
  NonEmptyString,
  PositiveInt,
  QuerySql,
  SetupSql,
  given,
}
import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.circe.given

case class CreateChallengeRequest(
    title: NonEmptyString,
    instructions: NonEmptyString,
    setupSql: SetupSql,
    expectedSolution: QuerySql,
    allottedTime: PositiveInt,
    difficulty: ChallengeDifficulty,
)

object CreateChallengeRequest:
  given Encoder[CreateChallengeRequest] = deriveEncoder[CreateChallengeRequest]
  given Decoder[CreateChallengeRequest] = deriveDecoder[CreateChallengeRequest]

case class UpdateChallengeRequest(
    title: Option[NonEmptyString],
    instructions: Option[NonEmptyString],
    setupSql: Option[SetupSql],
    status: Option[ChallengeStatus],
    expectedSolution: Option[QuerySql],
    output: Option[io.circe.Json],
    allottedTime: Option[PositiveInt],
    difficulty: Option[ChallengeDifficulty],
)

object UpdateChallengeRequest:
  given Encoder[UpdateChallengeRequest] = deriveEncoder[UpdateChallengeRequest]
  given Decoder[UpdateChallengeRequest] = deriveDecoder[UpdateChallengeRequest]
