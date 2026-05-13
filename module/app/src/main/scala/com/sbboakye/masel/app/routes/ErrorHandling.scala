package com.sbboakye.masel.app.routes

import cats.MonadThrow
import cats.syntax.all.*
import com.sbboakye.masel.core.errors.AppError
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax.*
import org.http4s.Response
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDsl

final case class ErrorResponse(message: String)

object ErrorResponse:
  given Encoder[ErrorResponse] = deriveEncoder

object ErrorHandling:

  def mapError[F[_]: MonadThrow](dsl: Http4sDsl[F])(e: Throwable): F[Response[F]] =
    import dsl.*
    e match
      case AppError.NotFound(entity, id) =>
        NotFound(ErrorResponse(s"$entity not found: $id"))
      case AppError.ValidationFailed(errors) =>
        BadRequest(ErrorResponse(s"Validation failed: ${errors.mkString(", ")}"))
      case AppError.InternalError(message, _) =>
        InternalServerError(ErrorResponse(message))
      case other =>
        InternalServerError(ErrorResponse(Option(other.getMessage).getOrElse("Internal server error")))

  extension [F[_]: MonadThrow](fa: F[Response[F]])
    def recoverAppErrors(dsl: Http4sDsl[F]): F[Response[F]] =
      fa.handleErrorWith(mapError(dsl))
