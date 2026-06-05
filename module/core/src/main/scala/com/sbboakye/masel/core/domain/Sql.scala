package com.sbboakye.masel.core.domain

object Sql:
  opaque type Sql = String

  sealed trait ValidationError
  private case object MultipleStatementsNotAllowed extends ValidationError

  def from(value: String): Either[ValidationError, Sql] =
    if value.contains(";") then Left(MultipleStatementsNotAllowed)
    else Right(value)

  extension (sql: Sql) def value: String = sql
