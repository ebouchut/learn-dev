# Demote Markdown headings one level in lesson rendering

- Status: accepted
- Date: 2026-07-13
- Deciders: Eric Bouchut

## Context and Problem Statement

The lesson page renders the lesson title as the page `h1`, then emits the
instructor's Markdown as HTML (see
[ADR-0013](0013-render-lesson-markdown-with-commonmark-java.md)). Instructors
naturally start their content with `# Heading`, which CommonMark renders as a
second `h1`. Two `h1` elements on one page break the heading hierarchy that
docs/rgaa.md commits to for RGAA 9.1 (one `h1` per page, no skipped level),
and screen-reader users navigating by headings lose the page outline. How
does instructor-authored Markdown coexist with the page's own `h1`?

## Decision Drivers

- RGAA 9.1: exactly one `h1` per page, ordered heading levels
- Instructors should write natural Markdown, not remember a house rule
- The sanitization pipeline of ADR-0013 must stay unchanged
- Already-authored content must keep working without migration

## Considered Options

- Demote headings one level at render time (`#` becomes `h2`, capped at `h6`)
- Reject or lint Markdown that contains a level-1 heading at authoring time
- Strip `h1` from the sanitized output (jsoup allowlist change)
- Do nothing and document a "never use #" authoring convention

## Decision Outcome

Chosen: "demote headings one level at render time", because it makes the
accessible outcome structural instead of relying on author discipline:
whatever the instructor writes, the rendered fragment slots under the page's
`h1` with a correct hierarchy, and existing content is fixed retroactively
the next time it renders (the cache of ADR-0013 is content-addressed, so no
invalidation is needed). A custom commonmark-java `NodeRenderer` for
`Heading` shifts each level down by one, capping at `h6`; the jsoup
sanitization is untouched.

Rejecting `#` at authoring time would surprise instructors and does nothing
for content pasted from elsewhere; stripping `h1` in the sanitizer would
silently delete content; a documented convention is the option that RGAA
audits exist to catch.

### Consequences

- Good: one `h1` per page holds on lesson pages by construction (RGAA 9.1)
- Good: no authoring rule to teach; pasted Markdown behaves sensibly
- Good: no schema or content migration; re-rendering applies the new levels
- Trade-off: authored `######` (h6) and `#####` (h5) both render as `h6`,
  collapsing one distinction at the deepest levels
- Trade-off: the rendered HTML no longer matches what a generic CommonMark
  renderer would produce for the same source (a surprise when comparing with
  an external preview)
- Amendment (2026-07-28, issue #117): demotion turns the common habit of
  starting a document with its own title into an `h2` that duplicates the
  page `h1`, so the lesson route now strips a leading level-1 heading whose
  text equals the lesson title before rendering
  (`MarkdownRenderer.stripLeadingTitleHeading`); the stored Markdown is
  untouched

## Pros and Cons of the Options

### Demote at render time

- 👍 Structural guarantee, independent of author discipline
- 👍 Fixes existing and pasted content with no migration
- 👎 Deepest levels collapse (h5 and h6 both become h6)

### Reject level-1 headings at authoring time

- 👍 The stored source matches the rendered output
- 👎 Surprising validation error for natural Markdown; no help for existing rows

### Strip h1 in the sanitizer

- 👍 One-line allowlist change
- 👎 Silently deletes the author's text, the worst failure mode of the four

### Authoring convention only

- 👍 No code
- 👎 Exactly the class of promise automated audits keep finding broken
