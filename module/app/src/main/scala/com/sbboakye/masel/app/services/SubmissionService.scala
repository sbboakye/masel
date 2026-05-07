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
  def listSubmissions(limit: Int, offset: Int): EitherT[F, AppError, List[Submission]] =
    db.run(_.submissions.findAll(limit, offset))
      .attemptT
      .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))

  def getSubmission(id: SubmissionId): EitherT[F, AppError, Submission] =
    db.run(_.submissions.findById(id))
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
      created <- db
        .run(_.submissions.create(submission))
        .attemptT
        .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))
    } yield created

  def updateSubmission(id: SubmissionId, request: UpdateSubmissionRequest): EitherT[F, AppError, Submission] =
    def helper(repos: Repos[F]): F[Either[AppError, Submission]] =
      (for {
        existing <- repos.submissions
          .findById(id)
          .attemptT
          .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))
          .flatMap(EitherT.fromOption[F](_, AppError.NotFound("Submission", id.value.toString)))
        now <- EitherT.liftF(Clock[F].realTimeInstant.map(_.atOffset(ZoneOffset.UTC)))
        copied = existing.copy(
          candidateSolution = request.candidateSolution.getOrElse(existing.candidateSolution),
          output = request.output.orElse(existing.output),
          score = request.score.orElse(existing.score),
          updatedAt = now,
        )
        updated <- repos.submissions
          .update(copied)
          .attemptT
          .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))
          .flatMap(EitherT.fromOption[F](_, AppError.NotFound("Submission", id.value.toString)))
      } yield updated).value

    EitherT(db.transact(helper))

  def deleteSubmission(id: SubmissionId): EitherT[F, AppError, Unit] =
    for {
      deleted <- db
        .run(_.submissions.delete(id))
        .attemptT
        .leftMap(e => AppError.InternalError(e.getMessage, Some(e)))
      _ <- EitherT.cond(deleted, (), AppError.NotFound("Submission", id.value.toString))
    } yield ()
