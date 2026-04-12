# Masel

SaaS platform for realistic data engineering interview assessments. Candidates solve SQL challenges in sandboxed Postgres containers; submissions are auto-graded by result-set comparison.

## Build & Run

```bash
# Compile all modules
sbt compile

# Run the app
sbt app/run

# Run tests
sbt test

# Run tests for a specific module
sbt persistence/test

# Format code
sbt scalafmt
```

## Architecture

Four sbt modules with strict dependency order:

```
core  ←  persistence
core  ←  engine
core + persistence + engine  ←  app
```

- **core** — domain models (`Challenge`, `Submission`), opaque ID types, port traits (`ChallengeRepository`, `SubmissionRepository`), `AppError` hierarchy
- **persistence** — Skunk adapters implementing repository ports; Flyway migrations in `module/persistence/src/main/resources/db.migration/`
- **engine** — SQL executor port and implementations (sandboxed Postgres containers, grading logic)
- **app** — http4s routes, services, ciris config, `Main.scala` (IOApp)

All effectful code uses `F[_]: Sync` / `F[_]: Async` constraints. Services return `F[Either[AppError, A]]`.

## Tech Stack

| Concern        | Library                       |
|----------------|-------------------------------|
| FP             | Cats + Cats Effect 3          |
| HTTP           | http4s (Ember)                |
| Database       | Skunk (Postgres)              |
| Migrations     | Flyway                        |
| Streaming      | fs2                           |
| JSON           | circe + circe-yaml            |
| Config         | ciris                         |
| Logging        | log4cats + Logback            |
| Refinement     | Iron (Scala 3 opaque types)   |
| Testing        | MUnit + MUnit Cats Effect + ScalaCheck + Testcontainers |

## Package Structure

```
com.sbboakye.masel.<module>.*
```

- `com.sbboakye.masel.app` — Main, routes, services, requests
- `com.sbboakye.masel.persistence` — FlywayMigrator, repositories, queries, meta codecs
- `com.sbboakye.masel.engine` — executor, grading
- `com.sbboakye.masel.core` — domain, ports, errors

## Key Conventions

- **Scala 3** — use `given`/`using`, opaque types, extension methods, enum
- **Tagless Final** — all algebra traits are `trait Foo[F[_]]`; implementations take typeclass constraints
- **Iron** — use for refined types (validated strings, positive ints, etc.) rather than raw primitives at domain boundaries
- **Error handling** — `AppError` sealed hierarchy; routes map errors to HTTP status codes
- **Resource safety** — database sessions, HTTP server, and sandbox containers managed via `cats.effect.Resource`
- **No nulls, no exceptions** — use `Option`, `Either`, or `F[Either[AppError, A]]`

## Database

Flyway migrations live in `module/persistence/src/main/resources/db.migration/`.
Migration naming: `V{n}__{description}.sql` (e.g. `V1__initial_schema.sql`).

Postgres enums: `challenge_status` (`draft`, `validated`, `active`, `archived`), `difficulty` (`easy`, `medium`, `hard`).

Tables: `challenges`, `submissions` (FK: `submissions.challenge_id → challenges.id`).

## Testing

- Unit tests: MUnit + ScalaCheck (property-based)
- Integration tests (persistence): Testcontainers Postgres — tests spin up a real Postgres container; no mocking the database
- Test files mirror `src/main/scala` structure under `src/test/scala`

## Current Status

**Phase 1 (Foundation)** — in progress. Skeleton compiled; `FlywayMigrator` implemented; `Main.scala` is a stub returning `ExitCode.Success`. Next: wire ciris config, Skunk session pool, Flyway into `Main.scala`; implement repository port traits in `core`; implement Skunk adapters in `persistence`.

**Phase 2 (Executor Engine)** — not started. SQL execution in sandboxed containers, grading, HTTP routes.
