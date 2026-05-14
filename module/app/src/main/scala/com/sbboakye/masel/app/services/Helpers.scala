package com.sbboakye.masel.app.services

import cats.MonadThrow
import cats.syntax.all.*
import com.sbboakye.masel.core.errors.AppError
import com.sbboakye.masel.persistence.repositories.RepositoryError
import org.typelevel.log4cats.LoggerFactory

trait Helpers[F[_]: {MonadThrow, LoggerFactory}]:

  private val logger = LoggerFactory[F].getLogger

  def serviceHandler[A](fa: F[A]): F[A] =
    fa.handleErrorWith {
      case RepositoryError.DatabaseError =>
        logger.error("Database error occurred") *> RepositoryError.DatabaseError.raiseError
      case app: AppError => app.raiseError
      case _ =>
        logger.error("Unexpected error occurred") *>
          AppError.InternalError.raiseError
    }
