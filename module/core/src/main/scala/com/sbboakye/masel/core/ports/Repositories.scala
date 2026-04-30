package com.sbboakye.masel.core.ports

import com.sbboakye.masel.core.domain.dto.{UpdateChallengeRequest, UpdateSubmissionRequest}
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId, Submission, SubmissionId}
import fs2.Stream

trait ChallengeRepository[F[_]]:
  def findAll(limit: Int, offset: Int): Stream[F, Challenge]
  def findById(id: ChallengeId): F[Option[Challenge]]
  def create(challenge: Challenge): F[Challenge]
  def update(challenge: UpdateChallengeRequest): F[Option[Boolean]]
  def delete(id: ChallengeId): F[Option[Boolean]]

trait SubmissionRepository[F[_]]:
  def findAll(limit: Int, offset: Int): Stream[F, Submission]
  def findById(id: SubmissionId): F[Option[Submission]]
  def create(submission: Submission): F[Submission]
  def update(submission: UpdateSubmissionRequest): F[Option[Boolean]]
  def delete(id: SubmissionId): F[Option[Boolean]]
