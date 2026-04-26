package com.sbboakye.masel.persistence.repositories

import cats.effect.*
import cats.syntax.all.*
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId, ChallengeUpdate}
import com.sbboakye.masel.core.ports.ChallengeRepository
import com.sbboakye.masel.persistence.queries.ChallengeQueries
import skunk.Session
import fs2.Stream
import org.typelevel.log4cats.Logger
import skunk.data.Completion

class SkunkChallengeRepository[F[_]: {Concurrent, Logger}](session: Session[F]) extends ChallengeRepository[F]:
  override def findAll(limit: Int, offset: Int): Stream[F, Challenge] =
    session.stream(ChallengeQueries.findAll)((limit, offset), 10)

  override def findById(id: ChallengeId): F[Option[Challenge]] =
    session.option(ChallengeQueries.findById)(id)

  override def create(challenge: Challenge): F[Challenge] =
    session.unique(ChallengeQueries.create)(challenge)

  override def update(challenge: ChallengeUpdate): F[Option[Challenge]] = {
    val challengeTupleTyped = Tuple.fromProductTyped(challenge)
    session.option(ChallengeQueries.update)(challengeTupleTyped)
  }

  override def delete(id: ChallengeId): F[Int] =
    session.execute(ChallengeQueries.delete)(id).map {
      case Completion.Delete(n) => n
      case _ => 0
    }
