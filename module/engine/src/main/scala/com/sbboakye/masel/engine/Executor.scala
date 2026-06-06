package com.sbboakye.masel.engine

import cats.MonadThrow
import cats.effect.*
import cats.effect.std.Console
import cats.syntax.all.*
import com.sbboakye.masel.core.domain.{DatabaseConfig, QuerySql, SetupSql, SubmissionId}
import com.sbboakye.masel.core.errors.AppError
import com.sbboakye.masel.core.ports.SqlExecutor
import io.circe.Json
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.otel4s.metrics.Meter.Implicits.noop
import org.typelevel.otel4s.trace.Tracer.Implicits.noop
import skunk.*
import skunk.Session.Credentials
import skunk.data.Completion
import skunk.implicits.*

class Executor[F[_]: {Async, Console, LoggerFactory}](sandboxConfig: DatabaseConfig) extends SqlExecutor[F, Session[F]]:
  private val logger = LoggerFactory[F].getLogger

  override def sandbox(id: SubmissionId)(
      using F: MonadThrow[F],
  ): Resource[F, Session[F]] = {
    val schemaName: String = s"sub_${id.value.toString.replace("-", "")}"

    val createSchemaCommand: Command[Void] = sql"CREATE SCHEMA #$schemaName".command
    val searchPathCommand: Command[Void] = sql"SET search_path TO #$schemaName".command
    val dropSchemaCommand: Command[Void] = sql"DROP SCHEMA #$schemaName CASCADE".command

    def schemaDropped(c: Completion): F[Unit] = c match
      case Completion.DropSchema => F.unit
      case other =>
        AppError.InternalError(s"Unexpected completion type: $other").raiseError

    for
      session <- Session
        .Builder[F]
        .withHost(sandboxConfig.host)
        .withPort(sandboxConfig.port)
        .withDatabase(sandboxConfig.dbName)
        .withCredentials(
          Credentials(
            user = sandboxConfig.username,
            password = Some(sandboxConfig.password.show),
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
    yield session
  }

  override def execute(setup: SetupSql, query: QuerySql): F[Json] = ???
