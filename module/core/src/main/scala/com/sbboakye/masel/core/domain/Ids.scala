package com.sbboakye.masel.core.domain

import cats.effect.std.UUIDGen
import io.circe.{Decoder, Encoder}
import java.util.UUID

opaque type ChallengeId = UUID
opaque type SubmissionId = UUID

inline def uuidEncoder[A](inline valueOf: A => UUID): Encoder[A] =
  Encoder[UUID].contramap(a => valueOf(a))

inline def uuidDecoder[A](inline wrap: UUID => A): Decoder[A] =
  Decoder[UUID].map(wrap)

object ChallengeId:
  def apply(uuid: UUID): ChallengeId = uuid
  def generate[F[_]: UUIDGen]: F[ChallengeId] = UUIDGen.randomUUID
  extension (id: ChallengeId) def value: UUID = id
  given Encoder[ChallengeId] = uuidEncoder(_.value)
  given Decoder[ChallengeId] = uuidDecoder(ChallengeId.apply)

object SubmissionId:
  def apply(uuid: UUID): SubmissionId = uuid
  def generate[F[_]: UUIDGen]: F[SubmissionId] = UUIDGen.randomUUID
  extension (id: SubmissionId) def value: UUID = id
  given Encoder[SubmissionId] = uuidEncoder(_.value)
  given Decoder[SubmissionId] = uuidDecoder(SubmissionId.apply)
