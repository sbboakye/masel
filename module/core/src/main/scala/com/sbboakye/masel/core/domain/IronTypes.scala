package com.sbboakye.masel.core.domain

import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

type NonEmptyString = String :| Not[Empty]

type PositiveInt = Int :| Positive

type Score = Int :| (GreaterEqual[0] & LessEqual[100])

type DatabasePassword = String :| MinLength[8]

given Encoder[NonEmptyString] = Encoder.encodeString.asInstanceOf[Encoder[NonEmptyString]]
given Decoder[NonEmptyString] = Decoder.decodeString.emap(_.refineEither[Not[Empty]])

given Encoder[PositiveInt] = Encoder.encodeInt.asInstanceOf[Encoder[PositiveInt]]
given Decoder[PositiveInt] = Decoder.decodeInt.emap(_.refineEither[Positive])

given Encoder[Score] = Encoder.encodeInt.asInstanceOf[Encoder[Score]]
given Decoder[Score] = Decoder.decodeInt.emap(_.refineEither[GreaterEqual[0] & LessEqual[100]])
