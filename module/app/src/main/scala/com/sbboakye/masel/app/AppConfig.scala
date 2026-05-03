package com.sbboakye.masel.app

import cats.*
import cats.effect.Async
import cats.syntax.all.*
import ciris.*
import ciris.http4s.*
import com.comcast.ip4s.*
import com.sbboakye.masel.core.domain.{DatabasePassword, NonEmptyString}
import io.github.iltotore.iron.autoRefine
import io.github.iltotore.iron.cats.given
import io.github.iltotore.iron.ciris.given

case class DatabaseConfig(
    host: Host,
    port: Port,
    username: NonEmptyString,
    dbName: NonEmptyString,
    password: Secret[DatabasePassword],
    maxPoolSize: Int,
):
  val jdbcUrl: String = s"jdbc:postgresql://${host.toString}:${port.toString}/$dbName"

case class ServerConfig(
    host: Host,
    port: Port,
)

case class AppConfig(
    database: DatabaseConfig,
    server: ServerConfig,
)

object AppConfig:
  def loadF[F[_]: Async]: F[AppConfig] =
    val databaseConfig: ConfigValue[Effect, DatabaseConfig] =
      (
        env("DB_HOST").as[Host].default(ipv4"127.0.0.1"),
        env("DB_PORT").as[Port].default(port"5432"),
        env("DB_USERNAME").as[NonEmptyString].default("masel"),
        env("DB_NAME").as[NonEmptyString].default("masel"),
        env("DB_PASSWORD").as[DatabasePassword].secret.redacted,
        env("DB_MAX_POOL_SIZE").as[Int].default(10),
      ).parMapN(DatabaseConfig.apply)

    val serverConfig: ConfigValue[Effect, ServerConfig] =
      (
        env("SERVER_HOST").as[Host].default(ipv4"0.0.0.0"),
        env("SERVER_PORT").as[Port].default(port"8080"),
      ).parMapN(ServerConfig.apply)

    val config: ConfigValue[Effect, AppConfig] =
      (
        databaseConfig,
        serverConfig,
      ).parMapN(AppConfig.apply)

    config.load[F]
