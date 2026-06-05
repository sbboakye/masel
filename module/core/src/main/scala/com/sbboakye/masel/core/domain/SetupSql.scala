package com.sbboakye.masel.core.domain

import cats.data.NonEmptyList
import io.circe.Json

sealed trait SetupStep

opaque type TableName = String
opaque type ColumnName = String

enum SqlType:
  case Integer, BigInt, Text, Boolean, Timestamp, Json

case class Column(name: ColumnName, dataType: SqlType, nullable: Boolean = true, primaryKey: Boolean = false)

object SetupStep:
  final case class CreateTable(tableName: TableName, columns: NonEmptyList[Column]) extends SetupStep
  final case class Insert(tableName: TableName, values: List[Map[ColumnName, Json]]) extends SetupStep

final case class SetupSql(steps: NonEmptyList[SetupStep])
