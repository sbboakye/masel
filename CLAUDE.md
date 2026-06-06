# Masel

SaaS platform for realistic data engineering interview assessments. Candidates solve SQL challenges in isolated, ephemeral Postgres schemas; submissions are auto-graded by result-set comparison.

Sandbox isolation is **schema-based**, not container-based: each challenge/submission run gets its own `CREATE SCHEMA` inside a dedicated sandbox database, with `search_path` scoped to it and `DROP SCHEMA ... CASCADE` on release. (Testcontainers is used only in integration tests, not at runtime.)

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

- **core** — domain models (`Challenge`, `Submission`), opaque ID types (over `UUID`), refined SQL value types (`SetupSql`, `QuerySql`), `Score`, the `SchemaScope` typeclass, port traits (`ChallengeRepository`, `SubmissionRepository`, `AppDb`, `SqlExecutor`), `DatabaseConfig`/`ServerConfig`, `AppError` hierarchy
- **persistence** — Skunk adapters implementing repository ports; connection pooling (`PoolSession`); Flyway migrations in `module/persistence/src/main/resources/db/migration/`
- **engine** — `SqlExecutor` implementation (`Executor`): schema-isolated SQL execution. Grading logic is planned but not yet implemented
- **app** — http4s (Ember) routes, services, ciris config, `Main.scala` (IOApp)

All effectful code uses `F[_]: Sync` / `F[_]: Async` constraints. Services return `F[A]` and raise typed `AppError`s (via `MonadThrow`); route handlers recover them to HTTP status codes (`ErrorHandling.recoverAppErrors`).

## Tech Stack

| Concern        | Library                                 |
|----------------|-----------------------------------------|
| FP             | Cats + Cats Effect 3                    |
| HTTP           | http4s (Ember)                          |
| Database       | Skunk (Postgres)                        |
| Migrations     | Flyway                                  |
| Streaming      | fs2                                     |
| JSON           | circe + circe-yaml                      |
| Config         | ciris                                   |
| Logging        | log4cats + Logback                      |
| Refinement     | Iron (Scala 3 opaque types)             |
| Testing        | Scalatest + ScalaCheck + Testcontainers |

## Package Structure

```
com.sbboakye.masel.<module>.*
```

- `com.sbboakye.masel.app` — Main, routes, services, requests
- `com.sbboakye.masel.persistence` — FlywayMigrator, repositories, queries, meta codecs
- `com.sbboakye.masel.engine` — `Executor` (schema-isolated SQL execution)
- `com.sbboakye.masel.core` — domain, ports, errors

## Key Conventions

- **Scala 3** — use `given`/`using`, opaque types, extension methods, enum
- **Tagless Final** — all algebra traits are `trait Foo[F[_]]`; implementations take typeclass constraints
- **Iron** — use for refined types (validated strings, positive ints, etc.) rather than raw primitives at domain boundaries
- **Error handling** — `AppError` sealed hierarchy (extends `RuntimeException`); services raise `AppError`s via `MonadThrow`; routes recover them to HTTP status codes
- **Resource safety** — database sessions/pool, HTTP server, and sandbox schemas managed via `cats.effect.Resource` (sandbox schema is created on acquire, dropped on release)
- **No nulls, no exceptions** — use `Option`, `Either`, or raise typed `AppError`s
- **Refined SQL types** — `SetupSql` and `QuerySql` are opaque over `NonEmptyString` with smart constructors: `SetupSql.from` requires a `;`; `QuerySql.from` forbids `;` (single statement, safe to wrap as a subquery)
- **`SchemaScope`** — typeclass abstracting over id types that can name a sandbox schema (`ChallengeId` → `chal_…`, `SubmissionId` → `sub_…`); the executor is generic over `[A: SchemaScope]` rather than using a union type

## Database

Flyway migrations live in `module/persistence/src/main/resources/db/migration/`.
Migration naming: `V{n}__{description}.sql` (e.g. `V1__initial_schema.sql`).

Postgres enums: `challenge_status` (`draft`, `validated`, `active`, `archived`), `challenge_difficulty` (`easy`, `medium`, `hard`).

Tables: `challenges` (includes `setup_sql`, `expected_solution`, `output JSONB`), `submissions` (`candidate_solution`, `output JSONB`, `score`; FK: `submissions.challenge_id → challenges.id`).

## Testing

- Unit tests: Scalatest + ScalaCheck (property-based)
- Integration tests (persistence): Testcontainers Postgres — tests spin up a real Postgres container; no mocking the database
- Test files mirror `src/main/scala` structure under `src/test/scala`

## Current Status

**Phase 1 (Foundation)** — Done. ciris config, Skunk session pool (`PoolSession`), and Flyway are wired into `Main.scala`; repository ports (`core`) and Skunk adapters (`persistence`) are implemented; full CRUD HTTP routes exist for challenges and submissions plus a heartbeat endpoint, all backed by services over `AppDb`.

**Phase 2 (Executor Engine)** — In progress.
- Done: `SqlExecutor` port + `Executor` implementation. `sandbox[A: SchemaScope]` opens a single Skunk session, creates a per-id schema, sets `search_path` and a `statement_timeout` (10s), and drops the schema (`CASCADE`) on release. `execute` runs the (multi-statement) `setup` then wraps the candidate `query` in `SELECT coalesce(json_agg(t), '[]'::jsonb) FROM (<query>) t` to capture an arbitrary result set as a single `Json`.
- Not yet done: grading (result-set comparison + scoring), and wiring the executor into the services/routes (challenge validation on create, submission grading on submit). `Executor` is not yet referenced from `app`.
