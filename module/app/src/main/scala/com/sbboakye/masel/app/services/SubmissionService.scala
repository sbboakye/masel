package com.sbboakye.masel.app.services

import cats.*
import cats.data.EitherT
import cats.effect.{Clock, Sync}
import cats.syntax.all.*
import com.sbboakye.masel.app.requests.{CreateSubmissionRequest, UpdateSubmissionRequest}
import com.sbboakye.masel.core.domain.{Submission, SubmissionId}
import com.sbboakye.masel.core.errors.AppError
import com.sbboakye.masel.core.ports.{AppDb, Repos}
import java.time.ZoneOffset

class SubmissionService[F[_]: Sync](db: AppDb[F]):
  def listSubmissions(limit: Int, offset: Int): F[List[Submission]] =
    db.run(_.submissions.findAll(limit, offset))
      .adaptError(e => AppError.InternalError(e.getMessage, Some(e)))

  def getSubmission(id: SubmissionId): F[Option[Submission]] =
    db.run(_.submissions.findById(id))
      .adaptError(e => AppError.InternalError(e.getMessage, Some(e)))
      .flatTap {
        case None => MonadError[F, Throwable].raiseError(AppError.NotFound("Submission", id.value.toString))
        case Some(submission) => submission.pure
      }

  def createSubmission(request: CreateSubmissionRequest): F[Submission] =
    for {
      id <- SubmissionId.generate[F]
      now <- Clock[F].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
      submission = Submission(
        id = id,
        challengeId = request.challengeId,
        candidateSolution = request.candidateSolution,
        output = None,
        score = None,
        createdAt = now,
        updatedAt = now,
      )
      created <- db
        .run(_.submissions.create(submission))
        .adaptError(e => AppError.InternalError(e.getMessage, Some(e)))
    } yield created

  def updateSubmission(id: SubmissionId, request: UpdateSubmissionRequest): F[Option[Submission]] =
    def helper(repos: Repos[F]): F[Option[Submission]] =
      for {
        existing <- repos.submissions
          .findById(id)
          .adaptError(e => AppError.InternalError(e.getMessage, Some(e)))
          .flatMap {
            case None => MonadError[F, Throwable].raiseError(AppError.NotFound("Submission", id.value.toString))
            case Some(challenge) => challenge.pure
          }
        now <- Clock[F].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
        copied = existing.copy(
          candidateSolution = request.candidateSolution.getOrElse(existing.candidateSolution),
          output = request.output.orElse(existing.output),
          score = request.score.orElse(existing.score),
          updatedAt = now,
        )
        updated <- repos.submissions
          .update(copied)
          .adaptError(e => AppError.InternalError(e.getMessage, Some(e)))
          .flatTap {
            case None => MonadError[F, Throwable].raiseError(AppError.NotFound("Submission", id.value.toString))
            case Some(challenge) => challenge.pure
          }
      } yield updated

    db.transact(helper)

  def deleteSubmission(id: SubmissionId): F[Unit] =
    for {
      deleted <- db
        .run(_.submissions.delete(id))
        .adaptError(e => AppError.InternalError(e.getMessage, Some(e)))
      _ <- MonadError[F, Throwable].raiseWhen(!deleted)(AppError.NotFound("Challenge", id.value.toString))
    } yield ()
