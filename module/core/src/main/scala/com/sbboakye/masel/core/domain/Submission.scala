package com.sbboakye.masel.core.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*

import java.time.OffsetDateTime

case class Submission(
                     id: SubmissionId,
                     challengeId: ChallengeId,
                     candidateSolution: String,
                     output: Option[io.circe.Json],
                     score: Option[Int],
                     createdAt: OffsetDateTime,
                     updatedAt: OffsetDateTime
                     )

object Submission:
  given Encoder[Submission] = deriveEncoder[Submission]
  given Decoder[Submission] = deriveDecoder[Submission]

case class SubmissionUpdate(
                       candidateSolution: String,
                       id: SubmissionId
                     )

object SubmissionUpdate:
  given Encoder[SubmissionUpdate] = deriveEncoder[SubmissionUpdate]
  given Decoder[SubmissionUpdate] = deriveDecoder[SubmissionUpdate]