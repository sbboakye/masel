package com.sbboakye.masel.app

import cats.effect.*
import cats.effect.{ExitCode, IOApp}
import com.sbboakye.masel.persistence.FlywayMigrator
import com.sbboakye.masel.persistence.repositories.{SkunkChallengeRepository, SkunkSubmissionRepository}
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.LoggerFactory
import skunk.*
import skunk.Session.Credentials
import org.typelevel.otel4s.metrics.Meter.Implicits.noop
import org.typelevel.otel4s.trace.Tracer.Implicits.noop

object Main extends IOApp:

  given LoggerFactory[IO] = Slf4jFactory.create[IO]

  def run(args: List[String]): IO[ExitCode] = {
    for {
      config <- AppConfig.loadF[IO]
      _ <- buildApp(config).useForever
    } yield ExitCode.Success
  }

  private def buildApp(config: AppConfig): Resource[IO, Unit] =
    for
      logger <- Resource.eval(LoggerFactory[IO].create)
      _ <- Resource.eval(
        FlywayMigrator[IO](
          jdbcUrl = config.database.jdbcUrl,
          username = config.database.username,
          password = config.database.password.value
        ).migrate()
      )
      _ <- Resource.eval(logger.info("Database migration completed"))
      session <- Session.Builder[IO]
        .withHost(config.database.host)
        .withPort(config.database.port)
        .withDatabase(config.database.dbName)
        .withCredentials(
          Credentials(
            user = config.database.username,
            password = Some(config.database.password.value)
          )
        )
        .pooled(max = config.database.maxPoolSize)
      _ <- Resource.eval(logger.info("Database connection established"))

      // Repositories
      challengesRepo = SkunkChallengeRepository[IO](session)
      submissionRepo = SkunkSubmissionRepository[IO](session)

      // Server
      _ <- Resource.eval(logger.info("Starting server"))
    yield ()
