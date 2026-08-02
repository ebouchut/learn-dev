# Highlight lesson code client-side with highlight.js

- Status: accepted
- Date: 2026-07-31
- Deciders: Eric Bouchut

## Context and Problem Statement

Fenced code blocks in lessons render monochrome. The rendering pipeline
has anticipated highlighting since
[ADR-0013](0013-render-lesson-markdown-with-commonmark-java.md): the
sanitizer allowlist keeps `class` on `code` precisely so the
`language-*` hint survives, and a renderer test pins that contract.
Where should the tokens-to-colors transformation run, given that the
sanitizer deliberately strips every other class and the render cache
stores sanitized HTML by content hash?

## Decision Drivers

- ADR-0013's posture stays: the sanitizer allowlist must not widen for
  presentation markup (a server-side highlighter would need
  `span[class]` plus a large token-class value allowlist)
- The [ADR-0016](0016-render-mermaid-diagrams-client-side.md) precedent:
  server ships sanitized source, the browser upgrades, assets are
  self-hosted and version-pinned, features load lazily
- Colors must meet WCAG AA on the code-card surface in both themes
  (RGAA 3.2), without maintaining a vendor theme fork
- Predictability: no auto-detect guessing on unhinted blocks

## Considered Options

- highlight.js, client-side, lazy-loaded (self-hosted WebJar)
- Prism.js, client-side
- A server-side Java highlighter emitting token spans through the
  sanitizer
- Do nothing: code blocks stay monochrome

## Decision Outcome

Chosen: "highlight.js, client-side, lazy-loaded", because it consumes
exactly the markup the renderer already emits (`pre > code.language-*`),
ships as a single self-hosted browser bundle with the common languages
baked in, and leaves the sanitizer and the render cache byte-identical.
Prism expects a bundler or per-language script tags to reach the same
coverage; a server-side highlighter would force presentation spans
through the sanitizer, widening the allowlist ADR-0013 fought to keep
narrow, and binding highlighting into the content-addressed cache.

Implementation decisions that follow:

- The bundle ships as the version-pinned
  `org.webjars.npm:highlightjs__cdn-assets` dependency (zero
  transitives). The plain `highlight.js` WebJar is a known trap: since
  v11 it contains only bundler modules, no browser build.
- `lesson-highlight.js` mirrors `lesson-mermaid.js`: it loads the
  bundle only when the lesson contains a `language-*` block other than
  `language-mermaid` (Mermaid owns those), and highlights only blocks
  whose language `hljs.getLanguage()` recognizes; unknown hints and
  bare fences stay monochrome rather than getting auto-detect guesses.
- No vendor theme: hljs token classes map to the theme accent tokens
  already proven surface-safe in the light-theme contrast audit
  (keyword/type to primary, string to success, number/literal to
  warning, comment to muted italic, title/attr to link), so both themes
  pass AA by construction and follow future token retuning for free.

### Consequences

- Good: the "future syntax highlighter" seam ADR-0013 left open closes
  with zero server-side change; the contract test keeps guarding it
- Good: colors track the design tokens in both themes automatically
- Good: lessons without code hints pay nothing (lazy load)
- Trade-off: one more pinned client-side dependency to keep current
  (highlight.js has had ReDoS advisories; Dependabot watches the WebJar)
- Trade-off: readers without JavaScript see monochrome code, the same
  graceful floor as before

## Pros and Cons of the Options

### highlight.js client-side (chosen)

- 👍 Consumes the existing sanitized markup as-is; single-file bundle
- 👍 Common-languages build covers the curriculum (java, js, python,
  sql, bash, ...)
- 👎 One more client-side dependency to pin and update

### Prism.js client-side

- 👍 Fine-grained language plugins
- 👎 Reaching the same coverage needs a bundler or a script tag per
  language; no advantage for a no-build project

### Server-side Java highlighter

- 👍 No client-side work at all
- 👎 Forces `span[class]` and a token-class allowlist through the
  sanitizer, and bakes presentation into the content-addressed cache;
  no maintained Java highlighter matches hljs coverage

### Do nothing

- 👍 No code
- 👎 The renderer keeps emitting language hints nothing consumes
