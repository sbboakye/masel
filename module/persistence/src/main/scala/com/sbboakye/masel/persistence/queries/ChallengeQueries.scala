package com.sbboakye.masel.persistence.queries

import com.sbboakye.masel.core.domain.{Challenge, ChallengeId, ChallengeUpdate}
import com.sbboakye.masel.persistence.meta.SkunkCodec.{challengeDecoder, challengeDifficulty, challengeEncoder, challengeId, challengeStatus}
import cats.syntax.all.*
import skunk.*
import skunk.implicits.*
import skunk.codec.all.*


object ChallengeQueries:

  def findAll(limit: Int, offset: Int): Query[Void, Challenge] =
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
    """.query(challengeDecoder)

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
    """.query(challengeDecoder)

  def create: Command[Challenge] =
    sql"""
      INSERT INTO challenges (id, title, instructions, status, expected_solution, output, allotted_time, difficulty, created_at, updated_at)
      VALUES (
        $challengeEncoder
      )
    """.command

  def update: Command[ChallengeUpdate] =
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
      .contramap { (c: ChallengeUpdate) =>
        (c.title, c.instructions, c.status, c.expectedSolution, c.allottedTime, c.difficulty, c.id)
      }

  def delete: Command[ChallengeId] =
    sql"""
      DELETE FROM challenges
        WHERE id = $challengeId
    """.command
      .to[ChallengeId]
