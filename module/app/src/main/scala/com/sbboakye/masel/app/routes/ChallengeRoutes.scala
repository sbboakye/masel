package com.sbboakye.masel.app.routes

import cats.effect.Async
import cats.syntax.all.*
import com.sbboakye.masel.app.requests.{CreateChallengeRequest, UpdateChallengeRequest}
import com.sbboakye.masel.app.services.ChallengeService
import com.sbboakye.masel.core.domain.ChallengeId
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl

class ChallengeRoutes[F[_]: Async](service: ChallengeService[F]):

  private val dsl = new Http4sDsl[F] {}
  import ErrorHandling.recoverAppErrors
  import dsl.*

  private object LimitParam extends OptionalQueryParamDecoderMatcher[Int]("limit")
  private object OffsetParam extends OptionalQueryParamDecoderMatcher[Int]("offset")

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> basePath / "challenges" :? LimitParam(limit) +& OffsetParam(offset) =>
      service
        .listChallenges(limit.getOrElse(10), offset.getOrElse(0))
        .flatMap(Ok(_))
        .recoverAppErrors(dsl)

    case GET -> basePath / "challenges" / UUIDVar(id) =>
      service
        .getChallenge(ChallengeId(id))
        .flatMap(Ok(_))
        .recoverAppErrors(dsl)

    case req @ POST -> basePath / "challenges" =>
      req
        .as[CreateChallengeRequest]
        .flatMap(service.createChallenge)
        .flatMap(Created(_))
        .recoverAppErrors(dsl)

    case req @ PUT -> basePath / "challenges" / UUIDVar(id) =>
      req
        .as[UpdateChallengeRequest]
        .flatMap(service.updateChallenge(ChallengeId(id), _))
        .flatMap(Ok(_))
        .recoverAppErrors(dsl)

    case DELETE -> basePath / "challenges" / UUIDVar(id) =>
      service
        .deleteChallenge(ChallengeId(id))
        .flatMap(_ => NoContent())
        .recoverAppErrors(dsl)
  }
