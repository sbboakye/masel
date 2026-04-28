package com.sbboakye.masel.persistence.repositories

import cats.data.EitherT
import cats.effect.*
import cats.syntax.all.*
import com.sbboakye.masel.core.domain.dto.{CreateChallengeRequest, UpdateChallengeRequest}
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId}
import com.sbboakye.masel.core.errors.AppError
import com.sbboakye.masel.core.errors.AppError.{InternalError, NotFound}
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

  override def findById(id: ChallengeId): EitherT[F, AppError, Challenge] =
    EitherT(
      pool.use { session =>
        session.prepare(ChallengeQueries.findById).flatMap { ps =>
          ps.option(id)
        }
      }.flatMap {
        case None => Left(NotFound(s"Challenge with $id not found.")).pure
        case Some(challenge) => Right(challenge).pure
      }
    )

  override def create(challenge: CreateChallengeRequest): EitherT[F, AppError, Challenge] =
    EitherT(
      pool.use { session =>
        session.prepare(ChallengeQueries.create).flatMap { ps =>
          ps.unique(challenge)
        }
      }.attempt.map(_.leftMap(error => InternalError(error.getMessage, error.getCause.some)))
    )
    
  override def update(challenge: UpdateChallengeRequest): EitherT[F, AppError, Boolean] =
    EitherT(
      pool.use { session =>
        session.prepare(ChallengeQueries.update).flatMap { ps =>
          ps.execute(challenge).map {
            case Completion.Delete(0) => Left(NotFound(s"Challenge with ${challenge.id} not found."))
            case Completion.Delete(n) => Right(n > 0)
          }
        }
      }
    )

  override def delete(id: ChallengeId): EitherT[F, AppError, Boolean] =
    EitherT(
      pool.use { session =>
        session.prepare(ChallengeQueries.delete).flatMap { ps =>
          ps.execute(id).map {
            case Completion.Delete(0) => Left(NotFound(s"Challenge with $id not found."))
            case Completion.Delete(n) => Right(n > 0)
          }
        }
      }
    )
