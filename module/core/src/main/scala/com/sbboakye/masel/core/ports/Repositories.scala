package com.sbboakye.masel.core.ports

import cats.data.EitherT
import com.sbboakye.masel.core.domain.dto.{CreateChallengeRequest, CreateSubmissionRequest}
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId, ChallengeUpdate, Submission, SubmissionId, SubmissionUpdate}
import com.sbboakye.masel.core.errors.AppError
import fs2.Stream

trait ChallengeRepository[F[_]]:
  def findAll(limit: Int, offset: Int): Stream[F, Challenge]
  def findById(id: ChallengeId): EitherT[F, AppError, Challenge]
  def create(challenge: CreateChallengeRequest): EitherT[F, AppError, Challenge]
  def update(challenge: ChallengeUpdate): EitherT[F, AppError, Challenge]
  def delete(id: ChallengeId): EitherT[F, AppError, Boolean]

trait SubmissionRepository[F[_]]:
  def findAll(limit: Int, offset: Int): Stream[F, Submission]
  def findById(id: SubmissionId): EitherT[F, AppError, Submission]
  def create(submission: CreateSubmissionRequest): EitherT[F, AppError, Submission]
  def update(submission: SubmissionUpdate): EitherT[F, AppError, Submission]
  def delete(id: SubmissionId): EitherT[F, AppError, Boolean]
