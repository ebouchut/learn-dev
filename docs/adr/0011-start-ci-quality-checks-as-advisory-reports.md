# Start CI quality checks as advisory reports

- Status: accepted
- Date: 2026-07-06
- Deciders: Eric Bouchut

## Context and Problem Statement

CI adds linting (#47) and coverage (#48) to a codebase that has never been
linted and has no coverage baseline. Should these checks block merges from
day one, and should coverage go to an external service? A blocking linter on
an unlinted codebase fails every PR until a large style-only cleanup lands,
which stalls feature work.

## Decision Drivers

- Do not block feature PRs on pre-existing style debt
- Zero new config files and no external service accounts (solo project,
  certification deadline)
- Keep a clear path to tighten later

## Considered Options

- Blocking checks from day one (Checkstyle `check` goal, coverage threshold
  via `jacoco:check`, Codecov)
- Advisory reports first: Checkstyle report-only with the bundled
  `google_checks.xml`, JaCoCo report bound to the test phase, both published
  as workflow artifacts, no external service

## Decision Outcome

Chosen: "Advisory reports first", because it makes quality visible immediately
without gating merges on historical debt, adds zero config files (bundled
Google ruleset, plugin versions pinned in `pom.xml`: Checkstyle 3.6.0,
JaCoCo 0.8.15), and keeps secrets and accounts out of scope. The lint job also
sets `continue-on-error: true` so even setup errors stay advisory.

### Consequences

- Good: Every PR publishes a Checkstyle XML report and a JaCoCo HTML/XML
  report as workflow artifacts.
- Good: `mvn test` locally now produces `target/site/jacoco/` with no extra
  flags.
- Trade-off: Nothing enforces quality yet; violations can accumulate until
  the gate is switched on.
- Follow-up: To tighten, switch the lint job to `checkstyle:check` with
  `failOnViolation=true`, drop `continue-on-error`, and add a `jacoco:check`
  minimum threshold.

## Pros and Cons of the Options

### Blocking from day one

- 👍 Debt cannot grow
- 👎 Every PR is red until a big-bang style cleanup; discourages small PRs
- 👎 Codecov adds an account, a token, and an external dependency

### Advisory reports first

- 👍 Immediate visibility, zero friction, easy to tighten incrementally
- 👎 Relies on discipline until the gate exists
