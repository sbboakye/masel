package com.sbboakye.masel.persistence.queries

import com.sbboakye.masel.core.domain.{Submission, SubmissionId, SubmissionUpdate}
import com.sbboakye.masel.persistence.codec.SkunkCodec.{createSubmissionCodec, submissionCodec, submissionId}
import cats.syntax.all.*
import com.sbboakye.masel.core.domain.dto.CreateSubmissionRequest
import skunk.*
import skunk.implicits.*
import skunk.codec.all.*


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

  def create: Query[CreateSubmissionRequest, Submission] =
    sql"""
        INSERT INTO submissions (challenge_id, candidate_solution)
        VALUES (
          $createSubmissionCodec
        )
        RETURNING id, challenge_id, candidate_solution, output, score, created_at, updated_at
    """.query(submissionCodec)

  def update: Query[
    (String, SubmissionId),
    Submission
  ] =
    sql"""
        UPDATE submissions
        SET
            candidate_solution = $text
        WHERE id = $submissionId
        RETURNING id, challenge_id, candidate_solution, output, score, created_at, updated_at
      """.query(submissionCodec)

  def delete: Command[SubmissionId] =
    sql"""
        DELETE FROM submissions
          WHERE id = $submissionId
      """.command
      .to[SubmissionId]
