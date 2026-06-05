# Masel

A backend service for realistic data engineering interview assessments. Candidates solve SQL challenges in isolated sandbox environments with automated grading.

## Status

**Phase 1 — Foundation** (in progress)

Building the compilable project skeleton: domain types, persistence layer, Flyway migrations, and server wiring.

## Tech Stack

| Concern | Choice                 |
|---|------------------------|
| Language | Scala 3                |
| FP Core | Cats + Cats Effect 3   |
| HTTP | http4s (Ember)         |
| Database | PostgreSQL via Skunk   |
| Migrations | Flyway                 |
| Streaming | fs2                    |
| JSON | circe                  |
| Config | ciris                  |
| Refinement Types | Iron                   |
| Testing | Scalatest + ScalaCheck |
| Build | sbt 1.12.9             |

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
3. Executes submissions against an isolated database
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