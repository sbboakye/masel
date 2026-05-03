package com.sbboakye.masel.core.ports

import com.sbboakye.masel.core.domain.{Challenge, ChallengeId, Submission, SubmissionId}

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
