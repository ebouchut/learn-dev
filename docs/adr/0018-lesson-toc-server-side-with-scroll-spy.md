# Build the lesson TOC server-side with client-side scroll-spy

- Status: accepted
- Date: 2026-08-03
- Deciders: Eric Bouchut

## Context and Problem Statement

Lesson pages need a table of contents that stays viewable at any
scroll position. A TOC needs heading anchor ids, and the sanitizer of
[ADR-0013](0013-render-lesson-markdown-with-commonmark-java.md) strips
every `id` attribute today; that is a security feature, since an
author-supplied id could clobber `#main` and hijack the skip link.
Who mints the ids, who builds the list, and where does the "always
viewable" behavior live?

## Decision Drivers

- The TOC is a navigation aid: it should work without JavaScript and
  survive assistive-tech navigation (RGAA), like the rest of the
  lesson page
- Deep links (`#section-slug`) must work in every context, including
  a first page load with an anchor in the URL
- The sanitizer allowlist must not widen; author-controlled ids must
  keep dying
- The render cache stores sanitized output by content hash and must
  stay content-addressed
- Dynamic client-side behavior remains welcome (CP4), but as an
  enhancement, not a dependency

## Considered Options

- Hybrid: server mints ids and TOC entries post-sanitization; a small
  script adds scroll-spy and wide-screen auto-open
- Server-only: same server work, no client enhancement
- Client-only: JavaScript builds ids and the list in the browser
- The commonmark heading-anchor extension, letting `id` through the
  sanitizer with a value-validation pass

## Decision Outcome

Chosen: "hybrid", because the server half is the only way to satisfy
the navigation-aid, deep-link, and no-JavaScript drivers, and the
client half adds what only the client can know (the reader's scroll
position) as a pure enhancement.

Implementation decisions that follow:

- Ids are minted AFTER `Jsoup.clean()`, in the same post-processing
  pass as the alert class allowlisting: slugs fold accents (NFD),
  lowercase, hyphenate, and deduplicate with numeric suffixes. The
  sanitizer still strips author-supplied ids first, so spoofing the
  skip-link target stays structurally impossible and the allowlist is
  untouched.
- `render()` returns a `RenderedMarkdown(html, toc)` record; the
  content-addressed cache key is unchanged, the cached value widens to
  carry the TOC entries (rendered `h2..h4`).
- The template renders a `details`/`summary` panel inside
  `nav aria-label="Contents"` from 2 entries up; CSS makes it sticky
  (desktop sidebar with internal scroll, mobile collapsed bar), so the
  "always viewable" requirement is pure CSS and holds without
  JavaScript.
- `lesson-toc.js` auto-opens the panel on wide viewports (markup ships
  closed, the right mobile-first floor) and marks the section in view
  with `aria-current` via `IntersectionObserver`; styling keys off the
  attribute so visual and assistive state cannot drift apart.
- The TOC list is flat with per-level indentation rather than nested
  lists: three levels at most, a simple template, and hierarchy stays
  visible; revisit if lessons grow deeper structures.

### Consequences

- Good: the TOC and its anchors exist in server HTML, so no-JS
  readers, deep links, and MockMvc tests all see the real thing
- Good: no sanitizer or cache-key change; the spoof posture of
  ADR-0013 is preserved verbatim
- Good: the scroll-spy is honest CP4 material (IntersectionObserver,
  ARIA state management) instead of list-building JavaScript
- Trade-off: `render()` widens from a String to a record, touching
  every caller and renderer test once (mechanical `.html()` updates)
- Trade-off: the flat list trades nested-list semantics for template
  simplicity; the visual indent carries the hierarchy
- Trade-off: heading ids change if heading text changes, so shared
  deep links can go stale after a lesson edit (they fall back to the
  page top, never an error)

## Pros and Cons of the Options

### Hybrid (chosen)

- 👍 No-JS floor, reliable deep links, testable server HTML
- 👍 Scroll-spy where the client genuinely knows more than the server
- 👎 Two layers to keep coherent (ids server-side, spy client-side)

### Server-only

- 👍 Same floor, least code
- 👎 Gives up the current-section indicator entirely

### Client-only

- 👍 Cheapest; zero server change
- 👎 No TOC without JavaScript; anchors do not exist at first paint,
  so deep links land before ids do; weakest RGAA story

### Heading-anchor extension through the sanitizer

- 👍 Upstream-maintained id generation
- 👎 Forces `id` into the allowlist plus a value-validation pass, the
  exact surface ADR-0013 closed; post-sanitization minting gets the
  same result without reopening it
