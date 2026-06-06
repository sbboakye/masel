# Masel

A backend service for realistic data engineering interview assessments. Candidates solve SQL challenges in isolated, ephemeral Postgres schemas with automated grading.

Sandboxing is schema-based: each run gets its own `CREATE SCHEMA` inside a dedicated sandbox database (scoped `search_path`, a statement timeout, and `DROP SCHEMA ... CASCADE` on teardown) rather than a per-run container.

## Status

**Phase 1 — Foundation** ✅ Done

Project skeleton, domain types, persistence layer (Skunk repositories + connection pool), Flyway migrations, server wiring, and full CRUD HTTP routes for challenges and submissions.

**Phase 2 — Executor Engine** 🚧 In progress

The schema-isolated SQL executor (`Executor`/`SqlExecutor`) is implemented: it provisions a per-id sandbox schema, runs the challenge setup SQL, and executes the candidate's query, capturing the result set as JSON. Still to do: result-set comparison / grading, and wiring the executor into challenge validation and submission grading.

## Tech Stack

| Concern | Choice                                  |
|---|-----------------------------------------|
| Language | Scala 3.8.3                             |
| FP Core | Cats + Cats Effect 3                    |
| HTTP | http4s (Ember)                          |
| Database | PostgreSQL via Skunk                    |
| Migrations | Flyway                                  |
| Streaming | fs2                                     |
| JSON | circe (+ skunk-circe codecs)            |
| Config | ciris                                   |
| Logging | log4cats + Logback                      |
| Refinement Types | Iron                                    |
| Testing | Scalatest + ScalaCheck + Testcontainers |
| Build | sbt 1.12.9                              |

## Project Structure

Base package: `com.sbboakye.masel`

```
masel/
├── build.sbt
├── docker-compose.yml
├── project/
│   ├── Dependencies.scala
│   ├── build.properties
│   └── plugins.sbt
└── module/
    ├── core/          # com.sbboakye.masel.core — Domain models, port traits, error types
    ├── persistence/   # com.sbboakye.masel.persistence — Skunk repositories, Flyway migrations
    ├── engine/        # com.sbboakye.masel.engine — SQL execution and grading
    └── app/           # com.sbboakye.masel.app — Wiring, HTTP routes, Main
```

## Prerequisites

- JDK 21+
- sbt 1.12.9
- Docker & Docker Compose

## Getting Started

Start the database:

```bash
docker compose up -d
```

Run the application:

```bash
sbt app/run
```

Run tests:

```bash
sbt test
```

## v0.1 Scope

Backend-only HTTP service (curl is the client) that:

1. Hosts SQL challenges with expected result sets
2. Accepts candidate SQL submissions via HTTP
3. Executes submissions against an isolated sandbox schema
4. Compares actual vs. expected result sets
5. Returns a graded response with diagnostics

## Architecture

```
HTTP routes (http4s) → Services → Ports ← Persistence (Skunk)
                                       ← Engine (executor)
```

`core` defines domain models and port traits. `persistence` and `engine` implement those ports. `app` wires everything together.

See [docs/architecture.md](docs/architecture.md) for full details.

## License

TBD