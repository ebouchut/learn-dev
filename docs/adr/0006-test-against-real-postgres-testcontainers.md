# Test the persistence layer against a real PostgreSQL (Testcontainers), not H2

- Status: accepted
- Date: 2026-06-16
- Deciders: Eric Bouchut

## Context and Problem Statement

Repository and integration tests need a database. The schema (hand-written
Liquibase migrations, see ADR-0005) relies on PostgreSQL-specific types and
functions: `UUID` with `gen_random_uuid()`, `INET`, `JSONB`, `TIMESTAMPTZ`, and
`BIGINT GENERATED ALWAYS AS IDENTITY`. Which database should the tests run
against?

## Decision Drivers

- Fidelity: tests must exercise the same schema, types, and migrations as production.
- The migrations are PostgreSQL formatted SQL and must apply unchanged.
- Isolation: tests must never touch the dev or production database.
- Test speed and CI simplicity.
- Avoid maintaining a second, test-only schema.

## Considered Options

- In-memory H2 (optionally in PostgreSQL-compatibility mode).
- Real PostgreSQL via Testcontainers (Docker), wired with `@ServiceConnection`.

## Decision Outcome

Chosen: **real PostgreSQL via Testcontainers**. A shared, static
`PostgreSQLContainer` (base class `AbstractPostgresIT`) is wired to Spring Boot
through `@ServiceConnection`; Liquibase applies the real migrations to it and
Hibernate `validate` checks the entity mappings. H2 cannot execute
`gen_random_uuid()`, `INET`, or `JSONB`, so it would require a divergent
test-only schema and give false confidence.

### Test isolation

- Each test run starts a **brand-new, ephemeral PostgreSQL container** (its own
  database, on a random host port) that is destroyed when the JVM exits. It is a
  **distinct database**, separate from the dev database (`learndev` on `:5433`)
  and from production.
- `@ServiceConnection` **overrides `spring.datasource.*`** at test time, so tests
  point at the container and never reach the dev/production database — even though
  `application.yaml` names the dev DB.
- The schema is rebuilt **fresh by Liquibase** on each run, starting from a clean
  state.
- `@DataJpaTest` wraps each test method in a transaction that is **rolled back**,
  so tests do not leak state into one another. (`@SpringBootTest` flows do not
  auto-roll back, so they use unique data.)

### Consequences

- Good: tests run against the real schema, types, and migrations — the migrations
  are themselves exercised on every run; no schema divergence; high-fidelity.
- Good: complete isolation from real data (ephemeral, distinct database; datasource
  overridden), so tests cannot corrupt dev or production.
- Trade-off: requires Docker in dev and CI; container startup adds a few seconds,
  amortized via the shared static container and Spring's context cache.
- Note: `spring-boot-testcontainers`, `testcontainers:junit-jupiter` and
  `testcontainers:postgresql` were added as test dependencies.

## Pros and Cons of the Options

### In-memory H2

- 👍 Fastest; no Docker required
- 👎 Cannot execute Postgres-specific types/functions (`gen_random_uuid()`,
  `INET`, `JSONB`); needs a separate test schema → divergence and false confidence

### Real PostgreSQL via Testcontainers (chosen)

- 👍 Same engine, types, and migrations as production; exercises the migrations;
  fully isolated ephemeral database; no divergence
- 👎 Requires Docker; slower startup (amortized across the run)
