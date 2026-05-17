package com.sbboakye.masel.app.routes

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.sbboakye.masel.core.errors.AppError
import io.circe.Json
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.{
  InvalidMessageBodyFailure,
  MalformedMessageBodyFailure,
  MediaRange,
  MediaType,
  MediaTypeMismatch,
  MediaTypeMissing,
  Response,
  Status,
}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

class ErrorHandlingTests extends AsyncFreeSpec with AsyncIOSpec with Matchers:

  given LoggerFactory[IO] = Slf4jFactory.create[IO]

  private val dsl: Http4sDsl[IO] = new Http4sDsl[IO] {}
  import ErrorHandling.recoverAppErrors

  private def runRecovery(eff: IO[Response[IO]]): IO[Response[IO]] =
    eff.recoverAppErrors(dsl)

  private def messageOf(json: Json): Option[String] =
    json.hcursor.get[String]("message").toOption

  "recoverAppErrors" - {

    "passes a successful response through unchanged" in {
      import dsl.*
      runRecovery(Ok("hello")).flatMap { resp =>
        resp.status shouldBe Status.Ok
        resp.as[String].asserting(_ shouldBe "hello")
      }
    }

    "maps AppError.NotFound to 404 with an ErrorResponse body" in {
      val err: IO[Response[IO]] = IO.raiseError(AppError.NotFound("Challenge", "abc-123"))
      runRecovery(err).flatMap { resp =>
        resp.status shouldBe Status.NotFound
        resp.as[Json].asserting(json => messageOf(json) shouldBe Some("Challenge with id abc-123 not found"))
      }
    }

    "maps AppError.ValidationFailed to 400 with the concatenated errors in the body" in {
      val err: IO[Response[IO]] =
        IO.raiseError(AppError.ValidationFailed(List("title required", "score out of range")))
      runRecovery(err).flatMap { resp =>
        resp.status shouldBe Status.BadRequest
        resp.as[Json].asserting { json =>
          messageOf(json) shouldBe Some("Validation failed: title required, score out of range")
        }
      }
    }

    "maps AppError.InternalError to 500 with the supplied message (no cause)" in {
      val err: IO[Response[IO]] =
        IO.raiseError(AppError.InternalError("db is down", None))
      runRecovery(err).flatMap { resp =>
        resp.status shouldBe Status.InternalServerError
        resp.as[Json].asserting(json => messageOf(json) shouldBe Some("db is down"))
      }
    }

    "maps AppError.InternalError to 500 with the supplied message (cause present, logged)" in {
      val cause = new IllegalStateException("downstream blew up")
      val err: IO[Response[IO]] =
        IO.raiseError(AppError.InternalError("db is down", Some(cause)))
      runRecovery(err).flatMap { resp =>
        resp.status shouldBe Status.InternalServerError
        resp.as[Json].asserting(json => messageOf(json) shouldBe Some("db is down"))
      }
    }

    "maps MalformedMessageBodyFailure to 400" in {
      val err: IO[Response[IO]] =
        IO.raiseError(MalformedMessageBodyFailure("not valid JSON", None))
      runRecovery(err).flatMap { resp =>
        resp.status shouldBe Status.BadRequest
        resp.as[Json].asserting(json => messageOf(json).exists(_.contains("not valid JSON")) shouldBe true)
      }
    }

    "maps InvalidMessageBodyFailure to 422 (Unprocessable Content)" in {
      val err: IO[Response[IO]] =
        IO.raiseError(InvalidMessageBodyFailure("missing field: title", None))
      runRecovery(err).flatMap { resp =>
        resp.status shouldBe Status.UnprocessableEntity
        resp.as[Json].asserting(json => messageOf(json).exists(_.contains("missing field: title")) shouldBe true)
      }
    }

    "maps MediaTypeMissing to 415 (Unsupported Media Type)" in {
      val err: IO[Response[IO]] = IO.raiseError(MediaTypeMissing(Set(MediaRange.`*/*`)))
      runRecovery(err).flatMap(resp => IO.pure(resp.status shouldBe Status.UnsupportedMediaType))
    }

    "maps MediaTypeMismatch to 415 (Unsupported Media Type)" in {
      val err: IO[Response[IO]] =
        IO.raiseError(MediaTypeMismatch(MediaType.text.plain, Set(MediaType.application.json)))
      runRecovery(err).flatMap(resp => IO.pure(resp.status shouldBe Status.UnsupportedMediaType))
    }

    "maps an arbitrary Throwable to 500 with a generic body (no internal details leaked)" in {
      val err: IO[Response[IO]] = IO.raiseError(new RuntimeException("secret SQL state: 42P01 relation does not exist"))
      runRecovery(err).flatMap { resp =>
        resp.status shouldBe Status.InternalServerError
        resp.as[Json].asserting(json => messageOf(json) shouldBe Some("Internal server error"))
      }
    }

    "maps a Throwable with a null message to a generic 500 body" in {
      val err: IO[Response[IO]] = IO.raiseError(new RuntimeException())
      runRecovery(err).flatMap { resp =>
        resp.status shouldBe Status.InternalServerError
        resp.as[Json].asserting(json => messageOf(json) shouldBe Some("Internal server error"))
      }
    }
  }
