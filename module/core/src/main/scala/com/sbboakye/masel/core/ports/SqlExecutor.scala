package com.sbboakye.masel.core.ports

import cats.MonadThrow
import cats.effect.Resource
import com.sbboakye.masel.core.domain.{QuerySql, SchemaScope, SetupSql}
import io.circe.Json

trait SqlExecutor[F[_], B]:
  def sandbox[A: SchemaScope](id: A)(using F: MonadThrow[F]): Resource[F, B]
  def execute[A: SchemaScope](id: A, setup: SetupSql, query: QuerySql): F[Json]
