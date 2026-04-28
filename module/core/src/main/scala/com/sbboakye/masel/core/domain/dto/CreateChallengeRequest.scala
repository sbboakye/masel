package com.sbboakye.masel.core.domain.dto

import com.sbboakye.masel.core.domain.{ChallengeDifficulty, NonEmptyString, PositiveInt}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
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
