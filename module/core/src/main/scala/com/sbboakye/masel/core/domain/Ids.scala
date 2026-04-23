package com.sbboakye.masel.core.domain

import io.circe.{Decoder, Encoder}

import java.util.UUID

opaque type ChallengeId = UUID
opaque type SubmissionId = UUID

inline def uuidEncoder[A](inline valueOf: A => UUID): Encoder[A] =
  Encoder.encodeString.contramap(a => valueOf(a).toString)

inline def uuidDecoder[A](inline wrap: UUID => A): Decoder[A] =
  Decoder.decodeString.emap { a =>
    scala.util.Try(UUID.fromString(a)).toEither.left.map(_.getMessage).map(wrap)
  }

object ChallengeId:
  def apply(uuid: UUID): ChallengeId = uuid
  def generate: ChallengeId = UUID.randomUUID()
  extension (id: ChallengeId) def value: UUID = id
  given ChallengeIdEncoder: Encoder[ChallengeId] = uuidEncoder(_.value)
  given ChallengeIdDecoder: Decoder[ChallengeId] = uuidDecoder(ChallengeId.apply)

object SubmissionId:
  def apply(uuid: UUID): SubmissionId = uuid
  def generate: SubmissionId = UUID.randomUUID()
  extension (id: SubmissionId) def value: UUID = id
  given SubmissionIdEncoder: Encoder[SubmissionId] = uuidEncoder(_.value)
  given SubmissionIdDecoder: Decoder[SubmissionId] = uuidDecoder(SubmissionId.apply)