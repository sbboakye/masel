package com.sbboakye.masel.core.domain.dto

import com.sbboakye.masel.core.domain.{NonEmptyString, SubmissionId}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.circe.given

case class UpdateSubmissionRequest(
    id: SubmissionId,
    candidateSolution: NonEmptyString,
)

object UpdateSubmissionRequest:
  given Encoder[UpdateSubmissionRequest] = deriveEncoder[UpdateSubmissionRequest]
  given Decoder[UpdateSubmissionRequest] = deriveDecoder[UpdateSubmissionRequest]
