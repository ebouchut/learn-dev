# Share one Testcontainers PostgreSQL as a static singleton, not @Container

- Status: accepted
- Date: 2026-06-23
- Deciders: Eric Bouchut

## Context and Problem Statement

Several test classes (RoleRepositoryTest, UserRepositoryTest,
LearnDevApplicationTests) share a common base, AbstractPostgresIT, that provides
a PostgreSQL container via Testcontainers (see ADR-0006). The container field is
static so it can be shared across classes. With @Testcontainers and @Container
managing the lifecycle, the suite failed when run as a whole: every test passed
in isolation, but a class failed once another class had already run. How should
the shared container's lifecycle be managed across multiple test classes?

## Decision Drivers

- One container shared across all test classes (start once, for speed).
- Reliable in a full multi-class run, not just in isolation.
- Minimal boilerplate.

## Considered Options

- @Testcontainers and @Container on the static field (JUnit extension manages the lifecycle).
- A static-initializer singleton (the JVM manages the lifecycle), with @ServiceConnection.

## Decision Outcome

Chosen: a **static-initializer singleton**. AbstractPostgresIT starts the
container once in a static block and never stops it explicitly; @ServiceConnection
wires Spring Boot's datasource to it. @Testcontainers and @Container are removed.

With @Container, the JUnit extension stops the container after each test class.
Because the container is shared by several classes, the first class stopped it and
the next class reused a dead container, failing with "connection refused" after a
30 second Hikari timeout. A static initializer ties the lifecycle to the JVM (the
whole test run), so the container stays up for every class.

### Consequences

- Good: the full suite is reliable; the container starts once and is reused, so
  later test classes see it already up (faster).
- Good: no @DynamicPropertySource boilerplate; @ServiceConnection still works
  because it only needs a started container.
- Trade-off: no explicit stop() in code; cleanup relies on JVM exit, and on Ryuk
  in CI. This is acceptable.
- Refines ADR-0006 (test against a real PostgreSQL via Testcontainers).

## Pros and Cons of the Options

### @Testcontainers and @Container (extension-managed)

- 👍 Declarative; automatic start and stop
- 👎 Lifecycle is per test class: it stops the shared container after the first
  class, breaking later classes in the same run (connection refused, 30 second timeout)

### Static-initializer singleton (chosen)

- 👍 One container for the whole run, shared by all classes; reliable; faster;
  less boilerplate
- 👎 No explicit stop in code; relies on JVM shutdown and on Ryuk (CI) for cleanup
