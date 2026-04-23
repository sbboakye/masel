package com.sbboakye.masel.persistence.queries

import com.sbboakye.masel.core.domain.{Submission, SubmissionId, SubmissionUpdate}
import com.sbboakye.masel.persistence.meta.SkunkCodec.{submissionDecoder, submissionEncoder, submissionId}
import cats.syntax.all.*
import skunk.*
import skunk.implicits.*
import skunk.codec.all.*


object SubmissionQueries:

  def findAll(limit: Int, offset: Int): Query[Void, Submission] =
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
      """.query(submissionDecoder)

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
      """.query(submissionDecoder)

  def create: Command[Submission] =
    sql"""
        INSERT INTO submissions (id, challenge_id, candidate_solution, output, score, created_at, updated_at)
        VALUES (
          $submissionEncoder
        )
      """.command

  def update: Command[SubmissionUpdate] =
    sql"""
        UPDATE submissions
        SET
            candidate_solution = $text
        WHERE id = $submissionId
      """.command
      .contramap { (s: SubmissionUpdate) =>
        (s.candidateSolution, s.id)
      }

  def delete: Command[SubmissionId] =
    sql"""
        DELETE FROM submissions
          WHERE id = $submissionId
      """.command
      .to[SubmissionId]
