package com.sbboakye.masel.core.ports

import cats.MonadThrow
import cats.effect.Resource
import com.sbboakye.masel.core.domain.{DatabaseConfig, QuerySql, SetupSql, SubmissionId}
import io.circe.Json

trait SqlExecutor[F[_], A]:
  def sandbox(id: SubmissionId)(using F: MonadThrow[F]): Resource[F, A]
  def execute(id: SubmissionId, setup: SetupSql, query: QuerySql): F[Json]
