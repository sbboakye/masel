package com.sbboakye.masel.core.domain.dto

import com.sbboakye.masel.core.domain.{ChallengeDifficulty, ChallengeId, ChallengeStatus, NonEmptyString, PositiveInt}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.circe.given

case class UpdateChallengeRequest(
    id: ChallengeId,
    title: NonEmptyString,
    instructions: NonEmptyString,
    status: ChallengeStatus,
    expectedSolution: NonEmptyString,
    allottedTime: PositiveInt,
    difficulty: ChallengeDifficulty,
)

object UpdateChallengeRequest:
  given Encoder[UpdateChallengeRequest] = deriveEncoder[UpdateChallengeRequest]
  given Decoder[UpdateChallengeRequest] = deriveDecoder[UpdateChallengeRequest]
