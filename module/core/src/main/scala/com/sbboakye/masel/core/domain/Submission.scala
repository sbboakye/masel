package com.sbboakye.masel.core.domain

import com.sbboakye.masel.core.domain.QuerySql
import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.circe.given
import java.time.OffsetDateTime

case class Submission(
    id: SubmissionId,
    challengeId: ChallengeId,
    candidateSolution: QuerySql,
    output: Option[io.circe.Json],
    score: Option[Score],
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime,
)

object Submission:
  given Encoder[Submission] = deriveEncoder[Submission]
  given Decoder[Submission] = deriveDecoder[Submission]
