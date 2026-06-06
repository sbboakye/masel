package com.sbboakye.masel.core.ports

import cats.MonadThrow
import cats.effect.Resource
import com.sbboakye.masel.core.domain.{DatabaseConfig, QuerySql, SetupSql, SubmissionId}
import io.circe.Json

trait SqlExecutor[F[_], A]:
  def execute(setup: SetupSql, query: QuerySql): F[Json]
  def sandbox(id: SubmissionId)(using F: MonadThrow[F]): Resource[F, A]
