package com.sbboakye.masel.core.domain

import ciris.Secret
import com.comcast.ip4s.{Host, Port}

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
