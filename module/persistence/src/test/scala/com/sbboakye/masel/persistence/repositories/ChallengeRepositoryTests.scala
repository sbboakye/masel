package com.sbboakye.masel.persistence.repositories

import cats.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{Clock, IO}
import com.sbboakye.masel.core.domain.ChallengeDifficulty.Hard
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId, ChallengeStatus, QuerySql, SetupSql}
import com.sbboakye.masel.persistence.{CoreFixture, CoreSpec}
import io.github.iltotore.iron.autoRefine
import java.time.ZoneOffset
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

class ChallengeRepositoryTests extends AsyncFreeSpec with AsyncIOSpec with Matchers with CoreSpec with CoreFixture:
  override val initSqlString: String = "db/migration/V1__initial_schema.sql"

  "ChallengeRepository" - {
    "findAll" - {
      "should return an empty stream when no challenges exist" in
        poolSession.use { pool =>
          pool.use { session =>
            val repo = SkunkChallengeRepository[IO](session)
            val result = repo.findAll(10, 0)
            result.asserting(_ shouldBe empty)
          }
        }

      "should return a stream with at least one challenge" in
        poolSession.use { pool =>
          pool.use { session =>
            session.transaction.use { xa =>
              val repo = SkunkChallengeRepository[IO](session)
              val result = for {
                challengeOne <- challengeIOOne
                challengeTwo <- challengeIOTwo
                _ <- repo.create(challengeOne)
                _ <- repo.create(challengeTwo)
                list <- repo.findAll(10, 0)
              } yield list
              result.asserting(_ should not be empty)
            }
          }
        }
    }

    "findById" - {
      "should return None if challenge does not exist" in
        poolSession.use { pool =>
          pool.use { session =>
            session.transaction.use { xa =>
              val repo = SkunkChallengeRepository[IO](session)
              val result = for {
                challenge <- challengeIOOne
                created <- repo.create(challenge)
                randomId <- ChallengeId.generate[IO]
                find <- repo.findById(randomId)
              } yield find
              result.asserting(_ shouldBe None)
            }
          }
        }

      "should return Some(Challenge) if challenge exists" in
        poolSession.use { pool =>
          pool.use { session =>
            session.transaction.use { xa =>
              val repo = SkunkChallengeRepository[IO](session)
              for {
                challenge <- challengeIOOne
                created <- repo.create(challenge)
                find <- repo.findById(created.id)
              } yield find shouldBe Some(challenge)
            }
          }
        }
    }

    "create" - {
      "should return a valid challenge" in
        poolSession.use { pool =>
          pool.use { session =>
            val repo = SkunkChallengeRepository[IO](session)
            for {
              challenge <- challengeIOOne
              result <- repo.create(challenge)
            } yield result shouldBe challenge
          }
        }
    }

    "update" - {
      "should return None if challenge to be updated does not exist" in
        poolSession.use { pool =>
          pool.use { session =>
            session.transaction.use { xa =>
              val repo = SkunkChallengeRepository[IO](session)
              for {
                challenge <- challengeIOOne
                _ <- repo.create(challenge)
                randomId <- ChallengeId.generate[IO]
                now <- Clock[IO].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
                challengeUpdate = Challenge(
                  id = randomId,
                  title = "Sum of Three Squares",
                  instructions = "sum three squares",
                  setupSql = SetupSql("CREATE TABLE t (n INT);"),
                  status = ChallengeStatus.Draft,
                  expectedSolution = QuerySql("x + x + y = yx"),
                  output = None,
                  allottedTime = 600,
                  difficulty = Hard,
                  createdAt = now,
                  updatedAt = now,
                )
                updated <- repo.update(challengeUpdate)
              } yield updated shouldBe None
            }
          }
        }

      "should return true if challenge to be updated does exist" in
        poolSession.use { pool =>
          pool.use { session =>
            session.transaction.use { xa =>
              val repo = SkunkChallengeRepository[IO](session)
              for {
                challenge <- challengeIOOne
                _ <- repo.create(challenge)
                now <- Clock[IO].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
                challengeUpdate = challenge.copy(
                  id = challenge.id,
                  title = "Sum of Three Squares",
                  instructions = "sum three squares",
                  status = ChallengeStatus.Draft,
                  expectedSolution = QuerySql("x + x + y = yx"),
                  output = None,
                  allottedTime = 600,
                  difficulty = Hard,
                  updatedAt = now,
                )
                updated <- repo.update(challengeUpdate)
              } yield updated.isDefined shouldBe true
            }
          }
        }
    }

    "delete" - {
      "should return false if challenge to be deleted does not exist" in
        poolSession.use { pool =>
          pool.use { session =>
            session.transaction.use { xa =>
              val repo = SkunkChallengeRepository[IO](session)
              for {
                challenge <- challengeIOOne
                _ <- repo.create(challenge)
                randomId <- ChallengeId.generate[IO]
                deleted <- repo.delete(randomId)
              } yield deleted shouldBe false
            }
          }
        }

      "should return true if challenge to be deleted exist" in
        poolSession.use { pool =>
          pool.use { session =>
            session.transaction.use { xa =>
              val repo = SkunkChallengeRepository[IO](session)
              for {
                challenge <- challengeIOOne
                _ <- repo.create(challenge)
                updated <- repo.delete(challenge.id)
              } yield updated shouldBe true
            }
          }
        }
    }
  }
