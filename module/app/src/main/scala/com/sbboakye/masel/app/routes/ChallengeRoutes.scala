package com.sbboakye.masel.app.routes

import cats.effect.Async
import cats.syntax.all.*
import com.sbboakye.masel.app.endpoints.BaseEndpoint
import com.sbboakye.masel.app.requests.{CreateChallengeRequest, UpdateChallengeRequest}
import com.sbboakye.masel.app.services.ChallengeService
import com.sbboakye.masel.core.domain.ChallengeId
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.LoggerFactory

class ChallengeRoutes[F[_]: {Async, LoggerFactory}](service: ChallengeService[F]):

  private val dsl = new Http4sDsl[F] {}
  import ErrorHandling.recoverAppErrors
  import dsl.*

  private object LimitParam extends OptionalQueryParamDecoderMatcher[Int]("limit")
  private object OffsetParam extends OptionalQueryParamDecoderMatcher[Int]("offset")

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> BaseEndpoint.basePath / "challenges" :? LimitParam(limit) +& OffsetParam(offset) =>
      service
        .listChallenges(limit.getOrElse(10), offset.getOrElse(0))
        .flatMap(Ok(_))
        .recoverAppErrors(dsl)

    case GET -> BaseEndpoint.basePath / "challenges" / UUIDVar(id) =>
      service
        .getChallenge(ChallengeId(id))
        .flatMap(Ok(_))
        .recoverAppErrors(dsl)

    case req @ POST -> BaseEndpoint.basePath / "challenges" =>
      req
        .as[CreateChallengeRequest]
        .flatMap(service.createChallenge)
        .flatMap(Created(_))
        .recoverAppErrors(dsl)

    case req @ PUT -> BaseEndpoint.basePath / "challenges" / UUIDVar(id) =>
      req
        .as[UpdateChallengeRequest]
        .flatMap(service.updateChallenge(ChallengeId(id), _))
        .flatMap(Ok(_))
        .recoverAppErrors(dsl)

    case DELETE -> BaseEndpoint.basePath / "challenges" / UUIDVar(id) =>
      service
        .deleteChallenge(ChallengeId(id))
        .flatMap(_ => NoContent())
        .recoverAppErrors(dsl)
  }
