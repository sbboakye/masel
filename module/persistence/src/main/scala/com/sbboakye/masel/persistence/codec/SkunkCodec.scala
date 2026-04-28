package com.sbboakye.masel.persistence.codec

import com.sbboakye.masel.core.domain.dto.{
  CreateChallengeRequest,
  CreateSubmissionRequest,
  UpdateChallengeRequest,
  UpdateSubmissionRequest,
}
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
import io.github.iltotore.iron.constraint.all.*
import skunk.*
import skunk.circe.codec.all.*
import skunk.codec.all.*
import skunk.data.Type

object SkunkCodec:
  private def refined[A, C](base: Codec[A])(using constraint: RuntimeConstraint[A, C]): Codec[A :| C] =
    base.eimap[A :| C](a => a.refineEither[C]) {
      identity
    }

  // challenge codecs
  val challengeId: Codec[ChallengeId] =
    uuid.imap(ChallengeId.apply)(ChallengeId.value)

  val challengeTitle: Codec[NonEmptyString] = refined[String, Not[Empty]](varchar)
  val challengeInstructions: Codec[NonEmptyString] = refined[String, Not[Empty]](text)
  val challengeExpectedSolutions: Codec[NonEmptyString] = refined[String, Not[Empty]](text)
  val challengeAllottedTime: Codec[PositiveInt] = refined[Int, Positive](int4)

  val challengeStatus: Codec[ChallengeStatus] =
    `enum`[ChallengeStatus](_.toString.toLowerCase, ChallengeStatus.fromString, Type("challenge_status"))

  val challengeDifficulty: Codec[ChallengeDifficulty] =
    `enum`[ChallengeDifficulty](_.toString.toLowerCase, ChallengeDifficulty.fromString, Type("challenge_difficulty"))

  val challengeCodecTypes = challengeId *:
    challengeTitle *:
    challengeInstructions *:
    challengeStatus *:
    challengeExpectedSolutions *:
    jsonb.opt *:
    challengeAllottedTime *:
    challengeDifficulty *:
    timestamptz *:
    timestamptz

  val challengeCodec: Codec[Challenge] =
    challengeCodecTypes.to[Challenge]

  val createChallengeCodecTypes = challengeTitle *:
    challengeInstructions *:
    challengeExpectedSolutions *:
    challengeAllottedTime *:
    challengeDifficulty

  val createChallengeCodec: Codec[CreateChallengeRequest] =
    createChallengeCodecTypes.to[CreateChallengeRequest]

  // submission codecs
  val submissionId: Codec[SubmissionId] =
    uuid.imap(SubmissionId.apply)(SubmissionId.value)
  val submissionSolution: Codec[NonEmptyString] = refined[String, Not[Empty]](text)
  val submissionScore: Codec[Score] = refined[Int, (GreaterEqual[0] & LessEqual[100])](int4)

  val submissionCodecTypes = submissionId *:
    challengeId *:
    submissionSolution *:
    jsonb.opt *:
    submissionScore.opt *:
    timestamptz *:
    timestamptz

  val submissionCodec: Codec[Submission] =
    submissionCodecTypes.to[Submission]

  val createSubmissionCodecTypes = challengeId *:
    submissionSolution

  val createSubmissionCodec: Codec[CreateSubmissionRequest] =
    createSubmissionCodecTypes.to[CreateSubmissionRequest]
