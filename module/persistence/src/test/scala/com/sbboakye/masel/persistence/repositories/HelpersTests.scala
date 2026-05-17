package com.sbboakye.masel.persistence.repositories

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.sbboakye.masel.core.errors.AppError
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import skunk.data.Completion

class HelpersTests extends AsyncFreeSpec with AsyncIOSpec with Matchers:

  given LoggerFactory[IO] = Slf4jFactory.create[IO]

  private val helpers: Helpers[IO] = new Helpers[IO] {}

  "wasDeleted" - {
    "returns true when Completion.Delete reports >0 rows" in
      helpers.wasDeleted(Completion.Delete(1)).asserting(_ shouldBe true)

    "returns true even for unusually large delete counts" in
      helpers.wasDeleted(Completion.Delete(1000)).asserting(_ shouldBe true)

    "returns false when Completion.Delete reports 0 rows" in
      helpers.wasDeleted(Completion.Delete(0)).asserting(_ shouldBe false)

    "raises AppError.InternalError for any non-Delete completion" in
      helpers
        .wasDeleted(Completion.Insert(1))
        .attempt
        .asserting {
          case Left(AppError.InternalError(msg, _)) =>
            msg should startWith("Unexpected completion type:")
          case other => fail(s"Expected InternalError, got $other")
        }
  }

  "repoHandler" - {
    "passes a successful effect through" in
      helpers.repoHandler(IO.pure(42)).asserting(_ shouldBe 42)

    "re-raises AppError variants untouched" in {
      val original = AppError.NotFound("Challenge", "abc")
      helpers
        .repoHandler(IO.raiseError[Int](original))
        .attempt
        .asserting(_ shouldBe Left(original))
    }

    "re-raises an existing AppError.InternalError untouched (no double-wrapping)" in {
      val original = AppError.InternalError("already internal", None)
      helpers
        .repoHandler(IO.raiseError[Int](original))
        .attempt
        .asserting(_ shouldBe Left(original))
    }

    "wraps an arbitrary Throwable as AppError.InternalError(\"Database error\", cause)" in {
      val underlying = new IllegalStateException("connection reset")
      helpers
        .repoHandler(IO.raiseError[Int](underlying))
        .attempt
        .asserting {
          case Left(AppError.InternalError(msg, cause)) =>
            msg shouldBe "Database error"
            cause shouldBe Some(underlying)
          case other => fail(s"Expected InternalError, got $other")
        }
    }
  }
