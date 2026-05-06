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
