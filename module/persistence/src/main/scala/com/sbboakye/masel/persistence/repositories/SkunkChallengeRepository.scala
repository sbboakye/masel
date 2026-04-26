package com.sbboakye.masel.persistence.repositories

import cats.effect.*
import cats.syntax.all.*
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId, ChallengeUpdate}
import com.sbboakye.masel.core.ports.ChallengeRepository
import com.sbboakye.masel.persistence.queries.ChallengeQueries
import skunk.Session
import fs2.Stream
import org.typelevel.log4cats.LoggerFactory
import skunk.data.Completion

class SkunkChallengeRepository[F[_]: {Concurrent, LoggerFactory}](pool: Resource[F, Session[F]]) extends ChallengeRepository[F]:
  override def findAll(limit: Int, offset: Int): Stream[F, Challenge] =
    Stream
      .resource(pool)
      .flatMap { session =>
        Stream.eval(session.prepare(ChallengeQueries.findAll)).flatMap { ps =>
          ps.stream((limit, offset), 10)
        }
      }

  override def findById(id: ChallengeId): F[Option[Challenge]] =
    pool.use { session =>
      session.prepare(ChallengeQueries.findById).flatMap { ps =>
        ps.option(id)
      }
    }

  override def create(challenge: Challenge): F[Challenge] =
    pool.use { session =>
      session.prepare(ChallengeQueries.create).flatMap { ps =>
        ps.unique(challenge)
      }
    }

  override def update(challenge: ChallengeUpdate): F[Option[Challenge]] =
    pool.use { session =>
      session.prepare(ChallengeQueries.update).flatMap { ps =>
        val challengeTupleTyped = Tuple.fromProductTyped(challenge)
        ps.option(challengeTupleTyped)
      }
    }

  override def delete(id: ChallengeId): F[Int] = {
    pool.use { session =>
      session.prepare(ChallengeQueries.delete).flatMap { ps =>
        ps.execute(id).map {
          case Completion.Delete(n) => n
          case _ => 0
        }
      }
    }
  }
