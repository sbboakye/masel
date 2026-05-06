package com.sbboakye.masel.app.requests

import com.sbboakye.masel.core.domain.{ChallengeDifficulty, NonEmptyString, PositiveInt}
import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.*
import io.github.iltotore.iron.circe.given
import io.github.iltotore.iron.constraint.*
import io.github.iltotore.iron.constraint.all.*

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
    expectedSolution: Option[NonEmptyString],
    allottedTime: Option[PositiveInt],
    difficulty: Option[ChallengeDifficulty],
    updatedAt: Option[java.time.OffsetDateTime],
)

object UpdateChallengeRequest:
  given Encoder[UpdateChallengeRequest] = deriveEncoder[UpdateChallengeRequest]
  given Decoder[UpdateChallengeRequest] = deriveDecoder[UpdateChallengeRequest]
