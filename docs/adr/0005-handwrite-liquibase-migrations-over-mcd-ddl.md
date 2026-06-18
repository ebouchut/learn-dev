# Hand-write the database schema as Liquibase migrations, not generated from the MCD

- Status: accepted
- Date: 2026-06-16
- Deciders: Eric Bouchut

## Context and Problem Statement

The Merise MCD (mocodo) can emit a PostgreSQL DDL via `mocodo -t postgres`, and
this was initially used to scaffold the schema. The generated DDL proved too
poor to create the real database: the MCD carries no physical types, so every
column came out as `VARCHAR(42)`, with no real types, constraints, defaults, or
indexes, and with invalid table names (e.g. `User`, a reserved word). How should
the database schema be authored and evolved?

## Decision Drivers

- The schema needs real PostgreSQL types, constraints, defaults, and indexes.
- The schema evolves over time and must be versioned and reviewable.
- A single, executable source of truth for the database structure.
- Avoid maintaining a generator whose output must be heavily rewritten anyway.

## Considered Options

- Generate the DDL from the MCD (`mocodo -t postgres`) and apply it.
- Hand-write the schema as Liquibase migrations; keep the MCD as documentation only.

## Decision Outcome

Chosen: **hand-write the schema as Liquibase migrations**
(`src/main/resources/db/changelog/changes/`, formatted SQL, append-only, one
changeset per file). The mocodo DDL generation is removed. The MCD/MLD/MPD remain
**documentation** of the model, not a source for the DDL. A `check-schema-drift`
guard verifies the MCD lists every column present in the migrations.

### Consequences

- Good: full control over PostgreSQL types, constraints, defaults, indexes, and
  naming; the schema is versioned, reviewable, and rollback-able.
- Good: one executable source of truth (the migrations); the MPD (`tbls`) is
  generated from the live database, so it cannot drift.
- Trade-off: the MCD must be kept in sync with the migrations by hand — mitigated
  by the `check-schema-drift` CI check.
- Note: the `mocodo -t postgres` DDL target and `docs/database/ddl/` were removed.

## Pros and Cons of the Options

### Generate DDL from the MCD (mocodo)

- 👍 Single source (the MCD); no hand-written SQL
- 👎 MCD has no physical types → `VARCHAR(42)` everywhere; no constraints,
  defaults, or indexes; invalid/reserved table names; output must be rewritten,
  so it is not actually a usable source

### Hand-write Liquibase migrations (chosen)

- 👍 Real types, constraints, indexes; versioned, reviewable, rollback-able;
  executable source of truth
- 👎 MCD and migrations kept in sync manually (mitigated by `check-schema-drift`)
