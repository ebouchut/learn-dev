# Use a UUID primary key for users, BIGINT identity elsewhere

- Status: accepted
- Date: 2026-06-06
- Deciders: Eric Bouchut

## Context and Problem Statement

Every table needs a primary-key type. Sequential integer keys are enumerable:
if exposed in a URL, API response, or token, they enable resource enumeration
and make IDOR bugs trivial to exploit. UUID keys are non-enumerable but larger,
and the random v4 variant hurts insert locality. The current frontend is
Thymeleaf (server-rendered, ids not exposed), but a possible React/API
version 2.0 after certification could expose the user id to clients. Which key
type should each table use?

## Decision Drivers

- Avoid exposing enumerable identifiers for client-reachable resources
- Insert and index efficiency on append-heavy internal tables
- Forward compatibility with a potential React/API v2 (post-certification)
- Keeping the entity/JPA layer reasonably simple

## Considered Options

- UUID primary key for all tables
- BIGINT identity primary key for all tables
- UUID for `users` only, BIGINT identity elsewhere (mixed)

## Decision Outcome

Chosen: **UUID for `users.user_id`, BIGINT identity for all other tables**.
`user_id` is the only identifier exposed outside the database — as the session
subject today, and potentially through a React/API v2 tomorrow — so it must be
non-enumerable. Every other table is internal and never exposes its key, so a
compact, sequentially-inserted BIGINT is the better physical choice.

### Consequences

- Good: user ids are non-enumerable and safe to expose now and for a future v2,
  avoiding a costly user-id type migration later.
- Good: internal tables keep small, sequential keys; token tables remain
  protected by their random `token` column regardless of key type.
- Trade-off: two id types coexist in the model; `audit_logs.entity_id` is stored
  as text because it may reference a UUID (users) or a BIGINT (other tables);
  FK columns to `users` are UUID while FKs to other tables are BIGINT.
- Note: `gen_random_uuid()` (UUIDv4) is used; revisit UUIDv7 for better insert
  locality if user write volume grows, or upon upgrading to PostgreSQL 18.

## Pros and Cons of the Options

### UUID for all tables

- 👍 Uniform JPA model; every id non-enumerable
- 👎 Larger keys/indexes on append-heavy tables (e.g. audit_logs); v4 randomness
  hurts insert locality

### BIGINT identity for all tables

- 👍 Smallest and fastest; uniform; human-readable
- 👎 Enumerable; exposing any id (especially the user id) invites enumeration and
  IDOR; would later require a separate public id column

### UUID for users only, BIGINT elsewhere (chosen)

- 👍 Non-enumerable where it matters; compact keys elsewhere; future-API-safe
  user id
- 👎 Mixed id types; `entity_id` stored as text; mixed FK column types
