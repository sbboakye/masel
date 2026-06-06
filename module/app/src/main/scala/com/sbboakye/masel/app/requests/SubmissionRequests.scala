package com.sbboakye.masel.app.requests

import com.sbboakye.masel.core.domain.{ChallengeId, NonEmptyString, QuerySql, Score}
import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.circe.given

case class CreateSubmissionRequest(
    challengeId: ChallengeId,
    candidateSolution: QuerySql,
)

object CreateSubmissionRequest:
  given Encoder[CreateSubmissionRequest] = deriveEncoder[CreateSubmissionRequest]
  given Decoder[CreateSubmissionRequest] = deriveDecoder[CreateSubmissionRequest]

case class UpdateSubmissionRequest(
    candidateSolution: Option[QuerySql],
    output: Option[io.circe.Json],
    score: Option[Score],
)

object UpdateSubmissionRequest:
  given Encoder[UpdateSubmissionRequest] = deriveEncoder[UpdateSubmissionRequest]
  given Decoder[UpdateSubmissionRequest] = deriveDecoder[UpdateSubmissionRequest]
