package com.sbboakye.masel.core.errors

sealed trait AppError extends Throwable

object AppError:
  case class NotFound(entity: String, id: String) extends Exception(s"$entity not found: $id") with AppError

  case class ValidationFailed(errors: List[String])
      extends Exception(s"Validation failed: ${errors.mkString(", ")}")
      with AppError

  case class InternalError(message: String, cause: Option[Throwable] = None)
      extends Exception(message, cause.orNull)
      with AppError
