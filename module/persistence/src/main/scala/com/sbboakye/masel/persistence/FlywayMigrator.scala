package com.sbboakye.masel.persistence

import cats.effect.Sync
import cats.syntax.all.*
import org.flywaydb.core.Flyway
import org.typelevel.log4cats.{Logger, LoggerFactory}

class FlywayMigrator[F[_]: {Sync, LoggerFactory}](
    jdbcUrl: String,
    username: String,
    password: String,
):
  private val logger: Logger[F] = LoggerFactory[F].getLogger

  def migrate(): F[Unit] =
    for {
      _ <- logger.info("Running flyway migrations...")
      count <- Sync[F].delay {
        Flyway
          .configure()
          .dataSource(jdbcUrl, username, password)
          .locations("db/migration")
          .load()
          .migrate()
          .migrationsExecuted
      }
      _ <- logger.info(s"Flyway applied $count migrations.")
    } yield ()
