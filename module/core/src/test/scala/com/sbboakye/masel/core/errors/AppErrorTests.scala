package com.sbboakye.masel.core.errors

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class AppErrorTests extends AnyFreeSpec with Matchers:

  "AppError.getMessage" - {
    "NotFound formats entity and id" in {
      AppError.NotFound("Challenge", "abc-123").getMessage shouldBe
        "Challenge with id abc-123 not found"
    }

    "NotFound handles an empty entity string without crashing" in {
      AppError.NotFound("", "x").getMessage shouldBe " with id x not found"
    }

    "ValidationFailed joins the errors with comma+space" in {
      AppError
        .ValidationFailed(List("title required", "score out of range"))
        .getMessage shouldBe "Validation failed: title required, score out of range"
    }

    "ValidationFailed with an empty list renders 'Validation failed: '" in {
      AppError.ValidationFailed(List.empty).getMessage shouldBe "Validation failed: "
    }

    "InternalError returns the supplied message verbatim" in {
      AppError.InternalError("db is down", None).getMessage shouldBe "db is down"
    }

    "InternalError with a cause still returns the supplied message (not the cause's)" in {
      val cause = new RuntimeException("underlying")
      AppError.InternalError("wrapper message", Some(cause)).getMessage shouldBe "wrapper message"
    }
  }

  "AppError is a Throwable" - {
    "extends RuntimeException so it can be raised by raiseError" in {
      val err: Throwable = AppError.NotFound("X", "1")
      err shouldBe a[RuntimeException]
    }
  }

  "AppError.InternalError" - {
    "attaches the cause via initCause so getCause returns it" in {
      val cause = new IllegalStateException("kaboom")
      val err = AppError.InternalError("oops", Some(cause))
      err.getCause shouldBe cause
    }

    "leaves getCause as null when no cause is supplied" in {
      AppError.InternalError("no cause", None).getCause shouldBe null
    }

    "default-arg overload (cause omitted) behaves like None" in {
      AppError.InternalError("x").getCause shouldBe null
    }
  }
