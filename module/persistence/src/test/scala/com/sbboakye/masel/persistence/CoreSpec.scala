package com.sbboakye.masel.persistence

import cats.*
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import com.dimafeng.testcontainers.{JdbcDatabaseContainer, PostgreSQLContainer}
import com.sbboakye.masel.persistence.session.PoolSession
import org.testcontainers.utility.DockerImageName
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import skunk.*
import skunk.implicits.*

trait CoreSpec:
  val initSqlString: String

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val postgres: Resource[IO, PostgreSQLContainer] = {
    val acquire = IO {
      val container = PostgreSQLContainer.Def(
        dockerImageName = DockerImageName.parse("postgres:16.0-alpine"),
        databaseName = "test-database-cotnainer",
        username = "scala",
        password = "scala",
        commonJdbcParams = JdbcDatabaseContainer.CommonParams(initScriptPath = Option(initSqlString)),
      )
      container.start()
    }
    val release = (container: PostgreSQLContainer) => IO(container.stop())
    Resource.make(acquire)(release)
  }

  val poolSession: Resource[IO, Resource[IO, Session[IO]]] = for {
    db <- postgres
    pool <- PoolSession[IO](
      host = db.containerIpAddress,
      port = db.mappedPort(5432),
      database = db.databaseName,
      username = db.username,
      password = db.password,
      maxPoolSize = 1,
    ).poolSession
  } yield pool

//  def executeSqlScript(scriptPath: String)(using pool: Resource[IO, Session[IO]]): IO[Unit] =
//    val script = scala.io.Source.fromResource(scriptPath).mkString
//    val statements = script.split(";").filter(_.trim.nonEmpty)
//    statements.toList.traverse { sqlScript =>
//      val sqlCommand = sql"$sqlScript".command
//      pool.use { session =>
//        session.prepare(sqlCommand)
//      }
//    }
//    IO.unit
