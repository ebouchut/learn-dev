# Use Mailpit as the local fake SMTP catcher

- Status: accepted
- Date: 2026-06-08
- Deciders: Eric Bouchut

## Context and Problem Statement

Features such as password reset (#51) and email verification send email. During
development and automated testing, these flows must be exercised without
delivering real messages to real inboxes. We need a local "fake SMTP" catcher
that traps outgoing mail for visual inspection and test assertions. Which tool
should the project standardise on?

## Decision Drivers

- Active maintenance and project health
- Quality of the inspection UI (search, HTML/source views)
- A REST API usable for automated integration tests
- Ease of running in Docker Compose alongside PostgreSQL
- Zero impact on application code (standard SMTP)

## Considered Options

- Mailpit
- MailHog

## Decision Outcome

Chosen: **Mailpit**. It is actively maintained, provides a modern UI with search
and a clean REST API for test assertions, runs as a small container using the
same SMTP/UI ports as MailHog, and requires no application changes (Spring simply
points `spring.mail.*` at it). MailHog, while historically popular, is
effectively unmaintained (last release 2020).

### Consequences

- Good: actively maintained; clean REST API enables end-to-end password-reset
  integration tests; modern UI and SQLite persistence ease local debugging;
  trivial Docker Compose service.
- Good: no application coupling — only `spring.mail.*` differs between dev
  (Mailpit) and production (a real provider).
- Trade-off: Mailpit is not a production mail service; a real SMTP provider
  (e.g. Amazon SES, Mailgun) is still required for production.
- Note: this choice applies to local development and testing only; it is never
  used in production.

## Pros and Cons of the Options

### Mailpit

- 👍 Actively maintained; modern UI with search; clean REST API; SQLite
  persistence; small image
- 👎 Newer and less historically ubiquitous than MailHog

### MailHog

- 👍 Long-established and widely documented
- 👎 Unmaintained since 2020; dated UI; weaker API tooling

Spring configuration is identical for both (SMTP on `1025`), so there is no
integration cost to choosing Mailpit.
