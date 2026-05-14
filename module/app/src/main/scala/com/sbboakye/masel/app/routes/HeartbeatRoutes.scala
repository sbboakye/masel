package com.sbboakye.masel.app.routes

import cats.effect.Async
import cats.syntax.all.*
import com.sbboakye.masel.app.endpoints.{HealthResponse, ReadinessResponse}
import com.sbboakye.masel.core.ports.AppDb
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl

class HeartbeatRoutes[F[_]: Async](appDb: AppDb[F]):
  private val dsl = new Http4sDsl[F] {}
  import dsl.*

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "health" =>
      Ok(HealthResponse("ok"))

    case GET -> Root / "ready" =>
      appDb.isReady.flatMap {
        case true => Ok(ReadinessResponse("ok"))
        case false => ServiceUnavailable(ReadinessResponse("not ready"))
      }
  }
