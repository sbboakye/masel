package com.sbboakye.masel.app

import cats.effect.*
import cats.effect.{ExitCode, IOApp}
import com.sbboakye.masel.persistence.FlywayMigrator
import com.sbboakye.masel.persistence.repositories.{SkunkChallengeRepository, SkunkSubmissionRepository}
import com.sbboakye.masel.persistence.session.PoolSession
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.{Router, Server}
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

object Main extends IOApp:

  given LoggerFactory[IO] = Slf4jFactory.create[IO]

  def run(args: List[String]): IO[ExitCode] =
    for {
      config <- AppConfig.loadF[IO]
      _ <- buildApp(config).useForever
    } yield ExitCode.Success

  private def buildApp(config: AppConfig): Resource[IO, Server] =
    for
      logger <- Resource.eval(LoggerFactory[IO].create)
      _ <- Resource.eval(
        FlywayMigrator[IO](
          jdbcUrl = config.database.jdbcUrl,
          username = config.database.username,
          password = config.database.password.value,
        ).migrate(),
      )
      _ <- Resource.eval(logger.info("Database migration completed"))
      session <- PoolSession[IO](
        host = config.database.host.toString,
        port = config.database.port.toString.toInt,
        database = config.database.dbName,
        username = config.database.username,
        password = config.database.password.value,
        maxPoolSize = config.database.maxPoolSize,
      ).poolSession
      _ <- Resource.eval(logger.info("Database connection established"))

      // Repositories to be used later in phase 2 with services
      challengesRepo = SkunkChallengeRepository[IO](session)
      submissionRepo = SkunkSubmissionRepository[IO](session)

      httpApp = Router(
        "/" -> HttpRoutes.empty[IO],
      ).orNotFound

      // Server
      _ <- Resource.eval(logger.info("Starting server"))
      emberServer <- EmberServerBuilder
        .default[IO]
        .withHost(config.server.host)
        .withPort(config.server.port)
        .withHttpApp(httpApp)
        .build
        .evalTap(server => logger.info(s"Server started on ${server.address}"))
    yield emberServer
