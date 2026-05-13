package com.sbboakye.masel.app.routes

import cats.effect.Async
import cats.syntax.all.*
import com.sbboakye.masel.app.requests.{CreateSubmissionRequest, UpdateSubmissionRequest}
import com.sbboakye.masel.app.services.SubmissionService
import com.sbboakye.masel.core.domain.SubmissionId
import java.util.UUID
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl

class SubmissionRoutes[F[_]: Async](service: SubmissionService[F]):

  private val dsl = new Http4sDsl[F] {}
  import dsl.*
  import ErrorHandling.recoverAppErrors

  private object LimitParam extends OptionalQueryParamDecoderMatcher[Int]("limit")
  private object OffsetParam extends OptionalQueryParamDecoderMatcher[Int]("offset")

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "api" / "v1" / "submissions" :? LimitParam(limit) +& OffsetParam(offset) =>
      service
        .listSubmissions(limit.getOrElse(10), offset.getOrElse(0))
        .flatMap(Ok(_))
        .recoverAppErrors(dsl)

    case GET -> Root / "api" / "v1" / "submissions" / UUIDVar(id) =>
      service
        .getSubmission(SubmissionId(id))
        .flatMap(Ok(_))
        .recoverAppErrors(dsl)

    case req @ POST -> Root / "api" / "v1" / "submissions" =>
      req
        .as[CreateSubmissionRequest]
        .flatMap(service.createSubmission)
        .flatMap(Created(_))
        .recoverAppErrors(dsl)

    case req @ PUT -> Root / "api" / "v1" / "submissions" / UUIDVar(id) =>
      req
        .as[UpdateSubmissionRequest]
        .flatMap(service.updateSubmission(SubmissionId(id), _))
        .flatMap(Ok(_))
        .recoverAppErrors(dsl)

    case DELETE -> Root / "api" / "v1" / "submissions" / UUIDVar(id) =>
      service
        .deleteSubmission(SubmissionId(id))
        .flatMap(_ => NoContent())
        .recoverAppErrors(dsl)
  }
