package com.sbboakye.masel.app.services

import cats.*
import cats.effect.{Clock, Sync}
import cats.syntax.all.*
import com.sbboakye.masel.app.requests.{CreateSubmissionRequest, UpdateSubmissionRequest}
import com.sbboakye.masel.core.domain.{Submission, SubmissionId}
import com.sbboakye.masel.core.errors.AppError
import com.sbboakye.masel.core.ports.SubmissionRepository
import java.time.ZoneOffset

class SubmissionService[F[_]: {MonadThrow, Sync}](repo: SubmissionRepository[F]):
  def listSubmissions(limit: Int, offset: Int): F[Either[AppError, List[Submission]]] =
    repo
      .findAll(limit, offset)
      .map(Right(_))
      .handleErrorWith(e => MonadThrow[F].pure(Left(AppError.InternalError(e.getMessage, Some(e)))))

  def getSubmission(id: SubmissionId): F[Either[AppError, Option[Submission]]] =
    repo
      .findById(id)
      .flatMap {
        case Some(submission) => MonadThrow[F].pure(Right(Some(submission)))
        case None => MonadThrow[F].pure(Left(AppError.NotFound("Submission", id.value.toString)))
      }
      .handleErrorWith(e => MonadThrow[F].pure(Left(AppError.InternalError(e.getMessage, Some(e)))))

  def createSubmission(request: CreateSubmissionRequest): F[Either[AppError, Submission]] =
    for {
      id <- SubmissionId.generate
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
      created <- repo
        .create(submission)
        .map(Right(_))
        .handleErrorWith(e => MonadThrow[F].pure(Left(AppError.InternalError(e.getMessage, Some(e)))))
    } yield created

  def updateSubmission(id: SubmissionId, request: UpdateSubmissionRequest): F[Either[AppError, Option[Submission]]] =
    for {
      now <- Clock[F].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
      maybeUpdate <- repo
        .findById(id)
        .flatMap {
          case None => MonadThrow[F].pure(Left(AppError.NotFound("Submission", id.value.toString)))
          case Some(existing) =>
            val updated = existing.copy(
              candidateSolution = request.candidateSolution.getOrElse(existing.candidateSolution),
              output = request.output,
              score = request.score,
              updatedAt = now,
            )
            repo
              .update(updated)
              .map {
                case Some(_) => Right(Some(updated))
                case None => Left(AppError.NotFound("Submission", id.value.toString))
              }
              .handleErrorWith(e => MonadThrow[F].pure(Left(AppError.InternalError(e.getMessage, Some(e)))))
        }
    } yield maybeUpdate

  def deleteSubmission(id: SubmissionId): F[Either[AppError, Unit]] =
    repo
      .delete(id)
      .map {
        case true => Right(())
        case false => Left(AppError.NotFound("Submission", id.value.toString))
      }
      .handleErrorWith(e => MonadThrow[F].pure(Left(AppError.InternalError(e.getMessage, Some(e)))))
