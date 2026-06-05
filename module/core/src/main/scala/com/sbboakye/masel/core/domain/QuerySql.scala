package com.sbboakye.masel.core.domain

import com.sbboakye.masel.core.errors.AppError
import com.sbboakye.masel.core.errors.AppError.ValidationFailed
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.circe.given

object QuerySql:
  opaque type QuerySql = NonEmptyString

  given Encoder[QuerySql] = Encoder[NonEmptyString]
  given Decoder[QuerySql] = Decoder[NonEmptyString]

  def from(value: NonEmptyString): Either[AppError, QuerySql] =
    if value.contains(";") then Left(ValidationFailed(List("Query SQL cannot contain semicolons")))
    else Right(value)

  extension (sql: QuerySql) def value: String = sql
