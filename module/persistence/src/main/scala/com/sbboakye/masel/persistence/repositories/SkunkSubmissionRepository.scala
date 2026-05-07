package com.sbboakye.masel.persistence.repositories

import cats.effect.*
import cats.syntax.all.*
import com.sbboakye.masel.core.domain.{Submission, SubmissionId}
import com.sbboakye.masel.core.ports.SubmissionRepository
import com.sbboakye.masel.persistence.queries.SubmissionQueries
import skunk.Session

class SkunkSubmissionRepository[F[_]: Concurrent](session: Session[F]) extends SubmissionRepository[F] with Helpers:
  override def findAll(limit: Int, offset: Int): F[List[Submission]] =
    session.prepare(SubmissionQueries.findAll).flatMap(ps => ps.stream((limit, offset), limit).compile.toList)

  override def findById(id: SubmissionId): F[Option[Submission]] =
    session.prepare(SubmissionQueries.findById).flatMap(ps => ps.option(id))

  override def create(submission: Submission): F[Submission] =
    session.prepare(SubmissionQueries.create).flatMap(ps => ps.unique(submission))

  override def update(submission: Submission): F[Option[Submission]] =
    session.prepare(SubmissionQueries.update).flatMap(ps => ps.option(submission))

  override def delete(id: SubmissionId): F[Boolean] =
    session.prepare(SubmissionQueries.delete).flatMap(ps => ps.execute(id).flatMap(wasDeleted))
