# Use PostgreSQL as the relational database, not MySQL

- Status: accepted
- Date: 2026-06-16
- Deciders: Eric Bouchut

## Context and Problem Statement

The platform needs a relational database for its core data (users, roles,
tokens, audit). MySQL was the familiar option from prior experience. The schema,
however, leans on several capabilities: `UUID` with `gen_random_uuid()`, `INET`,
`JSONB`, `TIMESTAMPTZ`, and safe (transactional) migrations. Which relational
engine should the project use?

## Decision Drivers

- Native data types the schema needs: `UUID`, `INET`, `JSONB`, `TIMESTAMPTZ`.
- Migration safety: a failed migration should not leave a half-applied schema.
- SQL standards compliance and advanced indexing (GIN for JSONB, partial and expression indexes).
- Strong data integrity and correctness.
- Good open-source tooling (Liquibase, tbls, Docker images).

## Considered Options

- PostgreSQL
- MySQL / MariaDB

## Decision Outcome

Chosen: **PostgreSQL 17**. It natively provides the types the schema relies on
(`UUID` / `gen_random_uuid()`, `INET`, `JSONB`, `TIMESTAMPTZ`), supports
**transactional DDL** (a failed migration rolls back atomically), and offers
strong standards compliance and advanced indexing. These directly serve
decisions already made: UUID keys (ADR-0003), hand-written migrations (ADR-0005),
and real-Postgres tests (ADR-0006).

### Consequences

- Good: the schema uses native, validated types instead of workarounds (for
  example an IP stored as plain text, or JSON as an opaque string).
- Good: **transactional DDL** makes Liquibase migrations safer. A failure leaves
  the schema unchanged rather than partially applied, whereas MySQL implicitly
  commits DDL.
- Good: `JSONB` with GIN indexing fits the `audit_logs.metadata` use case.
- Trade-off: less prior familiarity than MySQL, and some PostgreSQL-specific SQL
  reduces engine portability. This is acceptable because database portability is
  not a goal.

## Pros and Cons of the Options

### PostgreSQL (chosen)

- 👍 Rich native types (`UUID`, `INET`, `JSONB`, `TIMESTAMPTZ`, arrays);
  transactional DDL; advanced indexing; standards-compliant; extensible
- 👎 Less prior familiarity; some Postgres-specific SQL

### MySQL / MariaDB

- 👍 Familiar; extremely widespread
- 👎 No first-class `INET`; weaker `JSON` and indexing story than `JSONB` plus GIN;
  non-transactional DDL (a failed migration can leave a partial schema);
  historically looser typing and standards compliance
