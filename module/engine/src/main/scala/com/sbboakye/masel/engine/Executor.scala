package com.sbboakye.masel.engine

import cats.MonadThrow
import cats.effect.*
import cats.effect.std.Console
import cats.syntax.all.*
import com.sbboakye.masel.core.domain.{DatabaseConfig, QuerySql, SchemaScope, SetupSql}
import com.sbboakye.masel.core.errors.AppError
import com.sbboakye.masel.core.ports.SqlExecutor
import io.circe.Json
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.otel4s.metrics.Meter.Implicits.noop
import org.typelevel.otel4s.trace.Tracer.Implicits.noop
import skunk.*
import skunk.Session.Credentials
import skunk.circe.codec.all.jsonb
import skunk.data.Completion
import skunk.implicits.*

class Executor[F[_]: {Async, Console, LoggerFactory}](dbConfig: DatabaseConfig) extends SqlExecutor[F, Session[F]]:
  private val logger = LoggerFactory[F].getLogger

  override def sandbox[A: SchemaScope](id: A)(
      using F: MonadThrow[F],
  ): Resource[F, Session[F]] = {
    val schemaName: String = s"${summon[SchemaScope[A]].prefix}_${id.uuid.toString.replace("-", "")}"

    val createSchemaCommand: Command[Void] = sql"CREATE SCHEMA #$schemaName".command
    val searchPathCommand: Command[Void] = sql"SET search_path TO #$schemaName".command
    val timeoutCommand: Command[Void] = sql"SET statement_timeout = '10s'".command
    val dropSchemaCommand: Command[Void] = sql"DROP SCHEMA #$schemaName CASCADE".command

    def schemaDropped(c: Completion): F[Unit] = c match
      case Completion.DropSchema => F.unit
      case other =>
        AppError.InternalError(s"Unexpected completion type: $other").raiseError

    for
      session <- Session
        .Builder[F]
        .withHost(dbConfig.host)
        .withPort(dbConfig.port)
        .withDatabase(dbConfig.dbName)
        .withCredentials(
          Credentials(
            user = dbConfig.username,
            password = Some(dbConfig.password.show),
          ),
        )
        .withTypingStrategy(TypingStrategy.SearchPath)
        .single
      _ <- Resource.eval(logger.info("Sandbox connection established"))
      _ <- Resource.make(session.execute(createSchemaCommand))(_ =>
        session.execute(dropSchemaCommand).flatMap(schemaDropped),
      )
      _ <- Resource.eval(
        session.execute(searchPathCommand),
      )
      _ <- Resource.eval(session.execute(timeoutCommand))
    yield session
  }

  override def execute[A: SchemaScope](id: A, setup: SetupSql, query: QuerySql): F[Json] =
    sandbox(id).use { session =>
      val setupCommand: Command[Void] = sql"#${setup.value}".command
      val resultQuery: Query[Void, Json] =
        sql"SELECT coalesce(json_agg(t), '[]'::jsonb) FROM (#${query.value}) AS t".query(jsonb)
      for
        _ <- session.execute(setupCommand)
        result <- session.unique(resultQuery)
      yield result
    }
