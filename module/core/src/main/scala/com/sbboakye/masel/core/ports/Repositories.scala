package com.sbboakye.masel.core.ports

import com.sbboakye.masel.core.domain.{Challenge, ChallengeId, ChallengeUpdate, Submission, SubmissionId, SubmissionUpdate}
import fs2.Stream

trait ChallengeRepository[F[_]]:
  def findAll(limit: Int, offset: Int): Stream[F, Challenge]
  def findById(id: ChallengeId): F[Option[Challenge]]
  def create(challenge: Challenge): F[Challenge]
  def update(challenge: ChallengeUpdate): F[Option[Challenge]]
  def delete(id: ChallengeId): F[Int]

trait SubmissionRepository[F[_]]:
  def findAll(limit: Int, offset: Int): Stream[F, Submission]
  def findById(id: SubmissionId): F[Option[Submission]]
  def create(submission: Submission): F[Submission]
  def update(submission: SubmissionUpdate): F[Option[Submission]]
  def delete(id: SubmissionId): F[Int]
