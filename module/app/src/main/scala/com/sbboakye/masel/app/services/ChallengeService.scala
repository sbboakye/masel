package com.sbboakye.masel.app.services

import cats.*
import cats.data.EitherT
import cats.effect.{Clock, Sync}
import cats.syntax.all.*
import com.sbboakye.masel.app.requests.{CreateChallengeRequest, UpdateChallengeRequest}
import com.sbboakye.masel.core.domain.ChallengeStatus.Draft
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId}
import com.sbboakye.masel.core.errors.AppError
import com.sbboakye.masel.core.ports.ChallengeRepository
import java.time.ZoneOffset

class ChallengeService[F[_]: Sync](repo: ChallengeRepository[F]):
  def listChallenges(limit: Int, offset: Int): EitherT[F, AppError, List[Challenge]] =
    repo
      .findAll(limit, offset)
      .attemptT
      .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))

  def getChallenge(id: ChallengeId): EitherT[F, AppError, Challenge] =
    repo
      .findById(id)
      .attemptT
      .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))
      .flatMap(EitherT.fromOption[F](_, AppError.NotFound("Challenge", id.value.toString)))

  def createChallenge(request: CreateChallengeRequest): EitherT[F, AppError, Challenge] =
    for {
      id <- EitherT.liftF(ChallengeId.generate[F])
      now <- EitherT.liftF(Clock[F].realTimeInstant.map(_.atOffset(ZoneOffset.UTC)))
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
      created <- repo
        .create(challenge)
        .attemptT
        .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))
    } yield created

  def updateChallenge(id: ChallengeId, request: UpdateChallengeRequest): EitherT[F, AppError, Challenge] =
    for {
      now <- EitherT.liftF(Clock[F].realTimeInstant.map(_.atOffset(ZoneOffset.UTC)))
      maybeUpdated <- repo
        .findById(id)
        .attemptT
        .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))
        .flatMap(EitherT.fromOption[F](_, AppError.NotFound("Challenge", id.value.toString)))
        .flatMap { existing =>
          val updated = existing.copy(
            title = request.title.getOrElse(existing.title),
            instructions = request.instructions.getOrElse(existing.instructions),
            status = request.status.getOrElse(existing.status),
            expectedSolution = request.expectedSolution.getOrElse(existing.expectedSolution),
            output = request.output.orElse(existing.output),
            allottedTime = request.allottedTime.getOrElse(existing.allottedTime),
            difficulty = request.difficulty.getOrElse(existing.difficulty),
            updatedAt = now,
          )
          repo
            .update(updated)
            .attemptT
            .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))
            .flatMap(EitherT.fromOption[F](_, AppError.NotFound("Challenge", id.value.toString)))
        }
    } yield maybeUpdated

  def deleteChallenge(id: ChallengeId): EitherT[F, AppError, Unit] =
    for {
      deleted <- repo
        .delete(id)
        .attemptT
        .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))
      _ <- EitherT.cond(deleted, (), AppError.NotFound("Challenge", id.value.toString))
    } yield ()
