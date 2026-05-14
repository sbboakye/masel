package com.sbboakye.masel.core.ports

import cats.MonadThrow
import cats.effect.syntax.all.*
import cats.syntax.all.*
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId, Submission, SubmissionId}
import com.sbboakye.masel.core.errors.AppError

trait ChallengeRepository[F[_]]:
  def findAll(limit: Int, offset: Int): F[List[Challenge]]
  def findById(id: ChallengeId): F[Option[Challenge]]
  def create(challenge: Challenge): F[Challenge]
  def update(challenge: Challenge): F[Option[Challenge]]
  def delete(id: ChallengeId): F[Boolean]

trait SubmissionRepository[F[_]]:
  def findAll(limit: Int, offset: Int): F[List[Submission]]
  def findById(id: SubmissionId): F[Option[Submission]]
  def create(submission: Submission): F[Submission]
  def update(submission: Submission): F[Option[Submission]]
  def delete(id: SubmissionId): F[Boolean]

trait Repos[F[_]]:
  val challenges: ChallengeRepository[F]
  val submissions: SubmissionRepository[F]

trait AppDb[F[_]: MonadThrow]:
  def withSession[A](use: Repos[F] => F[A]): F[A]
  def withTransaction[A](use: Repos[F] => F[A]): F[A]
  def isReady: F[Boolean]
