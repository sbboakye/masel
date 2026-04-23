package com.sbboakye.masel.core.errors

import java.util.UUID

sealed trait AppError

object AppError:
  final case class NotFound(entity: String, id: UUID)
    extends Exception(s"$entity with id $id not found")
      with AppError

  final case class ValidationFailed(errors: List[String])
    extends Exception(s"Validation failed: ${errors.mkString(", ")}")
      with AppError

  final case class InternalError(message: String, cause: Option[Throwable])
    extends Exception(s"Internal error: $message", cause.orNull)
      with AppError