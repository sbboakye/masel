package com.sbboakye.masel.app.services

import cats.*
import cats.effect.std.UUIDGen
import cats.effect.{Async, Clock}
import cats.syntax.all.*
import com.sbboakye.masel.app.requests.{CreateChallengeRequest, UpdateChallengeRequest}
import com.sbboakye.masel.core.domain.ChallengeStatus.Draft
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId}
import com.sbboakye.masel.core.errors.AppError
import com.sbboakye.masel.core.ports.{AppDb, Repos}
import java.time.ZoneOffset

class ChallengeService[F[_]: {Clock, MonadThrow, UUIDGen}](
    db: AppDb[F],
):

  def listChallenges(limit: Int, offset: Int): F[List[Challenge]] =
    db.withSession(_.challenges.findAll(limit, offset))
      .adaptError(e => AppError.InternalError(e.getMessage, Some(e)))

  def getChallenge(id: ChallengeId): F[Challenge] =
    db.withSession(_.challenges.findById(id))
      .adaptError(e => AppError.InternalError(e.getMessage, Some(e)))
      .flatMap {
        case None => MonadError[F, Throwable].raiseError(AppError.NotFound("Challenge", id.value.toString))
        case Some(challenge) => challenge.pure
      }

  def createChallenge(request: CreateChallengeRequest): F[Challenge] =
    for {
      id <- ChallengeId.generate[F]
      now <- Clock[F].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
      challenge = Challenge(
        id = id,
        title = request.title,
        instructions = request.instructions,
        status = Draft,
        expectedSolution = request.expectedSolution,
        output = None,
        allottedTime = request.allottedTime,
        difficulty = request.difficulty,
        createdAt = now,
        updatedAt = now,
      )
      created <- db
        .withSession(_.challenges.create(challenge))
        .adaptError(e => AppError.InternalError(e.getMessage, Some(e)))

    } yield created

  def updateChallenge(id: ChallengeId, request: UpdateChallengeRequest): F[Challenge] =
    def helper(repos: Repos[F]): F[Challenge] =
      for {
        existing <- repos.challenges
          .findById(id)
          .adaptError(e => AppError.InternalError(e.getMessage, Some(e)))
          .flatMap {
            case None => MonadError[F, Throwable].raiseError(AppError.NotFound("Challenge", id.value.toString))
            case Some(challenge) => challenge.pure
          }
        now <- Clock[F].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
        copied = existing.copy(
          title = request.title.getOrElse(existing.title),
          instructions = request.instructions.getOrElse(existing.instructions),
          status = request.status.getOrElse(existing.status),
          expectedSolution = request.expectedSolution.getOrElse(existing.expectedSolution),
          allottedTime = request.allottedTime.getOrElse(existing.allottedTime),
          difficulty = request.difficulty.getOrElse(existing.difficulty),
          updatedAt = now,
        )
        updated <- repos.challenges
          .update(copied)
          .adaptError(e => AppError.InternalError(e.getMessage, Some(e)))
          .flatMap {
            case None => MonadError[F, Throwable].raiseError(AppError.NotFound("Challenge", id.value.toString))
            case Some(challenge) => challenge.pure
          }
      } yield updated

    db.withTransaction(helper)

  def deleteChallenge(id: ChallengeId): F[Unit] =
    for {
      deleted <- db
        .withSession(_.challenges.delete(id))
        .adaptError(e => AppError.InternalError(e.getMessage, Some(e)))
      _ <- MonadError[F, Throwable].raiseWhen(!deleted)(AppError.NotFound("Challenge", id.value.toString))
    } yield ()
