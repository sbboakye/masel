package com.sbboakye.masel.persistence.queries

import com.sbboakye.masel.core.domain.{Challenge, ChallengeDifficulty, ChallengeId, ChallengeStatus, ChallengeUpdate}
import com.sbboakye.masel.persistence.codec.SkunkCodec.{challengeCodec, challengeDifficulty, challengeId, challengeStatus, createChallengeCodec}
import cats.syntax.all.*
import com.sbboakye.masel.core.domain.dto.CreateChallengeRequest
import skunk.*
import skunk.implicits.*
import skunk.codec.all.*


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

  def create: Query[CreateChallengeRequest, Challenge] =
    sql"""
      INSERT INTO challenges (title, instructions, expected_solution, allotted_time, difficulty)
      VALUES (
        $createChallengeCodec
      )
      RETURNING id, title, instructions, status, expected_solution, output, allotted_time, difficulty, created_at, updated_at
    """.query(challengeCodec)

  def update: Query[
    (String, String, ChallengeStatus, String, Int, ChallengeDifficulty, ChallengeId), 
    Challenge
  ] =
    sql"""
      UPDATE challenges
      SET title = $varchar,
          instructions = $text,
          status = $challengeStatus,
          expected_solution = $text,
          allotted_time = $int4,
          difficulty = $challengeDifficulty
      WHERE id = $challengeId
      RETURNING id, title, instructions, status, expected_solution, output, allotted_time, difficulty, created_at, updated_at
    """.query(challengeCodec)

  def delete: Command[ChallengeId] =
    sql"""
      DELETE FROM challenges
        WHERE id = $challengeId
    """.command
      .to[ChallengeId]
