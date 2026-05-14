package com.sbboakye.masel.persistence.repositories

import cats.effect.*
import cats.syntax.all.*
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId}
import com.sbboakye.masel.core.ports.ChallengeRepository
import com.sbboakye.masel.persistence.queries.ChallengeQueries
import org.typelevel.log4cats.LoggerFactory
import skunk.Session

class SkunkChallengeRepository[F[_]: {Concurrent, LoggerFactory}](session: Session[F])
    extends ChallengeRepository[F]
    with Helpers[F]:
  override def findAll(limit: Int, offset: Int): F[List[Challenge]] =
    repoHandler(
      session.prepare(ChallengeQueries.findAll).flatMap(ps => ps.stream((limit, offset), limit).compile.toList),
    )

  override def findById(id: ChallengeId): F[Option[Challenge]] =
    repoHandler(session.prepare(ChallengeQueries.findById).flatMap(ps => ps.option(id)))

  override def create(challenge: Challenge): F[Challenge] =
    repoHandler(session.prepare(ChallengeQueries.create).flatMap(ps => ps.unique(challenge)))

  override def update(challenge: Challenge): F[Option[Challenge]] =
    repoHandler(session.prepare(ChallengeQueries.update).flatMap(ps => ps.option(challenge)))

  override def delete(id: ChallengeId): F[Boolean] =
    repoHandler(session.prepare(ChallengeQueries.delete).flatMap(ps => ps.execute(id).flatMap(wasDeleted)))
