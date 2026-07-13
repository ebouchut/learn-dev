# Architecture

How learn-dev is structured and how a request flows through it. This file answers
**"how do the pieces fit together, and why is it built this way?"** For the list of
tools and versions see [docs/tech-stacks.md](docs/tech-stacks.md); for term
definitions see [GLOSSARY.md](GLOSSARY.md); for individual decisions and their
trade-offs see the [ADRs](docs/adr/README.md).

## Overview

learn-dev is a server-rendered, layered Spring Boot monolith. The browser talks to
Spring MVC controllers; controllers delegate to services; services use Spring Data
JPA repositories over a PostgreSQL relational core. HTML is produced server-side by
Thymeleaf. The app is a monolith today, with a longer-term intent to split selected
concerns into microservices (which is why service-to-service auth is already being
considered in [ADR-0002](docs/adr/0002-service-to-service-auth-via-service-token.md)).

## Architectural style

- **Server-rendered MVC.** Controllers return logical view names, not HTML or JSON;
  a Thymeleaf `ViewResolver` renders the corresponding template.
- **Layered.** Each layer depends only on the one below it:

  ```
  Browser
    │  HTTP (form posts, GETs)
    ▼
  Controller  (web layer: @Controller, request mapping, validation)
    │  calls
    ▼
  Service     (business logic, @Transactional boundaries)
    │  calls
    ▼
  Repository  (Spring Data JPA interfaces)
    │  SQL via Hibernate
    ▼
  PostgreSQL  (relational core)
  ```

## Package structure

Packages are organised by **feature**, not by technical layer, so a feature's
controller, service, entity, and repository live together:

```
com.ericbouchut.learndev
├── audit     # AuditService, entity/AuditLog: security audit trail (audit_logs)
├── auth      # AuthController, RegistrationService, CustomUserDetailsService,
│             # PasswordResetController/Service/Mailer, entity/PasswordResetToken,
│             # dto/*Form, exception/Duplicate*Exception
├── course    # entity/Course, Lesson, Enrollment (+EnrollmentId,
│             # EnrollmentStatus, PublicationStatus), repository/*,
│             # MarkdownRenderer (lesson Markdown to sanitized, cached HTML)
├── legal     # LegalController (privacy policy page)
├── user      # entity/User, repository/UserRepository
├── role      # entity/Role, repository/RoleRepository
├── common
│   └── config  # SecurityConfig (filter chain, PasswordEncoder)
└── (test) support  # AbstractPostgresIT (shared Testcontainers base)
```

## Request and rendering flow

1. A controller method (for example `AuthController.home()`) returns a **view name**
   such as `"home"` (a lookup key, not HTML).
2. The Thymeleaf `ViewResolver` maps the name to `src/main/resources/templates/home.html`.
3. The template engine renders the HTML, evaluating `th:*` attributes, escaping
   output (XSS defence), and injecting the CSRF token into forms that use `th:action`.
4. The `DispatcherServlet` writes the HTML as the response body with the appropriate
   headers and status.

A method may instead return a `redirect:` prefix (for example
`redirect:/auth/login?registered`), which produces a `302` rather than rendering a view.

## Authentication and authorization

- **Session-based form login.** Credentials are verified once; the session is kept
  server-side and referenced by the `JSESSIONID` cookie
  (see [ADR-0001](docs/adr/0001-use-server-side-sessions-over-jwt.md)). JWT was
  rejected for the browser flow.
- **Filter chain.** `SecurityConfig` defines the `SecurityFilterChain`: public paths
  (`/`, `/auth/**`, static assets) are permitted; everything else requires
  authentication. Auth endpoints are grouped under the `/auth/` URL prefix.
- **User loading.** `CustomUserDetailsService` loads a `User` by username and maps
  each `Role` to a Spring Security authority prefixed with `ROLE_` (so `ADMIN`
  becomes `ROLE_ADMIN`). Account flags map to `disabled` (`is_active`) and
  `accountLocked` (`is_locked`).
- **Passwords.** Hashed with BCrypt; the raw password is never persisted.
- **CSRF.** Enabled by default; Thymeleaf injects a per-form token. `SameSite=Lax`
  on the session cookie adds browser-level defence in depth.
- **Session cookie hardening.** `HttpOnly` and `SameSite=Lax` are set in the base
  config; `Secure` is enabled once served over HTTPS.

### Registration flow

`RegisterForm` (validated with Bean Validation) → `AuthController` → `RegistrationService`
(checks unique username/email, hashes the password, assigns the default `STUDENT`
role, saves) → redirect to the login page. Duplicate username/email surface as
field errors on the re-rendered form.

### Password reset flow

`ForgotPasswordForm` → `PasswordResetController` → `PasswordResetService`. The
service answers with the same neutral confirmation whether or not the email
exists (no account enumeration), rate-limits requests per user and per IP,
stores only the **SHA-256 hash** of a 32-byte random token (single active
token per user, 30 minute TTL, configurable under `learndev.password-reset.*`),
and emails the raw link via `PasswordResetMailer` over SMTP — Mailpit in
development (see [ADR-0004](docs/adr/0004-use-mailpit-as-local-smtp-catcher.md)).
Consuming the link stores the new BCrypt hash, marks the token used,
invalidates any others, and records the outcome in `audit_logs` through
`AuditService`. The sequence diagram lives in
[CONTRIBUTING.md](CONTRIBUTING.md#password-reset-sequence-diagram).

## Data architecture

- **Relational core (PostgreSQL).** Users, roles, password-reset tokens, the
  audit trail, courses, lessons, and enrollments. Users use a **UUID** primary key
  to avoid enumeration; other tables use `BIGINT` identity
  (see [ADR-0003](docs/adr/0003-uuid-pk-for-users-bigint-elsewhere.md)).
- **Document store (MongoDB).** Provisioned and configured for future content
  storage; not yet used by any feature.
- **Schema evolution.** Managed by Liquibase, run at startup. Migrations are
  hand-written formatted-SQL files, one atomic changeset per file, append-only
  (see [ADR-0005](docs/adr/0005-handwrite-liquibase-migrations-over-mcd-ddl.md)).
- **Modelling.** The schema is designed in Merise (MCD → MLD → MPD) and cross-checked
  against the migrations by a schema-drift CI job.

## Configuration and environments

- **Profiles.** `application.yaml` holds base config; `application-dev.yaml` holds
  dev overrides. `SPRING_PROFILES_ACTIVE=dev` selects the profile and the Liquibase
  `dev` context.
- **Secrets.** Loaded from `./.env` (a FIFO filled by an external secrets
  manager) via spring-dotenv at startup, so the working directory must be the
  project root.

## Testing strategy

Tests form a pyramid, all run under Surefire in `mvn test`
(see [ADR-0009](docs/adr/0009-run-tests-under-surefire-not-failsafe.md)):

- **Unit tests** — mock collaborators (Mockito), no I/O
  (`RegistrationServiceTest`, `CustomUserDetailsServiceTest`).
- **Slice tests** — `@DataJpaTest` against a real Postgres container
  (`UserRepositoryTest`, `RoleRepositoryTest`).
- **Integration test** — full context + MockMvc for the end-to-end auth journey
  (`AuthFlowTest`).
- **Smoke test** — verifies the context starts (`LearnDevApplicationTests`).

Tests use a real PostgreSQL via Testcontainers rather than H2
(see [ADR-0006](docs/adr/0006-test-against-real-postgres-testcontainers.md)), shared
as a static singleton container (see [ADR-0008](docs/adr/0008-share-singleton-testcontainers-postgres.md)).

## Build and run

- `make test` — run the suite (Podman-aware Testcontainers wiring).
- `make run` — start the databases and run the app (`http://localhost:8080/`).
- `docker compose up -d` — start Postgres, Mongo, and Mailpit (`docker` is
  Podman here). Mailpit's web UI (caught emails) is at `http://localhost:8025`.

## Direction of travel

- The course web layer on top of the shipped domain: student catalogue,
  enrollment and lesson reading, instructor authoring and rosters, admin
  account and content lifecycle (see
  [docs/plans/2026-07-12-course-management-by-role.md](docs/plans/2026-07-12-course-management-by-role.md)).
- Possible extraction of microservices, with service-to-service authentication
  ([ADR-0002](docs/adr/0002-service-to-service-auth-via-service-token.md)).
- A `SUPERADMIN` role (deferred under YAGNI; issue #65).
