package com.sbboakye.masel.persistence.repositories

import cats.*
import cats.effect.IO
import cats.effect.syntax.all.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.*
import com.sbboakye.masel.persistence.{CoreFixture, CoreSpec}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

class ChallengeRepositoryTests extends AsyncFreeSpec with AsyncIOSpec with Matchers with CoreSpec with CoreFixture:
  override val initSqlString: String = "db/migration/V1__initial_schema.sql"
  val additionalScript: String = "challenges.sql"

  "ChallengeRepository" - {
    "findAll" - {
      "should return an empty stream when no challenges exist" in
        poolSession.use { pool =>
          val repo = SkunkChallengeRepository[IO](pool)
          val stream = repo.findAll(10, 0)
          val result = stream.compile.toList
          result.asserting(_ shouldBe empty)
        }

      "should return a stream with at least one challenge" in
        poolSession.use { pool =>
          val repo = SkunkChallengeRepository[IO](pool)
          val result = for {
            challengeOne <- challengeIOOne
            challengeTwo <- challengeIOTwo
            _ <- repo.create(challengeOne)
            _ <- repo.create(challengeTwo)
            list <- repo.findAll(10, 0).compile.toList
            _ <- IO.println(s"List of challenges: $list")
          } yield list
          result.asserting(_ should not be empty)
        }
    }

    "create" - {
      "should return a valid challenge" in
        poolSession.use { pool =>
          val repo = SkunkChallengeRepository[IO](pool)
          for {
            challenge <- challengeIOOne
            result <- repo.create(challenge)
          } yield result shouldBe challenge
        }
    }
  }
