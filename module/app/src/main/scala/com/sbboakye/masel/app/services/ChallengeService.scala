package com.sbboakye.masel.app.services

import cats.*
import cats.data.EitherT
import cats.effect.{Clock, Sync}
import cats.syntax.all.*
import com.sbboakye.masel.app.requests.{CreateChallengeRequest, UpdateChallengeRequest}
import com.sbboakye.masel.core.domain.ChallengeStatus.Draft
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId}
import com.sbboakye.masel.core.errors.AppError
import com.sbboakye.masel.core.ports.{AppDb, Repos}
import java.time.ZoneOffset

class ChallengeService[F[_]: Sync](
    db: AppDb[F],
):

  def listChallenges(limit: Int, offset: Int): EitherT[F, AppError, List[Challenge]] =
    db.run(_.challenges.findAll(limit, offset))
      .attemptT
      .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))

  def getChallenge(id: ChallengeId): EitherT[F, AppError, Challenge] =
    db.run(_.challenges.findById(id))
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
      created <- db
        .run(_.challenges.create(challenge))
        .attemptT
        .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))
    } yield created

  def updateChallenge(id: ChallengeId, request: UpdateChallengeRequest): EitherT[F, AppError, Challenge] =
    def helper(repos: Repos[F]): F[Either[AppError, Challenge]] =
      (for {
        existing <- repos.challenges
          .findById(id)
          .attemptT
          .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))
          .flatMap(EitherT.fromOption[F](_, AppError.NotFound("Challenge", id.value.toString)))
        now <- EitherT.liftF(Clock[F].realTimeInstant.map(_.atOffset(ZoneOffset.UTC)))
        copied = existing.copy(
          title = request.title.getOrElse(existing.title),
          instructions = request.instructions.getOrElse(existing.instructions),
          status = request.status.getOrElse(existing.status),
          expectedSolution = request.expectedSolution.getOrElse(existing.expectedSolution),
          output = request.output.orElse(existing.output),
          allottedTime = request.allottedTime.getOrElse(existing.allottedTime),
          difficulty = request.difficulty.getOrElse(existing.difficulty),
          updatedAt = now,
        )
        updated <- repos.challenges
          .update(copied)
          .attemptT
          .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))
          .flatMap(EitherT.fromOption[F](_, AppError.NotFound("Challenge", id.value.toString)))
      } yield updated).value

    EitherT(db.transact(helper))

  def deleteChallenge(id: ChallengeId): EitherT[F, AppError, Unit] =
    for {
      deleted <- db
        .transact(_.challenges.delete(id))
        .attemptT
        .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))
      _ <- EitherT.cond(deleted, (), AppError.NotFound("Challenge", id.value.toString))
    } yield ()
