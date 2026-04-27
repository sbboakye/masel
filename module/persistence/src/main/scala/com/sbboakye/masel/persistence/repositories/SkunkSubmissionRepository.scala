package com.sbboakye.masel.persistence.repositories

import cats.effect.*
import cats.syntax.all.*
import com.sbboakye.masel.core.domain.{Submission, SubmissionId, SubmissionUpdate}
import com.sbboakye.masel.core.errors.AppError.NotFound
import com.sbboakye.masel.core.ports.SubmissionRepository
import com.sbboakye.masel.persistence.queries.SubmissionQueries
import org.typelevel.log4cats.LoggerFactory
import fs2.Stream
import skunk.Session
import skunk.data.Completion

class SkunkSubmissionRepository[F[_]: {Concurrent, LoggerFactory}](pool: Resource[F, Session[F]]) extends SubmissionRepository[F]:
  override def findAll(limit: Int, offset: Int): Stream[F, Submission] =
    Stream
      .resource(pool)
      .flatMap { session =>
        Stream.eval(session.prepare(SubmissionQueries.findAll)).flatMap {ps =>
          ps.stream((limit, offset), 10)
        }
    }

  override def findById(id: SubmissionId): F[Option[Submission]] = {
    pool.use { session =>
      session.prepare(SubmissionQueries.findById).flatMap { ps =>
        ps.option(id)
      }
    }
  }

  override def create(submission: Submission): F[Submission] = {
    pool.use { session =>
      session.prepare(SubmissionQueries.create).flatMap { ps =>
        ps.unique(submission)
      }
    }
  }

  override def update(submission: SubmissionUpdate): F[Option[Submission]] =
    pool.use { session =>
      session.prepare(SubmissionQueries.update).flatMap {ps =>
        val submissionTupleTyped = Tuple.fromProductTyped(submission)
        ps.option(submissionTupleTyped)
      }
    }

  override def delete(id: SubmissionId): F[Either[NotFound, Boolean]] =
    pool.use { session =>
      session.prepare(SubmissionQueries.delete).flatMap { ps =>
        ps.execute(id).map {
          case Completion.Delete(n) => Right(n > 0)
          case _ => Left(NotFound(s"Submission with $id not found", id.value))
        }
      }
    }
