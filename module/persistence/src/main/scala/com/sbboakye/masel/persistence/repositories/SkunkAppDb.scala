package com.sbboakye.masel.persistence.repositories

import cats.effect.*
import cats.syntax.all.*
import com.sbboakye.masel.core.ports.{AppDb, ChallengeRepository, Repos, SubmissionRepository}
import skunk.Session
import skunk.codec.all.*
import skunk.implicits.*

object SkunkAppDb:
  def make[F[_]: Concurrent](pool: Resource[F, Session[F]]): AppDb[F] = new AppDb[F] {
    private def repos(session: Session[F]): Repos[F] = new Repos[F] {
      override def challenges: ChallengeRepository[F] = SkunkChallengeRepository[F](session)
      override def submissions: SubmissionRepository[F] = SkunkSubmissionRepository[F](session)
    }

    override def run[A](use: Repos[F] => F[A]): F[A] = pool.use(session => use(repos(session)))

    override def transact[A](use: Repos[F] => F[A]): F[A] =
      pool.use(session => session.transaction.use(_ => use(repos(session))))

    override def isReady: F[Boolean] =
      pool.use(session => session.execute(sql"SELECT 1".command).void).attempt.map(_.isRight)
  }
