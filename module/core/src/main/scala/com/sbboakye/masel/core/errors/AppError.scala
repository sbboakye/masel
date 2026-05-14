package com.sbboakye.masel.core.errors

sealed trait AppError extends Throwable

object AppError:
  case class NotFound(entity: String, id: String) extends AppError

  case class ValidationFailed(errors: List[String]) extends AppError

  case object InternalError extends AppError
