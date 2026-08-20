# Render Mermaid diagrams client-side in lessons

- Status: accepted
- Date: 2026-07-28
- Deciders: Eric Bouchut

## Context and Problem Statement

Instructors want diagrams in lessons, in the Mermaid fence syntax GitHub
and Obsidian share. `commonmark-java` has no Mermaid extension and needs
none: a mermaid fence is an ordinary fenced code block, and the renderer
already lets `code class="language-mermaid"` survive sanitization (see
[ADR-0013](0013-render-lesson-markdown-with-commonmark-java.md)). The real
question is where the text-to-SVG transformation runs, given that the
sanitization pipeline deliberately never lets SVG markup through and the
render cache stores sanitized HTML by content hash.

## Decision Drivers

- ADR-0013's posture stays: no SVG, no script, no style crosses the
  sanitizer, whatever the Markdown contains
- Author familiarity: content pasted from GitHub or Obsidian should work
- No new server-side infrastructure (no Node, no headless browser, no
  extra container) for a feature of this size
- Self-hosted and version-pinned assets, no CDN at page load (privacy,
  supply-chain stability)
- The project needs a legitimate, non-decorative client-side JavaScript
  feature (DWWM competency CP4, dynamic interfaces)
- Readers without JavaScript, and diagrams with syntax errors, must
  degrade to something readable

## Considered Options

- Client-side rendering with the Mermaid library, loaded lazily on lesson
  pages that contain a diagram
- Server-side rendering with `mermaid-cli` (*Node* plus headless *Chromium* on
  the server)
- Server-side rendering through a self-hosted *Kroki* container, embedded
  back as images
- Do nothing: mermaid fences stay visible as source code

## Decision Outcome

Chosen: *"client-side rendering with the Mermaid library"*, because it is
the only option that adds zero server infrastructure while keeping the
sanitizer contract byte-identical: the server keeps emitting the fenced
source as a sanitized code block (a contract pinned by a renderer test),
and the browser upgrades it to SVG after the fact. The render cache is
unaffected since cached HTML still contains only the code block.

Implementation decisions that follow:

- The library ships as the version-pinned `org.webjars.npm:mermaid`
  dependency with wildcard exclusions (the *npm* `WebJar` otherwise drags 21
  npm-mirror transitives). *Spring Boot* serves it from the classpath; no
  *CDN* is involved. Lesson pages require authentication, so the existing
  `anyRequest().authenticated()` rule already covers the *WebJar* path.
- `lesson-mermaid.js` loads the 3.3 MB bundle lazily, only when the page
  actually contains a `language-mermaid` block, and initializes Mermaid
  with `startOnLoad: false` and `securityLevel: "strict"`.
- Handing instructor-controlled text to a large rendering library is a
  new client-side attack surface the server sanitizer cannot see. The
  mitigations are the strict security level (labels escaped, click
  callbacks disabled), the pinned self-hosted version, and keeping the
  library off every page that has no diagram.
- The original code block stays in the *DOM*, hidden behind a
  "show diagram source" toggle (`aria-expanded`/`aria-controls`): it is
  the no-JavaScript fallback, the syntax-error fallback, and the textual
  alternative that accompanies the SVG. Diagrams follow the color scheme
  (Mermaid `default`/`dark` themes) and re-render on scheme change.

### Consequences

- Good: GitHub/Obsidian-style mermaid fences just work; no house syntax
- Good: sanitizer, allowlist, and render cache are untouched
- Good: the project gains its first substantive JavaScript feature (CP4)
  with progressive enhancement built in
- Trade-off: diagram rendering costs client CPU and a 3.3 MB lazy asset
  on diagram-bearing lessons; readers on very old browsers see source
- Trade-off: a client-side dependency with a history of XSS advisories
  must be kept current (pinned version, Dependabot watches the WebJar)
- Trade-off: diagram accessibility depends on authors adding `accTitle`
  and `accDescr` lines; the lesson form hints at this but cannot enforce it

## Pros and Cons of the Options

### Client-side Mermaid (chosen)

- 👍 Zero server infrastructure; sanitizer and cache untouched
- 👍 Lazy, self-hosted, version-pinned asset
- 👎 New client-side attack surface, mitigated but real
- 👎 3.3 MB library on diagram-bearing pages

### mermaid-cli on the server

- 👍 Clients get plain SVG images, no client-side surface
- 👎 Node plus headless Chromium inside a Spring Boot deployment, the
  heaviest possible dependency for the smallest feature

### Self-hosted Kroki service

- 👍 Clean HTTP contract, many diagram languages beyond Mermaid
- 👎 One more container to run, secure, and monitor; cache invalidation
  questions for regenerated images

### Do nothing

- 👍 No code
- 👎 Pasted GitHub/Obsidian content shows raw diagram source forever
