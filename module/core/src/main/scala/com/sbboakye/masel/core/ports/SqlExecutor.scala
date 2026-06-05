package com.sbboakye.masel.core.ports

import com.sbboakye.masel.core.domain.SetupSql
import com.sbboakye.masel.core.domain.Sql.Sql
import com.sbboakye.masel.core.errors.AppError.ValidationFailed
import io.circe.Json

trait SqlExecutor[F[_]]:
  def execute(setup: SetupSql, query: Sql): F[Either[ValidationFailed, Json]]
