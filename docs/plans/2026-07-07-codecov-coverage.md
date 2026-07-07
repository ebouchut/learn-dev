# Codecov Coverage Reporting Implementation Plan

> **Status: executed** on 2026-07-07, branch `docs-link-coverage-reports`.
> Retrospective record; the CI-side checks at the bottom complete after the
> PR runs and the Codecov account is wired.

**Goal:** A live code coverage badge in the README and coverage feedback on
every PR, without CI pushing commits. A CI-committed badge SVG was tried and
reverted for that exact reason; Codecov replaces it (see
[ADR-0012](../adr/0012-publish-test-coverage-to-codecov.md), which
supersedes [ADR-0011](../adr/0011-start-ci-quality-checks-as-advisory-reports.md)
while keeping Checkstyle advisory).

## Tasks

- [x] [ADR-0012](../adr/0012-publish-test-coverage-to-codecov.md): decision
      record; mark ADR-0011 superseded; update the ADR index
      => commit `docs(adr): Adopt Codecov for coverage reporting`
- [x] Tests workflow: upload `target/site/jacoco/jacoco.xml` with
      codecov-action (pinned to the commit SHA of v7.0.0),
      `fail_ci_if_error: false`; add root `codecov.yml` with informational
      project and patch statuses (coverage never blocks a merge)
      => commit `ci(coverage): Upload the JaCoCo report to Codecov`
- [x] Docs: README badge (`codecov.io/gh/ebouchut/learn-dev/branch/dev`)
      linking to the dashboard; README documentation entry; CONTRIBUTING
      "On Codecov" bullet; Codecov entries in GLOSSARY.md and GLOSSAIRE.md
      (kept in sync); CI and quality table in docs/tech-stacks.md
      => commit `docs(coverage): Add the Codecov badge and links`

## User prerequisites (manual)

- [ ] Sign in at https://about.codecov.io/ with GitHub and activate the
      `ebouchut/learn-dev` repository
- [ ] Store the repository upload token as the `CODECOV_TOKEN` Actions
      secret (`gh secret set CODECOV_TOKEN`); until then, uploads from this
      public repository run tokenless (rate-limited)

## Verification

- [x] `test.yml` and `codecov.yml` parse as valid YAML
- [x] README badge reference labels each appear exactly twice (use + definition)
- [ ] The PR's Tests run shows the "Upload coverage to Codecov" step green
- [ ] The PR receives a Codecov comment (project delta and patch coverage)
- [ ] The README badge renders a percentage once dev has its first upload
