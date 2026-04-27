package com.sbboakye.masel.core.errors

import java.util.UUID

sealed trait AppError

object AppError:
  final case class NotFound(entity: String, id: UUID) extends AppError

  final case class ValidationFailed(errors: List[String]) extends AppError

  final case class InternalError(message: String, cause: Option[Throwable]) extends AppError