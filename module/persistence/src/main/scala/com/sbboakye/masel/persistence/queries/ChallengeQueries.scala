package com.sbboakye.masel.persistence.queries

import cats.syntax.all.*
import com.sbboakye.masel.core.domain.dto.UpdateChallengeRequest
import com.sbboakye.masel.core.domain.{
  Challenge,
  ChallengeDifficulty,
  ChallengeId,
  ChallengeStatus,
  NonEmptyString,
  PositiveInt,
}
import com.sbboakye.masel.persistence.codec.SkunkCodec.{
  challengeAllottedTime,
  challengeCodec,
  challengeCodecTypes,
  challengeDifficulty,
  challengeExpectedSolutions,
  challengeId,
  challengeInstructions,
  challengeStatus,
  challengeTitle,
}
import io.github.iltotore.iron.autoRefine
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

object ChallengeQueries:

  def findAll: Query[(Int, Int), Challenge] =
    sql"""
         SELECT
          id,
          title,
          instructions,
          status,
          expected_solution,
          output,
          allotted_time,
          difficulty,
          created_at,
          updated_at
         FROM challenges
         ORDER BY updated_at desc
         LIMIT $int4 OFFSET $int4
    """.query(challengeCodec)

  def findById: Query[ChallengeId, Challenge] =
    sql"""
        SELECT
          id,
          title,
          instructions,
          status,
          expected_solution,
          output,
          allotted_time,
          difficulty,
          created_at,
          updated_at
         FROM challenges
         WHERE id = $challengeId
    """.query(challengeCodec)

  def create: Query[Challenge, Challenge] =
    sql"""
      INSERT INTO challenges (id, title, instructions, status, expected_solution, output, allotted_time, difficulty, created_at, updated_at)
      VALUES (
        $challengeCodec
      )
      RETURNING id, title, instructions, status, expected_solution, output, allotted_time, difficulty, created_at, updated_at
    """.query(challengeCodec)

  def update: Query[UpdateChallengeRequest, Challenge] =
    sql"""
      UPDATE challenges
      SET title = $challengeTitle,
          instructions = $challengeInstructions,
          status = $challengeStatus,
          expected_solution = $challengeExpectedSolutions,
          allotted_time = $challengeAllottedTime,
          difficulty = $challengeDifficulty,
          updated_at = $timestamptz
      WHERE id = $challengeId
      RETURNING id, title, instructions, status, expected_solution, output, allotted_time, difficulty, created_at, updated_at
    """
      .query(challengeCodec)
      .contramap[UpdateChallengeRequest] { req =>
        (
          (req.title: NonEmptyString),
          (req.instructions: NonEmptyString),
          req.status,
          (req.expectedSolution: NonEmptyString),
          (req.allottedTime: PositiveInt),
          req.difficulty,
          req.updatedAt,
          req.id,
        )
      }

  def delete: Command[ChallengeId] =
    sql"""
      DELETE FROM challenges
        WHERE id = $challengeId
    """.command
      .to[ChallengeId]
