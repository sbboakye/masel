package com.sbboakye.masel.persistence.repositories

sealed trait RepositoryError extends Throwable

object RepositoryError:
  case object DatabaseError extends RepositoryError
