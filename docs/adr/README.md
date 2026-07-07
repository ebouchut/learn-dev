# Architecture Decision Records (ADR)

This directory holds the **Architecture Decision Records** for learn-dev.

An ADR is a short, dated document that captures one significant architectural or
design decision — its context, the choice made, and its consequences. ADRs are
an **append-only, numbered log**: a decision is never silently rewritten; when a
choice changes, a new ADR *supersedes* the old one, preserving the history of
*why* the system is the way it is.

## Format

All ADRs use the **MADR** (Markdown Any Decision Records) short form.
Copy [`template.md`](template.md) when writing a new one.

## Naming convention

```
NNNN-short-title-in-kebab-case.md
```

- `NNNN` — 4-digit zero-padded, sequential (`0001`, `0002`, …). Never reused.
- Title — kebab-case, concise, topic-first.
- Files are never deleted; a superseded ADR stays and its **Status** is updated.

## Status values

`proposed` · `accepted` · `superseded by ADR-NNNN` · `deprecated`

## Index

| ADR | Title | Status |
|-----|-------|--------|
| [0001](0001-use-server-side-sessions-over-jwt.md) | Use server-side sessions instead of JWT for user authentication | accepted |
| [0002](0002-service-to-service-auth-via-service-token.md) | Authenticate service-to-service calls with a service token | proposed |
| [0003](0003-uuid-pk-for-users-bigint-elsewhere.md) | Use a UUID primary key for users, BIGINT identity elsewhere | accepted |
| [0004](0004-use-mailpit-as-local-smtp-catcher.md) | Use Mailpit as the local fake SMTP catcher | accepted |
| [0005](0005-handwrite-liquibase-migrations-over-mcd-ddl.md) | Hand-write the schema as Liquibase migrations, not generated from the MCD | accepted |
| [0006](0006-test-against-real-postgres-testcontainers.md) | Test the persistence layer against a real PostgreSQL (Testcontainers), not H2 | accepted |
| [0007](0007-use-postgresql-over-mysql.md) | Use PostgreSQL as the relational database, not MySQL | accepted |
| [0008](0008-share-singleton-testcontainers-postgres.md) | Share one Testcontainers PostgreSQL as a static singleton, not @Container | accepted |
| [0009](0009-run-tests-under-surefire-not-failsafe.md) | Run all tests under Surefire with the *Test suffix, not Failsafe/*IT | accepted |
| [0010](0010-structure-ci-as-focused-workflows-per-concern.md) | Structure CI as focused workflows per concern, not a monolithic ci.yml | accepted |
| [0011](0011-start-ci-quality-checks-as-advisory-reports.md) | Start CI quality checks as advisory reports, gates come later | superseded by ADR-0012 |
| [0012](0012-publish-test-coverage-to-codecov.md) | Publish test coverage to Codecov | accepted |