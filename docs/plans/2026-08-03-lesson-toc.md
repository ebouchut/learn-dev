# Lesson Table of Contents Implementation Plan

**Goal:** Give lesson pages a sticky table of contents (issue #131):
readers of long lessons see the structure, jump to sections, and keep
the TOC in reach at any scroll position.

**Architecture:** Hybrid, recorded in
[ADR-0018](../adr/0018-lesson-toc-server-side-with-scroll-spy.md). The
server mints heading anchors and the TOC structure inside the rendering
pipeline of
[ADR-0013](../adr/0013-render-lesson-markdown-with-commonmark-java.md),
so the TOC works without JavaScript and deep links are reliable; a
small `lesson-toc.js` adds the scroll-spy (`IntersectionObserver`,
`aria-current`) and auto-opens the panel on wide screens. Ids are
minted by our code AFTER `Jsoup.clean()`, exactly like the alert class
second pass: the sanitizer keeps stripping author-supplied ids (which
protects the `#main` skip-link target) and the allowlist is unchanged.

**Out of scope:** a TOC outside lesson pages, nested list markup (the
flat list with per-level indentation keeps the template simple; see the
ADR trade-offs), and per-user TOC preferences.

---

## Version Control (GitButler)

- Commit with `but commit <changes> --branch feat/lesson-toc` from the
  main repository.
- **NEVER push.** The user reviews in GitButler and pushes manually.

---

## Tasks

- [ ] `docs(plan): Add the lesson table of contents plan` (this document)
- [ ] `docs(adr): Record the hybrid lesson TOC decision`
  ([ADR-0018](../adr/0018-lesson-toc-server-side-with-scroll-spy.md)
  and the ADR index row)
- [ ] `feat(markdown): Mint heading anchors and a table of contents`
  (renderer returns a `RenderedMarkdown(html, toc)` record; slugs fold
  accents, deduplicate, and cover rendered `h2..h4`; tests incl. the
  spoof case where a raw `h2 id="main"` loses the author id)
- [ ] `feat(course): Render the sticky lesson table of contents`
  (controller model, `details`/`nav` template shown from 2 entries,
  grid + sticky CSS per the design appendix; MockMvc assertions)
- [ ] `feat(frontend): Highlight the current section in the lesson TOC`
  (lesson-toc.js: wide-screen auto-open + IntersectionObserver
  scroll-spy writing `aria-current`; anchor comfort CSS:
  `scroll-margin-top`, reduced-motion-guarded smooth scroll;
  closes #131)

## CSS design (appendix)

- `lesson-layout` grid, mobile-first stacked (`"toc" "content"` areas,
  aside first in DOM so AT meets the nav early); from 46rem:
  `"content toc"` areas, `minmax(0, 1fr) 16rem` columns (the
  `minmax(0, ...)` keeps wide code blocks from blowing the grid). The
  aside keeps the grid default stretch: the sticky element inside it
  needs the full-height containing block to travel through, otherwise
  it never pins (verified the hard way; an earlier draft claimed the
  opposite).
- `.lesson-toc`: `position: sticky`, `top: var(--space-3)`,
  `max-height: calc(100vh - 2 * var(--space-3))`, `overflow-y: auto`
  so long TOCs scroll inside the pinned panel. Sticky dies under
  clipping ancestors; verification checks pinning at top, middle, and
  bottom of a long lesson.
- Mobile (under 46rem): the closed `details` summary is the pinned bar
  (`top: 0`, opaque surface background, hairline border,
  `max-height: 60vh`).
- Scroll-spy state is written as `aria-current` and styled purely via
  the attribute selector, the same pattern as the header nav.
- `scroll-margin-top` on `h2..h4` (larger on mobile to clear the bar);
  `scroll-behavior: smooth` only under
  `prefers-reduced-motion: no-preference`.

## Verification

- Full test suite + Checkstyle after each code commit.
- Browser, long seeded lesson (restored afterwards): TOC pinned at
  top, middle, and bottom scroll positions on desktop and mobile
  presets; internal scroll on long TOCs; mobile bar expands on tap;
  anchors jump and update the hash; `aria-current` follows the scroll;
  coexists with the Mermaid and highlighting scripts; both themes
  axe-clean; no horizontal scroll (RGAA 10.11).
- MockMvc pins the no-JavaScript floor: nav and anchors are in the
  server HTML.
- Throwaway data cleaned; `but status` clean; nothing pushed.
