package com.sbboakye.masel.persistence.repositories

import cats.*
import cats.syntax.all.*
import skunk.data.Completion

trait Helpers:

  private def deletedRow[F[_]: MonadThrow](c: Completion): F[Int] = c match {
    case Completion.Delete(n) => n.pure[F]
    case other => MonadThrow[F].raiseError(new Exception(s"Expected Delete Completion, got: $other"))
  }

  def wasDeleted[F[_]: MonadThrow](c: Completion): F[Boolean] = deletedRow(c).map(_ > 0)
