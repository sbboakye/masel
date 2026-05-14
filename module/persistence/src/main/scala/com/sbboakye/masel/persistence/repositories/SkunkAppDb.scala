package com.sbboakye.masel.persistence.repositories

import cats.Applicative
import cats.effect.*
import cats.syntax.all.*
import com.sbboakye.masel.core.ports.{AppDb, ChallengeRepository, Repos, SubmissionRepository}
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}
import skunk.Session
import skunk.codec.all.*
import skunk.implicits.*

object SkunkAppDb:
  def make[F[_]: {Concurrent, LoggerFactory}](pool: Resource[F, Session[F]]): AppDb[F] = new AppDb[F] {
    val logger: SelfAwareStructuredLogger[F] = LoggerFactory[F].getLogger

    private def repos(session: Session[F]): Repos[F] = new Repos[F] {
      override val challenges: ChallengeRepository[F] = SkunkChallengeRepository[F](session)
      override val submissions: SubmissionRepository[F] = SkunkSubmissionRepository[F](session)
    }

    override def withSession[A](use: Repos[F] => F[A]): F[A] = pool.use(session => use(repos(session)))

    override def withTransaction[A](use: Repos[F] => F[A]): F[A] =
      pool.use(session => session.transaction.use(_ => use(repos(session))))

    override def isReady: F[Boolean] =
      pool
        .use(session => session.execute(sql"SELECT 1".query(int4)).void)
        .attempt
        .flatTap {
          case Left(err) => logger.error(s"Error connecting to database: $err")
          case Right(_) => Applicative[F].unit
        }
        .map(_.isRight)
  }
