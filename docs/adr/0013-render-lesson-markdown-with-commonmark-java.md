# Render lesson Markdown with commonmark-java, sanitized by jsoup

- Status: accepted
- Date: 2026-07-11
- Deciders: Eric Bouchut

## Context and Problem Statement

Lesson content (issue #34) is authored in Markdown (`lessons.content_markdown`)
and must reach the browser as HTML. The converter choice (issue #35) shapes
security and performance: lesson Markdown is instructor input, so the rendered
HTML is an XSS vector if served unsanitized; and lessons are read far more
often than they change, so re-rendering on every view is wasted work
(issue #38). Which Markdown library converts lesson content, and how are its
output and its cost kept under control?

## Decision Drivers

- Security first: instructor-authored content must not be able to inject
  scripts (XSS), whatever the Markdown contains
- Spec compliance: predictable rendering of the common Markdown constructs
- Small dependency surface, maintained library, permissive license
- Cheap repeated reads: render once per content version, then serve from memory
- Stay in the JVM: no extra runtime or service for v1

## Considered Options

- commonmark-java (CommonMark reference implementation) plus jsoup sanitizing
- flexmark-java (feature-rich implementation with many extensions)
- Server-side JavaScript renderer (markdown-it under GraalVM or Node)

## Decision Outcome

Chosen: "commonmark-java plus jsoup sanitizing", because commonmark-java is
the reference implementation of the CommonMark spec: small, without
transitive dependencies, fast, and BSD-2-Clause licensed; v1 needs none of
flexmark's extension zoo. Raw HTML embedded in Markdown passes through the
renderer by design, so sanitization is not optional: the rendered HTML goes
through jsoup's `Safelist.relaxed()` allowlist (plus the `class` attribute on
`code` to keep language hints), which strips scripts, event handlers, and
frames no matter what the source says.

Rendered HTML is cached with Spring Cache over Caffeine (Boot-managed
version), keyed by the SHA-256 hash of the Markdown source: a
content-addressed key cannot go stale (edited content is a new key), so no
invalidation logic exists at all, and a 1000-entry bound caps memory. The
Redis option (issue #40) stays deferred; swapping the cache backend later
would not touch the renderer.

### Consequences

- Good: XSS defense is structural (an allowlist), not reliant on authors
- Good: repeated lesson reads cost a hash lookup, not a parse and render
- Good: three small libraries; Caffeine is version-managed by Spring Boot
- Trade-off: GFM extensions (tables, strikethrough) need their own
  `commonmark-ext-*` artifacts when a lesson actually needs them
- Trade-off: the allowlist is stricter than raw CommonMark output
  (for example relative links and images are dropped)
- Neutral: cache entries are in-memory; an application restart clears them

## Pros and Cons of the Options

### commonmark-java plus jsoup

- 👍 Reference implementation of the CommonMark spec; tiny and dependency-free
- 👍 BSD-2-Clause; actively maintained
- 👍 jsoup's Safelist is a battle-tested HTML allowlist
- 👎 Extensions (GFM tables, strikethrough) are separate artifacts, added on
  demand

### flexmark-java

- 👍 The most complete extension set (GFM, footnotes, TOC, admonitions)
- 👎 Large dependency tree and API surface for needs v1 does not have

### Server-side JavaScript renderer

- 👍 The same renderer as many editors (markdown-it)
- 👎 A second runtime to operate and secure; disproportionate for v1
