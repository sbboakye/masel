package com.sbboakye.masel.core.ports

import com.sbboakye.masel.core.domain.QuerySql.QuerySql
import com.sbboakye.masel.core.domain.SetupSql.SetupSql
import com.sbboakye.masel.core.errors.AppError.ValidationFailed
import io.circe.Json

trait SqlExecutor[F[_]]:
  def execute(setup: SetupSql, query: QuerySql): F[Either[ValidationFailed, Json]]
