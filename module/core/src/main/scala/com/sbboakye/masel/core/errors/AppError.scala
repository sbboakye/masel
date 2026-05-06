package com.sbboakye.masel.core.errors

enum AppError extends Throwable:
  case NotFound(entity: String, id: String)
  case ValidationFailed(errors: List[String])
  case InternalError(message: String, cause: Option[Throwable])
