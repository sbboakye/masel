package com.sbboakye.masel.persistence.repositories

import cats.effect.*
import cats.syntax.all.*
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId}
import com.sbboakye.masel.core.ports.ChallengeRepository
import com.sbboakye.masel.persistence.queries.ChallengeQueries
import skunk.Session

class SkunkChallengeRepository[F[_]: Concurrent](pool: Resource[F, Session[F]])
    extends ChallengeRepository[F]
    with Helpers:
  override def findAll(limit: Int, offset: Int): F[List[Challenge]] =
    pool
      .use(session => session.prepare(ChallengeQueries.findAll))
      .flatMap(ps => ps.stream((limit, offset), limit).compile.toList)

  override def findById(id: ChallengeId): F[Option[Challenge]] =
    pool.use(session => session.prepare(ChallengeQueries.findById).flatMap(ps => ps.option(id)))

  override def create(challenge: Challenge): F[Challenge] =
    pool
      .use(session => session.prepare(ChallengeQueries.create).flatMap(ps => ps.unique(challenge)))

  override def update(challenge: Challenge): F[Option[Challenge]] =
    pool.use(session => session.prepare(ChallengeQueries.update).flatMap(ps => ps.option(challenge)))

  override def delete(id: ChallengeId): F[Boolean] =
    pool.use(session => session.prepare(ChallengeQueries.delete).flatMap(ps => ps.execute(id).flatMap(wasDeleted)))
