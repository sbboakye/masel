package com.sbboakye.masel.app.endpoints

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class HealthResponse(status: String)

object HealthResponse:
  given Encoder[HealthResponse] = deriveEncoder
  given Decoder[HealthResponse] = deriveDecoder

final case class ReadinessResponse(status: String)

object ReadinessResponse:
  given Encoder[ReadinessResponse] = deriveEncoder
  given Decoder[ReadinessResponse] = deriveDecoder
