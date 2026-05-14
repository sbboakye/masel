package com.sbboakye.masel.app.services

import cats.*
import cats.effect.Clock
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import com.sbboakye.masel.app.requests.CreateSubmissionRequest
import com.sbboakye.masel.core.domain.{Submission, SubmissionId}
import com.sbboakye.masel.core.errors.AppError
import com.sbboakye.masel.core.ports.AppDb
import java.time.ZoneOffset

class SubmissionService[F[_]: {Clock, MonadThrow, UUIDGen}](db: AppDb[F]):
  def listSubmissions(limit: Int, offset: Int): F[List[Submission]] =
    db.withSession(_.submissions.findAll(limit, offset))
      .adaptError(e => AppError.InternalError(e.getMessage, Some(e)))

  def getSubmission(id: SubmissionId): F[Submission] =
    db.withSession(_.submissions.findById(id))
      .adaptError(e => AppError.InternalError(e.getMessage, Some(e)))
      .flatMap {
        case None => MonadError[F, Throwable].raiseError(AppError.NotFound("Submission", id.value.toString))
        case Some(submission) => submission.pure
      }

  def createSubmission(request: CreateSubmissionRequest): F[Submission] =
    for {
      id <- SubmissionId.generate[F]
      now <- Clock[F].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
      challenge <- db
        .withSession(_.challenges.findById(request.challengeId))
        .flatMap {
          case None =>
            MonadError[F, Throwable].raiseError(AppError.NotFound("Challenge", request.challengeId.value.toString))
          case Some(challenge) => challenge.pure
        }
        .adaptError(e => AppError.InternalError(e.getMessage, Some(e)))
      submission = Submission(
        id = id,
        challengeId = challenge.id,
        candidateSolution = request.candidateSolution,
        output = None,
        score = None,
        createdAt = now,
        updatedAt = now,
      )
      created <- db
        .withSession(_.submissions.create(submission))
        .adaptError(e => AppError.InternalError(e.getMessage, Some(e)))
    } yield created

  def deleteSubmission(id: SubmissionId): F[Unit] =
    for {
      deleted <- db
        .withSession(_.submissions.delete(id))
        .adaptError(e => AppError.InternalError(e.getMessage, Some(e)))
      _ <- MonadError[F, Throwable].raiseWhen(!deleted)(AppError.NotFound("Submission", id.value.toString))
    } yield ()
