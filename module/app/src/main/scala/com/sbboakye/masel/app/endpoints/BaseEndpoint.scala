package com.sbboakye.masel.app.endpoints

import com.sbboakye.masel.core.errors.AppError
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

case class ErrorResponse(message: String)

object ErrorResponse:
  given Encoder[ErrorResponse] = deriveEncoder
  given Decoder[ErrorResponse] = deriveDecoder
  given Schema[ErrorResponse] = Schema.derived

type ApiError = (StatusCode, ErrorResponse)

object BaseEndpoint:

  val base: Endpoint[Unit, Unit, ApiError, Unit, Any] =
    endpoint.in("api" / "v1").errorOut(statusCode.and(jsonBody[ErrorResponse]))

  def mapError(e: Throwable): ApiError = e match {
    case AppError.InternalError(message, _) => (StatusCode.InternalServerError, ErrorResponse(message))
    case AppError.NotFound(entity, id) => (StatusCode.NotFound, ErrorResponse(s"$entity not found: $id"))
    case AppError.ValidationFailed(errors) =>
      (StatusCode.BadRequest, ErrorResponse(s"Validation failed: ${errors.mkString(", ")}"))
  }
