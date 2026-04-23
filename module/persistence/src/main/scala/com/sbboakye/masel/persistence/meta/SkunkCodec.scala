package com.sbboakye.masel.persistence.meta

import skunk.*
import skunk.codec.all.*
import com.sbboakye.masel.core.domain.{Challenge, ChallengeDifficulty, ChallengeId, ChallengeStatus, ChallengeUpdate, Submission, SubmissionId}
import skunk.circe.codec.all.*
import skunk.data.Type

object SkunkCodec:

  val challengeId: Codec[ChallengeId] =
    uuid.imap(ChallengeId.apply)(ChallengeId.value)

  val submissionId: Codec[SubmissionId] =
    uuid.imap(SubmissionId.apply)(SubmissionId.value)

  val challengeStatus: Codec[ChallengeStatus] =
    `enum`[ChallengeStatus](_.toString.toLowerCase, ChallengeStatus.fromString, Type("chalengestatus"))

  val challengeDifficulty: Codec[ChallengeDifficulty] =
    `enum`[ChallengeDifficulty](_.toString.toLowerCase, ChallengeDifficulty.fromString, Type("challengedifficulty"))

  val challengeCodecTypes = challengeId *:
    varchar *:
    text *:
    challengeStatus *:
    text *:
    jsonb.opt *:
    int4 *:
    challengeDifficulty *:
    timestamptz *:
    timestamptz

  val challengeEncoder: Encoder[Challenge] =
    challengeCodecTypes.values.contramap { (c: Challenge) =>
      (c.id,
        c.title,
        c.instructions,
        c.status,
        c.expectedSolution,
        c.output,
        c.allottedTime,
        c.difficulty,
        c.createdAt,
        c.updatedAt
      )
    }

  val challengeDecoder: Decoder[Challenge] =
    challengeCodecTypes.to[Challenge]

  val submissionCodecTypes = submissionId *:
    challengeId *:
    text *:
    jsonb.opt *:
    int4.opt *:
    timestamptz *:
    timestamptz

  val submissionEncoder: Encoder[Submission] =
    submissionCodecTypes.values.contramap { (s: Submission) =>
      (s.id,
        s.challengeId,
        s.candidateSolution,
        s.output,
        s.score,
        s.createdAt,
        s.updatedAt
      )
    }

  val submissionDecoder: Decoder[Submission] =
    submissionCodecTypes.to[Submission]
