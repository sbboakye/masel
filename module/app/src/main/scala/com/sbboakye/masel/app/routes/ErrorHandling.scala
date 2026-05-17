package com.sbboakye.masel.app.routes

import cats.MonadThrow
import cats.syntax.all.*
import com.sbboakye.masel.core.errors.AppError
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDsl
import org.http4s.{
  InvalidMessageBodyFailure,
  MalformedMessageBodyFailure,
  MediaTypeMismatch,
  MediaTypeMissing,
  Response,
}
import org.typelevel.log4cats.LoggerFactory

final case class ErrorResponse(message: String)

object ErrorResponse:
  given Encoder[ErrorResponse] = deriveEncoder

object ErrorHandling:

  private def mapError[F[_]: {MonadThrow, LoggerFactory}](
      dsl: Http4sDsl[F],
  )(e: Throwable): F[Response[F]] =
    import dsl.*
    val logger = LoggerFactory[F].getLogger
    e match
      case AppError.NotFound(_, _) =>
        NotFound(ErrorResponse(e.getMessage))
      case AppError.ValidationFailed(errors) =>
        BadRequest(ErrorResponse(s"Validation failed: ${errors.mkString(", ")}"))
      case AppError.InternalError(message, cause) =>
        cause.traverse_(c => logger.error(c)(s"InternalError: $message")) *>
          InternalServerError(ErrorResponse(message))
      case _: MalformedMessageBodyFailure =>
        BadRequest(ErrorResponse(e.getMessage))
      case _: InvalidMessageBodyFailure =>
        UnprocessableContent(ErrorResponse(e.getMessage))
      case _: MediaTypeMissing | _: MediaTypeMismatch =>
        UnsupportedMediaType(ErrorResponse(e.getMessage))
      case other =>
        logger.error(other)("Unexpected error while serving request") *>
          InternalServerError(ErrorResponse("Internal server error"))

  extension [F[_]: {MonadThrow, LoggerFactory}](fa: F[Response[F]])
    def recoverAppErrors(dsl: Http4sDsl[F]): F[Response[F]] =
      fa.handleErrorWith(mapError(dsl))
