package com.sbboakye.masel.app

import cats.*
import cats.syntax.all.*
import ciris.*
import ciris.http4s.*
import cats.effect.Async
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.ciris.given
import io.github.iltotore.iron.cats.given
import com.comcast.ip4s.*

type NonEmptyString = String :| Not[Empty]

type DatabasePassword = String :| MinLength[8]

case class DatabaseConfig(
                           jdbcUrl: NonEmptyString,
                           host: Host,
                           port: Port,
                           username: NonEmptyString,
                           dbName: NonEmptyString,
                           password: Secret[DatabasePassword],
                           maxPoolSize: Int
                         )

case class ServerConfig(
                         host: Host,
                         port: Port
                       )

case class AppConfig(
                    database: DatabaseConfig,
                     server: ServerConfig
                    )

object AppConfig:
  def loadF[F[_]: Async]: F[AppConfig] =
    val databaseConfig: ConfigValue[Effect, DatabaseConfig] = {
      (
        env("DB_URL").as[NonEmptyString].default("jdbc:postgresql://localhost:5432/masel"),
        env("DB_HOST").as[Host].default(ipv4"127.0.0.1"),
        env("DB_PORT").as[Port].default(port"5432"),
        env("DB_USERNAME").as[NonEmptyString].default("masel"),
        env("DB_NAME").as[NonEmptyString].default("masel"),
        env("DB_PASSWORD").as[DatabasePassword].secret.redacted,
        env("DB_MAX_POOL_SIZE").as[Int].default(10)
      ).parMapN(DatabaseConfig.apply)
    }

    val serverConfig: ConfigValue[Effect, ServerConfig] =
      (
        env("SERVER_HOST").as[Host].default(ipv4"0.0.0.0"),
        env("SERVER_PORT").as[Port].default(port"8080")
      ).parMapN(ServerConfig.apply)

    val config: ConfigValue[Effect, AppConfig] =
      (
        databaseConfig,
        serverConfig
      ).parMapN(AppConfig.apply)

    config.load[F]