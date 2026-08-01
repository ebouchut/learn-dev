# Lesson Syntax Highlighting Implementation Plan

**Goal:** Color fenced code blocks in lessons by language (issue #128).
The server side has been ready since
[ADR-0013](../adr/0013-render-lesson-markdown-with-commonmark-java.md):
the renderer emits `code class="language-java"` and the sanitizer keeps
the class for a future syntax highlighter. This plan adds that
highlighter, following the client-side precedent of
[ADR-0016](../adr/0016-render-mermaid-diagrams-client-side.md); the
decision is recorded in
[ADR-0017](../adr/0017-highlight-lesson-code-client-side.md).

**Out of scope:** highlighting outside lessons, line numbers, copy
buttons, and languages beyond the highlight.js common bundle.

---

## Version Control (GitButler)

- Commit with `but commit <changes> --branch feat/lesson-syntax-highlighting`
  from the main repository.
- **NEVER push.** The user reviews in GitButler and pushes manually.

---

## Tasks

- [ ] `docs(plan): Add the lesson syntax highlighting plan` (this document)
- [ ] `docs(adr): Record the client-side highlighting decision`
  ([ADR-0017](../adr/0017-highlight-lesson-code-client-side.md) and the
  ADR index row)
- [ ] `build(deps): Add the pinned highlight.js webjar`
  (`org.webjars.npm:highlightjs__cdn-assets` 11.11.1; the plain
  `highlight.js` webjar ships no browser bundle since v11; jar content
  and zero transitives verified before committing)
- [ ] `feat(frontend): Highlight lesson code blocks by language`
  (closes #128)
  - `static/js/lesson-highlight.js`, same shape as `lesson-mermaid.js`:
    select `.lesson-content pre > code[class*="language-"]`, skip
    `language-mermaid`, bail out when nothing matches, lazy-load the
    bundle from the script tag's `data-hljs-src`, highlight only blocks
    whose language `hljs.getLanguage()` recognizes
  - `base.css`: map hljs token classes to the surface-safe accent
    tokens (keyword/type => primary, string => success, number/literal
    => warning, comment => text-muted italic, title/attr => link); no
    vendor theme CSS
  - `templates/courses/lesson.html`: script tag with the versioned
    webjar path in `data-hljs-src`
  - `templates/instructor/lesson-form.html`: one hint sentence about
    language hints on fences

## Verification

- Full test suite and Checkstyle; the existing renderer test pinning
  `language-java` survival must stay green untouched.
- Browser, seeded lesson (restored afterwards): java, python, sql, and
  bash fences show colored tokens in both themes; an unknown-language
  fence and a bare fence stay monochrome; a mermaid fence still renders
  as a diagram; axe (WCAG 2.1 A/AA) zero violations; no horizontal page
  scroll (RGAA 10.11).
- Throwaway data cleaned; `but status` clean; nothing pushed.
