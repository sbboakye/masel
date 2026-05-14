# Error Handling Review — Service & Repository Layers

_Date: 2026-05-14_

## TL;DR

The overall shape is sound — `MonadThrow` channel, boundary translation in `repoHandler` / `serviceHandler` / `mapError`, sealed `AppError` ADT. But the current implementation has four real issues:

1. **`RepositoryError` leaks past the service layer** and into HTTP responses as an opaque "Internal server error". The service layer never translates it to `AppError`.
2. **`SubmissionService` doesn't use `serviceHandler` at all** — only `ChallengeService` does. So submissions get zero error translation.
3. **All error context is destroyed.** `repoHandler` collapses every Skunk `Throwable` into the parameterless `RepositoryError.DatabaseError`; `serviceHandler` collapses anything else into the parameterless `AppError.InternalError`. The original cause never reaches logs.
4. **Logging drops the `Throwable`.** `logger.error("Database error occurred")` doesn't include the exception, so production logs will be useless for debugging Skunk failures.

Everything else is small cleanup.

---

## How errors flow today

```
Skunk raises SqlException / PostgresErrorException
        │
        ▼
SkunkChallengeRepository.findById        (module/persistence/.../SkunkChallengeRepository.scala)
        │   wrapped in repoHandler
        ▼
Helpers.repoHandler                      (module/persistence/.../repositories/Helpers.scala:18-22)
   .handleErrorWith {
     case err: RepositoryError => err.raiseError      // pass-through
     case _                    => DatabaseError.raiseError  // ★ loses original Throwable
   }
        │
        ▼
Service                                  (module/app/.../services/ChallengeService.scala)
   wrapped in serviceHandler
        ▼
Helpers.serviceHandler                   (module/app/.../services/Helpers.scala:13-21)
   .handleErrorWith {
     case RepositoryError.DatabaseError => log("Database error occurred") *> DatabaseError.raiseError  // ★ no translation
     case app: AppError                 => app.raiseError
     case _                             => log("Unexpected") *> AppError.InternalError.raiseError      // ★ loses original Throwable
   }
        │
        ▼
ChallengeRoutes / SubmissionRoutes — `.recoverAppErrors(dsl)`
        ▼
ErrorHandling.mapError                   (module/app/.../routes/ErrorHandling.scala:20-30)
   case AppError.NotFound / ValidationFailed / InternalError => structured response
   case other => InternalServerError(Option(other.getMessage).getOrElse("Internal server error"))
```

**Net effect of the leak:** when a Skunk error fires, the HTTP response is `500 Internal Server Error` with body `{"message": "Internal server error"}` — produced by the `case other =>` fallback in `mapError`, because `RepositoryError.DatabaseError` was *never translated* in the service layer. The logs say "Database error occurred" with no stack trace, no SQLSTATE, no query.

---

## What works well

- **Hexagonal layering is correct.** Repository ports live in `core`, Skunk adapters in `persistence`, services depend only on the ports.
- **Centralised boundary translation** via `repoHandler` / `serviceHandler` / `mapError` is the right pattern — every error must pass through a known funnel.
- **`AppError extends Throwable`** so it composes cleanly with `MonadThrow` semantics (consistent with the [[error-channel]] decision in our previous discussion).
- **Transactions roll back on raised errors.** Skunk's `session.transaction.use` re-raises whatever the body raised, so the `NotFound` raised inside `updateChallenge`'s helper correctly aborts the transaction. Good.

---

## Issues (prioritized)

### 1. `RepositoryError` leaks to the HTTP layer

**Where:** `module/app/src/main/scala/com/sbboakye/masel/app/services/Helpers.scala:15-16`

```scala
case RepositoryError.DatabaseError =>
  logger.error("Database error occurred") *> RepositoryError.DatabaseError.raiseError
```

The service catches `DatabaseError` only to log it, then re-raises **the same `RepositoryError`** — a persistence-module type. The route's `mapError` doesn't know about `RepositoryError`, so it falls through to the unstructured `case other =>` branch.

This also violates the port abstraction: the whole point of `ChallengeRepository` being in `core` is that the service doesn't depend on persistence types. The service currently does (`import com.sbboakye.masel.persistence.repositories.RepositoryError`).

**Fix:** translate to `AppError` at the boundary.

```scala
case RepositoryError.DatabaseError =>
  logger.error("Database error occurred") *>
    AppError.InternalError("Database unavailable").raiseError
```

(See issue #3 — `InternalError` needs to take a message first.)

---

### 2. `SubmissionService` bypasses `serviceHandler` entirely

`ChallengeService` extends `Helpers[F]` and wraps every repository call in `serviceHandler(...)`. `SubmissionService` does not — it calls `db.withSession(...)` directly. So any Skunk error from `findAll`/`findById`/`create`/`update`/`delete` on submissions:

- isn't logged in the service layer,
- isn't translated to `AppError`,
- arrives at `mapError` as a raw `RepositoryError.DatabaseError` and ends up in the `case other =>` fallback.

**Fix:** make `SubmissionService` extend `Helpers[F]` and wrap every effect, the same way `ChallengeService` does.

---

### 3. `AppError.InternalError` is a parameterless case object

```scala
// module/core/src/main/scala/com/sbboakye/masel/core/errors/AppError.scala:10
case object InternalError extends AppError
```

This means every internal failure surfaces with literally zero context. The HTTP response is hardcoded `"Server error"`, and the logs lose the original cause. For debugging a production Skunk failure you'd have to grep the timestamp and pray.

**Fix:** make it a case class that carries a message and the originating cause.

```scala
final case class InternalError(message: String, cause: Option[Throwable] = None)
    extends RuntimeException(message, cause.orNull)
    with AppError
```

And in `mapError`:

```scala
case AppError.InternalError(message, _) =>
  InternalServerError(ErrorResponse(message))
```

Keep the response body conservative (don't leak internals to clients), but preserve the cause in logs.

---

### 4. `repoHandler` swallows all Skunk error detail

```scala
// module/persistence/.../repositories/Helpers.scala:18-22
fa.handleErrorWith {
  case err: RepositoryError => err.raiseError
  case _ => DatabaseError.raiseError
}
```

Every `Throwable` — `SqlException`, `EofException`, `PostgresErrorException`, connection failure, codec mismatch — becomes the same parameterless `DatabaseError`. Two consequences:

- **Operational:** you can't tell connection loss apart from a malformed query in your alerting.
- **Domain:** legitimate domain-level signals are lost. A `unique_violation` (SQLSTATE `23505`) on `submissions(id)` could become `AppError.Conflict`; a `foreign_key_violation` (`23503`) on `submissions.challenge_id` could become `AppError.NotFound("Challenge", ...)`.

**Two options, in order of effort:**

**Option A — preserve the cause (minimal):**

```scala
sealed trait RepositoryError extends Throwable
object RepositoryError:
  final case class DatabaseError(cause: Throwable)
      extends RuntimeException(cause.getMessage, cause)
      with RepositoryError
```

**Option B — promote SQLSTATE to domain errors (medium):**

```scala
import skunk.exception.PostgresErrorException

def repoHandler[A](fa: F[A]): F[A] = fa.handleErrorWith {
  case err: RepositoryError => err.raiseError
  case e: PostgresErrorException if e.code == "23505" =>
    AppError.Conflict(e.constraintName.getOrElse("unknown")).raiseError
  case e: PostgresErrorException if e.code == "23503" =>
    AppError.ValidationFailed(List(s"Referenced row does not exist: ${e.detail.getOrElse("")}")).raiseError
  case other =>
    RepositoryError.DatabaseError(other).raiseError
}
```

Option B couples persistence to `AppError` (adding a `core` dependency, which it already has transitively via `ChallengeRepository`), but it's the cleanest place to do SQLSTATE → domain translation. The alternative — passing SQLSTATE-bearing exceptions up to the service — also works but spreads the translation logic.

---

### 5. Logging drops the actual `Throwable`

```scala
case RepositoryError.DatabaseError =>
  logger.error("Database error occurred") *> ...
case _ =>
  logger.error("Unexpected error occurred") *> ...
```

log4cats supports `logger.error(t)("message")` — use it. Without the throwable you lose stack trace, cause chain, and (for `PostgresErrorException`) the SQLSTATE / query / detail fields.

```scala
case RepositoryError.DatabaseError(cause) =>
  logger.error(cause)("Database error") *>
    AppError.InternalError("Database unavailable", cause.some).raiseError
case other =>
  logger.error(other)("Unexpected error in service layer") *>
    AppError.InternalError("Internal error", other.some).raiseError
```

---

## Smaller cleanups

### Use `MonadThrow[F]` syntax, not `MonadError[F, Throwable]`

You already import the syntax (`cats.syntax.all.*`). Throughout `ChallengeService` and `SubmissionService`:

```scala
// before
MonadError[F, Throwable].raiseError(AppError.NotFound("Challenge", id.value.toString))

// after
AppError.NotFound("Challenge", id.value.toString).raiseError[F, Challenge]
```

Same with `raiseWhen`. Reads better and doesn't make the reader chase the type lambda.

### Lift the `.flatMap { case None => raiseError ... }` pattern into a helper

It appears in `getChallenge`, `updateChallenge` (×2), `getSubmission`, `updateSubmission` (×2). Worth one extension:

```scala
extension [F[_]: MonadThrow, A](fa: F[Option[A]])
  def orNotFound(entity: String, id: String): F[A] =
    fa.flatMap {
      case Some(a) => a.pure[F]
      case None    => AppError.NotFound(entity, id).raiseError
    }
```

Usage:

```scala
repos.challenges.findById(id).orNotFound("Challenge", id.value.toString)
```

### Standardise the delete-vs-not-found shape

Both delete methods use:

```scala
_ <- MonadError[F, Throwable].raiseWhen(!deleted)(AppError.NotFound("Challenge", ...))
```

which is fine. Just collapse to:

```scala
_ <- AppError.NotFound("Challenge", id.value.toString).raiseError[F, Unit].whenA(!deleted)
```

or simpler with the helper above.

### Drop `RepositoryError` if you adopt Option A *and* keep the leak fixed

Once `repoHandler` translates to a richer error type (or directly to `AppError.InternalError(msg, cause)`), `RepositoryError` is just a redundant intermediate type with a single inhabitant. Either:
- expand it (more cases like `ConnectionLost`, `TransactionDeadlock`), or
- delete it and have `repoHandler` produce `AppError.InternalError` directly.

The persistence module already depends on `core`, so depending on `AppError` is fine.

---

## Recommended target shape

A cleaner version of the funnel, with all four critical issues addressed:

```scala
// core/errors/AppError.scala
sealed trait AppError extends RuntimeException:
  override def getMessage: String = this match
    case AppError.NotFound(entity, id)   => s"$entity not found: $id"
    case AppError.ValidationFailed(errs) => errs.mkString("; ")
    case AppError.Conflict(detail)       => detail
    case AppError.InternalError(m, _)    => m

object AppError:
  final case class NotFound(entity: String, id: String) extends AppError
  final case class ValidationFailed(errors: List[String]) extends AppError
  final case class Conflict(detail: String) extends AppError
  final case class InternalError(message: String, cause: Option[Throwable] = None) extends AppError {
    cause.foreach(initCause)
  }
```

```scala
// persistence/repositories/Helpers.scala
trait Helpers[F[_]: {MonadThrow, LoggerFactory}]:
  private val logger = LoggerFactory[F].getLogger

  def wasDeleted(c: Completion): F[Boolean] = c match
    case Completion.Delete(n) => (n > 0).pure[F]
    case other =>
      AppError.InternalError(s"Unexpected completion: $other").raiseError

  def repoHandler[A](fa: F[A]): F[A] = fa.handleErrorWith {
    case app: AppError => app.raiseError
    case e: PostgresErrorException if e.code == "23505" =>
      AppError.Conflict(e.constraintName.getOrElse(e.detail.getOrElse("conflict"))).raiseError
    case other =>
      logger.error(other)("Database error") *>
        AppError.InternalError("Database error", other.some).raiseError
  }
```

```scala
// app/services/Helpers.scala
trait Helpers[F[_]: {MonadThrow, LoggerFactory}]:
  private val logger = LoggerFactory[F].getLogger

  def serviceHandler[A](fa: F[A]): F[A] = fa.handleErrorWith {
    case app: AppError => app.raiseError
    case other =>
      logger.error(other)("Unexpected service-layer error") *>
        AppError.InternalError("Unexpected error", other.some).raiseError
  }

  extension [A](fa: F[Option[A]])
    def orNotFound(entity: String, id: String): F[A] =
      fa.flatMap {
        case Some(a) => a.pure[F]
        case None    => AppError.NotFound(entity, id).raiseError
      }
```

```scala
// app/routes/ErrorHandling.scala
def mapError[F[_]: MonadThrow](dsl: Http4sDsl[F])(e: Throwable): F[Response[F]] =
  import dsl.*
  e match
    case AppError.NotFound(entity, id)         => NotFound(ErrorResponse(s"$entity not found: $id"))
    case AppError.ValidationFailed(errs)       => BadRequest(ErrorResponse(s"Validation failed: ${errs.mkString(", ")}"))
    case AppError.Conflict(detail)             => Conflict(ErrorResponse(detail))
    case AppError.InternalError(message, _)    => InternalServerError(ErrorResponse(message))
    case _                                     => InternalServerError(ErrorResponse("Internal server error"))
```

Net effect:
- `RepositoryError` is gone; everything in the funnel is `AppError`.
- Skunk causes are preserved in `InternalError.cause` and logged with stack trace.
- SQLSTATE-driven domain mapping is centralised in `repoHandler`.
- Service layer is thinner — no need to enumerate `RepositoryError` cases; just `AppError` pass-through + catch-all.
- `SubmissionService` extends `Helpers` like `ChallengeService` does (don't forget this).

---

## Open questions

- **Should `ValidationFailed` be `NonEmptyList[String]`?** A list of zero errors is meaningless. Probably yes.
- **Do you want `Forbidden` / `Unauthorized` in `AppError` now or when auth lands?** Adding them now means the routes' `mapError` is complete from day one; adding them later means a small refactor of every existing match. Not a forcing function either way.
- **Where should request validation errors be raised?** Today the request types (e.g. `CreateChallengeRequest`) are decoded via circe and rely on Iron to constrain. If decoding fails, http4s returns a 422 with circe's message; that bypasses `AppError.ValidationFailed`. Worth deciding whether to intercept and re-shape.
