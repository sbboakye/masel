package com.sbboakye.masel.persistence.session

import cats.effect.std.Console
import cats.effect.{Async, Resource}
import org.typelevel.otel4s.metrics.Meter.Implicits.noop
import org.typelevel.otel4s.trace.Tracer.Implicits.noop
import skunk.Session
import skunk.Session.Credentials

case class PoolSession[F[_]: {Async, Console}](
    host: String,
    port: Int,
    database: String,
    username: String,
    password: String,
    maxPoolSize: Int,
):
  val poolSession: Resource[F, Resource[F, Session[F]]] = Session
    .Builder[F]
    .withHost(host)
    .withPort(port)
    .withDatabase(database)
    .withCredentials(
      Credentials(
        user = username,
        password = Some(password),
      ),
    )
    .pooled(maxPoolSize)
