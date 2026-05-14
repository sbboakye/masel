package com.sbboakye.masel.persistence.repositories

import cats.*
import cats.syntax.all.*
import com.sbboakye.masel.core.errors.AppError
import org.typelevel.log4cats.LoggerFactory
import skunk.data.Completion

trait Helpers[F[_]: {MonadThrow, LoggerFactory}]:

  private val logger = LoggerFactory[F].getLogger

  def wasDeleted(c: Completion): F[Boolean] = c match {
    case Completion.Delete(n) => (n > 0).pure[F]
    case other =>
      AppError.InternalError(s"Unexpected completion type: $other").raiseError
  }

  def repoHandler[A](fa: F[A]): F[A] =
    fa.handleErrorWith {
      case app: AppError => app.raiseError
      case other =>
        logger.error(other)(s"Database error was caught: ${other.getMessage}") *> AppError
          .InternalError("Database error", other.some)
          .raiseError
    }
