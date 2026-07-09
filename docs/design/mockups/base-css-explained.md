# `base.css` explained, rule by rule

An educational walkthrough of `css/base.css`, the theme-independent
stylesheet of the learn-dev mockups. It is written for a developer preparing
the French DWWM (Developpeur Web et Web Mobile) certification: every rule is
quoted, every declaration is explained (what it does and why that value was
chosen), and the interactions between rules (cascade, specificity, source
order) are called out explicitly, because that is exactly what an examiner
will probe.

Companion documents:

- [theme-catppuccin-css-explained.md](theme-catppuccin-css-explained.md)
  (default theme, plus the explanation of the token architecture)
- [theme-soft-paper-css-explained.md](theme-soft-paper-css-explained.md)
  (alternate theme)

`base.css` never hardcodes a color. Every `var(--color-*)` and
`var(--code-*)` it consumes is defined by whichever theme stylesheet is
linked in the page head. `base.css` itself only defines the **structural**
tokens: fonts, sizes, spacing, radii, shadow, layout widths.

---

## Primer: the BEM naming convention

Every component class in this file follows **BEM**
(Block, Element, Modifier), with the `block__element--modifier` syntax:

- **Block**: a standalone component, e.g. `.button`, `.form-card`,
  `.site-header`. The name describes purpose, not appearance.
- **Element**: a part that only makes sense inside its block, joined with
  a double underscore: `.site-header__brand`, `.form__input`,
  `.stat-card__value`.
- **Modifier**: a variant of a block or element, joined with a double
  hyphen: `.button--primary`, `.site-main--narrow`,
  `.form__input--invalid`.

Why BEM matters for the cascade: **every selector is a single class**, so
almost every rule has specificity `(0,1,0)`. Nothing wins by being "more
specific"; variants win by **source order** (the modifier rule is written
after the base rule) or by an added pseudo-class/attribute. That makes the
stylesheet predictable: to know which rule applies, you mostly just read
top to bottom.

### Reading specificity: the `(a,b,c)` triple

Throughout this document, specificity is noted as `(a,b,c)`:

- `a` counts ID selectors (`#foo`), never used in this file;
- `b` counts classes (`.foo`), attribute selectors (`[aria-current]`),
  and pseudo-classes (`:hover`, `:focus-visible`);
- `c` counts element types (`body`, `h1`) and pseudo-elements
  (`::before`).

Higher `a` beats any `b`; higher `b` beats any `c`. On a tie, the **later
rule in source order** wins. `!important` sits outside this system and
beats normal declarations regardless of specificity (used exactly once in
this file, in the reduced-motion block, see below).

### `em` vs `rem`, once and for all

Both units appear in this file, deliberately:

- **`rem`** is relative to the **root** (`<html>`) font size. Used for the
  type scale, spacing scale and layout widths, so that a user who raises
  their browser's default font size scales the *whole page* consistently.
  This is an accessibility requirement in practice: `px` media queries and
  `px` font sizes ignore that user preference.
- **`em`** is relative to the **current element's own font size**. Used
  where a measurement should follow the text it decorates: button padding
  (`0.65em 1.4em` grows with the button label), `code { font-size: 0.9em }`
  (always 90% of the surrounding prose, whatever its size),
  `text-underline-offset: 0.2em` (the underline gap scales with the link
  text).

Rule of thumb: `rem` for page-level rhythm, `em` for element-local,
text-relative measurements.

---

## Section: Structural tokens

### `:root` (structural custom properties)

```css
:root {
  --font-display: "Sora", ui-sans-serif, sans-serif;
  --font-body: "Atkinson Hyperlegible", ui-sans-serif, system-ui, sans-serif;
  --font-mono: "JetBrains Mono", ui-monospace, monospace;

  --font-size-sm: 0.875rem;
  --font-size-base: 1rem;
  --font-size-lg: 1.125rem;
  --font-size-xl: 1.375rem;
  --font-size-2xl: 1.75rem;
  --font-size-3xl: 2.5rem;
  --line-height: 1.6;

  --space-1: 0.25rem;
  --space-2: 0.5rem;
  --space-3: 1rem;
  --space-4: 1.5rem;
  --space-5: 2rem;
  --space-6: 3rem;

  --radius-sm: 4px;
  --radius: 8px;
  --radius-lg: 14px;
  --shadow: 0 1px 3px rgb(0 0 0 / 0.08), 0 4px 14px rgb(0 0 0 / 0.06);

  --content-width: 64rem;
  --form-width: 26rem;
}
```

- `:root` is a pseudo-class matching the document root (`<html>`), with
  specificity `(0,1,0)`. Custom properties declared here are **inherited by
  every element**, which is what makes them usable as global design tokens.
- **Font stacks**: each token names a webfont first, then fallbacks.
  - `--font-display` (headings, buttons, big numbers): "Sora", a geometric
    display face; falls back to `ui-sans-serif` (the platform UI font)
    then generic `sans-serif`.
  - `--font-body`: "Atkinson Hyperlegible", a font designed by the Braille
    Institute specifically for low-vision readability (distinct letterforms
    for I/l/1, b/d, etc.), a deliberate accessibility choice for a learning
    platform; `system-ui` is an extra fallback.
  - `--font-mono`: "JetBrains Mono" for code, then `ui-monospace`
    (SF Mono, Cascadia...) and generic `monospace`.
- **Type scale** in `rem` (see the em/rem primer above): 0.875 / 1 / 1.125 /
  1.375 / 1.75 / 2.5 rem, i.e. 14 / 16 / 18 / 22 / 28 / 40 px at the default
  16 px root. A small modular scale: each step is visibly distinct without
  hardcoding pixel values, and everything grows if the user raises the
  default font size.
- `--line-height: 1.6` is **unitless on purpose**. A unitless line-height
  is a *factor* recomputed against each element's own font size when
  inherited. If you wrote `1.6em` or `160%`, the *computed pixel value* of
  the body would be inherited, so a large heading would get the body's
  line height in pixels and its lines would overlap. Unitless is the only
  safe inheritable form. 1.6 (rather than the browser default of roughly
  1.2) is a readability choice for long-form learning content and matches
  WCAG 1.4.12 (text spacing) comfort levels.
- **Spacing scale** `--space-1` through `--space-6`: 0.25 to 3 rem
  (4 to 48 px at default size). Every margin/padding/gap in the file uses
  one of these six values, which produces a consistent vertical rhythm and
  makes redesign a one-line change.
- **Radii** in `px`, not `rem`: corner rounding is a decorative constant
  that should *not* grow with the user's font size (a 4 px curve is a
  4 px curve). Three sizes: `sm` for small chips/focus rings, default for
  buttons/inputs, `lg` for cards.
- `--shadow` stacks **two layers**: a tight one (`0 1px 3px`, 8% black)
  simulating contact, and a wide soft one (`0 4px 14px`, 6% black)
  simulating ambient light. Layered shadows look softer and more realistic
  than a single big blur. Note the modern space-separated
  `rgb(0 0 0 / 0.08)` alpha syntax.
- `--content-width: 64rem` (1024 px at default) caps line length for
  readability; `--form-width: 26rem` (416 px) is a much narrower cap for
  authentication forms, where a full-width input would be hostile.

---

## Section: Reset-lite and typography

### Universal `box-sizing`

```css
*,
*::before,
*::after {
  box-sizing: border-box;
}
```

- The classic minimal reset. `border-box` makes `width`/`height` include
  padding and border, so `.form__input { width: 100%; padding: ... }`
  never overflows its container. Without it, `width: 100%` plus padding
  would exceed 100%.
- The selector list explicitly includes `::before`/`::after` because the
  universal selector `*` does **not** match pseudo-elements.
- Specificity `(0,0,0)`: `*` counts for nothing, so any other rule can
  override this trivially. That is exactly what you want from a reset.

### `body`

```css
body {
  margin: 0;
  font-family: var(--font-body);
  font-size: var(--font-size-base);
  line-height: var(--line-height);
  color: var(--color-text);
  background: var(--color-bg);
}
```

- `margin: 0` removes the browser's default 8 px body margin (the header
  and footer must reach the viewport edges).
- Typography is set once here and **inherited** by everything: font
  family, base size, the unitless line height.
- `color` and `background` are the first two **theme tokens** consumed:
  the page ink and paper. Setting both together is also an accessibility
  habit: never set a text color without controlling what it sits on.

### Headings

```css
h1,
h2,
h3 {
  font-family: var(--font-display);
  line-height: 1.2;
  margin: 0 0 var(--space-3);
  letter-spacing: -0.015em;
}
```

- Only three heading levels are styled because the mockups use only three;
  a real design system would go to `h6`.
- `--font-display` switches headings to Sora, creating the display/body
  font pairing.
- `line-height: 1.2` overrides the inherited 1.6: large text needs tighter
  leading or multi-line headings fall apart visually. Again unitless.
- `margin: 0 0 var(--space-3)` zeroes the browser's default top margin and
  keeps only a bottom margin, a "spacing flows downward" convention that
  avoids adjacent-margin surprises.
- `letter-spacing: -0.015em`: large display type looks better slightly
  tightened; in `em` so the tracking scales with the heading size.

### Paragraphs

```css
p {
  margin: 0 0 var(--space-3);
}
```

- Same downward-only margin policy as headings, using the same spacing
  token, so headings and paragraphs share one vertical rhythm.

### Links

```css
a {
  color: var(--color-link);
  text-underline-offset: 0.2em;
}

a:hover {
  text-decoration-thickness: 2px;
}
```

- `--color-link` is a dedicated theme token (distinct from
  `--color-primary`), guaranteed by both themes to pass the 4.5:1 contrast
  ratio on the page background.
- The default underline is **kept** (WCAG 1.4.1: color must not be the
  only cue that something is a link); `text-underline-offset: 0.2em`
  only pushes it below the descenders for legibility, scaling with the
  text (`em`).
- On hover the underline gets thicker instead of appearing/disappearing:
  a state change with no layout shift. `a:hover` is `(0,1,1)` vs `a` at
  `(0,0,1)`, so it wins on specificity, not just order.

### Images

```css
img {
  max-width: 100%;
}
```

- The standard fluid-image rule: an image can never overflow its
  container, mandatory for responsive layouts. `max-width` (not `width`)
  so small images keep their natural size.

### Inline code

```css
code {
  font-family: var(--font-mono);
  font-size: 0.9em;
}
```

- Switches to the mono stack. `0.9em` (element-relative, see the primer)
  compensates for monospace fonts looking optically larger than the
  surrounding proportional text, at every size where `code` appears.

### Global focus indicator

```css
/* Visible focus, never removed (WCAG 2.4.7) */
:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
  border-radius: var(--radius-sm);
}
```

- WCAG 2.4.7 requires a visible keyboard focus indicator. This rule styles
  it instead of the anti-pattern `outline: none`.
- `:focus-visible` (not `:focus`) lets the browser show the ring for
  keyboard/assistive navigation but skip it after a mouse click, removing
  the historical motivation for deleting outlines.
- `outline` (not `border`) because outlines take **no layout space**:
  focusing an element never shifts the page.
- `outline-offset: 2px` puts a gap between the element and the ring so the
  ring stays visible whatever the element's own background.
- `border-radius` rounds the ring itself (modern browsers make the outline
  follow the radius).
- `--color-focus` is a theme token chosen to meet the WCAG 3:1 contrast
  requirement for UI components.
- Specificity `(0,1,0)`. Keep this number in mind: the form input defines
  its own `.form__input:focus-visible` at `(0,2,0)` which overrides this
  rule (covered below).

### Reduced motion kill switch

```css
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation: none !important;
    transition: none !important;
  }
}
```

- `prefers-reduced-motion: reduce` is set by users whose OS accessibility
  settings ask for less motion (vestibular disorders, distraction).
- The selector is the weakest possible, `(0,0,0)`, so it could never win
  against `.reveal { animation: ... }` at `(0,1,0)` on its own. That is
  why **`!important` is legitimate here**: it is a user-preference
  override that must beat every animation and transition in the file
  (`.skip-link`'s `transition: top`, `.button`'s transform transition,
  the `.reveal` animation) and any future ones, without having to know
  about them. This is the textbook justified use of `!important`.
- Because `.reveal` uses `animation: rise-in 500ms ease both` (the `both`
  fill mode applies the `to` keyframe), cancelling the animation leaves
  elements in their normal, fully visible state: content is never lost,
  only the movement.

---

## Section: Utilities

### `.visually-hidden`

```css
/* Visually hidden but available to assistive technology */
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip-path: inset(50%);
  white-space: nowrap;
  border: 0;
}
```

- The standard "screen-reader only" utility. `display: none` or
  `visibility: hidden` would hide content from assistive technology too;
  this recipe hides it *visually only*.
- Each declaration plays a role in the trick:
  - `position: absolute` removes it from the flow (no layout gap);
  - `width/height: 1px` shrinks it to one pixel (a 0x0 box may be skipped
    by some screen readers, 1x1 is the safe historical choice);
  - `clip-path: inset(50%)` clips even that pixel invisible (modern
    replacement for the deprecated `clip: rect(...)`);
  - `overflow: hidden` prevents content spilling out of the 1 px box;
  - `padding: 0; border: 0` remove anything that would enlarge the box;
  - `margin: -1px` pulls the 1 px box out of any layout interaction;
  - `white-space: nowrap` prevents word-per-line wrapping, which would
    make some screen readers announce words with pauses between them.
- Used in the mockups for labels and headings that sighted users infer
  from layout but screen-reader users need announced.

---

## Section: Skip link

### `.skip-link` and its focus state

```css
.skip-link {
  position: absolute;
  left: var(--space-3);
  top: -3rem;
  z-index: 10;
  padding: var(--space-2) var(--space-3);
  background: var(--color-primary);
  color: var(--color-on-primary);
  border-radius: 0 0 var(--radius) var(--radius);
  text-decoration: none;
  transition: top 150ms ease;
}

.skip-link:focus-visible {
  top: 0;
}
```

- A skip link is the first focusable element of the page; it jumps
  keyboard users straight to `<main>` (WCAG 2.4.1, bypass blocks) so they
  need not tab through the whole navigation on every page.
- The hiding technique is the interesting part: the link is positioned
  **absolutely** and parked at `top: -3rem`, i.e. fully *above* the
  viewport. Unlike `display: none`, an off-screen element **stays
  focusable**, which is the whole point.
- Declaration by declaration:
  - `position: absolute` takes it out of the flow so it never leaves a
    gap at the top of the page;
  - `left: var(--space-3)` aligns it near the top-left corner, where
    keyboard users expect it;
  - `top: -3rem` hides it (the value just needs to exceed the link's own
    height; in `rem` so it scales with the user's font size and can never
    half-show a taller-than-expected link);
  - `z-index: 10` keeps it above the header when it slides in;
  - `padding` gives it a comfortable touch/click target;
  - `background: var(--color-primary)` with `color: var(--color-on-primary)`
    is the themed high-contrast pair reserved for primary surfaces;
  - `border-radius: 0 0 var(--radius) var(--radius)` rounds **only the
    bottom corners** (the four-value corner order is top-left, top-right,
    bottom-right, bottom-left), so the link looks like a tab sliding down
    from the viewport edge;
  - `text-decoration: none` drops the underline: here the button-like
    styling carries the affordance;
  - `transition: top 150ms ease` animates the slide (and is neutralized
    by the reduced-motion block above, where the link then appears
    instantly).
- **Interaction**: `.skip-link:focus-visible { top: 0 }` has specificity
  `(0,2,0)` (one class + one pseudo-class), beating `.skip-link` at
  `(0,1,0)`. When the link receives keyboard focus, only `top` changes,
  and the transition declared on the *base* rule animates it in. Putting
  the transition on the base rule (not the focus rule) means it also
  animates back out on blur.
- The global `:focus-visible` outline still applies on top of this rule
  (different properties, no conflict): the skip link gets both the slide
  and the focus ring.

---

## Section: Site header

### `.site-header`

```css
.site-header {
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface);
}
```

- The `<header>` landmark's skin. `--color-surface` is the theme's "one
  step off the background" tone; the 1 px `--color-border` bottom edge
  separates it from the page without a heavy shadow.
- Note what is *absent*: no width or padding. The header background spans
  the full viewport; the content is constrained by the inner wrapper
  below. This two-element pattern (full-bleed background, capped content)
  is used identically by the footer.

### `.site-header__inner`: the flex header

```css
.site-header__inner {
  max-width: var(--content-width);
  margin: 0 auto;
  padding: var(--space-3) var(--space-4);
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex-wrap: wrap;
}
```

- **`max-width` + `margin: 0 auto`** is the canonical centering idiom for
  block content: the box is capped at `--content-width` (64rem) and the
  two `auto` horizontal margins split the leftover space equally, so the
  content column is centered at any viewport wider than the cap, and
  simply full-width (minus padding) below it. No media query needed.
- `padding: var(--space-3) var(--space-4)` (1rem vertical, 1.5rem
  horizontal): the horizontal padding is what keeps content off the
  screen edge on small viewports.
- `display: flex` lays out brand and nav on one row; `align-items:
  center` aligns them on the cross axis regardless of their different
  heights; `gap: var(--space-4)` spaces them without margins.
- `flex-wrap: wrap` is the cheap responsive strategy: on a narrow screen
  the nav wraps to a second line instead of overflowing. Combined with
  the `margin-left: auto` trick below, this gives a usable mobile header
  with zero media queries.

### `.site-header__brand` and `.site-header__brand-mark`

```css
.site-header__brand {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: var(--font-size-lg);
  color: var(--color-text);
  text-decoration: none;
}

.site-header__brand-mark {
  color: var(--color-primary);
}
```

- The brand is an `<a>` to the home page, styled as a wordmark: display
  font, bold, slightly larger, and crucially `color: var(--color-text)`
  which **overrides the `a { color: var(--color-link) }` element rule**:
  `(0,1,0)` class beats `(0,0,1)` element, whatever the source order.
  `text-decoration: none` drops the underline (acceptable because a
  masthead brand is a well-understood convention).
- `__brand-mark` is a `<span>` wrapping part of the logo text and painting
  it in the primary color: a one-token accent that changes with the theme.

### `.site-header__nav`: the `margin-left: auto` idiom

```css
.site-header__nav {
  margin-left: auto;
}
```

- The deep idiom of this header. In a flex container, an **`auto` margin
  absorbs all the free space on that side** before `justify-content` even
  gets a say. Putting `margin-left: auto` on the nav pushes it to the
  right edge while the brand stays left: the classic
  "logo left, nav right" layout in one declaration, with no extra wrapper
  and no `justify-content: space-between` (which would also spread any
  future third child in ways you might not want).
- When `flex-wrap` moves the nav to its own line, the same `auto` margin
  then right-aligns it on that line, still sensible.

### `.nav__list`

```css
.nav__list {
  list-style: none;
  display: flex;
  gap: var(--space-3);
  margin: 0;
  padding: 0;
  align-items: center;
}
```

- The nav is a real `<ul>` (screen readers announce "list, N items",
  useful information), visually flattened: `list-style: none` removes
  bullets, `margin: 0; padding: 0` remove the default indentation.
- `display: flex` + `gap` lays the items horizontally with even spacing;
  `align-items: center` keeps links and the nav button (if any) on one
  midline.
- Note `nav` is its own BEM block (`.nav__list`, `.nav__link`), not an
  element of `site-header`: it could be reused elsewhere (a footer nav)
  without renaming.

### `.nav__link`, hover, and current page

```css
.nav__link {
  text-decoration: none;
  color: var(--color-text);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
}

.nav__link:hover {
  color: var(--color-primary);
  text-decoration: underline;
}

.nav__link[aria-current="page"] {
  font-weight: 700;
  color: var(--color-primary);
  box-shadow: inset 0 -2px 0 var(--color-primary);
}
```

- Base state: text-colored (overrides the `a` element rule again,
  `(0,1,0)` vs `(0,0,1)`), no underline; the padding enlarges the
  clickable/tappable target beyond the text itself, and the small radius
  shapes the focus outline around that padded box.
- Hover restores an underline *and* recolors: two cues, not just color.
  `.nav__link:hover` is `(0,2,0)`, beating the base rule.
- The current page is marked in the HTML with `aria-current="page"`, the
  correct accessible way to say "you are here" (screen readers announce
  it). The CSS **selects on the ARIA attribute itself**,
  `.nav__link[aria-current="page"]` at `(0,2,0)`: styling and semantics
  cannot drift apart, because the style only exists if the attribute is
  set.
- The "underline" for the current page is `box-shadow: inset 0 -2px 0`,
  an inset shadow with no blur, offset 2 px upward from the bottom edge:
  a 2 px bar inside the padded box. Compared to `border-bottom` it takes
  no layout space, and compared to `text-decoration` it spans the full
  padded width and stays clear of descenders.
- Since `:hover` and `[aria-current="page"]` rules tie at `(0,2,0)`,
  hovering the current link applies **both**; the identical
  `color: var(--color-primary)` makes any override moot, and the two
  effects (underline + bar) simply combine.

---

## Section: Layout

### `.site-main` and `.site-main--narrow`

```css
.site-main {
  max-width: var(--content-width);
  margin: 0 auto;
  padding: var(--space-5) var(--space-4) var(--space-6);
}

.site-main--narrow {
  max-width: var(--form-width);
}
```

- The `<main>` landmark gets the same `max-width` + `margin: 0 auto`
  centering idiom as the header inner (see above). Three-value padding:
  2rem top, 1.5rem sides, 3rem bottom (more air before the footer).
- `--narrow` is a BEM modifier used on the login/register pages: it only
  swaps the cap from 64rem to `--form-width` (26rem). Both classes sit on
  the same element (`class="site-main site-main--narrow"`); both rules are
  `(0,1,0)`, so the modifier wins `max-width` purely by **source order**,
  and inherits everything else from the base rule. This is the same
  pattern as `.button--primary` below.

### `.site-footer` and `.site-footer__inner`

```css
.site-footer {
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.site-footer__inner {
  max-width: var(--content-width);
  margin: 0 auto;
  padding: var(--space-4);
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
  justify-content: space-between;
}
```

- Mirror of the header: full-bleed surface with a top border, capped and
  centered inner wrapper. Footer text is demoted to muted color and small
  size (it is metadata, not content); both themes still guarantee the
  muted color passes 4.5:1.
- The inner flex row uses `justify-content: space-between` (copyright
  left, links right) rather than the `margin-left: auto` trick: with
  exactly two children the two approaches are equivalent, and
  `space-between` reads naturally here. `flex-wrap: wrap` again handles
  narrow screens by stacking.

---

## Section: Buttons

### `.button` (base block)

```css
.button {
  display: inline-block;
  font-family: var(--font-display);
  font-size: var(--font-size-base);
  font-weight: 600;
  padding: 0.65em 1.4em;
  border-radius: var(--radius);
  border: 1px solid transparent;
  cursor: pointer;
  text-decoration: none;
  transition: transform 120ms ease, box-shadow 120ms ease;
}
```

- One class shared by `<button>` elements and link-buttons (`<a
  class="button ...">`), hence declarations that only matter for one of
  the two: `text-decoration: none` neutralizes the link underline,
  `display: inline-block`, `font-family`/`font-size` and `cursor: pointer`
  normalize the `<a>` against the `<button>` (buttons do not inherit page
  fonts by default; links do not get the pointer cursor by default).
- `padding: 0.65em 1.4em` in **`em`**: the hit area scales with the
  button's own font size, so a future large CTA stays proportioned
  (see the em/rem primer).
- `border: 1px solid transparent` is a subtle trick: the base button
  reserves the border's 1 px of space even when invisible, so
  `.button--ghost`, which colors the border, is **exactly the same size**
  as `.button--primary`. Without it, adding a border in the modifier
  would make ghost buttons 2 px larger and misalign rows of buttons.
- The `transition` names exactly the two properties the hover state
  changes (transform, box-shadow); transitioning `all` would be wasteful
  and can animate surprises.

### `.button:hover`

```css
.button:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow);
}
```

- The hover feedback: a 1 px lift plus the token shadow, reading as the
  button rising toward the cursor. `transform` and `box-shadow` are both
  **compositor-friendly** properties: animating them does not trigger
  layout or repaint of surrounding content (animating `margin-top`
  instead would reflow the page).
- `(0,2,0)`, wins over any single-class rule. Disabled by the
  reduced-motion block only in its *transition* (the state still changes,
  just instantly).

### `.button--primary`

```css
.button--primary {
  background: var(--color-primary);
  color: var(--color-on-primary);
}
```

- The filled call-to-action variant. `--color-on-primary` exists as its
  own token because "text on a primary surface" needs its own contrast
  guarantee (both themes document ratios above 4.5:1 for this pair).
- **Source-order dependency, worth memorizing**: `.button` and
  `.button--primary` are both `(0,1,0)`. On an element carrying both
  classes, the modifier's `background` and `color` win **only because
  this rule appears later in the file**. If someone reordered the file,
  primary buttons would silently lose their fill. This is the BEM
  convention's one discipline: base block first, modifiers after.

### `.button--ghost` and its hover

```css
.button--ghost {
  background: transparent;
  color: var(--color-text);
  border-color: var(--color-border);
}

.button--ghost:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
```

- The secondary, outlined variant: transparent fill, text-colored label,
  and it colors the border that `.button` already reserved as transparent
  (only `border-color` needs setting, width and style are inherited from
  the base shorthand).
- `.button--ghost:hover` at `(0,2,0)` recolors border and label; it
  **combines** with `.button:hover` (also `(0,2,0)`, different
  properties): a hovered ghost button lifts, casts a shadow, *and* turns
  primary-colored. Same-specificity rules only compete on the properties
  they share.

---

## Section: Hero (home)

### `.hero`: the `fr` grid

```css
.hero {
  display: grid;
  gap: var(--space-5);
  align-items: center;
  grid-template-columns: 1.1fr 0.9fr;
  padding: var(--space-6) 0;
}

@media (max-width: 46rem) {
  .hero {
    grid-template-columns: 1fr;
  }
}
```

- A two-column grid: text on the left, decorative code card on the right.
- **The `fr` unit** distributes the container's *free* space by ratio.
  `1.1fr 0.9fr` gives the text column 1.1 / (1.1 + 0.9) = 55% and the
  card 45%. Writing it as `1.1fr 0.9fr` rather than `55% 45%` matters:
  `fr` shares the space **left after the `gap` is subtracted**, whereas
  percentages ignore the gap and would overflow by exactly the gap width.
  The slight asymmetry gives the headline room to breathe while keeping
  the card prominent.
- `align-items: center` vertically centers the shorter column against the
  taller one; `padding: var(--space-6) 0` adds generous vertical air
  (horizontal padding is already provided by `.site-main`).
- **The 46rem breakpoint**: below roughly 736 px the grid collapses to a
  single `1fr` column (text above card). Two things are worth defending
  in an exam:
  - the breakpoint is in **`rem`**, so a user with a larger default font
    size hits the single-column layout sooner, exactly when the
    two-column layout would actually run out of room for their bigger
    text. A `px` breakpoint ignores that user setting;
  - the value is **content-driven** (the point where two columns get
    cramped), not a device name ("tablet"). Media queries at `(0,1,0)`
    do not add specificity; this override wins because the `@media` block
    appears **after** the base `.hero` rule, same specificity, later
    source order.

### `.hero__title`, `.hero__title-accent`, `.hero__lead`, `.hero__actions`

```css
.hero__title {
  font-size: var(--font-size-3xl);
}

.hero__title-accent {
  color: var(--color-primary);
}

.hero__lead {
  font-size: var(--font-size-lg);
  color: var(--color-text-muted);
  max-width: 34rem;
}

.hero__actions {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
  margin-top: var(--space-4);
}
```

- `__title` just picks the largest step of the type scale; family,
  tightened line height and letter spacing come from the `h1` element
  rule (the class rule and element rule style **different properties**,
  so they compose rather than compete).
- `__title-accent` is a `<span>` painting key words in the theme primary,
  same one-token accent technique as the brand mark.
- `__lead` is the standfirst: one step up from body size, muted color,
  and `max-width: 34rem` caps the line length at roughly 65 characters,
  the classic readability measure, independent of the grid column width.
- `__actions` is a small flex row for the two CTA buttons: token gap
  instead of margins between buttons, and `flex-wrap` so the pair stacks
  on very narrow screens instead of overflowing.

### `.code-card`: the decorative signature element

```css
/* Decorative code card: signature element, hidden from AT */
.code-card {
  background: var(--code-bg);
  color: var(--code-text);
  font-family: var(--font-mono);
  font-size: var(--font-size-sm);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  box-shadow: var(--shadow);
  rotate: 1.5deg;
  position: relative;
}
```

- A fake code-editor window in the hero. In the HTML it carries
  `aria-hidden="true"`: it is pure eye candy, so screen readers skip it
  entirely; that is also why its colors are allowed to come from the
  decorative `--code-*` token family, which carries **no contrast
  guarantee**.
- `--code-bg`/`--code-text` keep an editor-dark look in both themes and
  both color schemes; mono font and small size complete the illusion.
- `rotate: 1.5deg` is the modern **individual transform property**
  (equivalent to `transform: rotate(1.5deg)` but composable without
  overwriting other transforms): the slight tilt makes the card read as a
  casually placed object.
- `position: relative` is not decoration: it establishes the **containing
  block** for the absolutely positioned `::before` traffic lights that
  follow. Without it, the dots would position themselves against the page.

### `.code-card::before`: traffic lights with one box-shadow

```css
.code-card::before {
  content: "";
  position: absolute;
  inset: var(--space-3) auto auto var(--space-4);
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--color-error);
  box-shadow: 18px 0 0 var(--color-warning), 36px 0 0 var(--color-success);
}
```

- The macOS window "traffic lights" (red, yellow, green), drawn **without
  any HTML**: perfect pseudo-element material, since they are pure
  decoration.
- `content: ""` is mandatory or the pseudo-element does not render at all.
- `inset: var(--space-3) auto auto var(--space-4)` is the four-value
  shorthand for `top right bottom left`: top 1rem, left 1.5rem, the two
  `auto` values leaving right/bottom unconstrained. One line instead of
  four properties.
- A 10 px square with `border-radius: 50%` becomes the first dot, filled
  with `--color-error` (red).
- **The trick**: the other two dots are *box shadows* of the first.
  A box shadow with zero blur and zero spread (`18px 0 0`) is an exact,
  sharp **copy of the element's shape**, offset 18 px right; the second
  copy sits at 36 px. Each shadow takes its own color: yellow
  (`--color-warning`) then green (`--color-success`). Three dots, one
  element, zero extra markup. (10 px dot + 8 px gap = the 18 px step.)
- These are the semantic status colors used decoratively; harmless, since
  the whole card is aria-hidden.

### `.code-card__code` and the syntax color spans

```css
.code-card__code {
  display: block;
  margin-top: var(--space-4);
  white-space: pre;
  overflow-x: auto;
}

.code-card__keyword { color: var(--code-keyword); }
.code-card__string  { color: var(--code-string); }
.code-card__function { color: var(--code-function); }
.code-card__comment { color: var(--code-comment); }
```

- The `<code>` element is inline by default; `display: block` lets it
  take margins and scroll. `margin-top` clears the traffic-light row.
- `white-space: pre` preserves the code's indentation and line breaks
  exactly as written in the HTML (the code is *not* wrapped in `<pre>`,
  so CSS restores that behavior).
- `overflow-x: auto` adds a horizontal scrollbar only if a long line
  overflows, instead of breaking the layout: the standard guard for
  preformatted content on small screens.
- The four one-liner classes are hand-rolled syntax highlighting: `<span>`
  elements in the markup pick keyword/string/function/comment colors from
  the theme's decorative token family. Single-declaration rules formatted
  on one line, an accepted readability exception.

---

## Section: Feature cards (home)

### `.features`: the auto-fit grid

```css
.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(15rem, 1fr));
  gap: var(--space-4);
  padding: 0;
  margin: var(--space-5) 0 0;
  list-style: none;
}
```

- The signature **responsive grid without media queries**, worth
  unpacking term by term:
  - `minmax(15rem, 1fr)`: each column may shrink to 15rem (240 px) but
    no further, and may grow to an equal share (`1fr`) of the row;
  - `auto-fit`: the browser creates **as many columns as fit** at their
    minimum width, then collapses the empty tracks and lets the real
    columns stretch to fill the row.
  - Net effect: 3 columns on a desktop, 2 on a small laptop, 1 on a
    phone, each transition happening exactly when the cards would drop
    below a readable 15rem, with zero breakpoints to maintain.
  - (The sibling keyword `auto-fill` would keep the empty tracks, leaving
    cards at minimum width with a gap on the right; `auto-fit` is the one
    you want when the *content* should fill the row.)
  - The minimum is in `rem` for the same accessibility reason as the hero
    breakpoint: bigger user font size means wider minimum, means fewer
    columns, exactly when needed.
- The container is a `<ul>` (a list of features, announced as such), so
  `list-style: none`, `padding: 0` and a top-only margin flatten the
  default list styling, as with `.nav__list`.

### `.feature-card` and its elements

```css
.feature-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.feature-card__icon {
  font-size: var(--font-size-2xl);
  display: block;
  margin-bottom: var(--space-2);
}

.feature-card__title {
  font-size: var(--font-size-lg);
  margin-bottom: var(--space-2);
}

.feature-card__text {
  color: var(--color-text-muted);
  margin: 0;
}
```

- `.feature-card` is the standard card recipe of this design system,
  reused by `.stat-card` and `.course-card`: surface background, 1 px
  border for definition in both light and dark schemes (where shadows
  read poorly), large radius, token padding. Borders instead of shadows
  for resting cards is deliberate; the shadow token is reserved for
  elevation moments (hover, form card, code card).
- `__icon` is an emoji `<span>` (with `aria-hidden` in the markup):
  `display: block` puts it on its own line and lets `margin-bottom` apply
  (vertical margins do not affect inline elements).
- `__title` overrides only the size of the `h3` element rule; margin
  bottom is tightened from the heading default (`--space-3`) to
  `--space-2` for a compact card. `(0,1,0)` class beats `(0,0,1)`
  element for the properties they share.
- `__text` is muted (supporting copy) and drops the paragraph's default
  bottom margin so the card's own padding defines the bottom edge.

---

## Section: Forms

### `.form-card` and `.form-card__title`

```css
.form-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  box-shadow: var(--shadow);
}

.form-card__title {
  font-size: var(--font-size-2xl);
  margin-bottom: var(--space-4);
}
```

- The card around the login/register form: same recipe as
  `.feature-card`, but with more padding and the token shadow. The shadow
  singles out the form as *the* interactive object of an auth page (it
  sits inside `.site-main--narrow`, so it is already a narrow centered
  column).
- The title tunes the `h1` element rule: size from the scale, margin
  opened to `--space-4` before the first field.

### `.form__group`, `.form__label`, `.form__hint`

```css
.form__group {
  margin-bottom: var(--space-4);
}

.form__label {
  display: block;
  font-weight: 700;
  margin-bottom: var(--space-1);
}

.form__hint {
  display: block;
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
  margin-bottom: var(--space-1);
}
```

- `__group` is the wrapper for one label + hint + input + error unit;
  its only job is the vertical rhythm between fields.
- `__label`: `<label>` is inline by default; `display: block` stacks it
  above its input (the accessible pattern, always visible, never a
  placeholder-as-label) with a tight margin. Bold, full-contrast text:
  labels are primary content.
- `__hint` is the format hint ("at least 12 characters..."), a block line
  between label and input: small, muted. In the markup it carries an `id`
  referenced by the input's `aria-describedby`, so screen readers read
  the hint with the field; the CSS class and the ARIA wiring work
  together but independently.

### `.form__input` and `font: inherit`

```css
.form__input {
  width: 100%;
  font: inherit;
  color: var(--color-text);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 0.6em 0.8em;
}
```

- `width: 100%` fills the form column (safe thanks to the global
  `border-box` sizing: padding and border are included in that 100%).
- **`font: inherit` is the load-bearing declaration.** Form controls are
  the one place browsers do *not* inherit typography: user-agent
  stylesheets give `<input>`, `<select>` and `<textarea>` their own font
  family and size (often 13 px). The `font` **shorthand** resets family,
  size, weight, style and line-height in one go to the surrounding page
  values, so inputs match body text. Bonus: keeping input font size at
  1rem (16 px) prevents iOS Safari's automatic zoom-on-focus, which
  triggers on inputs below 16 px.
- Ink and paper are re-asserted from tokens (`--color-text` on
  `--color-bg`): the input deliberately uses the page *background* color
  while sitting on the card's *surface* color, so the field reads as a
  recessed well. The 1 px token border keeps it visible in dark mode.
- Padding in `em` so the comfortable hit area follows the (inherited)
  font size.

### `.form__input:focus-visible` vs the global focus rule

```css
.form__input:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 1px;
  border-color: var(--color-focus);
}
```

- **Interaction to know cold.** The global `:focus-visible` rule is
  `(0,1,0)`; this rule is `(0,2,0)` (class + pseudo-class) and overrides
  it for the properties both declare, on inputs only. It re-declares the
  same 2 px outline but tightens `outline-offset` from 2 px to 1 px (a
  floating ring 2 px away from a bordered box looks detached) and
  *additionally* recolors the input's own border to the focus color, so
  the focused field reads as a single strong ring rather than a border
  plus a distant halo.
- A subtlety on `border-radius`: the global focus rule sets
  `border-radius: var(--radius-sm)` and this input rule does not
  re-declare it, yet the focused input keeps its 8 px radius. Why: the
  base `.form__input` rule sets `border-radius: var(--radius)` at the
  same `(0,1,0)` specificity as the global `:focus-visible` rule, and
  `.form__input` comes **later in the file**, so it wins that one
  property on the source-order tiebreak. The exam-worthy lesson:
  overrides happen **per property**, never per rule.

### `.form__input--invalid` and `.form__error`

```css
.form__input--invalid {
  border-color: var(--color-error);
}

.form__error {
  display: block;
  color: var(--color-error);
  font-size: var(--font-size-sm);
  font-weight: 700;
  margin-top: var(--space-1);
}
```

- The invalid modifier recolors only the border. Same `(0,1,0)` as
  `.form__input`, later in source, so it wins `border-color`; and when
  the invalid field is focused, `.form__input:focus-visible` at
  `(0,2,0)` wins the border back to the focus color, an acceptable and
  even desirable behavior (while you are fixing the field, the focus
  state dominates).
- Color is never the only error signal (WCAG 1.4.1): the modifier is
  paired in the markup with `aria-invalid="true"` and with a visible
  `.form__error` message whose `id` is referenced by the input's
  `aria-describedby`. The message is block-level, small but **bold** (a
  second non-color cue), error-colored (a token guaranteed to pass 4.5:1
  by both themes).

### `.form__footer`

```css
.form__footer {
  margin-top: var(--space-4);
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}
```

- The "No account yet? Register" line under the submit button: separated
  by a token margin, demoted to small muted text. The link inside it
  still gets the `a` element rule's `--color-link`, keeping it
  distinguishable and compliant.

---

## Section: Alerts

### `.alert` and its three modifiers

```css
.alert {
  border: 1px solid;
  border-radius: var(--radius);
  padding: var(--space-3) var(--space-4);
  margin-bottom: var(--space-4);
  background: var(--color-surface);
}

.alert--success {
  color: var(--color-success);
  border-color: var(--color-success);
}

.alert--error {
  color: var(--color-error);
  border-color: var(--color-error);
}

.alert--info {
  color: var(--color-link);
  border-color: var(--color-link);
}
```

- The flash-message component (used with `role="alert"` or `role="status"`
  in the markup so screen readers announce it on arrival).
- The base rule declares `border: 1px solid` **without a color**, on
  purpose: a border with no explicit color defaults to `currentColor`,
  the element's text color. The modifiers each set `color` and
  `border-color` together anyway, but the colorless shorthand means even
  a modifier that only set `color` would get a matching border for free;
  it also documents the intent (border always matches ink).
- Each modifier maps one semantic state to one theme token pair, text and
  border in the same status color on the neutral surface background. All
  three status tokens are contrast-checked at 4.5:1 by the themes, so
  colored *text* (not just a colored stripe) stays legible.
- `--info` reuses `--color-link` rather than introducing a fourth status
  token: informational blue and link blue are the same voice in this
  design, one less token to keep compliant.
- Note there is no `.alert--warning` in the mockups; `--color-warning`
  exists as a token but is consumed only by the code card's traffic
  lights (see the theme documents).
- Modifier-after-base source order again: same `(0,1,0)` specificity,
  later rule wins the shared properties.

---

## Section: Dashboard

### `.page-title`

```css
.page-title {
  font-size: var(--font-size-2xl);
}
```

- Dashboard pages use a quieter `h1` than the hero: one class overriding
  only the size (the `h1` element rule supplies family, leading,
  tracking, margin). Note the page hierarchy is still `h1`; only the
  *visual* scale changes, semantics never bend to style.

### `.stats`

```css
.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(11rem, 1fr));
  gap: var(--space-4);
  margin: var(--space-4) 0 var(--space-5);
  padding: 0;
  list-style: none;
}
```

- Same `repeat(auto-fit, minmax(..., 1fr))` idiom as `.features` (see
  there for the full unpacking), with a smaller 11rem minimum: stat cards
  hold a number and a label, so they can pack four across where feature
  cards fit three.
- Again a flattened `<ul>`; three-value margin gives space above and
  below the row (`--space-4` top, `--space-5` bottom, 0 sides).

### `.stat-card`, `.stat-card__value`, `.stat-card__label`

```css
.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.stat-card__value {
  display: block;
  font-family: var(--font-display);
  font-size: var(--font-size-3xl);
  font-weight: 700;
  color: var(--color-primary);
}

.stat-card__label {
  color: var(--color-text-muted);
}
```

- The card shell is the same recipe as `.feature-card` (surface, border,
  large radius, padding).
- `__value` is the big KPI number: display font at the largest scale
  step, bold, primary-colored, and `display: block` so it stacks above
  its label (the markup keeps the number first, label second, matching
  reading order for screen readers).
- `__label` is the muted caption under it.

### `.course-list` and `.course-card`

```css
.course-list {
  list-style: none;
  margin: var(--space-4) 0 0;
  padding: 0;
  display: grid;
  gap: var(--space-3);
}

.course-card {
  display: grid;
  gap: var(--space-2);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}
```

- `.course-list` is a flattened `<ul>` again, but a **single-column**
  grid: `display: grid` with no `grid-template-columns` defaults to one
  column, used purely for its `gap` (uniform spacing between cards with
  no first/last margin exceptions). A common modern idiom: grid as a
  vertical stack with gap.
- `.course-card` does the same *inside* the card: title, meta line and
  progress bar stack with a tight uniform `--space-2` gap, which is why
  its child elements can zero their own margins. Shell styling is the
  standard card recipe.

### `.course-card__title`, `.course-card__meta`, `.course-card__progress`

```css
.course-card__title {
  font-size: var(--font-size-lg);
  margin: 0;
}

.course-card__meta {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  margin: 0;
}

.course-card__progress {
  accent-color: var(--color-primary);
  width: 100%;
  height: 0.6rem;
}
```

- Title and meta zero their element-rule margins (`h3` and `p` defaults
  from the top of the file) because the card's grid `gap` now owns the
  spacing: one source of truth instead of margin arithmetic.
- `__progress` styles a native `<progress>` element (which carries its
  own accessible semantics: role, value, max, announced by screen
  readers, no ARIA needed):
  - **`accent-color`** is the modern one-line way to theme native widgets
    (progress bars, checkboxes, radios, range sliders) without the old
    forest of `::-webkit-progress-*` pseudo-elements; the browser keeps
    rendering the control natively but fills it with the primary token;
  - `width: 100%` stretches it to the card; `height: 0.6rem` slims the
    default bar to an elegant strip, in `rem` so it scales with user
    font size.
  - Trade-off acknowledged: `accent-color` themes the *fill* but leaves
    the track color to the browser; full track control would require the
    vendor pseudo-elements this file deliberately avoids.

---

## Section: Page-load reveal

### `@keyframes rise-in` and `.reveal`

```css
@keyframes rise-in {
  from {
    opacity: 0;
    translate: 0 10px;
  }
  to {
    opacity: 1;
    translate: 0 0;
  }
}

.reveal {
  animation: rise-in 500ms ease both;
}

.reveal--2 { animation-delay: 100ms; }
.reveal--3 { animation-delay: 200ms; }
.reveal--4 { animation-delay: 300ms; }
```

- A CSS-only entrance: elements fade in while drifting up 10 px.
- `@keyframes` defines the named animation; `from`/`to` are aliases for
  `0%`/`100%`. It uses `translate` (the individual transform property,
  like `rotate` on the code card) and `opacity`: **both
  compositor-animatable**, so the animation never causes layout work.
- `.reveal` applies it via the `animation` shorthand: name, 500ms
  duration, `ease` timing, and the crucial **`both` fill mode**:
  - *backwards* fill applies the `from` state during any
    `animation-delay` (without it, a delayed element would flash fully
    visible, then jump to invisible when its animation starts);
  - *forwards* fill holds the `to` state after finishing.
  - `both` = the two combined. With staggered delays, `both` (or at least
    `backwards`) is not optional polish, it is what prevents the flash.
- **Staggering via `animation-delay`**: the modifiers `--2`, `--3`, `--4`
  reuse the *same* keyframes and only offset the start by 100ms steps.
  In the markup, successive blocks get `reveal`, `reveal reveal--2`,
  `reveal reveal--3`..., producing a cascade where each section follows
  the previous one. One animation definition, four entrance timings:
  cheap to maintain, and the 100ms step is short enough that the page
  feels alive, not slow.
- Modifier-after-base order matters once more (all `(0,1,0)`), though
  here they set a property the base rule's shorthand only resets
  implicitly: the `animation` shorthand sets `animation-delay: 0s`, so
  the modifiers **must** come after `.reveal` or the shorthand would wipe
  their delay.
- **Reduced motion**: the media block at the top of the file kills this
  animation with `animation: none !important`. Thanks to fill mode
  semantics, the elements then simply sit in their natural, fully
  visible state: motion is progressive enhancement here, never a
  gatekeeper to content.

---

## Recap: rule interactions worth remembering

| Interaction | Winner | Mechanism |
|---|---|---|
| `.button` vs `.button--primary` (background, color) | `.button--primary` | Tie at (0,1,0); modifier written later, source order wins |
| `.site-main` vs `.site-main--narrow` (max-width) | `.site-main--narrow` | Same tie-plus-source-order pattern |
| `.alert` vs `.alert--success/--error/--info` (color, border-color) | The modifier | Same pattern; base border falls back to `currentColor` |
| `.reveal` vs `.reveal--2/3/4` (animation-delay) | The modifier | Same pattern; the `animation` shorthand resets delay to 0s, so modifiers must follow it |
| `a { color }` vs `.site-header__brand`, `.nav__link` | The class | (0,1,0) beats (0,0,1), regardless of order |
| `h1/h3` element rules vs `.hero__title`, `.feature-card__title`, `.page-title` | Compose | Different properties mostly; where shared (font-size, margin), the class wins on specificity |
| `.skip-link` vs `.skip-link:focus-visible` (top) | The focus rule | (0,2,0) beats (0,1,0); transition on the base rule animates both directions |
| Global `:focus-visible` vs `.form__input:focus-visible` | The input rule | (0,2,0) beats (0,1,0), but only for the properties it declares; overrides are per property |
| `.nav__link` vs `:hover` vs `[aria-current="page"]` | Both pseudo/attribute rules | Each (0,2,0) beats the base; against each other they tie and merge (identical color) |
| `.form__input--invalid` vs `.form__input:focus-visible` (border-color) | The focus rule | (0,2,0) beats (0,1,0): while focused, focus color wins over error color |
| `.hero` base vs its `@media (max-width: 46rem)` override | The media rule | Media queries add no specificity; the block is later in source |
| Everything animated vs `prefers-reduced-motion` block | The media block | `(0,0,0)` selector but `!important`: user preference outranks all authors' animations |

If one meta-lesson survives the exam: in a BEM stylesheet almost
everything is `(0,1,0)`, so **order is architecture**. Base blocks before
modifiers, base states before pseudo-class states, and the two deliberate
escapes from that flat world are the global reset at `(0,0,0)` and the
reduced-motion `!important`.
