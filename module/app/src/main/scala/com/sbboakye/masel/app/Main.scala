package com.sbboakye.masel.app

import cats.*
import cats.effect.*
import cats.effect.{ExitCode, IOApp}
import cats.syntax.all.*
import com.sbboakye.masel.app.endpoints.{ChallengeEndpoints, SubmissionEndpoints}
import com.sbboakye.masel.app.routes.{ChallengeRoutes, SubmissionRoutes}
import com.sbboakye.masel.app.services.{ChallengeService, SubmissionService}
import com.sbboakye.masel.persistence.FlywayMigrator
import com.sbboakye.masel.persistence.repositories.SkunkAppDb
import com.sbboakye.masel.persistence.session.PoolSession
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.{Router, Server}
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object Main extends IOApp:

  given LoggerFactory[IO] = Slf4jFactory.create[IO]

  final case class HealthResponse(status: String)

  object HealthResponse:
    given Encoder[HealthResponse] = deriveEncoder
    given Decoder[HealthResponse] = deriveDecoder

  // Heartbeat Endpoint definitions
  private val healthEndpoint = endpoint.get
    .in("health")
    .out(jsonBody[HealthResponse])
    .tag("Health")
    .description("Check the health of the server")

  private val readyEndpoint = endpoint.get
    .in("ready")
    .out(jsonBody[HealthResponse])
    .errorOut(jsonBody[HealthResponse])
    .description("Readiness check - database is alive")

  def run(args: List[String]): IO[ExitCode] =
    for
      config <- AppConfig.loadF[IO]
      _ <- buildApp(config).useForever
    yield ExitCode.Success

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

      // App DB
      appDb = SkunkAppDb.make[IO](appPool)

      // Services
      challengeService = ChallengeService[IO](appDb)
      submissionService = SubmissionService[IO](appDb)

      // Routes
      heartbeatRoutes = Http4sServerInterpreter[IO]().toRoutes(
        List(
          healthEndpoint.serverLogic(_ => HealthResponse("ok").asRight[Throwable]),
          readyEndpoint.serverLogic(_ =>
            appDb.isReady.map {
              case true => HealthResponse("ok").asRight[Throwable]
              case false => HealthResponse("not ready").asLeft[Throwable]
            },
          ),
        ),
      )

      challengeRoutes = ChallengeRoutes[IO](challengeService)
      submissionRoutes = SubmissionRoutes[IO](submissionService)

      // Swagger Routes UI
      allEndpoints = List(healthEndpoint, readyEndpoint) ++ ChallengeEndpoints.all ++ SubmissionEndpoints.all
      swaggerRoutes = Http4sServerInterpreter[IO]().toRoutes(
        SwaggerInterpreter().fromEndpoints(allEndpoints, "Masel API", "1.0.0"),
      )

      httpApp = Router(
        "/" -> (heartbeatRoutes <+> challengeRoutes.routes <+> submissionRoutes.routes <+> swaggerRoutes),
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
