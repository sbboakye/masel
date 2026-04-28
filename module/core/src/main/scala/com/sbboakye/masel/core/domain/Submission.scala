package com.sbboakye.masel.core.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import io.github.iltotore.iron.circe.given
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.constraint.numeric.*

import java.time.OffsetDateTime

case class Submission(
                     id: SubmissionId,
                     challengeId: ChallengeId,
                     candidateSolution: NonEmptyString,
                     output: Option[io.circe.Json],
                     score: Option[Score],
                     createdAt: OffsetDateTime,
                     updatedAt: OffsetDateTime
                     )

object Submission:
  given Encoder[Submission] = deriveEncoder[Submission]
  given Decoder[Submission] = deriveDecoder[Submission]
