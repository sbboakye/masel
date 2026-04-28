package com.sbboakye.masel.persistence.repositories

import cats.*
import cats.effect.IO
import cats.effect.syntax.all.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.*
import com.sbboakye.masel.persistence.CoreSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

class ChallengeRepositoryTests extends AsyncFreeSpec with AsyncIOSpec with Matchers with CoreSpec:
  override val initSqlString: String = "db/migration/V1__initial_schema.sql"
  val additionalScript: String = ""

  "ChallengeRepository" - {
    "findAll" - {
      "should return an empty stream when no challenges exist" in
        poolSession.use { pool =>
          val repo = SkunkChallengeRepository[IO](pool)
          val stream = repo.findAll(0, 0)
          val result = stream.compile.toList
          result.asserting(_ shouldBe empty)
        }
    }
  }
