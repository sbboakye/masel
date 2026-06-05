package com.sbboakye.masel.persistence.codec

import com.sbboakye.masel.core.domain.{
  Challenge,
  ChallengeDifficulty,
  ChallengeId,
  ChallengeStatus,
  NonEmptyString,
  PositiveInt,
  Score,
  Submission,
  SubmissionId,
}
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.*
import io.github.iltotore.iron.constraint.all.*
import skunk.*
import skunk.circe.codec.all.*
import skunk.codec.all.*
import skunk.data.Type

object SkunkCodec:

  // iron codecs
  val domainVarchar: Codec[NonEmptyString] = refined[NonEmptyString](varchar)
  val domainText: Codec[NonEmptyString] = refined[NonEmptyString](text)
  val domainPositiveInt: Codec[PositiveInt] = refined[PositiveInt](int4)

  // challenge codecs
  val challengeId: Codec[ChallengeId] = uuid.imap(ChallengeId.apply)(ChallengeId.value)
  val challengeDifficulty: Codec[ChallengeDifficulty] =
    `enum`[ChallengeDifficulty](_.toString.toLowerCase, ChallengeDifficulty.fromString, Type("challenge_difficulty"))
  val challengeStatus: Codec[ChallengeStatus] =
    `enum`[ChallengeStatus](_.toString.toLowerCase, ChallengeStatus.fromString, Type("challenge_status"))

  val challengeCodecTypes = challengeId *:
    domainVarchar *:
    domainText *:
    domainText *:
    challengeStatus *:
    domainText *:
    jsonb.opt *:
    domainPositiveInt *:
    challengeDifficulty *:
    timestamptz *:
    timestamptz

  val challengeCodec: Codec[Challenge] =
    challengeCodecTypes.to[Challenge]

  // submission codecs
  val submissionId: Codec[SubmissionId] = uuid.imap(SubmissionId.apply)(SubmissionId.value)
  val submissionScore: Codec[Score] = refined[Score](int4)

  val submissionCodecTypes = submissionId *:
    challengeId *:
    domainText *:
    jsonb.opt *:
    submissionScore.opt *:
    timestamptz *:
    timestamptz

  val submissionCodec: Codec[Submission] =
    submissionCodecTypes.to[Submission]
