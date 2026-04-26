package com.sbboakye.masel.persistence.codec

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

  val challengeCodec: Codec[Challenge] =
    challengeCodecTypes.to[Challenge]

  val submissionCodecTypes = submissionId *:
    challengeId *:
    text *:
    jsonb.opt *:
    int4.opt *:
    timestamptz *:
    timestamptz

  val submissionCodec: Codec[Submission] =
    submissionCodecTypes.to[Submission]
