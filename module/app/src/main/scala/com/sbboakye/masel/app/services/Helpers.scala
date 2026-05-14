package com.sbboakye.masel.app.services

import cats.MonadThrow
import cats.syntax.all.*
import com.sbboakye.masel.core.errors.AppError
import org.typelevel.log4cats.LoggerFactory

trait Helpers[F[_]: {MonadThrow, LoggerFactory}]:

  private val logger = LoggerFactory[F].getLogger

  extension [A](fa: F[Option[A]])
    def orNotFound(entity: String, id: String): F[A] =
      fa.flatMap {
        case Some(a) => a.pure
        case None => AppError.NotFound(entity, id).raiseError[F, A]
      }

  def serviceHandler[A](fa: F[A]): F[A] =
    fa.handleErrorWith {
      case app: AppError => app.raiseError
      case other =>
        logger.error(other)("Unexpected service-error occurred") *>
          AppError.InternalError("Unexpected error", other.some).raiseError
    }
