package com.sbboakye.masel.core.errors

import java.util.UUID

enum AppError extends Throwable:
  case NotFound(entity: String, id: UUID)
  case ValidationFailed(errors: List[String])
  case InternalError(message: String, cause: Option[Throwable])
