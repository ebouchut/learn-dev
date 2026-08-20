# index.html, tag by tag

A short walkthrough of `index.html`, the **gallery page** of the learn-dev
mockups. It is not a page of the future application: it is the entry point for
reviewers, listing the four real mockups and explaining how theming works. It
still follows every convention of the other pages, which is itself the lesson:
even throwaway tooling pages deserve correct structure.

The document shell is explained in depth in
[home-html-explained.md](home-html-explained.md); section 1 lists it and notes
the two places where this page's shell *differs*.

---

## 1. Shared shell, and two deliberate differences

| Tag | Reminder |
|---|---|
| `<!DOCTYPE html>`, `html lang="en"` | Standards mode; page language (RGAA 8.3) |
| `meta charset` / `meta viewport` | UTF-8; responsive viewport |
| `title` | `learn-dev mockups` (RGAA 8.5) |
| `link` preconnect / stylesheet | Font-origin warm-up; theme then base CSS |
| `a.skip-link` + `main#main` | Skip repeated blocks (RGAA 12.7) |
| `header` / `footer` | Banner and contentinfo landmarks (RGAA 9.2) |

**Difference 1: the brand is a `<span>`, not an `<a>`.**

```html
<span class="site-header__brand">learn<span class="site-header__brand-mark">-dev</span> mockups</span>
```

On the other pages the brand links to `home.html`. Here the gallery *is* the
top of its little world; a link to itself would be a pointless tab stop.
Element choice follows function: no navigation, no anchor.

**Difference 2: there is no `<nav>` at all.** The links to the mockups are the
page's *content*, presented as cards in `<main>`, not site chrome. A `<nav>`
landmark is for repeated navigation blocks; a one-off list of content links
does not need one.

---

## 2. Intro block

```html
<h1>Mockup gallery</h1>
<p>
  Static HTML mockups for the learn-dev frontend, styled by the
  <strong>Catppuccin</strong> theme (default). Structure, BEM classes,
  and accessibility wiring mirror the future Thymeleaf templates.
  See <code>docs/design/theme-exploration.md</code> for tokens and
  WCAG contrast ratios.
</p>
```

- **`<h1>`.** Unique top-level heading; the outline is h1 "Mockup gallery",
  then h2 per card, then h2 "Themes" (RGAA **9.1**).
- **`<strong>`.** The theme name is the key fact of the sentence (which theme
  is applied), so it carries real importance, not mere bolding.
- **`<code>`.** A file path is computer input/output, so `<code>` is the honest
  inline element; unlike the decorative code card on the home page, this one is
  *informative* and therefore **not** hidden from assistive technology. Also
  used below for `prefers-color-scheme`, `css/theme-soft-paper.css`, and,
  neatly, `&lt;link&gt;`: to *display* the text `<link>` without the parser
  eating it, the angle brackets are written as the character entities `&lt;`
  and `&gt;`.

---

## 3. The gallery cards

```html
<ul class="features">
  <li class="feature-card">
    <h2 class="feature-card__title"><a href="home.html">Home</a></h2>
    <p class="feature-card__text">Hero, features, decorative code card.</p>
  </li>
  ...
</ul>
```

- **`<ul>`/`<li>`.** Four cards of the same kind = a list, announced "list,
  4 items" (RGAA **9.3**). The classes reuse the home page's feature-card grid;
  same CSS, different content.
- **`<h2>` wrapping the `<a>`.** Each card title is both a heading and a link.
  Heading-wrapped links are a deliberate power feature: screen reader users can
  scan the gallery by headings *and* activate them in place. The link text
  ("Home", "Log in", "Register", "Dashboard") is explicit on its own
  (RGAA **6.1**).
- **`<p class="feature-card__text">`.** One line saying which *state* each
  mockup freezes (success alert, field error, authenticated stats), which is
  the key to reading the mockups correctly.
- **Why not the alternative.** Making the whole card one big `<a>` is common
  but turns the entire text into one long link name; keeping the link on the
  title keeps names short and precise. Plain `<div>` cards would drop the list
  and heading semantics.

---

## 4. The Themes note

```html
<h2>Themes</h2>
<p>
  Dark mode follows the OS preference (<code>prefers-color-scheme</code>).
  To preview the alternate <strong>Soft Paper</strong> theme, edit the
  theme <code>&lt;link&gt;</code> in a page head to point at
  <code>css/theme-soft-paper.css</code>: both themes define the same
  token names (no runtime switcher in v1, by decision).
</p>
```

A plain `<h2>` + `<p>` documentation block. It explains why the shell loads
*two* stylesheets (theme file defining tokens, then `base.css` consuming them)
and how to swap themes by editing one `href`, the payoff of the token
architecture described in the shell section of
[home-html-explained.md](home-html-explained.md).

The footer differs from the app pages in text only: it says these are design
mockups, not the running app.

---

## 5. Recap table

| Tag | Implicit ARIA role | Key attributes here | Works with | Main purpose on this page |
|---|---|---|---|---|
| shell tags | see home-html-explained.md | none new | shell | Shared frame (no nav, brand not a link) |
| `span.site-header__brand` | none | `class` | header | Non-link brand (nothing to navigate to) |
| `main` | main | `id="main"` | skip link | Gallery content |
| `h1` | heading level 1 | none | outline | Page title (RGAA 9.1) |
| `p` | paragraph | `class` on cards | text content | Intro, card blurbs, theme note |
| `strong` | strong | none | intro text | Key facts (theme names) |
| `code` | code | none | intro / themes text | File paths, CSS feature, escaped `<link>` |
| `ul.features` | list | `class` | li cards | Gallery as a 4-item list (RGAA 9.3) |
| `li.feature-card` | listitem | `class` | h2, p | One mockup card |
| `h2` | heading level 2 | `class` | wraps the card link | Card titles and Themes section (RGAA 9.1) |
| `a` | link | `href` to each mockup | h2, gallery | Navigation to the four mockups (RGAA 6.1) |
| `footer` | contentinfo | `class` | p | Mockup-specific colophon |
