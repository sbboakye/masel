# Masel — Architecture

## Context

SaaS platform for realistic data engineering interview assessments: sandboxed coding against messy data with automated grading. Candidates are assessed with practical SQL questions evaluated in an isolated sandbox.

Sandboxing is **schema-based**, not container-based. Each challenge or submission run provisions a dedicated Postgres schema (`CREATE SCHEMA <prefix>_<uuid>`) inside a single sandbox database, scopes the connection's `search_path` to it, applies a `statement_timeout`, runs the SQL, and tears the schema down with `DROP SCHEMA ... CASCADE` when the `Resource` releases. Testcontainers is used only for integration tests, not at runtime.

## Tech Stack

| Concern              | Choice                 |
| -------------------- |------------------------|
| Language             | Scala 3.8.3                             |
| FP Core              | Cats + Cats Effect 3                    |
| HTTP                 | http4s (Ember)                          |
| Database             | Postgres via Skunk                      |
| Migrations           | Flyway                                  |
| Streaming            | fs2                                     |
| JSON                 | circe + circe-yaml (+ skunk-circe)      |
| Config               | ciris                                   |
| Logging              | log4cats + Logback                      |
| Testing              | Scalatest + ScalaCheck + Testcontainers |
| Refinement types     | Iron (Scala 3 native)                   |
| Build                | sbt 1.12.9                              |

## Architecture

Core defines domain models and port traits (interfaces). Persistence and engine modules implement those ports. The app module wires everything together and owns HTTP routes, services, and request DTOs.

```
HTTP routes (http4s) → Services (business logic) → Ports ← [Persistence (Skunk), Engine (executor)]
```

### Module Dependencies

```
core            (depends on nothing)
  ↑
  ├── persistence   (implements repository ports)
  ├── engine        (implements executor ports)
  │
  └── app           (depends on core, persistence, engine — wires everything)
```

## Project Structure

```
masel/
├── build.sbt
├── docker-compose.yml
├── project/
│   ├── Dependencies.scala
│   ├── build.properties
│   └── plugins.sbt
└── module/
    ├── core/
    │   └── src/main/scala/com/sbboakye/masel/core/
    │       ├── domain/
    │       │   ├── Ids.scala            # opaque ChallengeId / SubmissionId (over UUID)
    │       │   ├── IronTypes.scala      # NonEmptyString, PositiveInt, Score, …
    │       │   ├── Challenge.scala
    │       │   ├── Submission.scala
    │       │   ├── SetupSql.scala       # opaque SetupSql + smart constructor
    │       │   ├── QuerySql.scala       # opaque QuerySql + smart constructor
    │       │   ├── SchemaScope.scala    # typeclass: id -> sandbox schema name
    │       │   └── Config.scala         # DatabaseConfig / ServerConfig
    │       ├── ports/
    │       │   ├── Repositories.scala   # ChallengeRepository, SubmissionRepository, Repos, AppDb
    │       │   └── SqlExecutor.scala
    │       └── errors/
    │           └── AppError.scala
    ├── persistence/
    │   ├── src/main/resources/db/migration/
    │   │   └── V1__initial_schema.sql
    │   └── src/main/scala/com/sbboakye/masel/persistence/
    │       ├── FlywayMigrator.scala
    │       ├── codec/                   # SkunkCodec, RefinedCodec
    │       ├── queries/                 # ChallengeQueries, SubmissionQueries
    │       ├── repositories/            # SkunkChallengeRepository, SkunkSubmissionRepository, SkunkAppDb
    │       └── session/                 # PoolSession
    ├── engine/
    │   └── src/main/scala/com/sbboakye/masel/engine/
    │       └── Executor.scala           # SqlExecutor impl (schema-isolated execution)
    └── app/
        └── src/main/scala/com/sbboakye/masel/app/
            ├── Main.scala
            ├── AppConfig.scala
            ├── endpoints/               # BaseEndpoint (/api/v1), HeartbeatResponse
            ├── routes/                  # Challenge/Submission/Heartbeat routes, ErrorHandling
            ├── requests/                # request DTOs
            └── services/                # ChallengeService, SubmissionService, Helpers
```

## Domain Model

### Challenge

Represents a SQL problem for candidates to solve.

| Field              | Type                  | Notes                                                                                          |
| ------------------ | --------------------- | ---------------------------------------------------------------------------------------------- |
| `id`               | `ChallengeId`         | Opaque type wrapping `UUID`                                                                     |
| `title`            | `NonEmptyString`      | Iron-refined                                                                                    |
| `instructions`     | `NonEmptyString`      | Problem description shown to candidates                                                         |
| `setupSql`         | `SetupSql`            | DDL/DML run to build the sandbox (opaque over `NonEmptyString`; must contain `;`)               |
| `status`           | `ChallengeStatus`     | Enum: `Draft`, `Validated`, `Active`, `Archived`                                                |
| `expectedSolution` | `QuerySql`            | Reference query (opaque over `NonEmptyString`; must **not** contain `;`)                        |
| `output`           | `Option[Json]`        | Expected result set as JSONB. Populated by running `expectedSolution` against the sandbox at challenge validation time. Empty until validated. |
| `allottedTime`     | `PositiveInt`         | Time limit in minutes (Iron-refined)                                                            |
| `difficulty`       | `ChallengeDifficulty` | Enum: `Easy`, `Medium`, `Hard`                                                                  |
| `createdAt`        | `OffsetDateTime`      |                                                                                                |
| `updatedAt`        | `OffsetDateTime`      |                                                                                                |

### Submission

Represents a candidate's attempt at solving a challenge.

| Field               | Type              | Notes                                                                                          |
| ------------------- | ----------------- | ---------------------------------------------------------------------------------------------- |
| `id`                | `SubmissionId`    | Opaque type wrapping `UUID`                                                                     |
| `challengeId`       | `ChallengeId`     | FK to Challenge                                                                                 |
| `candidateSolution` | `QuerySql`        | SQL submitted by the candidate (opaque over `NonEmptyString`; must **not** contain `;`)         |
| `output`            | `Option[Json]`    | Candidate's result set as JSONB. Populated by running `candidateSolution` against the sandbox. Empty until executed. |
| `score`             | `Option[Score]`   | Grading result (Iron-refined `Int`). Empty until graded.                                        |
| `createdAt`         | `OffsetDateTime`  |                                                                                                |
| `updatedAt`         | `OffsetDateTime`  |                                                                                                |

### Enums

- **ChallengeStatus:** `Draft` | `Validated` | `Active` | `Archived` (Postgres type `challenge_status`)
- **ChallengeDifficulty:** `Easy` | `Medium` | `Hard` (Postgres type `challenge_difficulty`)

### Refined value types

- **`SetupSql`** — opaque over `NonEmptyString`. `SetupSql.from` requires a `;` (it's a multi-statement script).
- **`QuerySql`** — opaque over `NonEmptyString`. `QuerySql.from` rejects `;` so the candidate query is a single statement, safe to wrap as a subquery during execution.
- **`Score`**, **`PositiveInt`**, **`NonEmptyString`** — Iron-refined primitives used at domain boundaries.

### ID Types

`ChallengeId` and `SubmissionId` are opaque types over `UUID` (not type aliases) for compile-time safety. Because both erase to `UUID`, code that must work for either uses the **`SchemaScope`** typeclass rather than a union type — `SchemaScope` maps an id to its sandbox-schema prefix (`chal` / `sub`) and exposes its `UUID`.

### Relationships

- One Challenge → many Submissions

## Database Schema

Defined in `module/persistence/src/main/resources/db/migration/V1__initial_schema.sql`. Enum types: `challenge_status` and `challenge_difficulty`. Timestamps are `NOT NULL` without a DB default — the application supplies `created_at`/`updated_at`.

### `challenges`

```sql
CREATE TABLE challenges (
    id                UUID PRIMARY KEY,
    title             VARCHAR NOT NULL,
    instructions      TEXT NOT NULL,
    setup_sql         TEXT NOT NULL,
    status            challenge_status NOT NULL DEFAULT 'draft',
    expected_solution TEXT NOT NULL,
    output            JSONB,
    allotted_time     INT NOT NULL,
    difficulty        challenge_difficulty NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL
);
```

### `submissions`

```sql
CREATE TABLE submissions (
    id                  UUID PRIMARY KEY,
    challenge_id        UUID NOT NULL REFERENCES challenges(id),
    candidate_solution  TEXT NOT NULL,
    output              JSONB,
    score               INT,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL
);
```

### Indexes and Constraints

- `idx_challenges_status` on `challenges(status)`
- `idx_submissions_challenge_id` on `submissions(challenge_id)`
- FK: `submissions.challenge_id → challenges(id)`

## Error Handling

Typed error hierarchy in `core/errors/AppError.scala` (`AppError extends RuntimeException`):

```
sealed trait AppError
├── NotFound(entity: String, id: String)
├── ValidationFailed(errors: List[String])
└── InternalError(message: String, cause: Option[Throwable] = None)
```

Services return `F[A]` and **raise** `AppError`s via `MonadThrow` (with helpers like `orNotFound`). Route handlers recover them to HTTP status codes via `ErrorHandling.recoverAppErrors` — e.g. `NotFound → 404`, `ValidationFailed → 422/400`, `InternalError → 500`.

## API Endpoints

Base path: `/api/v1` (`BaseEndpoint`). All resource bodies are JSON (circe).

| Method | Path                              | Description                                              |
| ------ | --------------------------------- | ------------------------------------------------------- |
| GET    | `/api/v1/challenges?limit&offset` | List challenges (paginated; defaults limit 10/offset 0) |
| GET    | `/api/v1/challenges/{id}`         | Get a challenge by id                                   |
| POST   | `/api/v1/challenges`              | Create a challenge (status starts `Draft`)              |
| PUT    | `/api/v1/challenges/{id}`         | Update a challenge                                       |
| DELETE | `/api/v1/challenges/{id}`         | Delete a challenge                                       |
| GET    | `/api/v1/submissions?limit&offset`| List submissions (paginated)                            |
| GET    | `/api/v1/submissions/{id}`        | Get a submission by id                                  |
| POST   | `/api/v1/submissions`             | Create a submission                                     |
| PUT    | `/api/v1/submissions/{id}`        | Update a submission                                     |
| DELETE | `/api/v1/submissions/{id}`        | Delete a submission                                     |
| GET    | `/health`                         | Liveness check                                          |
| GET    | `/ready`                          | Readiness check (verifies DB readiness)                 |

> **Note:** these are the CRUD routes currently wired through the services. The assessment flow — validating `expectedSolution` on challenge creation and grading `candidateSolution` on submission — is not yet wired to the executor (see Phase 2 status below).

## Phases

### Phase 1 — Foundation ✅ Done

**Goal:** Compilable project skeleton with domain types, persistence, migrations, and wiring.

**Delivered:**

- Multi-module sbt build (`core`, `persistence`, `engine`, `app`) with `project/Dependencies.scala`
- Domain types: `Challenge`, `Submission`, opaque ID types, refined SQL types (`SetupSql`, `QuerySql`), `Score`, `ChallengeStatus`/`ChallengeDifficulty` enums, `AppError` hierarchy
- Port traits: `ChallengeRepository[F[_]]`, `SubmissionRepository[F[_]]`, `Repos`, `AppDb`
- Flyway migration `V1__initial_schema.sql` with both tables, enums, and indexes
- Skunk persistence adapters + connection pool (`PoolSession`, `SkunkAppDb`)
- `Main.scala` as `IOApp` — runs Flyway migration on startup, assembles pool + server via `Resource`
- ciris config (`AppConfig`, `DatabaseConfig`, `ServerConfig`)
- Full CRUD HTTP routes for challenges and submissions, plus `/health` and `/ready`
- Docker Compose with Postgres

### Phase 2 — Executor Engine 🚧 In progress

**Goal:** Schema-isolated SQL execution and grading via result-set comparison.

**Delivered so far:**

- `SqlExecutor[F[_], B]` port + `Executor` implementation
- `sandbox[A: SchemaScope]`: opens a single Skunk session, creates a per-id schema, sets `search_path` to it, applies `statement_timeout = '10s'`, and drops the schema (`CASCADE`) on `Resource` release — all id types handled generically via `SchemaScope` (no union types)
- `execute[A: SchemaScope]`: runs the multi-statement `setup` SQL, then executes the candidate `query` wrapped as `SELECT coalesce(json_agg(t), '[]'::jsonb) FROM (<query>) t`, capturing an arbitrary result set as a single `io.circe.Json` via the skunk-circe `jsonb` codec
- Resource-safe sandbox lifecycle via Cats Effect `Resource`

**Still to do:**

- Grading: compare candidate output against expected output (handling column/row ordering — e.g. row counts + set membership) and produce a `Score`
- Challenge validation flow: run `expectedSolution` on create, store `output`, set status to `Validated`
- Submission grading flow: run `candidateSolution`, compare, persist `score`
- Wire `Executor` into the services/routes (it is not yet referenced from `app`)
- Decide executor error semantics (likely `F[Either[AppError, Json]]` so candidate-SQL failures are normal outcomes, not raised errors)

**Definition of done:** `curl` can create a challenge with valid SQL, submit a candidate solution, and receive a graded score in the response.
