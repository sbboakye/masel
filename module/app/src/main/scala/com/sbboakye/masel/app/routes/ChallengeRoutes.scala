package com.sbboakye.masel.app.routes

import cats.effect.Async
import cats.syntax.all.*
import com.sbboakye.masel.app.endpoints.{BaseEndpoint, ChallengeEndpoints}
import com.sbboakye.masel.app.services.ChallengeService
import com.sbboakye.masel.core.domain.ChallengeId
import com.sbboakye.masel.core.errors.AppError
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.Http4sServerInterpreter

class ChallengeRoutes[F[_]: Async](service: ChallengeService[F]):
  import BaseEndpoint.mapError

  private val listChallengesRoute =
    ChallengeEndpoints.listChallenges.serverLogic { case (limit, offset) =>
      service
        .listChallenges(limit, offset)
        .map(Right(_))
        .handleError(e => Left(mapError(e)))
    }

  private val getChallengeRoute =
    ChallengeEndpoints.getChallenge.serverLogic(id =>
      service
        .getChallenge(ChallengeId(id))
        .map {
          case Some(challenge) => Right(challenge)
          case None => Left(mapError(AppError.NotFound("Challenge", id.toString)))
        }
        .handleError(e => Left(mapError(e))),
    )

  private val createChallengeRoute =
    ChallengeEndpoints.createChallenge.serverLogic(req =>
      service
        .createChallenge(req)
        .map(Right(_))
        .handleError(e => Left(mapError(e))),
    )

  private val updateChallengeRoute =
    ChallengeEndpoints.updateChallenge.serverLogic((id, req) =>
      service
        .updateChallenge(ChallengeId(id), req)
        .map {
          case Some(challenge) => Right(challenge)
          case None => Left(mapError(AppError.NotFound("Challenge", id.toString)))
        }
        .handleError(e => Left(mapError(e))),
    )

  private val deleteChallengeRoute =
    ChallengeEndpoints.deleteChallenge.serverLogic(id =>
      service
        .deleteChallenge(ChallengeId(id))
        .map(Right(_))
        .handleError(e => Left(mapError(e))),
    )

  val routes: HttpRoutes[F] = Http4sServerInterpreter[F]().toRoutes(
    List(
      listChallengesRoute,
      getChallengeRoute,
      createChallengeRoute,
      updateChallengeRoute,
      deleteChallengeRoute,
    ),
  )
