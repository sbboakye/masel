package com.sbboakye.masel.persistence.repositories

import cats.effect.*
import cats.syntax.all.*
import com.sbboakye.masel.core.domain.dto.UpdateChallengeRequest
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId}
import com.sbboakye.masel.core.ports.ChallengeRepository
import com.sbboakye.masel.persistence.queries.ChallengeQueries
import fs2.Stream
import skunk.Session
import skunk.data.Completion

class SkunkChallengeRepository[F[_]: Concurrent](pool: Resource[F, Session[F]]) extends ChallengeRepository[F]:
  override def findAll(limit: Int, offset: Int): Stream[F, Challenge] =
    Stream
      .resource(pool)
      .flatMap { session =>
        Stream
          .eval(session.prepare(ChallengeQueries.findAll))
          .flatMap(ps => ps.stream((limit, offset), 10))
      }

  override def findById(id: ChallengeId): F[Option[Challenge]] =
    pool.use(session => session.prepare(ChallengeQueries.findById).flatMap(ps => ps.option(id)))

  override def create(challenge: Challenge): F[Challenge] =
    pool
      .use(session => session.prepare(ChallengeQueries.create).flatMap(ps => ps.unique(challenge)))

  override def update(challenge: UpdateChallengeRequest): F[Boolean] =
    pool.use { session =>
      session.prepare(ChallengeQueries.update).flatMap { ps =>
        ps.execute(challenge).map {
          case Completion.Update(0) => false
          case Completion.Update(challenge) => true
        }
      }
    }

  override def delete(id: ChallengeId): F[Boolean] =
    pool.use { session =>
      session.prepare(ChallengeQueries.delete).flatMap { ps =>
        ps.execute(id).map {
          case Completion.Delete(0) => false
          case Completion.Delete(n) => true
        }
      }
    }
