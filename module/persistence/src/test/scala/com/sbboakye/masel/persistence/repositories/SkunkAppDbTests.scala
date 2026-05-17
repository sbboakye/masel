package com.sbboakye.masel.persistence.repositories

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.sbboakye.masel.persistence.session.PoolSession
import com.sbboakye.masel.persistence.{CoreFixture, CoreSpec}
import io.github.iltotore.iron.autoRefine
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

class SkunkAppDbTests extends AsyncFreeSpec with AsyncIOSpec with Matchers with CoreSpec with CoreFixture:

  override val initSqlString: String = "db/migration/V1__initial_schema.sql"

  "SkunkAppDb.isReady" - {
    "returns true when the database is reachable" in
      poolSession.use(pool => SkunkAppDb.make[IO](pool).isReady.asserting(_ shouldBe true))

    "returns false when the database is NOT reachable" in
      PoolSession
        .make[IO](
          host = "127.0.0.1",
          port = 1, // privileged port nothing is listening on
          database = "nope",
          username = "nope",
          password = "nope",
          maxPoolSize = 1,
        )
        .use(pool => SkunkAppDb.make[IO](pool).isReady.asserting(_ shouldBe false))
  }

  "SkunkAppDb.withSession" - {
    "executes the body and returns its value" in
      poolSession.use { pool =>
        val db = SkunkAppDb.make[IO](pool)
        db.withSession(repos => repos.challenges.findAll(10, 0)).asserting(_ shouldBe empty)
      }

    "exposes both the challenges and submissions repositories" in
      poolSession.use { pool =>
        val db = SkunkAppDb.make[IO](pool)
        db.withSession { repos =>
          for {
            cs <- repos.challenges.findAll(10, 0)
            ss <- repos.submissions.findAll(10, 0)
          } yield (cs, ss)
        }.asserting { case (cs, ss) =>
          cs shouldBe empty
          ss shouldBe empty
        }
      }
  }

  "SkunkAppDb.withTransaction" - {
    "commits the work on success (challenge persists after the transaction returns)" in
      poolSession.use { pool =>
        val db = SkunkAppDb.make[IO](pool)
        for {
          challenge <- challengeIOOne
          _ <- db.withTransaction(_.challenges.create(challenge))
          found <- db.withSession(_.challenges.findById(challenge.id))
        } yield found shouldBe Some(challenge)
      }

    "rolls back when the body raises (no challenge persists after the failure)" in
      poolSession.use { pool =>
        val db = SkunkAppDb.make[IO](pool)
        for {
          challenge <- challengeIOOne
          result <- db.withTransaction { repos =>
            repos.challenges.create(challenge) *>
              IO.raiseError[Unit](new RuntimeException("force rollback"))
          }.attempt
          found <- db.withSession(_.challenges.findById(challenge.id))
        } yield {
          result.isLeft shouldBe true
          found shouldBe None
        }
      }
  }
