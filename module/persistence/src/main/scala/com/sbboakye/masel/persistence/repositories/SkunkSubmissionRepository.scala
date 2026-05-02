package com.sbboakye.masel.persistence.repositories

import cats.effect.*
import cats.syntax.all.*
import com.sbboakye.masel.core.domain.dto.UpdateSubmissionRequest
import com.sbboakye.masel.core.domain.{Submission, SubmissionId}
import com.sbboakye.masel.core.ports.SubmissionRepository
import com.sbboakye.masel.persistence.queries.SubmissionQueries
import fs2.Stream
import skunk.Session
import skunk.data.Completion

class SkunkSubmissionRepository[F[_]: Concurrent](pool: Resource[F, Session[F]])
    extends SubmissionRepository[F]
    with Helpers:
  override def findAll(limit: Int, offset: Int): Stream[F, Submission] =
    Stream
      .resource(pool)
      .flatMap { session =>
        Stream
          .eval(session.prepare(SubmissionQueries.findAll))
          .flatMap(ps => ps.stream((limit, offset), 10))
      }

  override def findById(id: SubmissionId): F[Option[Submission]] =
    pool.use(session => session.prepare(SubmissionQueries.findById).flatMap(ps => ps.option(id)))

  override def create(submission: Submission): F[Submission] =
    pool
      .use(session => session.prepare(SubmissionQueries.create).flatMap(ps => ps.unique(submission)))

  override def update(submission: UpdateSubmissionRequest): F[Option[Submission]] =
    pool.use(session => session.prepare(SubmissionQueries.update).flatMap(ps => ps.option(submission)))

  override def delete(id: SubmissionId): F[Boolean] =
    pool.use(session => session.prepare(SubmissionQueries.delete).flatMap(ps => ps.execute(id).flatMap(wasDeleted)))
