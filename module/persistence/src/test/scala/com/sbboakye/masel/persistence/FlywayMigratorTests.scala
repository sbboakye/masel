package com.sbboakye.masel.persistence

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, Resource}
import com.dimafeng.testcontainers.PostgreSQLContainer
import java.sql.DriverManager
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.utility.DockerImageName
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

class FlywayMigratorTests extends AsyncFreeSpec with AsyncIOSpec with Matchers:

  given LoggerFactory[IO] = Slf4jFactory.create[IO]

  /** A Postgres container with NO init script, so the migrator has real work to do. */
  private val cleanPostgres: Resource[IO, PostgreSQLContainer] = {
    val acquire = IO {
      val container = PostgreSQLContainer.Def(
        dockerImageName = DockerImageName.parse("postgres:16.0-alpine"),
        databaseName = "migrator-test-db",
        username = "scala",
        password = "scala",
      )
      container.start()
    }
    Resource.make(acquire)(c => IO(c.stop()))
  }

  private def tableExists(c: PostgreSQLContainer, name: String): IO[Boolean] =
    IO.blocking {
      val conn = DriverManager.getConnection(c.jdbcUrl, c.username, c.password)
      try {
        val rs = conn
          .createStatement()
          .executeQuery(
            s"""SELECT 1 FROM information_schema.tables
               | WHERE table_schema = 'public' AND table_name = '$name'""".stripMargin,
          )
        rs.next()
      } finally conn.close()
    }

  private def schemaHistoryRowCount(c: PostgreSQLContainer): IO[Int] =
    IO.blocking {
      val conn = DriverManager.getConnection(c.jdbcUrl, c.username, c.password)
      try {
        val rs = conn.createStatement().executeQuery("SELECT count(*) FROM flyway_schema_history")
        rs.next()
        rs.getInt(1)
      } finally conn.close()
    }

  "FlywayMigrator" - {

    "creates the challenges and submissions tables in an empty database" in
      cleanPostgres.use { c =>
        for {
          _ <- new FlywayMigrator[IO](c.jdbcUrl, c.username, c.password).migrate()
          chall <- tableExists(c, "challenges")
          submis <- tableExists(c, "submissions")
        } yield {
          chall shouldBe true
          submis shouldBe true
        }
      }

    "records the migration in flyway_schema_history" in
      cleanPostgres.use { c =>
        for {
          _ <- new FlywayMigrator[IO](c.jdbcUrl, c.username, c.password).migrate()
          history <- tableExists(c, "flyway_schema_history")
          rows <- schemaHistoryRowCount(c)
        } yield {
          history shouldBe true
          rows should be > 0
        }
      }

    "is idempotent: a second migrate() call does not throw and the schema is intact" in
      cleanPostgres.use { c =>
        val migrator = new FlywayMigrator[IO](c.jdbcUrl, c.username, c.password)
        for {
          _ <- migrator.migrate()
          rowsAfterFirst <- schemaHistoryRowCount(c)
          _ <- migrator.migrate()
          rowsAfterSecond <- schemaHistoryRowCount(c)
          stillThere <- tableExists(c, "challenges")
        } yield {
          stillThere shouldBe true
          rowsAfterSecond shouldBe rowsAfterFirst
        }
      }

    "fails when the JDBC credentials are wrong" in
      cleanPostgres.use { c =>
        new FlywayMigrator[IO](c.jdbcUrl, c.username, "wrong-password")
          .migrate()
          .attempt
          .asserting(_.isLeft shouldBe true)
      }
  }
