# `theme-soft-paper.css` explained, token by token

An educational walkthrough of `css/theme-soft-paper.css`, the **alternate**
theme of the learn-dev mockups (generated and kept in the repo, but not
linked by default), written for a developer preparing the French DWWM
certification. The component styles that consume these tokens are
documented rule by rule in [base-css-explained.md](base-css-explained.md).

## Architecture: see the Catppuccin document

This file is built on exactly the same four mechanisms as the default
theme, explained once in
[theme-catppuccin-css-explained.md](theme-catppuccin-css-explained.md):

1. design tokens as custom properties on `:root`, inherited everywhere;
2. dark values redeclared inside `@media (prefers-color-scheme: dark)`,
   winning by source order when the OS is in dark mode;
3. `color-scheme: light dark` so browser-native UI (form controls,
   scrollbars, the progress track) follows the active scheme;
4. **the same token names as `theme-catppuccin.css`**, which is the whole
   point: activating Soft Paper means changing the single `<link>` in the
   page head to point at this file. Nothing in `base.css` or the markup
   changes.

What differs is only the *pronunciation* of the vocabulary, and one small
structural choice worth noticing: in this theme `--color-link`,
`--color-primary` and `--color-focus` share **one** teal in light mode
(and one blue in dark mode), where Catppuccin uses two distinct hues
(mauve primary, blue link/focus). The tokens stay separate anyway, so the
themes remain drop-in interchangeable and either could diverge later
without touching a consumer.

## Palette provenance

- **Light mode**: the custom warm paper palette extracted from
  [nickmilo/soft-paper](https://github.com/nickmilo/soft-paper), an
  Obsidian theme. Character: calm, warm, reads like paper.
- **Dark mode**: soft-paper's dark side, which is essentially
  **Catppuccin Frappe** (an interesting upstream finding: Soft Paper is
  itself built on Catppuccin variables). Several dark values below are
  exact Frappe named colors.
- As with Catppuccin, values **adjusted** from upstream to reach the WCAG
  2.1 AA ratio of 4.5:1 on `--color-bg` are flagged with the upstream
  original and both ratios, as measured in
  `docs/design/theme-exploration.md`. Three light-mode semantic colors
  needed darkening; every dark value passes untouched.

---

## The tokens, one by one

Each entry gives the light (warm paper) and dark (Frappe) values, what
`base.css` does with the token (identical to the other theme, by
construction), the upstream origin, and the WCAG ratio where
`theme-exploration.md` states one (against the same-mode `--color-bg`
unless said otherwise).

### `--color-bg`

- Light: `#eee6dd`, the warm paper tone that gives the theme its name.
  Dark: `#303446` (exactly Catppuccin Frappe *base*).
- Consumed for the `body` background and the `.form__input` background
  (inputs as recessed wells of page paper).
- No ratio of its own; it is the reference surface for the ratios below.

### `--color-surface`

- Light: `#e6dbd1` (a slightly deeper paper). Dark: `#292c3c` (exactly
  Frappe *mantle*).
- The header, footer, and every card and alert in `base.css`. Where
  Catppuccin's light surface is a cool near-white, this one is a warm
  cardboard tone: same role, different mood.

### `--color-surface-deep`

- Light: `#ddd0c6`. Dark: `#232634` (exactly Frappe *crust*).
- **Currently not consumed by `base.css` at all** (verified by grepping
  the stylesheet and the mockup HTML). Like its Catppuccin twin, it
  completes the three-level elevation ladder (bg / surface /
  surface-deep) and is reserved for a future deepest layer; it is defined
  so both theme files keep an identical token set.

### `--color-border`

- Light: `#dcd3cb`. Dark: `#414459` (close to Frappe *surface0*).
- All hairlines: header and footer edges, card borders, ghost button,
  input borders. On warm paper the border is a warm gray, so the lines
  read as pencil rather than ink; decorative hairlines carry no WCAG
  ratio requirement.

### `--color-text`

- Light: `#575279`, **5.89:1**, a muted violet-slate ink (noticeably
  warmer than a pure gray, in keeping with the paper mood). Dark:
  `#c6ceef`, **7.90:1**, Frappe-family light lavender text.
- The main ink: `body`, brand, nav links, ghost button, inputs.
- Both modes clear 4.5:1 comfortably; the dark value clears AAA's 7:1.

### `--color-text-muted`

- Light: `#525252`, **6.32:1**, a plain dark gray. Curiosity worth
  noticing: it measures *higher* than `--color-text` (6.32 vs 5.89),
  because the colored violet ink sacrifices some luminance contrast for
  hue; "muted" here is achieved by dropping the color, not the contrast.
  Dark: `#b5bddc`, **6.61:1**.
- Secondary ink everywhere: footer, hero lead, feature text, form hints,
  form footer, stat labels, course meta.

### `--color-primary`

- Light: `#286983`, **4.94:1**, a deep teal (soft-paper's accent, a
  pond-water blue-green against the paper). Dark: `#8caaee`, **5.34:1**
  (exactly Frappe *blue*).
- The identity color: skip link, brand mark, nav hover and current-page
  states, primary button fill, ghost button hover, hero accent, stat
  values, progress fill.
- Passing 4.5:1 as text in both modes is what lets `base.css` use it for
  colored text and not just fills, same contract as Catppuccin's mauve.

### `--color-on-primary`

- Light: `#ffffff`, **6.11:1 measured on `--color-primary`**. Dark:
  `#232634` (Frappe *crust*), **6.51:1 on primary**.
- Text on primary fills (`.button--primary`, `.skip-link`). Same
  light/dark inversion as the other theme: white on the deep teal, then
  near-black on the pastel blue. The pair, not the color, carries the
  guarantee.

### `--color-link`

- Light: `#286983`, **4.94:1**. Dark: `#8caaee`, **5.34:1**.
- Prose links (`a`) and `.alert--info`.
- Identical to `--color-primary` in this theme (see the architecture
  note above): Soft Paper is deliberately lower-key, one accent doing
  both jobs, closer to a reading environment than an interactive app.
  The separate token is kept so the two themes expose the same
  vocabulary, and so links could be split from the primary later without
  touching `base.css`.

### `--color-success`

- Light: `#2f6a4a`, **5.18:1**, **adjusted** from upstream `#3f7d5b`,
  which measures only 3.96:1 on the paper background. Dark: `#67c48f`,
  **5.79:1** (soft-paper's green, near Frappe green territory).
- `.alert--success` text and border, plus the green traffic-light dot on
  the decorative code card.

### `--color-warning`

- Light: `#7d570c`, **5.25:1**, **adjusted** from upstream `#96690f`
  (3.93:1). Dark: `#c9be3e`, **6.40:1**.
- Like in the default theme, currently consumed by `base.css` only for
  the middle traffic-light dot (`.code-card::before`); there is no
  `.alert--warning` yet, but the token is compliant and ready.

### `--color-error`

- Light: `#94425a`, **5.34:1**, **adjusted** from upstream `#a34e63`,
  which at 4.44:1 fails 4.5:1 by a hair, a nice reminder that "close" is
  still non-compliant. Dark: `#e78284`, **4.65:1** (exactly Frappe
  *red*), the tightest passing margin in either theme.
- The safety-critical token: invalid input borders, form error messages,
  `.alert--error`, plus the red traffic-light dot.

### `--color-focus`

- Light: `#286983`. Dark: `#8caaee`.
- The global `:focus-visible` outline and the input focus ring. Again
  equal to the primary/link teal, and since a focus ring is a UI
  component held to the 3:1 threshold, a color that passes 4.5:1 as text
  passes here with room to spare. Contrast with Catppuccin, where focus
  keeps a *brighter* blue than the link precisely because only 3:1 is
  required: two valid strategies for the same token.

### `--color-primary-soft`

- Light: `#dfe9ec` (a pale teal wash). Dark: `#3b415c`.
- **Currently not consumed by `base.css` or the mockup markup** (verified
  by grep), same as in the default theme: a reserved decorative slot
  ("wash behind hero art") kept so the token sets match.

### The `--code-*` family (decorative syntax colors)

Same design as the default theme: the home-page code card is
`aria-hidden` decoration, exempt from contrast rules, and stays dark in
both modes like a real editor. Here the dark palette is the Frappe side
of soft-paper rather than Mocha.

- `--code-bg`: light `#303446` (Frappe *base*), dark `#232634` (Frappe
  *crust*): in dark mode the card again drops one level below the page
  background so it still stands out. Consumed by `.code-card`.
- `--code-text`: `#c6ceef` in both modes, the same lavender ink as the
  dark `--color-text`. Consumed by `.code-card`.
- `--code-keyword`: `#bb93d6`, a Frappe-family mauve, consumed by
  `.code-card__keyword`.
- `--code-string`: `#67c48f`, the theme's green (same value as the dark
  `--color-success`), consumed by `.code-card__string`.
- `--code-function`: `#8caaee` (Frappe *blue*, same as the dark primary),
  consumed by `.code-card__function`.
- `--code-comment`: `#838ba7` (Frappe *overlay1*), consumed by
  `.code-card__comment`.

---

## Summary table

"Adjusted" flags a light-mode value darkened from the soft-paper upstream
original (shown with its failing ratio) to reach WCAG 2.1 AA. Ratios are
against the same-mode `--color-bg`, except `--color-on-primary`, measured
on `--color-primary`.

| Token | Light (warm paper) | Ratio | Dark (Frappe) | Ratio | Consumed by (base.css) |
|---|---|---|---|---|---|
| `--color-bg` | `#eee6dd` | n/a | `#303446` base | n/a | `body`, `.form__input` |
| `--color-surface` | `#e6dbd1` | n/a | `#292c3c` mantle | n/a | header, footer, all cards, alerts |
| `--color-surface-deep` | `#ddd0c6` | n/a | `#232634` crust | n/a | **nothing yet** (reserved) |
| `--color-border` | `#dcd3cb` | n/a | `#414459` | n/a | all hairlines and card/input borders |
| `--color-text` | `#575279` | 5.89:1 | `#c6ceef` | 7.90:1 | `body`, brand, nav, ghost button, inputs |
| `--color-text-muted` | `#525252` | 6.32:1 | `#b5bddc` | 6.61:1 | footer, lead, hints, captions, meta |
| `--color-primary` | `#286983` | 4.94:1 | `#8caaee` blue | 5.34:1 | skip link, brand mark, nav states, primary button, accents, progress fill |
| `--color-on-primary` | `#ffffff` | 6.11:1 on primary | `#232634` crust | 6.51:1 on primary | text of `.button--primary`, `.skip-link` |
| `--color-link` | `#286983` (= primary) | 4.94:1 | `#8caaee` (= primary) | 5.34:1 | `a`, `.alert--info` |
| `--color-success` | `#2f6a4a` **adjusted** (`#3f7d5b`, 3.96:1) | 5.18:1 | `#67c48f` | 5.79:1 | `.alert--success`, traffic light |
| `--color-warning` | `#7d570c` **adjusted** (`#96690f`, 3.93:1) | 5.25:1 | `#c9be3e` | 6.40:1 | traffic light only (no warning alert yet) |
| `--color-error` | `#94425a` **adjusted** (`#a34e63`, 4.44:1) | 5.34:1 | `#e78284` red | 4.65:1 | invalid input, form errors, `.alert--error`, traffic light |
| `--color-focus` | `#286983` (= primary) | n/a stated | `#8caaee` | n/a stated | focus outlines (global and input) |
| `--color-primary-soft` | `#dfe9ec` | n/a | `#3b415c` | n/a | **nothing yet** (reserved) |
| `--code-bg` | `#303446` Frappe base | exempt | `#232634` Frappe crust | exempt | `.code-card` |
| `--code-text` | `#c6ceef` | exempt | same | exempt | `.code-card` |
| `--code-keyword` | `#bb93d6` | exempt | same | exempt | `.code-card__keyword` |
| `--code-string` | `#67c48f` | exempt | same | exempt | `.code-card__string` |
| `--code-function` | `#8caaee` Frappe blue | exempt | same | exempt | `.code-card__function` |
| `--code-comment` | `#838ba7` Frappe overlay1 | exempt | same | exempt | `.code-card__comment` |

Takeaways specific to this theme, on top of the shared architecture:

- Soft Paper compresses the accent vocabulary: primary, link and focus
  share one teal (light) / one blue (dark), a calmer, more bookish voice
  than Catppuccin's two-hue scheme; the tokens stay distinct so the
  themes remain interchangeable.
- The same compliance policy holds: three light-mode semantic colors
  (success, warning, error) are darkened from upstream, and the error
  adjustment (4.44:1 upstream) shows that a value can *almost* pass and
  still fail; only the measured ratio decides.
- Its dark mode being Catppuccin Frappe while Catppuccin's is Catppuccin
  Mocha means the two themes differ far more in light mode than in dark
  mode, which you can verify by toggling the OS scheme with each theme
  linked.
