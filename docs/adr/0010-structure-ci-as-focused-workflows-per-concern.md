# Structure CI as focused workflows per concern

- Status: accepted
- Date: 2026-07-06
- Deciders: Eric Bouchut

## Context and Problem Statement

The project needs continuous integration on GitHub Actions (issues #45 to #48):
build verification, test execution, linting, and coverage reporting.
Should all of this live in a single `ci.yml` workflow, or in separate workflow
files? Issue #49 (create a monolithic `ci.yml`) was closed as wontfix; this ADR
records the reasoning so the layout survives the issue tracker.

## Decision Drivers

- Readability of CI results on a PR (one status per concern vs one opaque status)
- Independent evolution (tighten linting without touching the test workflow)
- Consistency with the existing `schema-drift.yml` workflow
- Duplication cost (checkout and setup-java repeated per file)

## Considered Options

- A single monolithic `ci.yml` with multiple jobs
- One focused workflow file per concern (`build.yml`, `test.yml`, `lint.yml`)

## Decision Outcome

Chosen: "One focused workflow file per concern", because each PR check maps to
one small readable file, failures are identifiable at a glance from the check
name, and each workflow can change (or be made blocking) independently.
This matches the precedent set by `schema-drift.yml`.

### Consequences

- Good: PR checks read as Build / Tests / Lint / Schema drift; a red check
  names its concern directly.
- Good: The advisory lint workflow (ADR-0011) can later become blocking
  without touching build or test.
- Trade-off: The checkout + setup-java boilerplate is repeated in each file
  (about 10 lines per workflow).
- Trade-off: Shared changes (bumping the Java version) must be applied in
  several files.

## Pros and Cons of the Options

### Monolithic ci.yml

- 👍 One file to maintain; shared setup written once
- 👎 One check status hides which concern failed; jobs still rerun setup each
- 👎 Any change risks the whole pipeline; advisory and blocking concerns are
  entangled

### One workflow per concern

- 👍 Self-describing PR checks; independent lifecycles; matches `schema-drift.yml`
- 👎 Boilerplate repeated per file
