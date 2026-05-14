package com.sbboakye.masel.core.errors

sealed trait AppError extends RuntimeException:
  override def getMessage: String = this match
    case AppError.NotFound(entity, id) => s"$entity with id $id not found"
    case AppError.ValidationFailed(errors) => s"Validation failed: ${errors.mkString(", ")}"
    case AppError.InternalError(message, _) => message

object AppError:
  case class NotFound(entity: String, id: String) extends AppError
  case class ValidationFailed(errors: List[String]) extends AppError
  case class InternalError(message: String, cause: Option[Throwable] = None) extends AppError {
    cause.foreach(initCause)
  }
