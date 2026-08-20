# CI Test Pipeline Implementation Plan

> **Status: executed** on 2026-07-06, branch `ci-test-pipeline`.
> This is the retrospective record of the plan as it was carried out.

**Goal:** Give the project a GitHub Actions CI pipeline: verify the Maven
build (#45), run the test suite (#46), report code quality (#47), and publish
test coverage (#48) on every PR targeting `dev` and every push to `dev`/`main`.

**Architecture:** One focused workflow file per concern instead of a
monolithic `ci.yml` (issue #49 was closed as wontfix; rationale recorded in
[ADR-0010](../adr/0010-structure-ci-as-focused-workflows-per-concern.md)).
Quality checks start as advisory reports, not gates
([ADR-0011](../adr/0011-start-ci-quality-checks-as-advisory-reports.md)).
All jobs run on `ubuntu-latest` with `actions/setup-java@v4`
(Temurin 21, `cache: maven`) and invoke the wrapper as `./mvnw -B -ntp`.

**Key insight:** the tests need no `.env` on CI. Integration-style tests
extend `AbstractPostgresIT`, which provisions a real PostgreSQL 17 via
Testcontainers and wires the datasource with `@ServiceConnection`
(ADR-0006, ADR-0008); Mongo autoconfiguration is excluded in tests. The
Docker daemon built into GitHub's ubuntu runners is enough; the Podman
environment variables from the `Makefile` are local-only.

---

## Version Control (GitButler)

Same rules as every plan in this repository: the workspace is on
`gitbutler/workspace`, so all commits go through `but commit` (never
`git add`/`git commit`), and **nothing is pushed** by the agent; the user
reviews, pushes, and opens the PR targeting `dev`.

---

## File Structure

```
.github/workflows/
├── build.yml   # Build: ./mvnw -B -ntp compile
├── test.yml    # Tests: ./mvnw -B -ntp test + JaCoCo artifact upload
└── lint.yml    # Lint: ./mvnw -B -ntp checkstyle:checkstyle (advisory)

pom.xml         # + maven-checkstyle-plugin 3.6.0 (report-only)
                # + jacoco-maven-plugin 0.8.15 (prepare-agent, report@test)
```

---

## Task 1: Maven build verification (#45)

- [x] Create `.github/workflows/build.yml`: checkout@v4, setup-java
      (Temurin 21, Maven cache), `./mvnw -B -ntp compile`
- [x] Triggers: `pull_request: [dev]`, `push: [dev, main]`
      (same as `schema-drift.yml`)
- [x] Commit `ci(build): Verify the Maven build on every PR and push`
      (Fixes #45) => 9430e67

## Task 2: Test automation (#46)

- [x] Create `.github/workflows/test.yml`: same setup, `./mvnw -B -ntp test`
- [x] Document in the workflow why no `.env` or Podman wiring is needed
- [x] Surefire only, all `*Test` classes; no Failsafe (ADR-0009)
- [x] Commit `ci(test): Run the test suite on every PR and push`
      (Fixes #46) => 3650ab8

## Task 3: Code quality checks (#47)

- [x] Add `maven-checkstyle-plugin` (pinned 3.6.0, not managed by the Boot
      parent) to `pom.xml`: bundled `google_checks.xml`, `consoleOutput`,
      `failOnViolation=false`, `failsOnError=false`, not bound to any phase
      so `mvn test`/`mvn install` behavior is unchanged
- [x] Create `.github/workflows/lint.yml`: `checkstyle:checkstyle`
      (report goal, never fails on violations), `continue-on-error: true`
      on the job, upload `target/checkstyle-result.xml` as an artifact
- [x] Commit `ci(lint): Add a non-blocking Checkstyle report`
      (Fixes #47) => bde8cc5

## Task 4: Test coverage reports (#48)

- [x] Add `jacoco-maven-plugin` (pinned 0.8.15) to `pom.xml`:
      `prepare-agent` execution plus `report` bound to the `test` phase, so
      every `mvn test` produces `target/site/jacoco/` (HTML + XML)
- [x] Add an `actions/upload-artifact@v4` step (`if: always()`) to
      `test.yml` publishing `target/site/jacoco/`
- [x] No external coverage service (no Codecov): artifacts only
- [x] Commit `ci(coverage): Add JaCoCo and upload the coverage report`
      (Fixes #48) => 50f61d6

## Verification (performed)

- [x] All three workflow files parse as valid YAML
      (actionlint unavailable; parsed with a YAML library)
- [x] `make test` after the `pom.xml` changes:
      `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0` => BUILD SUCCESS
- [x] `target/site/jacoco/index.html` and `jacoco.xml` produced by the
      test phase
- [x] `./mvnw -B -ntp checkstyle:checkstyle` => BUILD SUCCESS,
      violations reported as warnings only
- [x] `but status`: exactly 4 commits on `ci-test-pipeline`, no unrelated
      changes swept in

## Deferred (deliberate)

- Coverage threshold gate (`jacoco:check`): add when a baseline exists
- Blocking Checkstyle (`checkstyle:check`, `failOnViolation=true`, drop
  `continue-on-error`): switch on once the existing violations are triaged
- Path filters on the workflows: build and tests should always run for now
