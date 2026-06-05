package com.sbboakye.masel.core.ports

import com.sbboakye.masel.core.domain.QuerySql
import com.sbboakye.masel.core.domain.SetupSql
import io.circe.Json

trait SqlExecutor[F[_]]:
  def execute(setup: SetupSql, query: QuerySql): F[Json]
