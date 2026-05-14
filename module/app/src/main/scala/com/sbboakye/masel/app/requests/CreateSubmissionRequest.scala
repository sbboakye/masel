package com.sbboakye.masel.app.requests

import com.sbboakye.masel.core.domain.{ChallengeId, NonEmptyString, Score}
import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.circe.given

case class CreateSubmissionRequest(
    challengeId: ChallengeId,
    candidateSolution: NonEmptyString,
)

object CreateSubmissionRequest:
  given Encoder[CreateSubmissionRequest] = deriveEncoder[CreateSubmissionRequest]
  given Decoder[CreateSubmissionRequest] = deriveDecoder[CreateSubmissionRequest]
