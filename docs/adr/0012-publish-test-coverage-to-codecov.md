# Publish test coverage to Codecov

- Status: accepted
- Date: 2026-07-07
- Deciders: Eric Bouchut

## Context and Problem Statement

[ADR-0011](0011-start-ci-quality-checks-as-advisory-reports.md) kept
coverage local: a JaCoCo report uploaded as a workflow artifact, no
external service. Living with it showed the limits: there is no
stable link to the latest coverage (artifact URLs are per-run), no coverage
percentage badge is possible without CI committing a generated SVG back to
dev (tried and reverted: CI must not add commits), and PRs get no coverage
feedback. How do we get a live badge and PR-level coverage insight without
CI commits?

## Decision Drivers

- A live coverage badge in the README, without CI pushing commits
- Coverage feedback on every PR (project and patch coverage)
- Keep quality checks advisory: coverage must not block merges
  (the spirit of [ADR-0011](0011-start-ci-quality-checks-as-advisory-reports.md))
- Minimal setup and cost (free for public repositories)

## Considered Options

- Keep artifacts only (the status quo of
  [ADR-0011](0011-start-ci-quality-checks-as-advisory-reports.md))
- A badge SVG generated and committed by CI (rejected: CI commits on dev)
- Codecov (external coverage service)

## Decision Outcome

Chosen: "Codecov", because it provides the badge and PR feedback from the
JaCoCo XML that CI already produces, with one upload step and zero commits.
The advisory stance is preserved: `codecov.yml` sets the project and patch
statuses to informational, so Codecov never fails a check.

This ADR supersedes [ADR-0011](0011-start-ci-quality-checks-as-advisory-reports.md).
The other half of ADR-0011 is unchanged and restated here: **Checkstyle
stays advisory** (report-only goal, `continue-on-error`, report artifact),
until the remaining warnings are fixed and the gate is switched on.

### Consequences

- Good: Live coverage badge in the README and a browsable dashboard
  (https://app.codecov.io/gh/ebouchut/learn-dev).
- Good: Every PR gets a coverage comment (project delta and patch coverage).
- Trade-off: An external service and an account; uploads use a
  `CODECOV_TOKEN` repository secret (public-repo uploads can run tokenless
  but are rate-limited).
- Trade-off: The Tests workflow gains a third-party action
  (codecov/codecov-action, pinned to a commit SHA).
- The JaCoCo artifact upload is kept as an offline fallback.

## Pros and Cons of the Options

### Keep artifacts only

- 👍 No external dependency
- 👎 No badge, no stable latest-coverage link, no PR feedback

### CI-committed badge SVG

- 👍 No external service
- 👎 CI pushes commits to dev (rejected by the maintainer)

### Codecov

- 👍 Badge, dashboard, PR comments; free for public repos; one workflow step
- 👎 External service, account, and upload token to manage
