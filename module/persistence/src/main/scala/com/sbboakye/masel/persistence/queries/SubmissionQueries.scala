package com.sbboakye.masel.persistence.queries

import cats.syntax.all.*
import com.sbboakye.masel.core.domain.dto.UpdateSubmissionRequest
import com.sbboakye.masel.core.domain.{NonEmptyString, Submission, SubmissionId}
import com.sbboakye.masel.persistence.codec.SkunkCodec.{submissionCodec, submissionId, submissionSolution}
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

object SubmissionQueries:

  def findAll: Query[(Int, Int), Submission] =
    sql"""
           SELECT
            id,
            challenge_id,
            candidate_solution,
            output,
            score,
            created_at,
            updated_at
           FROM submissions
           ORDER BY updated_at desc
           LIMIT $int4 OFFSET $int4
      """.query(submissionCodec)

  def findById: Query[SubmissionId, Submission] =
    sql"""
          SELECT
            id,
            challenge_id,
            candidate_solution,
            output,
            score,
            created_at,
            updated_at
           FROM submissions
           WHERE id = $submissionId
      """.query(submissionCodec)

  def create: Query[Submission, Submission] =
    sql"""
        INSERT INTO submissions (id, challenge_id, candidate_solution, output, score, created_at, updated_at)
        VALUES (
          $submissionCodec
        )
        RETURNING id, challenge_id, candidate_solution, output, score, created_at, updated_at
    """.query(submissionCodec)

  def update: Query[UpdateSubmissionRequest, Submission] =
    sql"""
        UPDATE submissions
        SET
            candidate_solution = $submissionSolution
        WHERE id = $submissionId
        RETURNING id, challenge_id, candidate_solution, output, score, created_at, updated_at
      """
      .query(submissionCodec)
      .contramap[UpdateSubmissionRequest](s => (s.candidateSolution: NonEmptyString, s.id))

  def delete: Command[SubmissionId] =
    sql"""
        DELETE FROM submissions
          WHERE id = $submissionId
      """.command
      .to[SubmissionId]
