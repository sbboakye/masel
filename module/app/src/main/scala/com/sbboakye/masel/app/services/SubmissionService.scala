package com.sbboakye.masel.app.services

import cats.*
import cats.effect.Clock
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import com.sbboakye.masel.app.requests.{CreateSubmissionRequest, UpdateSubmissionRequest}
import com.sbboakye.masel.core.domain.{Submission, SubmissionId}
import com.sbboakye.masel.core.errors.AppError
import com.sbboakye.masel.core.ports.{AppDb, Repos}
import java.time.ZoneOffset
import org.typelevel.log4cats.LoggerFactory

class SubmissionService[F[_]: {Clock, MonadThrow, UUIDGen, LoggerFactory}](db: AppDb[F]) extends Helpers:
  def listSubmissions(limit: Int, offset: Int): F[List[Submission]] =
    serviceHandler(db.withSession(_.submissions.findAll(limit, offset)))

  def getSubmission(id: SubmissionId): F[Submission] =
    serviceHandler(
      db.withSession(_.submissions.findById(id))
        .orNotFound("Submission", id.value.toString),
    )

  def createSubmission(request: CreateSubmissionRequest): F[Submission] =
    serviceHandler(db.withTransaction { repos =>
      for {
        id <- SubmissionId.generate[F]
        now <- Clock[F].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
        challenge <-
          serviceHandler(
            db
              .withSession(
                _.challenges
                  .findById(request.challengeId)
                  .orNotFound("Challenge", request.challengeId.value.toString),
              ),
          )
        submission = Submission(
          id = id,
          challengeId = challenge.id,
          candidateSolution = request.candidateSolution,
          output = None,
          score = None,
          createdAt = now,
          updatedAt = now,
        )
        created <- repos.submissions.create(submission)
      } yield created
    })

  def updateSubmission(id: SubmissionId, request: UpdateSubmissionRequest): F[Submission] =
    def helper(repos: Repos[F]): F[Submission] =
      for {
        existing <- repos.submissions
          .findById(id)
          .orNotFound("Submission", id.value.toString)
        now <- Clock[F].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
        copied = existing.copy(
          candidateSolution = request.candidateSolution.getOrElse(existing.candidateSolution),
          output = request.output.orElse(existing.output),
          score = request.score.orElse(existing.score),
          updatedAt = now,
        )
        updated <- repos.submissions
          .update(copied)
          .orNotFound("Submission", id.value.toString)
      } yield updated

    serviceHandler(db.withTransaction(helper))

  def deleteSubmission(id: SubmissionId): F[Unit] =
    serviceHandler(
      for {
        deleted <- db
          .withSession(_.submissions.delete(id))
        _ <- AppError.NotFound("Submission", id.value.toString).raiseError[F, Unit].whenA(!deleted)
      } yield (),
    )
