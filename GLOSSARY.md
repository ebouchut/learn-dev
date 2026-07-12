# Glossary

Definitions of the domain and technical terms used across the learn-dev
project. For the concrete tools and versions, see [docs/tech-stacks.md](docs/tech-stacks.md);
for how the pieces fit together, see [ARCHITECTURE.md](ARCHITECTURE.md); for the
rationale behind design decisions, see the [ADRs](docs/adr/README.md).

> [!NOTE]
> 🇫🇷 French version: [GLOSSAIRE.md](GLOSSAIRE.md).
> The two files are translations of each other: when you add, change, or
> remove an entry in one, apply the same change to the other.

## Domain terms

- **Archive** — Unpublish a course or lesson so it is no longer available to
  students, without deleting it.
- **Course** — A unit of learning content owned by an instructor; contains lessons.
- **Deactivate** — Disable an account (for example an instructor or student) so it
  can no longer be used, without deleting it. See also *disabled account*.
- **Drop a course** — A student withdrawing from a course before finishing it.
- **Enrollment** — The relationship linking a student to a course they have joined.
- **Lesson** — An individual piece of content within a course.
- **Role** — A named set of permissions granted to a user. The seeded roles are
  `STUDENT`, `INSTRUCTOR`, and `ADMIN`; `SUPERADMIN` is planned (see issue #65).

## Authentication and security

- **Authority** — In Spring Security, a single granted permission string held by an
  authenticated user. Roles are represented as authorities prefixed with `ROLE_`
  (for example the `ADMIN` role becomes the authority `ROLE_ADMIN`).
- **BCrypt** — An adaptive password-hashing function. Passwords are stored as BCrypt
  hashes, never in clear text.
- **CSRF (Cross-Site Request Forgery)** — An attack that tricks a logged-in user's
  browser into submitting an unwanted request. Defended with a per-form token
  (injected by Thymeleaf) and the `SameSite` cookie attribute.
- **Disabled account** — An account that exists but is not allowed to authenticate
  (mapped from the `is_active = false` flag). Distinct from a *locked account*.
- **HttpOnly** — A cookie attribute that hides the cookie from client-side
  JavaScript, mitigating session theft via XSS.
- **IDOR (Insecure Direct Object Reference)** — An access-control flaw where a
  client-supplied identifier is trusted without an authorization check. Using UUID
  primary keys for users mitigates enumeration (see [ADR-0003](docs/adr/0003-uuid-pk-for-users-bigint-elsewhere.md)).
- **Locked account** — An account temporarily blocked from authenticating (for
  example after too many failed logins), mapped from the `is_locked` flag. Distinct
  from a *disabled account*.
- **Principal** — The currently authenticated entity (typically the user) within a
  security context.
- **SameSite** — A cookie attribute controlling whether the browser sends the cookie
  on cross-site requests. Set to `Lax` here as CSRF defense in depth.
- **Secure (cookie)** — A cookie attribute that restricts the cookie to HTTPS.
  Enabled only once the app is served over TLS.
- **Session (server-side)** — Authentication state kept on the server and referenced
  by a session cookie (`JSESSIONID`), rather than a self-contained token
  (see [ADR-0001](docs/adr/0001-use-server-side-sessions-over-jwt.md)).
- **XSS (Cross-Site Scripting)** — Injection of malicious scripts into pages viewed
  by other users. Mitigated by Thymeleaf's automatic output escaping and `HttpOnly`.

## Persistence and data modelling

- **Changelog / Changeset (Liquibase)** — A changelog is the ordered list of
  migrations; a changeset is one atomic migration, identified by `path::id::author`.
- **ERD (Entity-Relationship Diagram)** — A diagram of entities and their
  relationships (rendered here with Mermaid).
- **Hibernate** — The JPA implementation (ORM) used to map Java entities to tables.
- **JPA (Jakarta Persistence API)** — The standard Java API for object-relational
  mapping; implemented by Hibernate.
- **JSESSIONID** — The default name of the servlet session cookie.
- **Liquibase** — The database schema migration tool. Migrations are hand-written
  formatted-SQL files applied at startup (see [ADR-0005](docs/adr/0005-handwrite-liquibase-migrations-over-mcd-ddl.md)).
- **Merise** — A French data-modelling method producing three views: MCD, MLD, MPD.
- **MCD (Modele Conceptuel de Donnees)** — Conceptual data model; the entities and
  relationships independent of any database.
- **MLD (Modele Logique des Donnees)** — Logical data model; the relational schema
  (tables, keys) derived from the MCD.
- **MPD (Modele Physique des Donnees)** — Physical data model; the concrete schema
  as implemented in PostgreSQL.
- **ORM (Object-Relational Mapping)** — Mapping between Java objects and relational
  tables; provided by Hibernate/JPA.
- **UUID** — A 128-bit identifier used as the primary key for users to avoid
  sequential-id enumeration.

## Build, testing, and tooling

- **ADR (Architecture Decision Record)** — A short, numbered, append-only document
  capturing one design decision and its trade-offs, in MADR format.
- **Bean Validation** — The Jakarta standard for declaring constraints
  (`@NotBlank`, `@Email`, `@Size`) on form/DTO fields, enforced with `@Valid`.
- **Checkstyle** — A static-analysis tool that checks Java source against a
  style ruleset. Runs here with the bundled Google ruleset (`google_checks.xml`)
  in report-only mode (see [ADR-0011](docs/adr/0011-start-ci-quality-checks-as-advisory-reports.md)).
- **Code coverage** — The percentage of code exercised by the test suite.
  Measured here by JaCoCo and published to Codecov; reported, not yet
  enforced as a threshold.
- **Codecov** — A hosted service that ingests coverage reports from CI,
  renders a dashboard and a README badge, and comments on PRs with the
  project and patch coverage. Statuses are informational here (see
  [ADR-0012](docs/adr/0012-publish-test-coverage-to-codecov.md)).
- **DTO (Data Transfer Object)** — An object carrying data across a boundary,
  deliberately separate from entities. A `...Form` DTO backs an HTML form.
- **Failsafe** — The Maven plugin that runs `*IT` integration tests in the `verify`
  phase. This project does **not** use it (see [ADR-0009](docs/adr/0009-run-tests-under-surefire-not-failsafe.md)).
- **FIFO (named pipe)** — A special file that streams data on read. The project's
  `.env` is a FIFO filled by 1Password; shell `source` cannot read it (0-byte stat).
- **HikariCP** — The JDBC connection pool bundled with Spring Boot.
- **Integration test** — A test that boots a Spring context and exercises multiple
  layers together (here `@SpringBootTest` against a real Postgres container).
- **JaCoCo (Java Code Coverage)** — The code-coverage tool for Java. Its Maven
  plugin instruments the tests (`prepare-agent`) and writes an HTML/XML report to
  `target/site/jacoco/` during the `test` phase; CI uploads it as a workflow artifact.
- **Linter** — A tool that flags style and quality issues in source code without
  running it (static analysis). The project's linter is Checkstyle.
- **Lombok** — A library that generates boilerplate (getters, constructors) from
  annotations at compile time.
- **MADR (Markdown ADR)** — The lightweight ADR template format used in `docs/adr/`.
- **Maven Wrapper (`mvnw`)** — A committed launcher script that downloads and runs
  the project's pinned Maven version, so builds do not depend on a locally
  installed Maven (used by CI: `./mvnw -B -ntp ...`).
- **Slice test** — A test that loads only one layer of the context (for example
  `@DataJpaTest` for the persistence layer).
- **Smoke test** — A minimal test that the application context starts at all
  (`LearnDevApplicationTests`).
- **Surefire** — The Maven plugin that runs `*Test`/`*Tests` unit and integration
  tests in the `test` phase. All tests here run under Surefire.
- **Testcontainers** — A library that starts throwaway Docker/Podman containers for
  tests; used to run a real PostgreSQL (see [ADR-0006](docs/adr/0006-test-against-real-postgres-testcontainers.md)).
- **Ryuk** — Testcontainers' companion container that cleans up resources; disabled
  under Podman in this project.
- **YAGNI (You Aren't Gonna Need It)** — The principle of not building features
  until they are actually needed (for example deferring the `SUPERADMIN` role).

## Infrastructure and process

- **Advisory check** — A CI check that reports problems without blocking the
  merge (report-only goal and/or `continue-on-error`). Linting and coverage
  start advisory here (see [ADR-0011](docs/adr/0011-start-ci-quality-checks-as-advisory-reports.md)).
- **CI (Continuous Integration)** — Automatically building and testing every
  change (each PR and push) to catch regressions early. Implemented with
  GitHub Actions (issues #45 to #48).
- **Docker Compose** — Declarative multi-container orchestration; here it runs
  Postgres and Mongo. `docker` on the dev machine is Podman.
- **GitButler** — The version-control tool wrapping Git; used via the `but` CLI when
  the current branch is `gitbutler/workspace`.
- **GitHub Actions** — GitHub's CI service. Each workflow is a YAML file under
  `.github/workflows/`; this project uses one focused workflow per concern
  (see [ADR-0010](docs/adr/0010-structure-ci-as-focused-workflows-per-concern.md)).
- **Mailpit** — A fake SMTP server for development: it accepts every email the
  app sends, delivers nothing, and shows the messages in a web UI
  (http://localhost:8025) and a REST API. Runs as a Docker Compose service
  (see [ADR-0004](docs/adr/0004-use-mailpit-as-local-smtp-catcher.md)).
- **Podman** — A daemonless container engine, used as the `docker` drop-in.
- **Runner** — The machine that executes a GitHub Actions job (`ubuntu-latest`
  here); it ships with a Docker daemon, which Testcontainers uses directly.
- **SMTP (Simple Mail Transfer Protocol)** — The protocol used to send email.
  The app talks SMTP to Mailpit in development (port 1025) and would talk it
  to a real provider in production.
- **Spring profile** — A named configuration set (for example `dev`) selecting
  profile-specific properties and Liquibase contexts.
- **Temurin** — The Eclipse Adoptium distribution of the OpenJDK; the Java 21
  build used locally (via SDKMAN) and on CI (via `actions/setup-java`).
- **Thymeleaf** — The server-side HTML template engine. Its Spring Security
  **dialect** (`sec:` namespace) exposes the authenticated user to templates.
- **Workflow (GitHub Actions)** — A YAML file declaring when (triggers) and how
  (jobs, steps) CI runs. This project has `build.yml`, `test.yml`, `lint.yml`,
  and `schema-drift.yml`.
- **Workflow artifact** — A file or folder uploaded from a workflow run and
  downloadable from the run page (here: the Checkstyle XML and JaCoCo reports).

## Certification

- **CCP (Certificat de Competences Professionnelles)** — A competency block of a
  French Titre Professionnel; the DWWM has a front-end and a back-end CCP.
- **DWWM (Developpeur Web et Web Mobile)** — The French Titre Professionnel this
  capstone targets.
- **REAC (Referentiel Emploi Activites Competences)** — The official competency
  reference framework defining what the certification assesses.
