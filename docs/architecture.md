# Masel — Architecture

## Context

SaaS platform for realistic data engineering interview assessments: sandboxed coding against messy data with automated grading. Candidates are assessed with practical SQL questions evaluated in an isolated sandbox environment.

## Tech Stack

| Concern              | Choice                 |
| -------------------- |------------------------|
| Language             | Scala 3                |
| FP Core              | Cats + Cats Effect 3   |
| HTTP                 | http4s                 |
| Database             | Postgres via Skunk     |
| Migrations           | Flyway                 |
| Streaming            | fs2                    |
| JSON                 | circe + circe-yaml     |
| Config               | ciris                  |
| Logging              | log4cats + Logback     |
| Testing              | Scalatest + ScalaCheck |
| Refinement types     | Iron (Scala 3 native)  |
| Build                | sbt                    |

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
├── project/
│   └── Dependencies.scala
├── migrations/
│   └── V1__initial_schema.sql
├── docker-compose.yml
└── modules/
    ├── core/
    │   └── src/main/scala/com/sbboakye/masel/core/
    │       ├── domain/
    │       │   ├── ids.scala
    │       │   ├── challenge.scala
    │       │   └── submission.scala
    │       ├── ports/
    │       │   └── repositories.scala
    │       └── errors/
    │           └── AppError.scala
    ├── persistence/
    │   └── src/main/scala/com/sbboakye/masel/persistence/
    │       ├── repositories/
    │       │   └── SkunkChallengeRepository.scala
    │       ├── queries/
    │       ├── meta/
    │       └── FlywayMigrator.scala
    ├── engine/
    │   └── src/main/scala/com/sbboakye/masel/engine/
    │       ├── executor/
    │       │   ├── ChallengeValidation.scala
    │       │   └── SubmissionValidation.scala
    │       └── ports/
    └── app/
        └── src/main/scala/com/sbboakye/masel/app/
            ├── Main.scala
            ├── routes/
            ├── requests/
            └── services/
```

## Domain Model

### Challenge

Represents a SQL problem for candidates to solve.

| Field              | Type                | Notes                                                                                          |
| ------------------ | ------------------- | ---------------------------------------------------------------------------------------------- |
| `id`               | `ChallengeId`       | Opaque type wrapping UUID                                                                      |
| `title`            | `String`            |                                                                                                |
| `instructions`     | `String`            | Problem description shown to candidates                                                       |
| `status`           | `ChallengeStatus`   | Enum: `Draft`, `Validated`, `Active`, `Archived`                                               |
| `expectedSolution` | `String`            | Reference SQL query provided by the challenge author                                           |
| `output`           | `Option[Json]`      | Expected result set as JSONB. Populated by running `expectedSolution` against the sandbox at challenge creation time. Empty until validated. |
| `allottedTime`     | `Int`               | Time limit in minutes                                                                          |
| `difficulty`       | `Difficulty`        | Enum                                                                                           |
| `createdAt`        | `Instant`           |                                                                                                |
| `updatedAt`        | `Instant`           |                                                                                                |

### Submission

Represents a candidate's attempt at solving a challenge.

| Field               | Type              | Notes                                                                                          |
| ------------------- | ----------------- | ---------------------------------------------------------------------------------------------- |
| `id`                | `SubmissionId`    | Opaque type wrapping UUID                                                                      |
| `challengeId`       | `ChallengeId`    | FK to Challenge                                                                                |
| `candidateSolution` | `String`          | SQL submitted by the candidate                                                                 |
| `output`            | `Option[Json]`    | Candidate's result set as JSONB. Populated by running `candidateSolution` against the sandbox. Empty until executed. |
| `score`             | `Option[Int]`     | Grading result (0–100). Empty until graded.                                                    |
| `createdAt`         | `Instant`         |                                                                                                |
| `updatedAt`         | `Instant`         |                                                                                                |

### Enums

- **ChallengeStatus:** `Draft` | `Validated` | `Active` | `Archived`
- **Difficulty:** TBD (e.g. `Easy` | `Medium` | `Hard`)

### ID Types

All IDs use opaque types (not type aliases) for compile-time safety.

### Relationships

- One Challenge → many Submissions

## Database Schema

### `challenges`

```sql
CREATE TABLE challenges (
    id               UUID PRIMARY KEY,
    title            VARCHAR NOT NULL,
    instructions     TEXT NOT NULL,
    status           challenge_status NOT NULL DEFAULT 'draft',
    expected_solution TEXT NOT NULL,
    output           JSONB,
    allotted_time    INT NOT NULL,
    difficulty       difficulty NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
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
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Indexes and Constraints

- FK indexes on `submissions.challenge_id`
- Index on `challenges.status`
- CHECK constraints where appropriate

## Error Handling

Typed error hierarchy in `core/errors/`:

```
sealed trait AppError
├── NotFound(entity: String, id: UUID)
├── ValidationFailed(errors: List[String])
└── InternalError(message: String, cause: Option[Throwable])
```

Services return `F[A]]`. Http4s route handlers map `AppError` variants to HTTP status codes.

## API Endpoints

| Method | Path                                        | Description                                                        |
| ------ | ------------------------------------------- | ------------------------------------------------------------------ |
| POST   | `/api/v1/challenges/`                       | Create a challenge. Validates `expectedSolution` by running it against a sandbox. Returns the challenge with status `Validated` on success. |
| POST   | `/api/v1/challenges/{challengeId}/submissions` | Submit a candidate solution. Executes it against the sandbox, compares output to the challenge's expected output, grades, and returns the submission with score. |

## Phases

### Phase 1 — Foundation

**Goal:** Compilable project skeleton with domain types, persistence, migrations, and wiring.

**Deliverables:**

- Multi-module sbt build (`core`, `persistence`, `engine`, `app`) with `project/Dependencies.scala`
- Domain types: `Challenge`, `Submission`, opaque ID types, `ChallengeStatus` enum, `AppError` hierarchy
- Port traits: `ChallengeRepository[F[_]]`, `SubmissionRepository[F[_]]`
- Flyway migration: `V1__initial_schema.sql` with both tables, enums, indexes, triggers
- Skunk persistence adapters implementing repository ports
- `Main.scala` as `IOApp` — runs Flyway migration on startup, assembles server via `Resource`
- Docker Compose with Postgres

**Definition of done:** `sbt app/run` starts the server, Flyway creates tables, and the app shuts down cleanly.

### Phase 2 — Executor Engine

**Goal:** SQL execution in sandboxed Postgres containers, grading via result set comparison.

**Deliverables:**

- Engine port trait (e.g. `SqlExecutor[F[_]]`) in `engine/ports/`
- Challenge validation: run `expectedSolution` against a sandboxed Postgres container, capture output as JSONB, update challenge status to `Validated`
- Submission execution: run `candidateSolution` against the same sandbox, capture output
- Grading: row-wise comparison of candidate output against expected output. Handles column ordering, row ordering (when order doesn't matter: compare row counts + set membership), and produces a score out of 100
- HTTP routes for both API endpoints wired through services
- Resource-safe container lifecycle management via Cats Effect `Resource`

**Definition of done:** `curl` can create a challenge with valid SQL, submit a candidate solution, and receive a graded score in the response.
