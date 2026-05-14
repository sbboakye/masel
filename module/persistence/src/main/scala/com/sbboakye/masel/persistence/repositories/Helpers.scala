package com.sbboakye.masel.persistence.repositories

import cats.*
import cats.syntax.all.*
import com.sbboakye.masel.core.errors.AppError.InternalError
import skunk.data.Completion

trait Helpers:

  private def deletedRow[F[_]: MonadThrow](c: Completion): F[Int] = c match {
    case Completion.Delete(n) => n.pure[F]
    case other =>
      InternalError.raiseError[F, Int]
  }

  def wasDeleted[F[_]: MonadThrow](c: Completion): F[Boolean] = deletedRow(c).map(_ > 0)
