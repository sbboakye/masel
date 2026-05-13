package com.sbboakye.masel.app.requests

import com.sbboakye.masel.core.domain.{ChallengeId, NonEmptyString, PositiveInt, Score, given}
import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}

case class CreateSubmissionRequest(
    challengeId: ChallengeId,
    candidateSolution: NonEmptyString,
)

object CreateSubmissionRequest:
  given Encoder[CreateSubmissionRequest] = deriveEncoder[CreateSubmissionRequest]
  given Decoder[CreateSubmissionRequest] = deriveDecoder[CreateSubmissionRequest]

case class UpdateSubmissionRequest(
    candidateSolution: Option[NonEmptyString],
    output: Option[io.circe.Json],
    score: Option[Score],
)

object UpdateSubmissionRequest:
  given Encoder[UpdateSubmissionRequest] = deriveEncoder[UpdateSubmissionRequest]
  given Decoder[UpdateSubmissionRequest] = deriveDecoder[UpdateSubmissionRequest]
