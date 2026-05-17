package com.sbboakye.masel.app.services

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.sbboakye.masel.core.errors.AppError
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

class HelpersTests extends AsyncFreeSpec with AsyncIOSpec with Matchers:

  given LoggerFactory[IO] = Slf4jFactory.create[IO]

  private val helpers: Helpers[IO] = new Helpers[IO] {}
  import helpers.*

  "orNotFound" - {
    "unwraps the value when the effect yields Some" in
      IO.pure(Option(42)).orNotFound("Thing", "id-1").asserting(_ shouldBe 42)

    "raises AppError.NotFound when the effect yields None" in
      IO.pure(Option.empty[Int])
        .orNotFound("Challenge", "missing-id")
        .attempt
        .asserting {
          case Left(AppError.NotFound(entity, id)) =>
            entity shouldBe "Challenge"
            id shouldBe "missing-id"
          case other => fail(s"Expected NotFound, got $other")
        }

    "lets effect errors propagate unchanged (before serviceHandler runs)" in
      IO.raiseError[Option[Int]](new RuntimeException("boom"))
        .orNotFound("Thing", "x")
        .attempt
        .asserting {
          case Left(e: RuntimeException) => e.getMessage shouldBe "boom"
          case other => fail(s"Expected RuntimeException, got $other")
        }
  }

  "serviceHandler" - {
    "passes successful effects through" in
      helpers.serviceHandler(IO.pure("ok")).asserting(_ shouldBe "ok")

    "re-raises AppError.NotFound untouched" in {
      val original = AppError.NotFound("Challenge", "abc")
      helpers
        .serviceHandler(IO.raiseError[Int](original))
        .attempt
        .asserting(_ shouldBe Left(original))
    }

    "re-raises AppError.ValidationFailed untouched" in {
      val original = AppError.ValidationFailed(List("title is required"))
      helpers
        .serviceHandler(IO.raiseError[Int](original))
        .attempt
        .asserting(_ shouldBe Left(original))
    }

    "re-raises AppError.InternalError untouched (no double-wrapping)" in {
      val original = AppError.InternalError("already internal", None)
      helpers
        .serviceHandler(IO.raiseError[Int](original))
        .attempt
        .asserting(_ shouldBe Left(original))
    }

    "wraps arbitrary throwables as AppError.InternalError, preserving the cause" in {
      val underlying = new IllegalStateException("db kaboom")
      helpers
        .serviceHandler(IO.raiseError[Int](underlying))
        .attempt
        .asserting {
          case Left(AppError.InternalError(msg, cause)) =>
            msg shouldBe "Unexpected error"
            cause shouldBe Some(underlying)
          case other => fail(s"Expected InternalError, got $other")
        }
    }
  }
