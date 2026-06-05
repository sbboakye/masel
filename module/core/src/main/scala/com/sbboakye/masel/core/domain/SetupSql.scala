package com.sbboakye.masel.core.domain

import com.sbboakye.masel.core.errors.AppError
import com.sbboakye.masel.core.errors.AppError.ValidationFailed
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.circe.given

type SetupSql = NonEmptyString

object SetupSql:

  given Encoder[SetupSql] = Encoder[NonEmptyString]
  given Decoder[SetupSql] = Decoder[NonEmptyString]

  def from(value: NonEmptyString): Either[AppError, SetupSql] =
    if value.contains(";") then Right(value)
    else Left(ValidationFailed(List("Setup SQL must contain semicolons")))

  extension (sql: SetupSql) def value: String = sql
