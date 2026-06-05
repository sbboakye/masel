package com.sbboakye.masel.core.domain

import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.circe.given

object SetupSql:
  opaque type SetupSql = NonEmptyString

  given Encoder[SetupSql] = Encoder[NonEmptyString]
  given Decoder[SetupSql] = Decoder[NonEmptyString]

  extension (sql: SetupSql) def value: String = sql
