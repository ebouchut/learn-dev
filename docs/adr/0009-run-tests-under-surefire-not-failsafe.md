# Run all tests under Surefire with the *Test suffix, not Failsafe/*IT

- Status: accepted
- Date: 2026-06-30
- Deciders: Eric Bouchut

## Context and Problem Statement

Some tests are fast unit tests (mocked, no I/O); others boot a Spring context
and talk to a real PostgreSQL via Testcontainers. Maven offers two conventions:
Surefire runs `*Test`/`*Tests` in the `test` phase, while Failsafe runs `*IT`
in the `verify` phase to separate slow integration tests from unit tests.

An end-to-end test was first named `AuthFlowIT`. Because no Failsafe plugin is
configured, `mvn test` (and `make test`) silently skipped it: the suite reported
success while never exercising the flow. How should integration-style tests be
named and run so they are not skipped by accident?

## Decision Drivers

- Avoid silently skipped tests (a green build must mean every test ran).
- Keep one simple command (`make test`) that runs everything.
- Match the project's existing, de-facto convention.
- Low configuration and cognitive overhead for a solo capstone project.

## Considered Options

- Option A: Name every test `*Test`/`*Tests`; run all under Surefire in `mvn test`.
- Option B: Add the Failsafe plugin; name integration tests `*IT`; run them in `mvn verify`.

## Decision Outcome

Chosen: "Option A", because the container-backed tests already in the project
(`UserRepositoryTest`, `RoleRepositoryTest`, `LearnDevApplicationTests`) all run
under Surefire and use the `*Test`/`*Tests` suffix. Adding Failsafe would split
the suite across two phases and two commands for little benefit at this scale,
and the `*IT` suffix without Failsafe is the exact trap that caused a test to be
skipped. The `IT` suffix is reserved for non-test support classes such as
`AbstractPostgresIT` (a base class, never collected as a test).

### Consequences

- Good: `make test` runs the entire suite, including container-backed and
  end-to-end tests; a green build genuinely covers everything.
- Good: no new build plugin or second command to remember.
- Trade-off: no phase-level separation of fast unit tests from slow integration
  tests; the whole suite runs together. Acceptable while the suite is small. If
  it grows enough that this hurts, revisit by introducing Failsafe (a new ADR
  superseding this one).

## Pros and Cons of the Options

### Option A: All tests under Surefire (`*Test`)

- 👍 Single command runs everything; nothing is skipped by accident.
- 👍 Consistent with the tests already in the repo.
- 👍 Zero extra build configuration.
- 👎 Slow integration tests are not separated from fast unit tests.

### Option B: Failsafe plugin with `*IT`

- 👍 Textbook separation of integration tests from unit tests by Maven phase.
- 👍 `mvn test` stays fast; `mvn verify` adds the heavier tests.
- 👎 Requires plugin configuration and a second command (`make verify`), plus CI wiring.
- 👎 An `*IT` test is silently skipped under `mvn test` (the failure mode that triggered this ADR).
