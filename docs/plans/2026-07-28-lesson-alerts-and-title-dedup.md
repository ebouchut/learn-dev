# Lesson Alerts and Title Dedup Implementation Plan

**Goal:** Two improvements to lesson Markdown rendering, on the `fix/lesson-content-styles` branch: (1) alert/callout blocks (`> [!note]`) in the syntax GitHub and Obsidian share, styled with octicons and theme accents (issue #116); (2) stop showing a duplicated heading when a lesson's Markdown starts with a level-1 heading equal to the lesson title (issue #117).

**Architecture:** Both changes stay inside the rendering pipeline of [ADR-0013](../adr/0013-render-lesson-markdown-with-commonmark-java.md) and [ADR-0014](../adr/0014-demote-markdown-headings-in-lesson-rendering.md). Alerts are parsed by the official `commonmark-ext-gfm-alerts` extension (decision recorded in [ADR-0015](../adr/0015-render-lesson-alerts-with-commonmark-alerts.md)), which forces a commonmark 0.24.0 => 0.29.0 upgrade; sanitization stays allowlist-based with a second pass that rejects any class value the alert renderer does not emit. The title dedup is a pure static helper in `MarkdownRenderer`, applied by the lesson route before `render()` so the render cache stays content-addressed.

**Out of scope:** alert support outside lessons, a Markdown preview in the lesson form, and any change to stored lesson content.

---

## Version Control (GitButler)

- Commit with `but commit fix/lesson-content-styles -m "<msg>"` from the main repository; never `git add`/`git commit`.
- **NEVER push.** The user reviews in GitButler and pushes manually.
- One atomic commit per task below; tests land with the code they prove.

---

## Tasks

- [ ] `docs(plan): Add the lesson alerts and title dedup plan` (this document)
- [ ] `docs(adr): Record the lesson alert rendering decision` ([ADR-0015](../adr/0015-render-lesson-alerts-with-commonmark-alerts.md) plus the ADR index row)
- [ ] `build(deps): Upgrade commonmark to 0.29.0` (version property only; required by the alerts extension, which is version-locked to its core; changelog reviewed, no API removals; full suite green proves tables, heading demotion, and caching survive)
- [ ] `feat(markdown): Render GFM and Obsidian-style alerts in lessons` (alerts dependency; `MarkdownRenderer` wiring: 27 registered types where the five GFM types keep their GitHub identity and the Obsidian set joins them with aliases, custom titles, nesting; sanitizer allowlist for `div[class, data-alert-type]` and `p[class]` plus the `stripUnknownAlertClasses` second pass; renderer tests incl. a class-spoofing case)
- [ ] `feat(frontend): Style lesson alerts with octicon icons` (alert styles in the `.lesson-content` section of `base.css`: accent border and title color per type family via theme tokens, octicons as `mask` data URIs colored by `currentColor`, nested alerts inset; closes #116)
- [ ] `fix(markdown): Skip a leading heading duplicating the lesson title` (static `stripLeadingTitleHeading`; lesson route applies it before `render()`; tests for ATX/setext, case and whitespace tolerance, non-matching heading kept; lesson form hint; consequence note in [ADR-0014](../adr/0014-demote-markdown-headings-in-lesson-rendering.md); closes #117)

## Verification

- `make test` and `./mvnw checkstyle:check` green after the upgrade commit and again at the end.
- Browser pass on a lesson exercising several alert types, nesting, and a custom title: correct icon, accent, and title in both themes; axe reports zero violations; no horizontal page scroll (RGAA 10.11).
- A lesson whose Markdown starts with `# <lesson title>` renders exactly one heading with that text.
