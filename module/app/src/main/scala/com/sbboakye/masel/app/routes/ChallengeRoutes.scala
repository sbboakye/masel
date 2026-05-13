package com.sbboakye.masel.app.routes

import cats.effect.Async
import cats.syntax.all.*
import com.sbboakye.masel.app.requests.{CreateChallengeRequest, UpdateChallengeRequest}
import com.sbboakye.masel.app.services.ChallengeService
import com.sbboakye.masel.core.domain.ChallengeId
import java.util.UUID
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl

class ChallengeRoutes[F[_]: Async](service: ChallengeService[F]):

  private val dsl = new Http4sDsl[F] {}
  import dsl.*
  import ErrorHandling.recoverAppErrors

  private object LimitParam extends OptionalQueryParamDecoderMatcher[Int]("limit")
  private object OffsetParam extends OptionalQueryParamDecoderMatcher[Int]("offset")

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "api" / "v1" / "challenges" :? LimitParam(limit) +& OffsetParam(offset) =>
      service
        .listChallenges(limit.getOrElse(10), offset.getOrElse(0))
        .flatMap(Ok(_))
        .recoverAppErrors(dsl)

    case GET -> Root / "api" / "v1" / "challenges" / UUIDVar(id) =>
      service
        .getChallenge(ChallengeId(id))
        .flatMap(Ok(_))
        .recoverAppErrors(dsl)

    case req @ POST -> Root / "api" / "v1" / "challenges" =>
      req
        .as[CreateChallengeRequest]
        .flatMap(service.createChallenge)
        .flatMap(Created(_))
        .recoverAppErrors(dsl)

    case req @ PUT -> Root / "api" / "v1" / "challenges" / UUIDVar(id) =>
      req
        .as[UpdateChallengeRequest]
        .flatMap(service.updateChallenge(ChallengeId(id), _))
        .flatMap(Ok(_))
        .recoverAppErrors(dsl)

    case DELETE -> Root / "api" / "v1" / "challenges" / UUIDVar(id) =>
      service
        .deleteChallenge(ChallengeId(id))
        .flatMap(_ => NoContent())
        .recoverAppErrors(dsl)
  }
