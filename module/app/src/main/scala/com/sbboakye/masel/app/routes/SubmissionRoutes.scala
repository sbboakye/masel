package com.sbboakye.masel.app.routes

import cats.effect.Async
import cats.syntax.all.*
import com.sbboakye.masel.app.endpoints.BaseEndpoint
import com.sbboakye.masel.app.requests.{CreateSubmissionRequest, UpdateSubmissionRequest}
import com.sbboakye.masel.app.services.SubmissionService
import com.sbboakye.masel.core.domain.SubmissionId
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.LoggerFactory

class SubmissionRoutes[F[_]: {Async, LoggerFactory}](service: SubmissionService[F]):

  private val dsl = new Http4sDsl[F] {}
  import ErrorHandling.recoverAppErrors
  import dsl.*

  private object LimitParam extends OptionalQueryParamDecoderMatcher[Int]("limit")
  private object OffsetParam extends OptionalQueryParamDecoderMatcher[Int]("offset")

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> BaseEndpoint.basePath / "submissions" :? LimitParam(limit) +& OffsetParam(offset) =>
      service
        .listSubmissions(limit.getOrElse(10), offset.getOrElse(0))
        .flatMap(Ok(_))
        .recoverAppErrors(dsl)

    case GET -> BaseEndpoint.basePath / "submissions" / UUIDVar(id) =>
      service
        .getSubmission(SubmissionId(id))
        .flatMap(Ok(_))
        .recoverAppErrors(dsl)

    case req @ POST -> BaseEndpoint.basePath / "submissions" =>
      req
        .as[CreateSubmissionRequest]
        .flatMap(service.createSubmission)
        .flatMap(Created(_))
        .recoverAppErrors(dsl)

    case req @ PUT -> BaseEndpoint.basePath / "submissions" / UUIDVar(id) =>
      req
        .as[UpdateSubmissionRequest]
        .flatMap(service.updateSubmission(SubmissionId(id), _))
        .flatMap(Ok(_))
        .recoverAppErrors(dsl)

    case DELETE -> BaseEndpoint.basePath / "submissions" / UUIDVar(id) =>
      service
        .deleteSubmission(SubmissionId(id))
        .flatMap(_ => NoContent())
        .recoverAppErrors(dsl)
  }
