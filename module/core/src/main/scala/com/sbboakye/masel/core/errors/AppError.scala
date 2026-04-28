package com.sbboakye.masel.core.errors

sealed trait AppError

object AppError:
  final case class NotFound(message: String) extends AppError

  final case class ValidationFailed(errors: List[String]) extends AppError

  final case class InternalError(message: String, cause: Option[Throwable]) extends AppError