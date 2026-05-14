package com.sbboakye.masel.persistence.repositories

import cats.*
import cats.syntax.all.*
import com.sbboakye.masel.persistence.repositories.RepositoryError.DatabaseError
import skunk.data.Completion

trait Helpers[F[_]: MonadThrow]:

  private def deletedRow(c: Completion): F[Int] = c match {
    case Completion.Delete(n) => n.pure[F]
    case other =>
      DatabaseError.raiseError[F, Int]
  }

  def wasDeleted(c: Completion): F[Boolean] = deletedRow(c).map(_ > 0)

  def repoHandler[A](fa: F[A]): F[A] =
    fa.handleErrorWith {
      case err: RepositoryError => err.raiseError
      case _ => DatabaseError.raiseError
    }
