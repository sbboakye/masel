package com.sbboakye.masel.core.ports

import cats.data.EitherT
import com.sbboakye.masel.core.domain.dto.{CreateChallengeRequest, CreateSubmissionRequest, UpdateChallengeRequest, UpdateSubmissionRequest}
import com.sbboakye.masel.core.domain.{Challenge, ChallengeId, Submission, SubmissionId}
import com.sbboakye.masel.core.errors.AppError
import fs2.Stream

trait ChallengeRepository[F[_]]:
  def findAll(limit: Int, offset: Int): Stream[F, Challenge]
  def findById(id: ChallengeId): EitherT[F, AppError, Challenge]
  def create(challenge: CreateChallengeRequest): EitherT[F, AppError, Challenge]
  def update(challenge: UpdateChallengeRequest): EitherT[F, AppError, Boolean]
  def delete(id: ChallengeId): EitherT[F, AppError, Boolean]

trait SubmissionRepository[F[_]]:
  def findAll(limit: Int, offset: Int): Stream[F, Submission]
  def findById(id: SubmissionId): EitherT[F, AppError, Submission]
  def create(submission: CreateSubmissionRequest): EitherT[F, AppError, Submission]
  def update(submission: UpdateSubmissionRequest): EitherT[F, AppError, Boolean]
  def delete(id: SubmissionId): EitherT[F, AppError, Boolean]
