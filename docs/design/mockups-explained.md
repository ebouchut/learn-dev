# learn-dev mockups explained: register.html and home.html

An annotated walkthrough of two mockup files
([mockups/register.html](mockups/register.html) and
[mockups/home.html](mockups/home.html))
and the CSS that styles them ([mockups/css/base.css](mockups/css/base.css) +
[mockups/css/theme-catppuccin.css](mockups/css/theme-catppuccin.css)).
For each element: what tag was chosen and why, what problem it solves, the
ARIA involved, and which RGAA 4.1 criteria it satisfies (RGAA is the French
application of WCAG 2.1 AA that the DWWM certification references).

> [!NOTE]
> 🇫🇷 French version: [mockups-explained-fr.md](mockups-explained-fr.md).
> Keep the two files in sync.

> Files chosen because together they exercise the whole system:
> `register.html` is the accessibility showcase (forms, errors, ARIA);
> `home.html` is the layout showcase (grids, decorative content, components).

---

## Part 1: HTML structure

### 1.1 The document shell (both pages)

```html
<!DOCTYPE html>
<html lang="en">
```

- **`<!DOCTYPE html>`** switches the browser to standards mode. Without it,
  browsers emulate 1990s quirks (broken box model, inconsistent rendering).
- **`lang="en"`** declares the page language. Screen readers pick their
  speech synthesis voice from it; search engines and translators use it too.
  **RGAA 8.3/8.4** (langue par defaut presente et pertinente). This is why
  the language decision matters before the real frontend: the attribute must
  match the actual content language.

```html
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Create an account — learn-dev</title>
```

- **`charset`** first, so the parser never mis-decodes bytes.
- **`viewport`** makes mobile browsers render at device width instead of a
  zoomed-out 980px canvas; prerequisite for any responsive behavior
  (**RGAA 10.11**, contenu consultable quelle que soit l'orientation /
  largeur).
- **`<title>`** is unique per page and pattern-consistent
  ("Page — site"). It is what a screen-reader user hears first, what tabs
  and bookmarks show. **RGAA 8.5/8.6** (titre de page present et pertinent).

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=..." rel="stylesheet">
<link rel="stylesheet" href="css/theme-catppuccin.css">
<link rel="stylesheet" href="css/base.css">
```

- `preconnect` opens the TCP/TLS connection to the font host early (saves a
  round trip when the font CSS is requested).
- **Stylesheet order is the theme mechanism**: the theme file only defines
  custom properties (design tokens); `base.css` consumes them. Loading a
  different theme file swaps every color with zero change to `base.css`.

### 1.2 The skip link (both pages)

```html
<a class="skip-link" href="#main">Skip to main content</a>
```

- **Problem solved**: keyboard and screen-reader users otherwise must tab
  through the entire header and nav on *every page* before reaching content.
  The skip link is the first focusable element and jumps straight to
  `<main id="main">`.
- It is an ordinary anchor to a fragment; no ARIA needed.
- CSS hides it off-screen until it receives focus (see Part 2.4), so
  sighted mouse users never see it, keyboard users always can.
- **RGAA 12.7** (lien d'evitement ou d'acces rapide a la zone de contenu
  principal). One of the most-checked criteria in audits.

### 1.3 Landmarks: header, nav, main, footer (both pages)

```html
<header class="site-header">...</header>
<nav class="site-header__nav" aria-label="Main">...</nav>
<main class="site-main" id="main">...</main>
<footer class="site-footer">...</footer>
```

- These four tags produce **ARIA landmarks for free**: `banner`,
  `navigation`, `main`, `contentinfo`. Screen readers expose a landmark
  menu, so users jump between zones without tabbing. This was verified in
  the accessibility tree of the rendered page (roles `banner`,
  `navigation "Main"`, `main`, `contentinfo` all present).
- **`aria-label="Main"` on `<nav>`**: names the navigation zone. Required
  the moment a page can contain more than one `<nav>` (main menu, footer
  menu, breadcrumb); labeling from day one costs nothing and scales.
- **Why not `<div class="header">`**: a div has no role; the landmark menu
  would be empty, and RGAA 9.2 / 12.6 would fail.
- **RGAA 9.2** (structure du document coherente: header, main, footer),
  **RGAA 12.6** (zones de regroupement atteignables ou activables).

### 1.4 Navigation list and the current page (both pages)

```html
<ul class="nav__list">
  <li><a class="nav__link" href="home.html">Home</a></li>
  <li><a class="nav__link" href="register.html" aria-current="page">Sign up</a></li>
</ul>
```

- **`<ul>/<li>`**: navigation is a *list of links*; the list semantics let
  a screen reader announce "list, 3 items", giving users the size of the
  menu upfront. **RGAA 9.3** (listes correctement structurees).
- **`aria-current="page"`** marks the link matching the current page. A
  screen reader announces "current page"; CSS also styles it (bold + mauve
  underline), so the information exists in *both* channels: assistive tech
  and vision. That duality is the core of **RGAA 3.1** (l'information n'est
  pas donnee uniquement par la couleur).
- Link texts ("Home", "Log in", "Sign up") are explicit out of context:
  **RGAA 6.1** (chaque lien est explicite).

### 1.5 Headings hierarchy

`register.html`: one `<h1>` ("Create an account").
`home.html`: `<h1>` (hero title) then `<h2>` ("Why learn-dev?") then one
`<h3>` per feature card.

- Screen-reader users navigate by headings (the `H` key) more than by any
  other mechanism. The hierarchy is strictly decreasing with no skipped
  levels, and there is exactly one `<h1>` per page.
- **RGAA 9.1** (information structuree par des titres pertinents).

### 1.6 The registration form (register.html), field by field

```html
<p class="alert alert--error reveal" role="alert">
  Your registration could not be completed. Check the highlighted field below.
</p>
```

- **`role="alert"`**: turns the paragraph into an assertive **live region**:
  when the page (re)renders with an error, screen readers announce it
  immediately without the user having to find it. In the real Thymeleaf
  template this block will be conditionally rendered after a failed POST.
- Placed **before** the form so it is encountered first in reading order.
- **RGAA 11.11** (le controle de saisie est accompagne de suggestions
  d'erreur) and part of the WCAG 4.1.3 "status messages" behavior.

```html
<form action="#" method="post" novalidate>
```

- **`method="post"`**: registration mutates state; GET would leak the
  password into URLs, logs, and history.
- **`novalidate`**: mockup-only. It suppresses native browser validation so
  the *server-rendered* error state can be demonstrated (that is how the
  Spring/Thymeleaf app behaves: Bean Validation runs server-side and the
  page re-renders with errors).

```html
<label class="form__label" for="username">Username</label>
<span class="form__hint" id="username-hint">3 to 50 characters.</span>
<input class="form__input" type="text" id="username" name="username"
       autocomplete="username" aria-describedby="username-hint" required>
```

- **`<label for>` + `id`**: the *programmatic* association between text and
  field. Clicking the label focuses the field (bigger touch target), and a
  screen reader announces "Username, edit text" when the input gets focus.
  **RGAA 11.1** (chaque champ a une etiquette) — the single most-audited
  form criterion.
- **`<span class="form__hint" id>` + `aria-describedby`**: the hint is
  attached as the input's *accessible description*: announced after the
  label, but not part of the name. Sighted users see it above the field;
  screen-reader users hear it in context. **RGAA 11.4/11.5** family
  (etiquettes et champs accoles, indications de saisie).
- **`autocomplete="username"`**: tells browsers and password managers the
  field's *purpose*, enabling autofill. **RGAA 11.13** (la finalite du champ
  peut etre deduite) = WCAG 1.3.5 "Identify Input Purpose".
- **`required`**: expresses the constraint in the markup (accessibility
  tree exposes "required"); the server still revalidates.

```html
<input class="form__input form__input--invalid" type="email" id="email"
       name="email" autocomplete="email" aria-describedby="email-error"
       aria-invalid="true" value="carol@example.org" required>
<span class="form__error" id="email-error">Email already registered</span>
```

The error-state pattern, and the heart of this mockup:

- **`type="email"`** gives semantic keyboards on mobile and native format
  checking (**RGAA 11.10**, controle de saisie pertinent).
- **`aria-invalid="true"`** flags the field as failed in the accessibility
  tree: screen readers announce "invalid entry".
- **`aria-describedby="email-error"`** points at the error message, so
  focusing the field reads: "Email, edit text, invalid entry, Email already
  registered". The error text is *findable from the field*, not just
  visually nearby. This was verified in the rendered page: the computed
  description of `#email` is exactly "Email already registered".
- The visual channel is redundant with the programmatic one: red border
  (`--invalid` modifier class) **and** bold red text below — never color
  alone (**RGAA 3.1**), with a contrast of 4.80:1 for the error color on
  the page background (**RGAA 3.2**, contraste des textes >= 4.5:1).
- **RGAA 11.11** (erreur identifiee + suggestion de correction).

```html
<button class="button button--primary" type="submit">Create my account</button>
```

- A true `<button type="submit">`, not a styled `<a>` or `<div>`: submits
  on click *and* on Enter/Space, is focusable, and exposes the `button`
  role natively. No ARIA needed — the first rule of ARIA is to prefer
  native elements.

### 1.7 The decorative code card (home.html)

```html
<div class="code-card reveal reveal--4" aria-hidden="true">
  <code class="code-card__code">...</code>
</div>
```

- **`aria-hidden="true"`**: the card is *eye candy* — a stylized Java
  snippet signaling "programming" visually. For a screen reader it would be
  noise (hearing raw code read aloud adds nothing to the hero's message),
  so the subtree is removed from the accessibility tree entirely.
  **RGAA 1.2** analog (contenu decoratif ignore par les technologies
  d'assistance).
- **`<div>`** is correct here precisely *because* the element carries no
  semantics: it is presentation only.
- **`<code>`** inside keeps honest semantics for the code text (and gets
  the mono font from a `code { font-family: var(--font-mono) }` rule).
- Same reasoning for the feature-card emoji:
  `<span class="feature-card__icon" aria-hidden="true">⚡</span>` — the
  adjacent `<h3>` already carries the meaning; the emoji would otherwise be
  read as "high voltage sign".

### 1.8 The feature cards as a list (home.html)

```html
<ul class="features">
  <li class="feature-card">
    <h3 class="feature-card__title">Interactive lessons</h3>
    <p class="feature-card__text">...</p>
  </li>
  ...
</ul>
```

- Three parallel items = a **list**, so `<ul>/<li>` (RGAA 9.3), not three
  sibling divs. "List, 3 items" tells the user the shape of the content.
- Each card holds a heading + paragraph, giving the H-key navigation a stop
  per feature.

---

## Part 2: Layout and CSS

### 2.1 Naming convention: BEM

Every class follows **BEM** (`block__element--modifier`):

| Piece | Syntax | Examples from these pages |
|---|---|---|
| Block: standalone component | `.block` | `.site-header`, `.form-card`, `.code-card`, `.alert`, `.button` |
| Element: a part that only makes sense inside its block | `.block__element` | `.site-header__brand`, `.form__input`, `.hero__title`, `.code-card__code` |
| Modifier: a variant or state of a block/element | `.block--modifier`, `.block__element--modifier` | `.button--primary`, `.alert--error`, `.form__input--invalid`, `.site-main--narrow` |

Why BEM here:

- **Flat specificity**: every selector is a single class, i.e. specificity
  `(0,1,0)`. No descendant chains, so no specificity wars: any rule can be
  overridden by another single class later in the cascade.
- **Self-documenting**: `form__error` tells you exactly where it lives;
  `button--ghost` tells you it is a variant of `.button`.
- **Grep-friendly**: searching `feature-card` finds the whole component.
- IDs are **never used for styling** (only for `label for`, fragment
  targets like `#main`, and `aria-describedby` plumbing). An ID selector
  (`#main`, specificity `(1,0,0)`) would crush every class rule and break
  the flat model.

### 2.2 Design tokens (theme file + structural tokens)

```css
:root {
  --color-bg: #eff1f5;
  --color-text: #4c4f69;
  --color-primary: #8839ef;
  ...
}
@media (prefers-color-scheme: dark) {
  :root { --color-bg: #1e1e2e; ... }
}
```

- **`:root`** is the `<html>` element with pseudo-class specificity
  `(0,1,0)`; custom properties declared there **inherit into every
  element**, making them global design tokens.
- **`--color-*` custom properties** are consumed with `var(--color-*)` in
  `base.css`. No component ever contains a hex value, so:
  1. swapping the theme `<link>` re-skins the entire site;
  2. dark mode is one `@media (prefers-color-scheme: dark)` block that
     reassigns the same token names — components are untouched.
- **`color-scheme: light dark`** tells the browser both schemes are
  supported, so native widgets (inputs, scrollbars) follow along.
- Structural tokens in `base.css` (`--space-1..6` on a 0.25rem scale,
  `--font-size-*` on a modular scale, `--radius`, `--shadow`) play the same
  role for geometry. All sizes are in **rem**, so everything scales when
  the user changes the browser's base font size (**RGAA 10.4**, texte
  agrandissable a 200%).

### 2.3 Reset and typography

```css
*, *::before, *::after { box-sizing: border-box; }
```

- Universal selector, specificity `(0,0,0)` — deliberately the weakest rule
  in the file, so anything can override it. `border-box` makes `width`
  include padding and border: the intuitive box model (`width: 100%` plus
  padding no longer overflows — exactly what `.form__input` relies on).

```css
body {
  font-family: var(--font-body);
  font-size: var(--font-size-base);
  line-height: var(--line-height);   /* 1.6, unitless */
  color: var(--color-text);
  background: var(--color-bg);
}
```

- Set once on `body`, inherited everywhere (type selector, `(0,0,1)`).
- **Unitless `line-height: 1.6`** is a multiplier that inherits correctly
  at any font size (a unit value like `px` would freeze it). 1.6 satisfies
  the RGAA/WCAG comfort recommendation (>= 1.5, **RGAA 10.12** context).
- The body font is **Atkinson Hyperlegible**, designed by the Braille
  Institute for low-vision legibility (distinct b/d/p/q letterforms): a
  typography choice that *is* an accessibility feature.

```css
:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}
```

- **The single most important accessibility rule in the file.** Every
  focusable element gets a visible 2px outline in the focus color
  (Catppuccin blue, chosen for the 3:1 UI-component contrast requirement).
- `:focus-visible` (not `:focus`) fires for keyboard focus but not for
  mouse clicks, so the outline never annoys pointer users and never
  disappears for keyboard users. The stylesheet **never** writes
  `outline: none`. **RGAA 10.7** (focus visible).

```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { animation: none !important; transition: none !important; }
}
```

- Users with vestibular disorders opt out of motion at the OS level; this
  honors it by killing all animation. The only `!important` in the project:
  justified because this preference must beat *any* other rule.
  **RGAA 13.x** family (contenu en mouvement controle par l'utilisateur).

### 2.4 The skip link mechanics

```css
.skip-link {
  position: absolute;
  top: -3rem;              /* parked above the viewport */
  transition: top 150ms ease;
}
.skip-link:focus-visible { top: 0; }   /* slides in when focused */
```

- Off-screen positioning (not `display: none`!) keeps the link focusable:
  `display: none` would remove it from the tab order and defeat the point.
- On focus, `top: 0` brings it into view; the compound selector
  `.skip-link:focus-visible` has specificity `(0,2,0)` and beats
  `.skip-link` `(0,1,0)` — state rules must outweigh base rules, and here
  the pseudo-class provides exactly one extra specificity point.

### 2.5 Header and navigation

```css
.site-header { border-bottom: 1px solid var(--color-border); background: var(--color-surface); }
.site-header__inner {
  max-width: var(--content-width);   /* 64rem */
  margin: 0 auto;                    /* the classic centering idiom */
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex-wrap: wrap;
}
.site-header__nav { margin-left: auto; }
```

- The block paints edge-to-edge (border + surface); the `__inner` element
  constrains content to a readable column and centers it with auto margins.
  This two-layer pattern repeats in the footer.
- **Flexbox** for the header because it is a one-dimensional row;
  `align-items: center` vertically centers brand and nav;
  `margin-left: auto` on the nav absorbs all free space, pushing the nav to
  the right without floats or positioning; `flex-wrap` lets the nav drop to
  a second line on narrow screens instead of overflowing.

```css
.nav__link[aria-current="page"] {
  font-weight: 700;
  color: var(--color-primary);
  box-shadow: inset 0 -2px 0 var(--color-primary);
}
```

- **Styling driven by the ARIA attribute itself**: the state lives once in
  the markup and CSS reads it — impossible for the visual state and the
  announced state to disagree.
- Specificity `(0,2,0)` (class + attribute selector), so it cleanly
  overrides `.nav__link` `(0,1,0)`.
- The "underline" is an inset box-shadow rather than `text-decoration`, so
  it sits at the link's padding edge and does not double with the hover
  underline.

### 2.6 Buttons

```css
.button { /* base: font, padding 0.65em/1.4em, radius, cursor, transition */ }
.button--primary { background: var(--color-primary); color: var(--color-on-primary); }
.button--ghost   { background: transparent; color: var(--color-text); border-color: var(--color-border); }
```

- Markup opts in with both classes: `class="button button--primary"`. Base
  and modifier have equal specificity `(0,1,0)`; the modifier wins for the
  properties it redefines purely by **source order** (it appears later in
  the file) — the intended BEM mechanism, no `!important`, no nesting.
- Padding in **em** scales with the button's own font size; radius and
  colors come from tokens. The white-on-mauve pair measures 5.41:1
  (**RGAA 3.2**; buttons are text, so 4.5:1 applies, not just the 3:1 UI
  minimum).
- The hover lift (`transform: translateY(-1px)` + shadow) is a transition,
  therefore disabled automatically by the reduced-motion block.

### 2.7 The hero grid and the code card (home.html)

```css
.hero {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: var(--space-5);
  align-items: center;
}
@media (max-width: 46rem) { .hero { grid-template-columns: 1fr; } }
```

- **Grid** because the hero is two-dimensional (columns whose heights must
  align). The `fr` units give the text column 55% of the space, the code
  card 45%, without magic pixel numbers.
- The **media query in rem** (46rem ≈ 736px at default zoom) collapses to
  one column: breakpoints in rem respect user font-size settings
  (**RGAA 10.11**, reflow).

```css
.code-card {
  background: var(--code-bg);      /* stays Mocha-dark even in light mode */
  font-family: var(--font-mono);
  rotate: 1.5deg;
  position: relative;
}
.code-card::before {
  content: "";
  position: absolute;
  width: 10px; height: 10px; border-radius: 50%;
  background: var(--color-error);
  box-shadow: 18px 0 0 var(--color-warning), 36px 0 0 var(--color-success);
}
```

- The card intentionally keeps the dark palette in both modes: it is a
  "terminal window" motif, and its syntax colors (`--code-*` tokens) are
  the Mocha pastels, which pass contrast on the dark background.
- **`::before` + box-shadow trick**: one pseudo-element paints all three
  macOS "traffic light" dots — the two extra dots are solid, offset
  box-shadows (18px and 36px to the right). Zero extra markup for pure
  decoration, which is exactly what pseudo-elements are for (and being CSS
  content, it is invisible to screen readers — consistent with the card's
  `aria-hidden`).
- `position: relative` on the card establishes the containing block that
  the absolutely-positioned pseudo-element is placed against.
- `rotate: 1.5deg` (the modern individual-transform property) gives the
  hand-placed sticker feel; `.code-card__code { white-space: pre; overflow-x: auto; }`
  preserves code indentation and scrolls horizontally *inside the card*
  rather than breaking the page (**RGAA 10.11** again).

```css
.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(15rem, 1fr));
  gap: var(--space-4);
}
```

- The **`auto-fit` + `minmax` idiom**: as many equal columns as fit, each
  at least 15rem, each stretching to share leftovers. Three cards on
  desktop, two on tablet, one on phone — responsive with **no media query
  at all**.

### 2.8 The form (register.html)

```css
.site-main--narrow { max-width: var(--form-width); }  /* 26rem column */
```

- A modifier on the `main` block swaps the 64rem content column for a
  26rem one: forms read best in a narrow measure. One class in the markup
  (`class="site-main site-main--narrow"`), no duplicate layout code.

```css
.form__input {
  width: 100%;
  font: inherit;
  border: 1px solid var(--color-border);
  background: var(--color-bg);
}
```

- **`font: inherit`** is load-bearing: form controls do *not* inherit fonts
  by default; without this, inputs render in the OS font at 13px, breaking
  both the design and the zoom behavior.
- `width: 100%` is safe because of the global `border-box`.

```css
.form__input:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 1px;
  border-color: var(--color-focus);
}
.form__input--invalid { border-color: var(--color-error); }
.form__error { color: var(--color-error); font-weight: 700; font-size: var(--font-size-sm); }
```

- Focus recolors the border *and* draws the outline (belt and braces for
  RGAA 10.7).
- `.form__input` and `.form__input--invalid` tie at `(0,1,0)`; the modifier
  wins `border-color` by source order. But
  `.form__input:focus-visible` is `(0,2,0)`, so **while focused, the focus
  color deliberately beats the error color** — you always see where you
  are; the error is still conveyed by the message and `aria-invalid`.
- The error text is bold *and* red *and* programmatically linked: three
  channels (RGAA 3.1).

```css
.alert { border: 1px solid; border-radius: var(--radius); background: var(--color-surface); }
.alert--error   { color: var(--color-error);   border-color: var(--color-error); }
.alert--success { color: var(--color-success); border-color: var(--color-success); }
```

- The base rule declares `border: 1px solid` **without a color**: CSS then
  uses `currentColor` for the border, and each modifier only needs to set
  `color` + `border-color`... in fact setting `color` alone would suffice
  for the border thanks to currentColor; the explicit `border-color` keeps
  the intent readable. Every variant's text passes 4.5:1 on the page
  background (see the theme exploration tables).

### 2.9 The reveal animation

```css
@keyframes rise-in {
  from { opacity: 0; translate: 0 10px; }
  to   { opacity: 1; translate: 0 0; }
}
.reveal    { animation: rise-in 500ms ease both; }
.reveal--2 { animation-delay: 100ms; }   /* --3: 200ms, --4: 300ms */
```

- One keyframe, staggered by delay-modifier classes: the hero title, lead,
  buttons, and code card rise in sequence — a single orchestrated page-load
  moment instead of scattered effects.
- `animation-fill-mode: both` (the `both` keyword) applies the `from` state
  before the animation starts (no flash of final state) and holds the `to`
  state after.
- 10px of travel and 500ms: perceptible, not theatrical. And the whole
  thing is erased by the `prefers-reduced-motion` block for users who need
  that (the page is fully usable with zero animation).

---

## RGAA quick map (what an auditor would check on these two pages)

| RGAA criterion (theme) | Where it is satisfied |
|---|---|
| 3.1 information pas seulement par la couleur | error = border + bold text + `aria-invalid`; current nav = bold + underline + `aria-current` |
| 3.2 contraste des textes (4.5:1) | every token pair computed in [theme-exploration.md](theme-exploration.md) (worst pair on these pages: 4.73:1) |
| 6.1 liens explicites | "Create your account", "Source on GitHub", "Log in" |
| 8.3/8.4 langue de page | `<html lang="en">` (to become `fr` when the language decision lands) |
| 8.5/8.6 titre de page | unique, patterned `<title>` per page |
| 9.1 titres pertinents | single `h1`, ordered `h2`/`h3` |
| 9.2 structure coherente | `header`/`nav`/`main`/`footer` landmarks |
| 9.3 listes | nav list, features list |
| 10.4 agrandissement du texte | all sizes in rem/em, unitless line-height |
| 10.7 focus visible | global `:focus-visible` outline, never removed |
| 10.11 reflow / responsive | rem breakpoints, auto-fit grids, internal overflow |
| 11.1 etiquettes de champs | `<label for>` on every input |
| 11.10 controle de saisie | `type="email"`, `required` |
| 11.11 erreurs et suggestions | `role="alert"` summary + per-field message via `aria-describedby` |
| 11.13 finalite des champs | `autocomplete="username|email|new-password"` |
| 12.7 lien d'evitement | `.skip-link` to `#main` |
| 1.2 decoration ignoree | `aria-hidden` on code card and emoji |

---

*The authoritative styling sources are
[mockups/css/base.css](mockups/css/base.css) and
[mockups/css/theme-catppuccin.css](mockups/css/theme-catppuccin.css);
contrast figures come from [theme-exploration.md](theme-exploration.md).
Per-file companion docs live in [mockups/](mockups/).*
