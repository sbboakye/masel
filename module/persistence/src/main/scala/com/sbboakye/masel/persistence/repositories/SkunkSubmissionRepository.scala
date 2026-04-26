package com.sbboakye.masel.persistence.repositories

import cats.effect.*
import cats.syntax.all.*
import com.sbboakye.masel.core.domain.{Submission, SubmissionId, SubmissionUpdate}
import com.sbboakye.masel.core.ports.SubmissionRepository
import com.sbboakye.masel.persistence.queries.SubmissionQueries
import org.typelevel.log4cats.Logger
import skunk.Session
import skunk.data.Completion

class SkunkSubmissionRepository[F[_]: {Concurrent, Logger}](session: Session[F]) extends SubmissionRepository[F]:
  override def findAll(limit: Int, offset: Int): fs2.Stream[F, Submission] =
    session.stream(SubmissionQueries.findAll)((limit, offset), 10)

  override def findById(id: SubmissionId): F[Option[Submission]] =
    session.option(SubmissionQueries.findById)(id)

  override def create(submission: Submission): F[Submission] =
    session.unique(SubmissionQueries.create)(submission)

  override def update(submission: SubmissionUpdate): F[Option[Submission]] = {
    val submissionTupleTyped = Tuple.fromProductTyped(submission)
    session.option(SubmissionQueries.update)(submissionTupleTyped)
  }

  override def delete(id: SubmissionId): F[Int] =
    session.execute(SubmissionQueries.delete)(id).map {
      case Completion.Delete(n) => n
      case _ => 0
    }
