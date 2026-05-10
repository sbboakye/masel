package com.sbboakye.masel.app.endpoints

import TapirSchemas.given
import com.sbboakye.masel.app.requests.{CreateSubmissionRequest, UpdateSubmissionRequest}
import com.sbboakye.masel.core.domain.Submission
import java.util.UUID
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

object SubmissionEndpoints:
  import BaseEndpoint.*

  val submissions: Endpoint[Unit, Unit, ApiError, Unit, Any] = base.in("submissions")

  val listSubmissions: Endpoint[Unit, (Int, Int), ApiError, List[Submission], Any] =
    submissions.get
      .in(query[Int]("limit").default(10))
      .in(query[Int]("offset").default(0))
      .out(jsonBody[List[Submission]])
      .description("List all submissions")

  val getSubmission: Endpoint[Unit, UUID, ApiError, Submission, Any] =
    submissions.get
      .in(path[UUID]("id"))
      .out(jsonBody[Submission])
      .description("Get a submission by ID")

  val createSubmission: Endpoint[Unit, CreateSubmissionRequest, ApiError, Submission, Any] =
    submissions.post
      .in(jsonBody[CreateSubmissionRequest])
      .out(jsonBody[Submission])
      .description("Create a new submission")

  val updateSubmission: Endpoint[Unit, (UUID, UpdateSubmissionRequest), ApiError, Submission, Any] =
    submissions.put
      .in(path[UUID]("id"))
      .in(jsonBody[UpdateSubmissionRequest])
      .out(jsonBody[Submission])
      .description("Update an existing submission")

  val deleteSubmission: Endpoint[Unit, UUID, ApiError, Unit, Any] =
    submissions.delete
      .in(path[UUID]("id"))
      .description("Delete a submission by ID")

  val all: List[AnyEndpoint] =
    List(listSubmissions, getSubmission, createSubmission, updateSubmission, deleteSubmission)
