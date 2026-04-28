package com.sbboakye.masel.persistence.repositories

import cats.data.EitherT
import cats.effect.*
import cats.syntax.all.*
import com.sbboakye.masel.core.domain.dto.{CreateSubmissionRequest, UpdateSubmissionRequest}
import com.sbboakye.masel.core.domain.{Submission, SubmissionId}
import com.sbboakye.masel.core.errors.AppError
import com.sbboakye.masel.core.errors.AppError.{InternalError, NotFound}
import com.sbboakye.masel.core.ports.SubmissionRepository
import com.sbboakye.masel.persistence.queries.SubmissionQueries
import fs2.Stream
import skunk.Session
import skunk.data.Completion

class SkunkSubmissionRepository[F[_]: Concurrent](pool: Resource[F, Session[F]]) extends SubmissionRepository[F]:
  override def findAll(limit: Int, offset: Int): Stream[F, Submission] =
    Stream
      .resource(pool)
      .flatMap { session =>
        Stream.eval(session.prepare(SubmissionQueries.findAll)).flatMap {ps =>
          ps.stream((limit, offset), 10)
        }
    }

  override def findById(id: SubmissionId): EitherT[F, AppError, Submission] =
    EitherT(
      pool.use { session =>
        session.prepare(SubmissionQueries.findById).flatMap { ps =>
          ps.option(id)
        }
      }.flatMap {
        case None => Left(NotFound(s"Submission with $id not found.")).pure
        case Some(submission) => Right(submission).pure
      }
    )

  override def create(submission: CreateSubmissionRequest): EitherT[F, AppError, Submission] =
    EitherT(
      pool.use { session =>
        session.prepare(SubmissionQueries.create).flatMap { ps =>
          ps.unique(submission)
        }
      }.attempt.map(_.leftMap(error => InternalError(error.getMessage, error.getCause.some)))
    )

  override def update(submission: UpdateSubmissionRequest): EitherT[F, AppError, Boolean] =
    EitherT(
      pool.use { session =>
        session.prepare(SubmissionQueries.update).flatMap {ps =>
          ps.execute(submission).map {
            case Completion.Update(0) => Left(NotFound(s"Submission with ${submission.id} not found."))
            case Completion.Update(n) => Right(n > 0)
          }
        }
      }
    )

  override def delete(id: SubmissionId): EitherT[F, AppError, Boolean] =
    EitherT(
      pool.use { session =>
        session.prepare(SubmissionQueries.delete).flatMap { ps =>
          ps.execute(id).map {
            case Completion.Delete(0) => Left(NotFound(s"Submission with $id not found"))
            case Completion.Delete(n) => Right(n > 0)
          }
        }
      }
    )
