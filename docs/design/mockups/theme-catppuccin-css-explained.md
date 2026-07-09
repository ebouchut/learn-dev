# `theme-catppuccin.css` explained, token by token

An educational walkthrough of `css/theme-catppuccin.css`, the **default**
theme of the learn-dev mockups, written for a developer preparing the
French DWWM certification. The rule-by-rule companion for the component
styles is [base-css-explained.md](base-css-explained.md); the alternate
theme is documented in
[theme-soft-paper-css-explained.md](theme-soft-paper-css-explained.md).

Palette provenance: the official **Catppuccin** palette
(https://github.com/catppuccin/palette), **Latte** flavor for light mode
and **Mocha** flavor for dark mode, as used by the Catppuccin Obsidian
theme that inspired the direction. A few light-mode values are darkened
from upstream to pass WCAG 2.1 AA; each is flagged below with the upstream
original and both contrast ratios, as measured in
`docs/design/theme-exploration.md`.

---

## The token architecture (read this once, it applies to both themes)

This file contains **no selectors for components and no layout**. It does
exactly one thing: define a fixed vocabulary of CSS custom properties
(design tokens) that `base.css` consumes. Four mechanisms make it work:

### 1. Custom properties on `:root`

```css
:root {
  --color-bg: #eff1f5;
  /* ... */
}
```

Custom properties (`--name: value`) are declared like any CSS
declaration and read with `var(--name)`. Declared on `:root` (the `<html>`
element), they are **inherited by every element in the document**, so any
rule in `base.css`, at any depth, can resolve `var(--color-primary)`.
This is what turns a stylesheet into a *themable* system: `base.css`
states intent ("paint this button in the primary color"), the theme file
states values.

### 2. Dark mode via `@media (prefers-color-scheme: dark)`

```css
@media (prefers-color-scheme: dark) {
  :root {
    --color-bg: #1e1e2e;
    /* ... */
  }
}
```

The same token names are **redeclared** inside a media query that matches
when the user's operating system is set to dark mode. Specificity is
identical (`:root` both times), so the redeclarations win purely by
**source order** when the media query matches. No JavaScript, no toggle,
no second stylesheet: every `var()` in `base.css` transparently resolves
to the dark value. This follows the project decision (see
`docs/design/theme-exploration.md`): dark mode follows the OS preference
and is not a runtime "switcher".

### 3. The `color-scheme` property

```css
:root {
  color-scheme: light dark;
  /* ... */
}
```

Tokens only recolor what `base.css` paints. Everything the **browser**
paints on its own (form control internals, scrollbars, the default
canvas behind the page, the `<progress>` track...) follows the
`color-scheme` property instead. Declaring `light dark` tells the
browser: this page supports both schemes, render your native UI in
whichever matches the user's preference. Without it, a dark-mode user
would get glaring white scrollbars and form widgets inside an otherwise
dark page.

### 4. Same token names in both themes = one-link switching

`theme-catppuccin.css` and `theme-soft-paper.css` define **exactly the
same set of token names**. Consequently the entire visual identity is
selected by a single line in the page head:

```html
<link rel="stylesheet" href="css/theme-catppuccin.css">
```

Point that `href` at `css/theme-soft-paper.css` and every page is
re-skinned, with zero changes to `base.css` or the markup. That is the
whole contract: `base.css` owns structure and consumes the vocabulary;
each theme file owns one complete pronunciation of it.

### Naming: semantic, not descriptive

Note the token names describe **roles** (`--color-primary`,
`--color-error`, `--color-surface`), never appearances (`--purple`,
`--light-gray`). This is what allows the same name to be mauve here and
teal in Soft Paper, or light lavender in Latte and dark violet in Mocha,
without any consumer changing.

---

## The tokens, one by one

Each entry gives: the light (Latte) and dark (Mocha) values with their
upstream Catppuccin color names, what `base.css` does with the token, and
the WCAG contrast ratio where `theme-exploration.md` states one (ratios
are computed against `--color-bg` of the same mode unless said
otherwise). WCAG 2.1 AA thresholds: **4.5:1** for normal text, **3:1**
for large text and UI components.

### `--color-bg`

- Light: `#eff1f5` (Latte *base*). Dark: `#1e1e2e` (Mocha *base*).
- Consumed by `base.css` for the `body` background and, deliberately,
  the `.form__input` background, so inputs read as wells of "page paper"
  recessed into their surface-colored card.
- Backgrounds carry no ratio of their own; they are the denominator every
  text ratio below is measured against.

### `--color-surface`

- Light: `#e6e9ef` (Latte *mantle*). Dark: `#181825` (Mocha *mantle*).
- The "one step off the background" tone: `base.css` uses it for
  `.site-header`, `.site-footer`, and every card
  (`.feature-card`, `.form-card`, `.stat-card`, `.course-card`,
  `.alert`).
- In Catppuccin's own layering model, *mantle* is precisely the step
  between *base* and *crust*, so the design system's elevation maps 1:1
  onto the upstream palette's.

### `--color-surface-deep`

- Light: `#dce0e8` (Latte *crust*). Dark: `#11111b` (Mocha *crust*).
- **Currently not consumed by `base.css` at all** (verified by grepping
  the stylesheet and the mockup HTML). It completes the Catppuccin
  base/mantle/crust elevation triad and is reserved for a future deepest
  layer (e.g. a custom `<progress>` track or inset wells). Defining it
  now keeps the two theme files interchangeable when a consumer appears.
- Fun fact: the dark value `#11111b` does appear in this theme, but via
  its own tokens (`--color-on-primary` and the dark `--code-bg`), not via
  this one.

### `--color-border`

- Light: `#ccd0da` (Latte *surface0*). Dark: `#313244` (Mocha *surface0*).
- The hairline color for `.site-header` / `.site-footer` edges, all card
  borders, `.button--ghost`, and `.form__input`. `base.css` relies on
  borders rather than shadows for resting definition, so this token does
  a lot of quiet work, especially in dark mode where shadows are nearly
  invisible.
- Decorative hairlines are not "UI components" in the WCAG sense, so no
  ratio is claimed for it.

### `--color-text`

- Light: `#4c4f69` (Latte *text*), **7.06:1**. Dark: `#cdd6f4`
  (Mocha *text*), **11.34:1**.
- The main ink: `body` color, and re-asserted by `.site-header__brand`,
  `.nav__link`, `.button--ghost`, and `.form__input`.
- Both ratios clear 4.5:1 with margin; the Latte value even clears the
  stricter AAA 7:1.

### `--color-text-muted`

- Light: `#5c5f77` (Latte *subtext1*), **5.53:1**. Dark: `#bac2de`
  (Mocha *subtext1*), **9.26:1**.
- Secondary ink for supporting copy: `.site-footer`, `.hero__lead`,
  `.feature-card__text`, `.form__hint`, `.form__footer`,
  `.stat-card__label`, `.course-card__meta`.
- The point worth making in an exam: even the *muted* tone passes 4.5:1.
  "Muted" is achieved inside the compliant range, not by sacrificing
  legibility, which is the most common real-world AA failure.

### `--color-primary`

- Light: `#8839ef` (Latte *mauve*), **4.79:1**. Dark: `#cba6f7`
  (Mocha *mauve*), **8.07:1**.
- The identity color and the busiest token: `.skip-link` background,
  `.site-header__brand-mark`, `.nav__link` hover and
  `[aria-current="page"]` states, `.button--primary` background,
  `.button--ghost` hover, `.hero__title-accent`, `.stat-card__value`,
  and the `accent-color` of `.course-card__progress`.
- Mauve is Catppuccin's signature accent; it passes 4.5:1 as *text* on
  the background in both modes, which is what allows `base.css` to use it
  for colored text (stat values, nav states), not only for fills.

### `--color-on-primary`

- Light: `#ffffff`, **5.41:1 measured on `--color-primary`** (not on the
  page background). Dark: `#11111b` (Mocha *crust*), **9.23:1 on
  primary**.
- The dedicated "text sitting on a primary fill" token, consumed by
  `.button--primary` and `.skip-link`. It exists because a color pair,
  not a color, is what carries a contrast guarantee.
- Note the inversion: light mode puts white on a dark mauve; dark mode
  puts near-black on a pastel mauve. A single "white" constant would have
  failed dark mode, which is exactly why this is a token.

### `--color-link`

- Light: `#1a5cd7`, **5.22:1**, **adjusted** from Latte *blue* `#1e66f5`
  which measures only 4.34:1 on this background. Dark: `#89b4fa`
  (Mocha *blue*, unmodified), **7.79:1**.
- Consumed by the `a` element rule (all prose links) and reused by
  `.alert--info` for informational alerts.
- This is the textbook example of the theme's compliance policy:
  Catppuccin's pastel-leaning accents were designed for dark backgrounds,
  so the light-mode value is darkened just enough to cross 4.5:1 while
  staying recognizably "Catppuccin blue". The upstream original survives
  untouched as `--color-focus` (see below), where only 3:1 is required.

### `--color-success`

- Light: `#2f7a1f`, **4.73:1**, **adjusted** from Latte *green* `#40a02b`
  which measures a badly failing 2.96:1. Dark: `#a6e3a1`
  (Mocha *green*, unmodified), **11.03:1**.
- Consumed by `.alert--success` (text and border) and decoratively by the
  third traffic-light dot of `.code-card::before`.
- The largest single adjustment in the theme: upstream Latte green is far
  too bright to serve as text, and success messages are exactly the text
  users must be able to read.

### `--color-warning`

- Light: `#8f5b08`, **5.06:1**, **adjusted** from Latte *yellow* `#df8e1d`
  which measures 2.31:1, the worst offender of the upstream set. Dark:
  `#f9e2af` (Mocha *yellow*, unmodified), **12.91:1**.
- Currently consumed by `base.css` only for the middle traffic-light dot
  of `.code-card::before` (there is no `.alert--warning` in the mockups).
  It is kept compliant anyway so that a future warning alert can be added
  without touching the theme.

### `--color-error`

- Light: `#d20f39` (Latte *red*, unmodified), **4.80:1**. Dark:
  `#f38ba8` (Mocha *red*, unmodified), **7.08:1**.
- Consumed by `.form__input--invalid` (border), `.form__error` (text),
  `.alert--error` (text and border), and decoratively by the first
  traffic-light dot of `.code-card::before`.
- The one Latte accent that passes AA as-is, so it needed no adjustment:
  error text, the most safety-critical text of a form, is compliant in
  both modes.

### `--color-focus`

- Light: `#1e66f5` (Latte *blue*, unmodified). Dark: `#89b4fa`
  (Mocha *blue*).
- Consumed by the global `:focus-visible` outline and by
  `.form__input:focus-visible` (outline and border).
- Why the *unadjusted* Latte blue is fine here while `--color-link` had
  to be darkened: a focus ring is a **UI component**, held to the 3:1
  threshold (WCAG 1.4.11), not the 4.5:1 text threshold. Keeping the
  brighter upstream blue actually gives a more vivid, more findable ring.
  Two tokens, two thresholds, one hex difference: a compact illustration
  of *why* semantic tokens beat sharing one "blue".

### `--color-primary-soft`

- Light: `#eadcfd` (a light mauve wash, custom-derived, not an upstream
  named color). Dark: `#2b2440` (custom dark counterpart).
- Commented in the file as a "decorative wash behind hero art", but
  **currently not consumed by `base.css` or the mockup markup**
  (verified by grep, same as `--color-surface-deep`). It is a reserved
  slot: both theme files define it so the vocabulary stays identical the
  day a consumer lands.

### The `--code-*` family (decorative syntax colors)

The code card on the home page is `aria-hidden` eye candy, so these
tokens are exempt from contrast requirements by design; the file comment
says as much. They are Catppuccin **Mocha** values in *both* modes: the
fake editor stays dark even on the light theme, like a real code editor
in a light IDE chrome.

- `--code-bg`: light `#1e1e2e` (Mocha *base*), dark `#11111b` (Mocha
  *crust*). In dark mode the card drops one level deeper than the page
  background (which is itself Mocha base), so the card still stands out.
  Consumed by `.code-card`.
- `--code-text`: `#cdd6f4` (Mocha *text*) in both modes. Default code
  ink, consumed by `.code-card`.
- `--code-keyword`: `#cba6f7` (Mocha *mauve*), consumed by
  `.code-card__keyword`.
- `--code-string`: `#a6e3a1` (Mocha *green*), consumed by
  `.code-card__string`.
- `--code-function`: `#89b4fa` (Mocha *blue*), consumed by
  `.code-card__function`.
- `--code-comment`: `#9399b2` (Mocha *overlay2*), consumed by
  `.code-card__comment`.

---

## Summary table

"Adjusted" flags a light-mode value darkened from the upstream Catppuccin
original (shown with its failing ratio) to reach WCAG 2.1 AA. Ratios are
against the same-mode `--color-bg`, except `--color-on-primary`, measured
on `--color-primary`.

| Token | Light (Latte) | Ratio | Dark (Mocha) | Ratio | Consumed by (base.css) |
|---|---|---|---|---|---|
| `--color-bg` | `#eff1f5` base | n/a | `#1e1e2e` base | n/a | `body`, `.form__input` |
| `--color-surface` | `#e6e9ef` mantle | n/a | `#181825` mantle | n/a | header, footer, all cards, alerts |
| `--color-surface-deep` | `#dce0e8` crust | n/a | `#11111b` crust | n/a | **nothing yet** (reserved) |
| `--color-border` | `#ccd0da` surface0 | n/a | `#313244` surface0 | n/a | all hairlines and card/input borders |
| `--color-text` | `#4c4f69` text | 7.06:1 | `#cdd6f4` text | 11.34:1 | `body`, brand, nav, ghost button, inputs |
| `--color-text-muted` | `#5c5f77` subtext1 | 5.53:1 | `#bac2de` subtext1 | 9.26:1 | footer, lead, hints, captions, meta |
| `--color-primary` | `#8839ef` mauve | 4.79:1 | `#cba6f7` mauve | 8.07:1 | skip link, brand mark, nav states, primary button, accents, progress fill |
| `--color-on-primary` | `#ffffff` | 5.41:1 on primary | `#11111b` crust | 9.23:1 on primary | text of `.button--primary`, `.skip-link` |
| `--color-link` | `#1a5cd7` **adjusted** (blue `#1e66f5`, 4.34:1) | 5.22:1 | `#89b4fa` blue | 7.79:1 | `a`, `.alert--info` |
| `--color-success` | `#2f7a1f` **adjusted** (green `#40a02b`, 2.96:1) | 4.73:1 | `#a6e3a1` green | 11.03:1 | `.alert--success`, traffic light |
| `--color-warning` | `#8f5b08` **adjusted** (yellow `#df8e1d`, 2.31:1) | 5.06:1 | `#f9e2af` yellow | 12.91:1 | traffic light only (no warning alert yet) |
| `--color-error` | `#d20f39` red | 4.80:1 | `#f38ba8` red | 7.08:1 | invalid input, form errors, `.alert--error`, traffic light |
| `--color-focus` | `#1e66f5` blue | 3:1 UI req. | `#89b4fa` blue | n/a stated | focus outlines (global and input) |
| `--color-primary-soft` | `#eadcfd` custom | n/a | `#2b2440` custom | n/a | **nothing yet** (reserved) |
| `--code-bg` | `#1e1e2e` Mocha base | exempt | `#11111b` Mocha crust | exempt | `.code-card` |
| `--code-text` | `#cdd6f4` Mocha text | exempt | same | exempt | `.code-card` |
| `--code-keyword` | `#cba6f7` Mocha mauve | exempt | same | exempt | `.code-card__keyword` |
| `--code-string` | `#a6e3a1` Mocha green | exempt | same | exempt | `.code-card__string` |
| `--code-function` | `#89b4fa` Mocha blue | exempt | same | exempt | `.code-card__function` |
| `--code-comment` | `#9399b2` Mocha overlay2 | exempt | same | exempt | `.code-card__comment` |

Takeaways to be able to defend orally:

- One theme = one file = two `:root` blocks (light, then dark inside
  `@media (prefers-color-scheme: dark)`), plus `color-scheme: light dark`
  for browser-native UI.
- Compliance is engineered *into the tokens*: three light-mode accents
  (link, success, warning) are darkened from upstream because pastel
  palettes designed for dark backgrounds rarely pass 4.5:1 on light ones;
  every dark-mode value passes untouched.
- Text tokens are held to 4.5:1, the focus ring to 3:1, and the
  aria-hidden code card to nothing: the WCAG threshold that applies
  depends on the *role*, which is precisely what semantic token names
  encode.
