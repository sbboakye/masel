package com.sbboakye.masel.persistence.queries

import cats.syntax.all.*
import com.sbboakye.masel.core.domain.dto.UpdateChallengeRequest
import com.sbboakye.masel.core.domain.{Challenge, ChallengeDifficulty, ChallengeId, ChallengeStatus}
import com.sbboakye.masel.persistence.codec.SkunkCodec.{
  challengeCodec,
  challengeDifficulty,
  challengeId,
  challengeStatus,
}
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

  def update: Command[UpdateChallengeRequest] =
    sql"""
      UPDATE challenges
      SET title = $varchar,
          instructions = $text,
          status = $challengeStatus,
          expected_solution = $text,
          allotted_time = $int4,
          difficulty = $challengeDifficulty
      WHERE id = $challengeId
    """.command
      .contramap[UpdateChallengeRequest] { c =>
        (c.title, c.instructions, c.status, c.expectedSolution, c.allottedTime, c.difficulty, c.id)
      }

  def delete: Command[ChallengeId] =
    sql"""
      DELETE FROM challenges
        WHERE id = $challengeId
    """.command
      .to[ChallengeId]
