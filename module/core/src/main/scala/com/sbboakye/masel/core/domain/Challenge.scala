package com.sbboakye.masel.core.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*

import java.time.OffsetDateTime

enum ChallengeStatus:
  case Draft, Validated, Active, Archived

object ChallengeStatus:
  def fromString(s: String): Option[ChallengeStatus] =
    values.find(_.toString.equalsIgnoreCase(s))
  given Encoder[ChallengeStatus] = Encoder[String].contramap(_.toString.toLowerCase)
  given Decoder[ChallengeStatus] = Decoder[String].emap { s =>
    fromString(s).toRight(s"Unknown challenge status: $s")
  }

enum ChallengeDifficulty:
  case Easy, Medium, Hard

object ChallengeDifficulty:
  def fromString(s: String): Option[ChallengeDifficulty] =
    values.find(_.toString.equalsIgnoreCase(s))
  given Encoder[ChallengeDifficulty] = Encoder[String].contramap(_.toString.toLowerCase)
  given Decoder[ChallengeDifficulty] = Decoder[String].emap { s =>
    fromString(s).toRight(s"Unknown difficulty: $s")
  }

case class Challenge(
                    id: ChallengeId,
                    title: String,
                    instructions: String,
                    status: ChallengeStatus,
                    expectedSolution: String,
                    output: Option[io.circe.Json],
                    allottedTime: Int,
                    difficulty: ChallengeDifficulty,
                    createdAt: OffsetDateTime,
                    updatedAt: OffsetDateTime
                    )

object Challenge:
  given Encoder[Challenge] = deriveEncoder[Challenge]
  given Decoder[Challenge] = deriveDecoder[Challenge]

case class ChallengeUpdate(
                            title: String,
                            instructions: String,
                            status: ChallengeStatus,
                            expectedSolution: String,
                            allottedTime: Int,
                            difficulty: ChallengeDifficulty,
                            id: ChallengeId,
                          )

object ChallengeUpdate:
  given Encoder[ChallengeUpdate] = deriveEncoder[ChallengeUpdate]
  given Decoder[ChallengeUpdate] = deriveDecoder[ChallengeUpdate]