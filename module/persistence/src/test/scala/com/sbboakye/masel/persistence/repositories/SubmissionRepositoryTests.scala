package com.sbboakye.masel.persistence.repositories

import cats.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{Clock, IO}
import com.sbboakye.masel.core.domain.{QuerySql, Submission, SubmissionId}
import com.sbboakye.masel.persistence.{CoreFixture, CoreSpec}
import io.github.iltotore.iron.autoRefine
import java.time.ZoneOffset
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

class SubmissionRepositoryTests extends AsyncFreeSpec with AsyncIOSpec with Matchers with CoreSpec with CoreFixture:
  override val initSqlString: String = "db/migration/V1__initial_schema.sql"

  "SubmissionRepository" - {
    "findAll" - {
      "should return an empty stream with with no submissions" in
        poolSession.use { pool =>
          pool.use { session =>
            val repo = SkunkSubmissionRepository[IO](session)
            val result = repo.findAll(10, 0)
            result.asserting(_ shouldBe empty)
          }
        }

      "should return a stream with at least one submission" in
        poolSession.use { pool =>
          pool.use { session =>
            session.transaction.use { xa =>
              val submissionRepo = SkunkSubmissionRepository[IO](session)
              val challengeRepo = SkunkChallengeRepository[IO](session)
              val result = for {
                challengeOne <- challengeIOOne
                challengeTwo <- challengeIOTwo
                _ <- challengeRepo.create(challengeOne)
                _ <- challengeRepo.create(challengeTwo)
                submissionOne <- submissionIO(challengeOne.id)
                submissionTwo <- submissionWithScoreIO(challengeTwo.id)
                _ <- submissionRepo.create(submissionOne)
                _ <- submissionRepo.create(submissionTwo)
                list <- submissionRepo.findAll(10, 0)
              } yield list
              result.asserting(_ should not be empty)
            }
          }
        }
    }

    "findById" - {
      "should return None if submission does not exist" in
        poolSession.use { pool =>
          pool.use { session =>
            session.transaction.use { xa =>
              val submissionRepo = SkunkSubmissionRepository[IO](session)
              val challengeRepo = SkunkChallengeRepository[IO](session)
              val result = for {
                challengeOne <- challengeIOOne
                _ <- challengeRepo.create(challengeOne)
                submissionOne <- submissionIO(challengeOne.id)
                _ <- submissionRepo.create(submissionOne)
                randomId <- SubmissionId.generate[IO]
                find <- submissionRepo.findById(randomId)
              } yield find
              result.asserting(_ shouldBe None)
            }
          }
        }

      "should return Some(Submission) if submission exists" in
        poolSession.use { pool =>
          pool.use { session =>
            session.transaction.use { xa =>
              val submissionRepo = SkunkSubmissionRepository[IO](session)
              val challengeRepo = SkunkChallengeRepository[IO](session)
              for {
                challengeOne <- challengeIOOne
                _ <- challengeRepo.create(challengeOne)
                submission <- submissionIO(challengeOne.id)
                created <- submissionRepo.create(submission)
                find <- submissionRepo.findById(created.id)
              } yield find shouldBe Some(submission)
            }
          }
        }
    }

    "create" - {
      "should return a valid submission" in
        poolSession.use { pool =>
          pool.use { session =>
            session.transaction.use { xa =>
              val submissionRepo = SkunkSubmissionRepository[IO](session)
              val challengeRepo = SkunkChallengeRepository[IO](session)
              for {
                challengeOne <- challengeIOOne
                _ <- challengeRepo.create(challengeOne)
                submission <- submissionIO(challengeOne.id)
                result <- submissionRepo.create(submission)
              } yield result shouldBe submission
            }
          }
        }
    }

    "update" - {
      "should return None if submission to be updated does not exist" in
        poolSession.use { pool =>
          pool.use { session =>
            session.transaction.use { xa =>
              val submissionRepo = SkunkSubmissionRepository[IO](session)
              val challengeRepo = SkunkChallengeRepository[IO](session)
              for {
                challengeOne <- challengeIOOne
                _ <- challengeRepo.create(challengeOne)
                submission <- submissionIO(challengeOne.id)
                _ <- submissionRepo.create(submission)
                id <- SubmissionId.generate[IO]
                now <- Clock[IO].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
                submissionUpdate = Submission(
                  id = id,
                  challengeId = challengeOne.id,
                  candidateSolution = QuerySql("x + x = y + x"),
                  output = None,
                  score = None,
                  createdAt = now,
                  updatedAt = now,
                )
                updated <- submissionRepo.update(submissionUpdate)
              } yield updated shouldBe None
            }
          }
        }

      "should return true if submission to be updated does exist" in
        poolSession.use { pool =>
          pool.use { session =>
            session.transaction.use { xa =>
              val submissionRepo = SkunkSubmissionRepository[IO](session)
              val challengeRepo = SkunkChallengeRepository[IO](session)
              for {
                challengeOne <- challengeIOOne
                _ <- challengeRepo.create(challengeOne)
                submission <- submissionIO(challengeOne.id)
                _ <- submissionRepo.create(submission)
                now <- Clock[IO].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
                submissionUpdate = submission.copy(
                  id = submission.id,
                  challengeId = challengeOne.id,
                  candidateSolution = QuerySql("x + x = y + x"),
                  output = None,
                  score = None,
                  updatedAt = now,
                )
                updated <- submissionRepo.update(submissionUpdate)
              } yield updated.isDefined shouldBe true
            }
          }
        }
    }

    "delete" - {
      "should return false if submission to be deleted does not exist" in
        poolSession.use { pool =>
          pool.use { session =>
            session.transaction.use { xa =>
              val submissionRepo = SkunkSubmissionRepository[IO](session)
              val challengeRepo = SkunkChallengeRepository[IO](session)
              for {
                challengeOne <- challengeIOOne
                _ <- challengeRepo.create(challengeOne)
                submission <- submissionIO(challengeOne.id)
                _ <- submissionRepo.create(submission)
                randomId <- SubmissionId.generate[IO]
                deleted <- submissionRepo.delete(randomId)
              } yield deleted shouldBe false
            }
          }
        }

      "should return true if submission to be deleted does exist" in
        poolSession.use { pool =>
          pool.use { session =>
            session.transaction.use { xa =>
              val submissionRepo = SkunkSubmissionRepository[IO](session)
              val challengeRepo = SkunkChallengeRepository[IO](session)
              for {
                challengeOne <- challengeIOOne
                _ <- challengeRepo.create(challengeOne)
                submission <- submissionIO(challengeOne.id)
                created <- submissionRepo.create(submission)
                deleted <- submissionRepo.delete(created.id)
              } yield deleted shouldBe true
            }
          }
        }
    }
  }
