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

class SubmissionService[F[_]: {Clock, MonadThrow, UUIDGen}](db: AppDb[F]):
  def listSubmissions(limit: Int, offset: Int): F[List[Submission]] =
    db.withSession(_.submissions.findAll(limit, offset))

  def getSubmission(id: SubmissionId): F[Submission] =
    db.withSession(_.submissions.findById(id))
      .flatMap {
        case None => MonadError[F, Throwable].raiseError(AppError.NotFound("Submission", id.value.toString))
        case Some(submission) => submission.pure
      }

  def createSubmission(request: CreateSubmissionRequest): F[Submission] =
    for {
      id <- SubmissionId.generate[F]
      now <- Clock[F].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
      challenge <-
        db
          .withSession(
            _.challenges
              .findById(request.challengeId)
              .flatMap {
                case None =>
                  MonadError[F, Throwable]
                    .raiseError(AppError.NotFound("Challenge", request.challengeId.value.toString))
                case Some(challenge) => challenge.pure
              },
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
      created <-
        db
          .withSession(_.submissions.create(submission))
    } yield created

  def updateSubmission(id: SubmissionId, request: UpdateSubmissionRequest): F[Submission] =
    def helper(repos: Repos[F]): F[Submission] =
      for {
        existing <- repos.submissions
          .findById(id)
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
          .flatMap {
            case None => MonadError[F, Throwable].raiseError(AppError.NotFound("Submission", id.value.toString))
            case Some(challenge) => challenge.pure
          }
      } yield updated

    db.withTransaction(helper)

  def deleteSubmission(id: SubmissionId): F[Unit] =
    for {
      deleted <-
        db
          .withSession(_.submissions.delete(id))
      _ <- MonadError[F, Throwable].raiseWhen(!deleted)(AppError.NotFound("Submission", id.value.toString))
    } yield ()
