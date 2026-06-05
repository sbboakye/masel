package com.sbboakye.masel.app.services

import cats.*
import cats.effect.Clock
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import com.sbboakye.masel.app.requests.{CreateChallengeRequest, UpdateChallengeRequest}
import com.sbboakye.masel.core.domain.ChallengeStatus.Draft
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId}
import com.sbboakye.masel.core.errors.AppError
import com.sbboakye.masel.core.ports.{AppDb, Repos}
import java.time.ZoneOffset
import org.typelevel.log4cats.LoggerFactory

class ChallengeService[F[_]: {Clock, MonadThrow, UUIDGen, LoggerFactory}](
    db: AppDb[F],
) extends Helpers[F]:

  def listChallenges(limit: Int, offset: Int): F[List[Challenge]] =
    serviceHandler(db.withSession(_.challenges.findAll(limit, offset)))

  def getChallenge(id: ChallengeId): F[Challenge] =
    serviceHandler(
      db.withSession(_.challenges.findById(id))
        .orNotFound("Challenge", id.value.toString),
    )

  def createChallenge(request: CreateChallengeRequest): F[Challenge] =
    serviceHandler(
      for {
        id <- ChallengeId.generate[F]
        now <- Clock[F].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
        challenge = Challenge(
          id = id,
          title = request.title,
          instructions = request.instructions,
          setupSql = request.setupSql,
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

      } yield created,
    )

  def updateChallenge(id: ChallengeId, request: UpdateChallengeRequest): F[Challenge] =
    def helper(repos: Repos[F]): F[Challenge] =
      for {
        existing <- repos.challenges
          .findById(id)
          .orNotFound("Challenge", id.value.toString)
        now <- Clock[F].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
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
          .orNotFound("Challenge", id.value.toString)
      } yield updated

    serviceHandler(db.withTransaction(helper))

  def deleteChallenge(id: ChallengeId): F[Unit] =
    serviceHandler(for {
      deleted <-
        db
          .withSession(_.challenges.delete(id))
      _ <- AppError.NotFound("Challenge", id.value.toString).raiseError[F, Unit].whenA(!deleted)
    } yield ())
