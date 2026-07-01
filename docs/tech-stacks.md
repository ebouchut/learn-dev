# Tech Stack

A catalogue of the tools, languages, frameworks, and libraries used in learn-dev,
with versions and a one-line reason for each. This file answers **"what do we
use?"** For **"how do the pieces fit together?"** see [ARCHITECTURE.md](../ARCHITECTURE.md);
for **"why this over the alternative?"** see the [ADRs](adr/README.md); for term
definitions see [GLOSSARY.md](../GLOSSARY.md).

> Versions come from `pom.xml`, `.sdkmanrc`, and `docker-compose.yaml`. Update this
> file when those change.

## Language and runtime

| Technology | Version | Why here |
|------------|---------|----------|
| Java | 21 (`21.0.8-tem`) | LTS runtime; project language. Pinned via `.sdkmanrc`. |

## Application framework

| Technology | Version | Why here |
|------------|---------|----------|
| Spring Boot | 3.5.14 | Application framework and dependency management (parent POM). |
| Spring Web (MVC) | via Boot | Server-side MVC controllers and view rendering. |
| Spring Security | via Boot | Authentication and authorization (session form login). |
| Spring Data JPA | via Boot | Repository abstraction over the relational store. |
| Spring Boot Actuator | via Boot | Operational endpoints (health, info). |
| Bean Validation (Hibernate Validator) | via Boot | Declarative form/DTO constraints enforced with `@Valid`. |

## View layer

| Technology | Version | Why here |
|------------|---------|----------|
| Thymeleaf | via Boot | Server-side HTML template engine. |
| thymeleaf-extras-springsecurity6 | via Boot | `sec:` dialect to read the authenticated user in templates. |
| HTML / CSS / JavaScript | — | Front-end markup, styling, and behaviour. |

## Persistence and data

| Technology | Version | Why here |
|------------|---------|----------|
| Hibernate ORM | via Boot (JPA) | Maps Java entities to relational tables. |
| PostgreSQL | 17 | Relational core (users, roles, courses). See [ADR-0007](adr/0007-use-postgresql-over-mysql.md). |
| PostgreSQL JDBC driver | via Boot | Database connectivity. |
| Liquibase | via Boot | Schema migrations, applied at startup. See [ADR-0005](adr/0005-handwrite-liquibase-migrations-over-mcd-ddl.md). |
| MongoDB | 8 | Provisioned (Docker) and configured (URI) for future content storage; not yet wired to a feature (Mongo auto-config is excluded in tests). |

## Configuration and secrets

| Technology | Version | Why here |
|------------|---------|----------|
| spring-dotenv (`springboot3-dotenv`) | BOM-managed | Loads `./.env` at startup from the working directory. |
| 1Password Environments | — | Provisions `.env` (a FIFO) and the `gh` token; never edited by hand. |

## Build and dependency management

| Technology | Version | Why here |
|------------|---------|----------|
| Maven | 3.9.16 | Build and dependency management. Pinned via `.sdkmanrc`. |
| Maven Wrapper (`./mvnw`) | — | Reproducible Maven invocation without a global install. |
| spring-boot-maven-plugin | via Boot | Runs the app (`spring-boot:run`) and builds the executable jar. |
| Lombok | via Boot | Compile-time boilerplate generation (getters, constructors). |
| SDKMAN | — | Activates the pinned Java and Maven versions (`sdk env`). |

## Testing

| Technology | Version | Why here |
|------------|---------|----------|
| JUnit 5 (Jupiter) | via Boot | Test framework. |
| Mockito | via Boot | Mocking for unit tests. |
| AssertJ | via Boot | Fluent assertions. |
| Spring Boot Test | via Boot | Context-loading and slice-test support. |
| Spring Security Test | via Boot | `formLogin()`, `csrf()`, and auth assertions in MockMvc. |
| MockMvc | via Boot | Drives HTTP requests without a live server. |
| Testcontainers (`junit-jupiter`, `postgresql`) | via Boot | Real PostgreSQL for tests. See [ADR-0006](adr/0006-test-against-real-postgres-testcontainers.md), [ADR-0008](adr/0008-share-singleton-testcontainers-postgres.md). |

All tests run under the Maven Surefire plugin (no Failsafe); see [ADR-0009](adr/0009-run-tests-under-surefire-not-failsafe.md).

## Containers and local infrastructure

| Technology | Version | Why here |
|------------|---------|----------|
| Podman | — | Daemonless container engine; the `docker` drop-in on the dev machine. |
| Docker Compose | — | Runs Postgres and Mongo locally (`docker compose up -d`). |
| Mailpit | — | Planned local fake SMTP catcher for the email flow. See [ADR-0004](adr/0004-use-mailpit-as-local-smtp-catcher.md). |

## Documentation and modelling tooling

| Technology | Version | Why here |
|------------|---------|----------|
| Mocodo | 4.3.3+ | Generates Merise MCD and MLD diagrams from a single `.mcd` source. |
| tbls | — | Generates the MPD from the live PostgreSQL schema. |
| Mermaid | — | Renders the ERD in Markdown. |
| MADR | — | ADR template format in `docs/adr/`. |

## Quality, security, and version control

| Technology | Version | Why here |
|------------|---------|----------|
| Semgrep | 1.16x | Static analysis run as a tooling hook. |
| Git / GitButler | — | Version control; `but` CLI when on `gitbutler/workspace`. |
| GitHub Actions | — | CI (schema-drift check; more planned). |
