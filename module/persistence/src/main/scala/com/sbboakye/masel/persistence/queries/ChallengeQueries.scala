package com.sbboakye.masel.persistence.queries

import cats.syntax.all.*
import com.sbboakye.masel.core.domain.{
  Challenge,
  ChallengeDifficulty,
  ChallengeId,
  ChallengeStatus,
  NonEmptyString,
  PositiveInt,
  SetupSql,
}
import com.sbboakye.masel.persistence.codec.SkunkCodec.{
  challengeCodec,
  challengeDifficulty,
  challengeId,
  challengeStatus,
  domainPositiveInt,
  domainText,
  domainVarchar,
  querySqlCodec,
  setupSqlCodec,
}
import skunk.*
import skunk.circe.codec.all.jsonb
import skunk.codec.all.*
import skunk.implicits.*

object ChallengeQueries:

  def findAll: Query[(Int, Int), Challenge] =
    sql"""
         SELECT
          id,
          title,
          instructions,
          setup_sql,
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
          setup_sql,
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
      INSERT INTO challenges (id, title, instructions, setup_sql, status, expected_solution, output, allotted_time, difficulty, created_at, updated_at)
      VALUES (
        $challengeCodec
      )
      RETURNING id, title, instructions, status, expected_solution, output, allotted_time, difficulty, created_at, updated_at
    """.query(challengeCodec)

  def update: Query[Challenge, Challenge] =
    sql"""
      UPDATE challenges
      SET title = $domainVarchar,
          instructions = $domainText,
          setup_sql = $setupSqlCodec,
          status = $challengeStatus,
          expected_solution = $querySqlCodec,
          output = ${jsonb.opt},
          allotted_time = $domainPositiveInt,
          difficulty = $challengeDifficulty,
          updated_at = $timestamptz
      WHERE id = $challengeId
      RETURNING id, title, instructions, status, expected_solution, output, allotted_time, difficulty, created_at, updated_at
    """
      .query(challengeCodec)
      .contramap[Challenge] { c =>
        (
          (c.title: NonEmptyString),
          (c.instructions: NonEmptyString),
          c.setupSql,
          c.status,
          c.expectedSolution,
          c.output,
          (c.allottedTime: PositiveInt),
          c.difficulty,
          c.updatedAt,
          c.id,
        )
      }

  def delete: Command[ChallengeId] =
    sql"""
      DELETE FROM challenges
        WHERE id = $challengeId
    """.command
      .to[ChallengeId]
