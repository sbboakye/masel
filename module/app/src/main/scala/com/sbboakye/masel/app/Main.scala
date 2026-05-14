package com.sbboakye.masel.app

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.all.*
import com.sbboakye.masel.app.endpoints.{HealthResponse, ReadinessResponse}
import com.sbboakye.masel.app.routes.{ChallengeRoutes, SubmissionRoutes}
import com.sbboakye.masel.app.services.{ChallengeService, SubmissionService}
import com.sbboakye.masel.persistence.FlywayMigrator
import com.sbboakye.masel.persistence.repositories.SkunkAppDb
import com.sbboakye.masel.persistence.session.PoolSession
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.{Router, Server}
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

object Main extends IOApp:

  given LoggerFactory[IO] = Slf4jFactory.create[IO]

  def run(args: List[String]): IO[ExitCode] =
    for
      config <- AppConfig.loadF[IO]
      _ <- buildApp(config).useForever
    yield ExitCode.Success

  private def heartbeatRoutes(appDb: com.sbboakye.masel.core.ports.AppDb[IO]): HttpRoutes[IO] =
    val dsl = new Http4sDsl[IO] {}
    import dsl.*
    HttpRoutes.of[IO] {
      case GET -> Root / "health" =>
        Ok(HealthResponse("ok"))
      case GET -> Root / "ready" =>
        appDb.isReady.flatMap {
          case true => Ok(ReadinessResponse("ok"))
          case false => ServiceUnavailable(ReadinessResponse("not ready"))
        }
    }

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

      appPool <- PoolSession.make[IO](
        host = config.database.host.show,
        port = config.database.port.value,
        database = config.database.dbName,
        username = config.database.username,
        password = config.database.password.value,
        maxPoolSize = config.database.maxPoolSize,
      )
      _ <- Resource.eval(logger.info("Database connection established"))

      appDb = SkunkAppDb.make[IO](appPool)

      challengeService = ChallengeService[IO](appDb)
      submissionService = SubmissionService[IO](appDb)

      challengeRoutes = ChallengeRoutes[IO](challengeService)
      submissionRoutes = SubmissionRoutes[IO](submissionService)

      httpApp = Router(
        "/" -> (heartbeatRoutes(appDb) <+> challengeRoutes.routes <+> submissionRoutes.routes),
      ).orNotFound

      _ <- Resource.eval(logger.info("Starting server"))
      emberServer <- EmberServerBuilder
        .default[IO]
        .withHost(config.server.host)
        .withPort(config.server.port)
        .withHttpApp(httpApp)
        .build
        .evalTap(server => logger.info(s"Server started on ${server.address}"))
    yield emberServer
