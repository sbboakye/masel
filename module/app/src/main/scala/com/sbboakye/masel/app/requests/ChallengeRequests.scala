package com.sbboakye.masel.app.requests

import com.sbboakye.masel.core.domain.{ChallengeDifficulty, ChallengeStatus, NonEmptyString, PositiveInt, given}
import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.circe.given

case class CreateChallengeRequest(
    title: NonEmptyString,
    instructions: NonEmptyString,
    expectedSolution: NonEmptyString,
    allottedTime: PositiveInt,
    difficulty: ChallengeDifficulty,
)

object CreateChallengeRequest:
  given Encoder[CreateChallengeRequest] = deriveEncoder[CreateChallengeRequest]
  given Decoder[CreateChallengeRequest] = deriveDecoder[CreateChallengeRequest]

case class UpdateChallengeRequest(
    title: Option[NonEmptyString],
    instructions: Option[NonEmptyString],
    status: Option[ChallengeStatus],
    expectedSolution: Option[NonEmptyString],
    output: Option[io.circe.Json],
    allottedTime: Option[PositiveInt],
    difficulty: Option[ChallengeDifficulty],
)

object UpdateChallengeRequest:
  given Encoder[UpdateChallengeRequest] = deriveEncoder[UpdateChallengeRequest]
  given Decoder[UpdateChallengeRequest] = deriveDecoder[UpdateChallengeRequest]
