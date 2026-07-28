# Render lesson alerts with the commonmark-java alerts extension

- Status: accepted
- Date: 2026-07-28
- Deciders: Eric Bouchut

## Context and Problem Statement

Instructors write lesson content in Markdown (see
[ADR-0013](0013-render-lesson-markdown-with-commonmark-java.md)) and expect
the callout syntax they know from GitHub and Obsidian to work:
`> [!note]`, `> [!tip] Custom title`, nested callouts. Plain CommonMark
renders these as ordinary blockquotes with the marker as literal text. How
should lessons render alert/callout blocks while keeping the sanitization
pipeline allowlist-based and the rendered HTML free of author-controlled
markup tricks?

## Decision Drivers

- Author familiarity: the GitHub/Obsidian syntax should just work, including
  content pasted from either tool
- ADR-0013's pipeline stays: parse, render, sanitize against an allowlist;
  no raw SVG or style attributes may survive into the page
- Maintenance cost: prefer an official, tested module over in-repo parser code
- GitHub fidelity: content authored for GitHub must render with the same
  type semantics as on GitHub

## Considered Options

- The official `commonmark-ext-gfm-alerts` extension (requires upgrading
  commonmark 0.24.0 => 0.29.0)
- A custom commonmark-java block parser and renderer kept in this repository
- A jsoup post-processing pass that rewrites `[!note]` blockquotes after
  rendering
- Do nothing: callout markers render as literal blockquote text

## Decision Outcome

Chosen: "the official `commonmark-ext-gfm-alerts` extension", because it is
maintained by the commonmark-java project itself, implements the exact GFM
syntax plus the options needed to cover Obsidian (custom types, custom
titles, nested alerts), and emits plain `div`/`p` markup with predictable
class names that the sanitizer can allowlist precisely. The extension is
version-locked to its same-version core and its configuration API only
exists in 0.29.0, so the whole commonmark stack moves 0.24.0 => 0.29.0
(changelog reviewed: no API removals; the notable parser change is that
tables no longer require a preceding blank line).

Configuration decisions that follow:

- The five GFM types (`NOTE`, `TIP`, `IMPORTANT`, `WARNING`, `CAUTION`) keep
  their GitHub identity: `IMPORTANT` and `CAUTION` are registered as
  standalone types, not as Obsidian aliases, so GitHub-authored content
  renders with GitHub's semantics. The remaining Obsidian callout set joins
  them with all aliases (27 registered types in total); markers are
  case-insensitive; custom titles and nesting are enabled.
- Icons are Octicons (the set GitHub uses for alerts), matched per type
  against Obsidian's icon semantics, and shipped as CSS `mask-image` data
  URIs colored by `currentColor`. The sanitizer therefore never has to let
  SVG through.
- The jsoup allowlist must now admit `class` on `div` and `p` plus
  `data-alert-type`. To keep raw HTML in lessons from borrowing site classes
  (`alert`, `site-header`, ...), a second sanitization pass strips every
  class value the alert renderer does not emit: only
  `markdown-alert markdown-alert-<type>` on `div` and `markdown-alert-title`
  on `p` survive.

### Consequences

- Good: GitHub- and Obsidian-authored content renders as its authors expect,
  with no house syntax to teach
- Good: the sanitization posture of ADR-0013 is preserved; the allowlist
  widens by two attributes and is immediately narrowed by an exact-value pass
- Good: the commonmark stack is current again (0.24.0 dated from the initial
  integration)
- Trade-off: a major-feature upgrade of the Markdown stack rides along; the
  full test suite is the regression net for tables, heading demotion, and
  caching
- Trade-off: 27 registered types means 27 CSS selectors and a set of icon
  data URIs in `base.css`, a one-time styling cost paid in this change

## Pros and Cons of the Options

### Official commonmark-ext-gfm-alerts extension

- 👍 Maintained upstream by the commonmark-java project, GFM-exact syntax
- 👍 Options cover the full Obsidian feature set (types, titles, nesting)
- 👍 Emits sanitizer-friendly `div`/`p` markup, no inline styles or SVG
- 👎 Forces the 0.24.0 => 0.29.0 core upgrade in the same change

### Custom block parser in this repository

- 👍 No version constraint on the core
- 👎 Reimplements and then maintains non-trivial parsing (nesting, titles,
  lazy continuation) that upstream already tests

### jsoup post-processing of rendered blockquotes

- 👍 No parser changes at all
- 👎 Fragile text matching inside rendered HTML; nesting and custom titles
  get hard quickly; the marker has already been mangled by inline rendering

### Do nothing

- 👍 No code
- 👎 Pasted GitHub/Obsidian content degrades into blockquotes with visible
  `[!note]` markers, exactly what instructors would report as a bug
