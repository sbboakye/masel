package com.sbboakye.masel.app.routes

import cats.effect.Async
import cats.syntax.all.*
import com.sbboakye.masel.app.endpoints.{BaseEndpoint, SubmissionEndpoints}
import com.sbboakye.masel.app.services.SubmissionService
import com.sbboakye.masel.core.domain.SubmissionId
import com.sbboakye.masel.core.errors.AppError
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.Http4sServerInterpreter

class SubmissionRoutes[F[_]: Async](service: SubmissionService[F]):
  import BaseEndpoint.mapError

  private val listSubmissionsRoute =
    SubmissionEndpoints.listSubmissions.serverLogic { case (limit, offset) =>
      service
        .listSubmissions(limit, offset)
        .map(Right(_))
        .handleError(e => Left(mapError(e)))
    }

  private val getSubmissionRoute =
    SubmissionEndpoints.getSubmission.serverLogic(id =>
      service
        .getSubmission(SubmissionId(id))
        .map {
          case Some(submission) => Right(submission)
          case None => Left(mapError(AppError.NotFound("Submission", id.toString)))
        }
        .handleError(e => Left(mapError(e))),
    )

  private val createSubmissionRoute =
    SubmissionEndpoints.createSubmission.serverLogic(req =>
      service
        .createSubmission(req)
        .map(Right(_))
        .handleError(e => Left(mapError(e))),
    )

  private val updateSubmissionRoute =
    SubmissionEndpoints.updateSubmission.serverLogic((id, req) =>
      service
        .updateSubmission(SubmissionId(id), req)
        .map {
          case Some(submission) => Right(submission)
          case None => Left(mapError(AppError.NotFound("Submission", id.toString)))
        }
        .handleError(e => Left(mapError(e))),
    )

  private val deleteSubmissionRoute =
    SubmissionEndpoints.deleteSubmission.serverLogic(id =>
      service
        .deleteSubmission(SubmissionId(id))
        .map(Right(_))
        .handleError(e => Left(mapError(e))),
    )

  val routes: HttpRoutes[F] = Http4sServerInterpreter[F]().toRoutes(
    List(
      listSubmissionsRoute,
      getSubmissionRoute,
      createSubmissionRoute,
      updateSubmissionRoute,
      deleteSubmissionRoute,
    ),
  )
