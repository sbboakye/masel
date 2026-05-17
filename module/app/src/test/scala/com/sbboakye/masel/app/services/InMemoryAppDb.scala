package com.sbboakye.masel.app.services

import cats.effect.{Concurrent, Ref}
import cats.syntax.all.*
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId, Submission, SubmissionId}
import com.sbboakye.masel.core.ports.{AppDb, ChallengeRepository, Repos, SubmissionRepository}

final class InMemoryAppDb[F[_]: Concurrent](
    val challengesRef: Ref[F, Map[ChallengeId, Challenge]],
    val submissionsRef: Ref[F, Map[SubmissionId, Submission]],
) extends AppDb[F]:

  private val challengeRepo: ChallengeRepository[F] = new ChallengeRepository[F]:
    override def findAll(limit: Int, offset: Int): F[List[Challenge]] =
      challengesRef.get.map(m => m.values.toList.sortBy(_.createdAt.toInstant).slice(offset, offset + limit))

    override def findById(id: ChallengeId): F[Option[Challenge]] =
      challengesRef.get.map(_.get(id))

    override def create(challenge: Challenge): F[Challenge] =
      challengesRef.update(_ + (challenge.id -> challenge)).as(challenge)

    override def update(challenge: Challenge): F[Option[Challenge]] =
      challengesRef.modify { m =>
        if m.contains(challenge.id) then (m + (challenge.id -> challenge), Some(challenge))
        else (m, None)
      }

    override def delete(id: ChallengeId): F[Boolean] =
      challengesRef.modify(m => if m.contains(id) then (m - id, true) else (m, false))

  private val submissionRepo: SubmissionRepository[F] = new SubmissionRepository[F]:
    override def findAll(limit: Int, offset: Int): F[List[Submission]] =
      submissionsRef.get.map(m => m.values.toList.sortBy(_.createdAt.toInstant).slice(offset, offset + limit))

    override def findById(id: SubmissionId): F[Option[Submission]] =
      submissionsRef.get.map(_.get(id))

    override def create(submission: Submission): F[Submission] =
      submissionsRef.update(_ + (submission.id -> submission)).as(submission)

    override def update(submission: Submission): F[Option[Submission]] =
      submissionsRef.modify { m =>
        if m.contains(submission.id) then (m + (submission.id -> submission), Some(submission))
        else (m, None)
      }

    override def delete(id: SubmissionId): F[Boolean] =
      submissionsRef.modify(m => if m.contains(id) then (m - id, true) else (m, false))

  private val theRepos: Repos[F] = new Repos[F]:
    override val challenges: ChallengeRepository[F] = challengeRepo
    override val submissions: SubmissionRepository[F] = submissionRepo

  override def withSession[A](use: Repos[F] => F[A]): F[A] = use(theRepos)
  override def withTransaction[A](use: Repos[F] => F[A]): F[A] = use(theRepos)
  override def isReady: F[Boolean] = true.pure[F]

  def seedChallenge(challenge: Challenge): F[Unit] =
    challengesRef.update(_ + (challenge.id -> challenge))

  def seedSubmission(submission: Submission): F[Unit] =
    submissionsRef.update(_ + (submission.id -> submission))

object InMemoryAppDb:
  def make[F[_]: Concurrent]: F[InMemoryAppDb[F]] =
    for {
      challenges <- Ref.of[F, Map[ChallengeId, Challenge]](Map.empty)
      submissions <- Ref.of[F, Map[SubmissionId, Submission]](Map.empty)
    } yield new InMemoryAppDb[F](challenges, submissions)

/**
 * AppDb whose every repository call raises the supplied throwable. Used to verify that services correctly propagate
 * AppErrors and wrap arbitrary throwables as InternalError.
 */
object FailingAppDb:
  def apply[F[_]: Concurrent](error: Throwable): AppDb[F] = new AppDb[F]:
    private def boom[A]: F[A] = error.raiseError[F, A]

    private val failingRepos: Repos[F] = new Repos[F]:
      override val challenges: ChallengeRepository[F] = new ChallengeRepository[F]:
        override def findAll(limit: Int, offset: Int): F[List[Challenge]] = boom
        override def findById(id: ChallengeId): F[Option[Challenge]] = boom
        override def create(challenge: Challenge): F[Challenge] = boom
        override def update(challenge: Challenge): F[Option[Challenge]] = boom
        override def delete(id: ChallengeId): F[Boolean] = boom

      override val submissions: SubmissionRepository[F] = new SubmissionRepository[F]:
        override def findAll(limit: Int, offset: Int): F[List[Submission]] = boom
        override def findById(id: SubmissionId): F[Option[Submission]] = boom
        override def create(submission: Submission): F[Submission] = boom
        override def update(submission: Submission): F[Option[Submission]] = boom
        override def delete(id: SubmissionId): F[Boolean] = boom

    override def withSession[A](use: Repos[F] => F[A]): F[A] = use(failingRepos)
    override def withTransaction[A](use: Repos[F] => F[A]): F[A] = use(failingRepos)
    override def isReady: F[Boolean] = false.pure[F]
