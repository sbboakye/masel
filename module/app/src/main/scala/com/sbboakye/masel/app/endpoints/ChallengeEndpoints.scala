package com.sbboakye.masel.app.endpoints

import TapirSchemas.given
import com.sbboakye.masel.app.requests.{CreateChallengeRequest, UpdateChallengeRequest}
import com.sbboakye.masel.core.domain.Challenge
import java.util.UUID
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

object ChallengeEndpoints:
  import BaseEndpoint.*

  val challenges: Endpoint[Unit, Unit, ApiError, Unit, Any] = base.in("challenges")

  val listChallenges: Endpoint[Unit, (Int, Int), ApiError, List[Challenge], Any] =
    challenges.get
      .in(query[Int]("limit").default(10))
      .in(query[Int]("offset").default(0))
      .out(jsonBody[List[Challenge]])
      .description("List all challenges")

  val getChallenge: Endpoint[Unit, UUID, ApiError, Challenge, Any] =
    challenges.get
      .in(path[UUID]("id"))
      .out(jsonBody[Challenge])
      .description("Get a challenge by ID")

  val createChallenge: Endpoint[Unit, CreateChallengeRequest, ApiError, Challenge, Any] =
    challenges.post
      .in(jsonBody[CreateChallengeRequest])
      .out(jsonBody[Challenge])
      .description("Create a new challenge")

  val updateChallenge: Endpoint[Unit, (UUID, UpdateChallengeRequest), ApiError, Challenge, Any] =
    challenges.put
      .in(path[UUID]("id"))
      .in(jsonBody[UpdateChallengeRequest])
      .out(jsonBody[Challenge])
      .description("Update an existing challenge")

  val deleteChallenge: Endpoint[Unit, UUID, ApiError, Unit, Any] =
    challenges.delete
      .in(path[UUID]("id"))
      .description("Delete a challenge by ID")

  val all: List[AnyEndpoint] = List(listChallenges, getChallenge, createChallenge, updateChallenge, deleteChallenge)
