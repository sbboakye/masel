package com.sbboakye.masel.app.endpoints

import com.sbboakye.masel.core.domain.{
  ChallengeDifficulty,
  ChallengeId,
  ChallengeStatus,
  NonEmptyString,
  PositiveInt,
  Score,
  SubmissionId,
}
import sttp.tapir.Schema

object TapirSchemas:

  // Opaque IDs -> represented as UUID Strings in API
  given Schema[ChallengeId] = Schema.string
  given Schema[SubmissionId] = Schema.string

  // Enums -> represented as strings in API
  given Schema[ChallengeStatus] = Schema.string
  given Schema[ChallengeDifficulty] = Schema.string

  // IronTypes
  given Schema[NonEmptyString] = Schema.string
  given Schema[PositiveInt] = Schema.schemaForInt.as[PositiveInt]
  given Schema[Score] = Schema.schemaForInt.as[Score]
