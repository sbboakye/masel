package com.sbboakye.masel.app.services

import cats.*
import cats.data.EitherT
import cats.effect.{Clock, Sync}
import cats.syntax.all.*
import com.sbboakye.masel.app.requests.{CreateSubmissionRequest, UpdateSubmissionRequest}
import com.sbboakye.masel.core.domain.{Submission, SubmissionId}
import com.sbboakye.masel.core.errors.AppError
import com.sbboakye.masel.core.ports.SubmissionRepository
import java.time.ZoneOffset

class SubmissionService[F[_]: {MonadThrow, Sync}](repo: SubmissionRepository[F]):
  def listSubmissions(limit: Int, offset: Int): EitherT[F, AppError, List[Submission]] =
    repo
      .findAll(limit, offset)
      .attemptT
      .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))

  def getSubmission(id: SubmissionId): EitherT[F, AppError, Submission] =
    repo
      .findById(id)
      .attemptT
      .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))
      .flatMap(EitherT.fromOption[F](_, AppError.NotFound("Submission", id.value.toString)))

  def createSubmission(request: CreateSubmissionRequest): EitherT[F, AppError, Submission] =
    for {
      id <- EitherT.liftF(SubmissionId.generate)
      now <- EitherT.liftF(Clock[F].realTimeInstant.map(_.atOffset(ZoneOffset.UTC)))
      submission = Submission(
        id = id,
        challengeId = request.challengeId,
        candidateSolution = request.candidateSolution,
        output = None,
        score = None,
        createdAt = now,
        updatedAt = now,
      )
      created <- repo
        .create(submission)
        .attemptT
        .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))
    } yield created

  def updateSubmission(id: SubmissionId, request: UpdateSubmissionRequest): EitherT[F, AppError, Submission] =
    for {
      now <- EitherT.liftF(Clock[F].realTimeInstant.map(_.atOffset(ZoneOffset.UTC)))
      maybeUpdate <- repo
        .findById(id)
        .attemptT
        .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))
        .flatMap(EitherT.fromOption[F](_, AppError.NotFound("Submission", id.value.toString)))
        .flatMap { existing =>
          val updated = existing.copy(
            candidateSolution = request.candidateSolution.getOrElse(existing.candidateSolution),
            output = request.output.orElse(existing.output),
            score = request.score.orElse(existing.score),
            updatedAt = now,
          )
          repo
            .update(updated)
            .attemptT
            .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))
            .flatMap(EitherT.fromOption[F](_, AppError.NotFound("Submission", id.value.toString)))
        }
    } yield maybeUpdate

  def deleteSubmission(id: SubmissionId): EitherT[F, AppError, Unit] =
    for {
      deleted <- repo
        .delete(id)
        .attemptT
        .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))
      _ <- EitherT.cond(deleted, (), AppError.NotFound("Submission", id.value.toString))
    } yield ()
