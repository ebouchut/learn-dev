# home.html, tag by tag

An educational walkthrough of `home.html`, the public landing page of the learn-dev
mockups. Audience: a developer preparing the French DWWM certification who wants to
understand every tag choice, not just copy it.

This file is also the **reference document for the shared page shell** (doctype,
`<html>`, `<head>`, header, footer, skip link). The other four walkthroughs
(`login-html-explained.md`, `register-html-explained.md`, `dashboard-html-explained.md`,
`index-html-explained.md`) explain their page-specific content in full and point back
here for the shell.

Note on quoting: the mockup's `<title>` and footer strings contain a typographic dash;
in the snippets below it is elided as `...`.

---

## 1. The document shell

### `<!DOCTYPE html>`

```html
<!DOCTYPE html>
```

- **Role.** Not a tag but a *document type declaration*. It is the very first thing in
  the file and tells the browser to parse the page in **standards mode** (also called
  no-quirks mode). Without it, browsers fall back to "quirks mode" and emulate 1990s
  layout bugs (different box model, different line-height handling), which breaks
  modern CSS.
- **Attributes.** None. The short HTML5 form replaced the long HTML4/XHTML doctypes
  with DTD URLs.
- **Position.** First line, before `<html>`. Nothing may precede it except whitespace
  or comments.
- **Why not the alternative.** There is no real alternative: omitting it is legal HTML
  but opts you into quirks mode, which no one wants.

### `<html lang="en">`

```html
<html lang="en">
```

- **Role.** The root element; every other element is its descendant. Maps to the ARIA
  `document` role implicitly.
- **Attributes.**
  - `lang="en"`: declares the default natural language of the whole page using a
    BCP 47 language code. Screen readers pick the correct speech synthesis voice and
    pronunciation rules from it; translation tools, spellcheckers, and CSS `hyphens`
    also rely on it. The mockups are written in English, hence `en` (the real app
    could later serve `fr` pages with `lang="fr"`).
- **Position.** Direct parent of exactly two children: `<head>` and `<body>`.
- **RGAA.** Criterion **8.3** (each page has a default language) and 8.4 (the code is
  valid). A missing `lang` is one of the most common audit failures.

### `<head>`

```html
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>learn-dev ... Learn programming by doing</title>
  ...
</head>
```

- **Role.** Machine-readable metadata container: nothing inside it is rendered in the
  page body. Holds the character encoding, viewport rule, title, and resource links.
- **Attributes.** None here.
- **Position.** First child of `<html>`; contains `<meta>`, `<title>`, `<link>`.
- **Why not the alternative.** No alternative; browsers would auto-create it anyway,
  but writing it explicitly keeps the document structure honest and readable.

### `<meta charset="UTF-8">`

```html
<meta charset="UTF-8">
```

- **Role.** Declares the byte-to-character encoding of the document.
- **Attributes.**
  - `charset="UTF-8"`: the only attribute this form of `<meta>` takes. UTF-8 covers
    every script and is the only encoding the HTML spec recommends. It must appear
    within the first 1024 bytes of the file so the parser knows the encoding before it
    meets any non-ASCII character (this page contains emoji and a `&nbsp;`, so it
    matters concretely).
- **Position.** Ideally the first child of `<head>`, as it is here.
- **Why not the alternative.** The old form
  `<meta http-equiv="Content-Type" content="text/html; charset=utf-8">` still works
  but is longer for zero benefit. Relying on an HTTP header alone is fragile once the
  file is opened from disk, exactly the use case of a static mockup.

### `<meta name="viewport" content="width=device-width, initial-scale=1">`

```html
<meta name="viewport" content="width=device-width, initial-scale=1">
```

- **Role.** Instructs mobile browsers how to size the layout viewport.
- **Attributes.**
  - `name="viewport"`: selects the viewport metadata key.
  - `content="width=device-width, initial-scale=1"`: `width=device-width` makes the
    CSS viewport match the physical device width instead of a faked 980px desktop
    canvas; `initial-scale=1` starts at 100% zoom. Together they make the responsive
    CSS actually take effect on phones.
- **Position.** Child of `<head>`.
- **Accessibility note.** It deliberately does **not** include `user-scalable=no` or
  `maximum-scale=1`: blocking pinch-zoom is an accessibility failure (RGAA criterion
  **10.8** family on content restitution, WCAG 1.4.4 resize text).

### `<title>`

```html
<title>learn-dev ... Learn programming by doing</title>
```

- **Role.** The page title shown in the browser tab, bookmarks, history, and search
  results. It is the **first thing a screen reader announces** when the page loads,
  so it must identify the page uniquely.
- **Attributes.** None.
- **Position.** Child of `<head>`; exactly one per document.
- **Pattern.** Each mockup follows `page name + site name` (or the reverse), so users
  juggling several tabs can tell them apart: compare `Log in ... learn-dev` and
  `Dashboard ... learn-dev`.
- **RGAA.** Criterion **8.5** (each page has a title) and 8.6 (the title is relevant).

### `<link>` (four flavours)

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Atkinson+Hyperlegible:wght@400;700&family=Sora:wght@600;700&family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
<link rel="stylesheet" href="css/theme-catppuccin.css">
<link rel="stylesheet" href="css/base.css">
```

- **Role.** A void element that relates the document to an external resource. The
  `rel` attribute decides *how* it relates.
- **Attributes.**
  - `rel="preconnect"`: a performance hint. It tells the browser to open the TCP
    connection, TLS handshake included, to that origin *now*, before any resource
    from it is requested. Google Fonts spans **two** origins: `fonts.googleapis.com`
    serves the CSS, `fonts.gstatic.com` serves the font binaries, so each origin gets
    its own preconnect.
  - `crossorigin` (on the `fonts.gstatic.com` preconnect only): font files are always
    fetched in CORS *anonymous* mode, and a preconnected socket is only reusable if
    its CORS mode matches the future request. Without `crossorigin`, that preconnect
    would warm up a socket the font request cannot use. The CSS file, by contrast, is
    fetched without CORS, so its preconnect has no `crossorigin`. Written bare, the
    attribute means `crossorigin="anonymous"` (no credentials sent).
  - `rel="stylesheet"`: loads and applies a CSS file.
  - `href="..."`: the resource URL. The two local stylesheets use relative paths so
    the mockups work from disk. Order matters: `theme-catppuccin.css` defines the
    design tokens (CSS custom properties), `base.css` consumes them, so the theme
    file comes first. Swapping the theme means editing this one `href`
    (see `index.html`).
  - In the fonts URL, `display=swap` asks the browser to render text immediately
    with a fallback font and swap when the webfont arrives (no invisible text).
- **Position.** Children of `<head>`; void elements, no closing tag.
- **Why not the alternative.** `@import` inside CSS would serialize the downloads
  (CSS must be parsed before the import is even discovered); `<link>` lets the
  preload scanner fetch everything in parallel.

---

## 2. Page skeleton and landmarks

The `<body>` reads, top to bottom: skip link, `<header>`, `<main>`, `<footer>`.
These map to ARIA **landmarks**, the coarse map assistive technology users navigate by.

### `<body>`

```html
<body>
  <a class="skip-link" href="#main">Skip to main content</a>
  <header class="site-header">...</header>
  <main class="site-main" id="main">...</main>
  <footer class="site-footer">...</footer>
</body>
```

- **Role.** Container of all rendered content. One per document, second child of
  `<html>`.
- **Attributes.** None; all styling hangs off classes on descendants.

### `<a class="skip-link" href="#main">` (the skip link)

```html
<a class="skip-link" href="#main">Skip to main content</a>
```

- **Role.** An internal anchor that lets keyboard and screen reader users jump over
  the repeated header and navigation straight into the unique page content.
- **Attributes.**
  - `class="skip-link"`: the CSS visually hides the link off-screen until it receives
    keyboard focus, then reveals it. It is hidden with an off-screen technique, never
    `display:none`, which would remove it from the tab order and defeat its purpose.
  - `href="#main"`: a fragment link targeting `id="main"` on the `<main>` element.
    The `href`/`id` pair is the whole mechanism: activating the link moves focus and
    scroll position to `<main>`.
- **Position.** Deliberately the **first focusable element** in `<body>`, so it is
  the first Tab stop on every page. Works with `main#main`.
- **Why not the alternative.** ARIA landmarks alone already let *screen reader* users
  jump around, but sighted keyboard-only users (motor impairments, power users) have
  no landmark shortcut; the skip link serves them too.
- **RGAA.** Criterion **12.7** (a skip-to-content link exists) and it must become
  visible on focus, which ties into **10.7** (visible focus).

### `<header class="site-header">`

```html
<header class="site-header">
  <div class="site-header__inner">
    <a class="site-header__brand" href="home.html">learn<span class="site-header__brand-mark">-dev</span></a>
    <nav class="site-header__nav" aria-label="Main">...</nav>
  </div>
</header>
```

- **Role.** Introductory content for its nearest ancestor sectioning context. When
  its ancestor is `<body>` (as here), it gets the implicit ARIA role **`banner`**:
  the site-wide masthead landmark. Screen readers announce it and list it in their
  landmark menus.
- **Attributes.** `class` only, following BEM naming (`block__element--modifier`)
  which mirrors the future Thymeleaf templates.
- **Position.** First rendered region of `<body>`. Contains a layout `<div>`, the
  brand link, and the `<nav>`.
- **Why not the alternative.** `<div class="header">` renders identically but exposes
  **no landmark**: assistive technology sees an anonymous box. The semantic element
  costs nothing and buys the `banner` role for free.
- **RGAA.** Criterion **9.2** (coherent document structure: the page defines header,
  main content, footer) and **12.2** (navigation is in the same place on every page).

### `<div>` (layout wrappers)

```html
<div class="site-header__inner">...</div>
```

- **Role.** The generic flow container. **No semantics, no ARIA role.** Here it
  exists purely so CSS can center the header content and cap its max width while the
  `<header>` background spans the full viewport.
- **Attributes.** `class` only, everywhere it appears.
- **Position.** Used in this page as: `site-header__inner` (inside `<header>`), the
  unclassed hero text column and `hero__actions` (inside `<section class="hero">`),
  and `site-footer__inner` (inside `<footer>`).
- **Why not the alternative.** This is the *correct* use of `<div>`: pure styling
  hooks with no meaning to convey. The rule of thumb the page follows everywhere:
  semantic element when meaning exists (`header`, `nav`, `main`, `section`, `ul`),
  `div` only when the box is purely presentational.

### `<nav aria-label="Main">`

```html
<nav class="site-header__nav" aria-label="Main">
  <ul class="nav__list">
    <li><a class="nav__link" href="home.html" aria-current="page">Home</a></li>
    <li><a class="nav__link" href="login.html">Log in</a></li>
    <li><a class="button button--primary" href="register.html">Sign up</a></li>
  </ul>
</nav>
```

- **Role.** A major block of navigation links. Implicit ARIA role **`navigation`**,
  another landmark.
- **Attributes.**
  - `aria-label="Main"`: gives the landmark an accessible name. A page often has
    several `<nav>` elements (main menu, breadcrumb, pagination, footer nav); the
    label lets a screen reader announce "Main navigation" instead of just
    "navigation", so users can tell them apart. Even with a single nav it is a cheap
    habit that scales.
- **Position.** Inside the site `<header>`; its only child is the `<ul>` of links.
- **Why not the alternative.** A bare `<div>` of links gives no landmark and no way
  to jump to the menu. Wrapping *every* link cluster in `<nav>` is the opposite
  mistake: reserve it for major navigation blocks.
- **RGAA.** **12.2** (consistent location) and **12.6** (grouped navigation zones can
  be reached or skipped).

### `<ul>` and `<li>` (navigation list)

```html
<ul class="nav__list">
  <li><a class="nav__link" href="home.html" aria-current="page">Home</a></li>
  ...
</ul>
```

- **Role.** `<ul>` is an unordered list (implicit ARIA role `list`), `<li>` a list
  item (role `listitem`). Marking the menu as a list makes screen readers announce
  "list, 3 items" before reading the links: the user instantly knows the size of the
  menu and can skip it as a unit.
- **Attributes.** `class` only. The bullets are removed in CSS (`list-style: none`),
  which does not remove the list semantics for most browsers (Safari with
  `list-style: none` is the known exception, one reason some teams add
  `role="list"` back; these mockups keep it simple).
- **Position.** `<ul>` is the sole child of `<nav>`; each `<li>` wraps exactly one
  link. Only `<li>` (and script-supporting elements) may be children of `<ul>`.
- **Why not the alternative.** A row of `<a>` in a `<div>` looks identical but loses
  the item count and list navigation shortcuts. An `<ol>` would claim the order
  itself is meaningful (steps, rankings), which a menu's order is not.
- **RGAA.** Criterion **9.3** (lists are correctly structured with `ul`/`li`).

### `<a>` (links, three visual variants)

```html
<a class="site-header__brand" href="home.html">learn<span class="site-header__brand-mark">-dev</span></a>
<a class="nav__link" href="home.html" aria-current="page">Home</a>
<a class="button button--primary" href="register.html">Create your account</a>
<a class="button button--ghost" href="login.html">I already have one</a>
<a href="https://github.com/ebouchut/learn-dev">Source on GitHub</a>
```

- **Role.** Hyperlink; implicit ARIA role `link`. Every `<a>` here **navigates to a
  different URL**, which is exactly the criterion for choosing `<a>`.
- **Attributes.**
  - `href="..."`: the destination. Relative (`home.html`, `register.html`) between
    mockup pages, absolute for GitHub. An `<a>` without `href` is not focusable and
    not a real link; every link here has one.
  - `aria-current="page"`: on the nav link matching the current page. Screen readers
    append "current page" to the announcement, giving non-visual users the same
    "you are here" cue that the highlighted style gives sighted users. The CSS also
    targets `[aria-current="page"]`, so the state and the style can never drift
    apart: one attribute drives both. Compare the same attribute on "Log in" in
    `login.html` and "Sign up" in `register.html`.
  - `class="button button--primary"` / `button--ghost`: the two calls to action are
    *styled* as buttons but remain links, because activating them navigates.
- **Position.** Inside `<li>` (nav), inside `<div class="hero__actions">` (CTAs),
  inside `<p>` (footer). The brand link wraps a `<span>` used purely to color the
  `-dev` part.
- **Why not the alternative (`<a>` vs `<button>`).** The rule: **link = go
  somewhere, button = do something**. "Sign up" merely navigates to
  `register.html`, so it is a link even though it looks like a button. Links respond
  to Enter (not Space), can be opened in a new tab, appear in link lists, and can be
  middle-clicked; a `<button>` styled as a link (or the reverse element choice)
  breaks all of those user expectations. The genuine `<button>` appears where an
  action happens: form submission (`login.html`, `register.html`) and logout
  (`dashboard.html`).
- **RGAA.** **6.1** (each link is explicit: the link text says where it goes), and
  the `aria-current` state supports **3.1** (information is not conveyed by color
  alone: the active item is not marked *only* by a color change).

### `<main id="main">`

```html
<main class="site-main" id="main">
  <section class="hero" aria-labelledby="hero-title">...</section>
  <section aria-labelledby="features-title">...</section>
</main>
```

- **Role.** The unique, central content of the page: what remains once you remove
  the repeated header and footer. Implicit ARIA role **`main`**, the landmark screen
  reader users jump to first.
- **Attributes.**
  - `id="main"`: the anchor target of the skip link. This `href="#main"` /
    `id="main"` pair is the page's most important id relationship.
  - `class="site-main"`: layout width and padding. (`login.html` and
    `register.html` add the `site-main--narrow` modifier for their single-column
    forms.)
- **Position.** Between `<header>` and `<footer>`; exactly one visible `<main>` per
  page, and it must not be nested inside `header`, `footer`, `nav`, `article`, or
  `aside`.
- **Why not the alternative.** `<div id="main">` would still make the skip link work
  but loses the `main` landmark. The element gives both for free.
- **RGAA.** **9.2** (document structure) and **12.7** (the skip link needs a valid
  target; `main` is the canonical one).

---

## 3. The hero section

### `<section class="hero" aria-labelledby="hero-title">`

```html
<section class="hero" aria-labelledby="hero-title">
  <div>
    <h1 class="hero__title reveal" id="hero-title">...</h1>
    ...
  </div>
  <div class="code-card ..." aria-hidden="true">...</div>
</section>
```

- **Role.** A thematic grouping of content. Crucially, a bare `<section>` is
  semantically almost a `<div>`; it only becomes a **`region` landmark** when it has
  an accessible name. That is what `aria-labelledby` provides.
- **Attributes.**
  - `aria-labelledby="hero-title"`: names the section *by reference* to the id of
    the `<h1>` inside it. The section is announced as the "Learn programming by
    doing" region. Referencing the existing heading instead of duplicating its text
    in an `aria-label` means there is one source of truth: edit the heading, the
    region name follows.
- **Position.** First child of `<main>`. Two children: a `<div>` holding the textual
  column (title, lead, actions) and the decorative code card `<div>`. The two-column
  layout is CSS grid on the section.
- **Works with.** `h1#hero-title` (its name), the second section repeats the same
  pattern with `h2#features-title`.
- **Why not the alternative.** A `<div>` would style identically but create no
  landmark; an unnamed `<section>` would be equally mute. Section + heading id is
  the pattern worth memorizing: *no name, no region*.
- **RGAA.** **9.2** (structure) via well-delimited regions.

### `<h1>` (page title in content)

```html
<h1 class="hero__title reveal" id="hero-title">
  Learn programming <span class="hero__title-accent">by&nbsp;doing</span>
</h1>
```

- **Role.** The single top-level heading, root of the page's heading outline
  (implicit ARIA role `heading`, `aria-level=1`). Screen reader users routinely
  navigate by headings (the H key), so the outline is a table of contents.
- **Attributes.**
  - `id="hero-title"`: referenced by the section's `aria-labelledby` (see above).
  - `class="hero__title reveal"`: `reveal` drives the staggered entrance animation
    in CSS (`reveal--2`, `reveal--3`, `reveal--4` on later elements set increasing
    delays). Purely presentational.
- **Content details.** The inner `<span class="hero__title-accent">` exists only to
  color "by doing"; `&nbsp;` (no-break space) keeps "by doing" from wrapping onto
  two lines, which would look broken since the two words form one highlighted unit.
- **Position.** One `<h1>` per page, here inside the hero. The outline on this page
  is: h1 "Learn programming by doing", then h2 "Why learn-dev?", then three h3
  feature titles. No level is skipped.
- **RGAA.** **9.1** (information is structured by correctly nested headings; every
  page has an `h1`).

### `<p>` (paragraphs)

```html
<p class="hero__lead reveal reveal--2">
  Interactive lessons, hands-on exercises, and instant feedback.
  Build real skills at your own pace, one lesson at a time.
</p>
```

- **Role.** A paragraph of prose. Screen readers pause between paragraphs and let
  users move paragraph by paragraph.
- **Attributes.** `class` only (visual size for the lead, animation stagger).
- **Position.** Here in the hero; also as `feature-card__text` inside each feature
  `<li>`, and in the footer.
- **Why not the alternative.** Loose text in a `<div>` or bare in a section loses
  paragraph navigation and default spacing semantics; `<br>`-separated lines would
  be a single run-on for assistive tech.

### `<span>` (inline styling hooks)

```html
<span class="hero__title-accent">by&nbsp;doing</span>
<span class="site-header__brand-mark">-dev</span>
<span class="feature-card__icon" aria-hidden="true">🧑‍💻</span>
```

- **Role.** The inline counterpart of `<div>`: a generic, meaning-free wrapper used
  when part of a line needs its own styling. No ARIA role.
- **Attributes.**
  - `class`: the styling hook (accent color, brand mark color, icon sizing).
  - `aria-hidden="true"` on the feature icons: the emoji are pure decoration; the
    adjacent `<h3>` already names the feature. Without it, a screen reader would
    announce "man technologist", "high voltage", "chart increasing", which is noise
    at best and confusion at worst. `aria-hidden="true"` removes the node (and its
    subtree) from the accessibility tree while leaving it visible.
- **Why not the alternative.** `<em>`/`<strong>` would add emphasis semantics that
  are not intended (the color accent is branding, not emphasis). When only paint
  changes, `<span>` is correct.

### `<div class="code-card" aria-hidden="true">` (the decorative code card)

```html
<div class="code-card reveal reveal--4" aria-hidden="true">
  <code class="code-card__code"><span class="code-card__comment">// your first lesson</span>
<span class="code-card__keyword">public class</span> <span class="code-card__function">Hello</span> {
  <span class="code-card__keyword">public static void</span> <span class="code-card__function">main</span>(String[] args) {
    System.out.println(<span class="code-card__string">"Hello, learn-dev!"</span>);
  }
}</code>
</div>
```

This is the page's most instructive accessibility decision, worth unpacking fully.

- **What it is.** A fake editor window showing a Java "Hello, world" with hand-rolled
  syntax highlighting. It exists to *look like* the product; it teaches nothing that
  the hero text does not already say.
- **`aria-hidden="true"` on the wrapper.** One attribute on the container removes the
  entire subtree, the `<code>` element and every highlight `<span>` included, from
  the accessibility tree. A screen reader user never hears it. Without it, they would
  wade through "slash slash your first lesson public class Hello brace public static
  void main..." announced span by span: pure cost, zero information. The rule:
  **decorative content is hidden; informative content never is.** Two safety checks
  before using `aria-hidden="true"`: (1) the content really is redundant or
  decorative, and (2) the subtree contains **no focusable element** (a hidden but
  tabbable link would create a "ghost stop" where focus lands on nothing announced).
  Both hold here.
- **Why `<code>` inside anyway?** Even though assistive tech never sees it, `<code>`
  is the honest element for computer code, it keeps the markup self-describing for
  human readers of the source, and it carries the monospace styling naturally. The
  inner `<span class="code-card__comment|keyword|function|string">` elements are the
  classic syntax-highlighting pattern: meaning-free inline hooks carrying only color.
  Note also that the whitespace inside `<code>` is laid out flush-left in the source
  on purpose: the CSS preserves whitespace (`white-space: pre`), so source
  indentation would leak into the rendering.
- **Why not the alternative.** An `<img>` of a screenshot would need `alt=""` to be
  equally silent, but would blur on zoom, ignore the theme tokens, and weigh more.
  Real markup themed by CSS stays crisp, dark-mode aware, and cheap. Marking the card
  visible to assistive tech with a proper label was considered and rejected: it has
  no information to convey.

### `<code>`

- **Role.** Inline computer code fragment. Also used *outside* the hidden card in
  `index.html` (file paths, `prefers-color-scheme`), where it **is** exposed to
  assistive technology because there it is informative.
- **Attributes.** `class` for the code-card styling.
- **Why not the alternative.** `<pre>` alone preserves whitespace but says "this is
  preformatted", not "this is code"; the canonical block-code pattern is
  `<pre><code>`. Here the card's CSS handles the whitespace, so `<code>` alone
  suffices.

---

## 4. The features section

### `<section aria-labelledby="features-title">` and `<h2>`

```html
<section aria-labelledby="features-title">
  <h2 id="features-title">Why learn-dev?</h2>
  <ul class="features">...</ul>
</section>
```

- Same named-region pattern as the hero: the `<h2>`'s `id` names the section.
- **`<h2>` role.** Second-level heading, directly under the `<h1>` in the outline.
  Its `id="features-title"` is consumed by the section's `aria-labelledby`.
- **RGAA.** **9.1** (heading hierarchy), **9.2** (regions).

### `<ul class="features">` with `<li class="feature-card">`

```html
<ul class="features">
  <li class="feature-card">
    <span class="feature-card__icon" aria-hidden="true">🧑‍💻</span>
    <h3 class="feature-card__title">Interactive lessons</h3>
    <p class="feature-card__text">Read, try, and experiment in the same place. ...</p>
  </li>
  ...
</ul>
```

- **Role.** The three feature cards are *a list of like items*, so they are marked up
  as one: announced "list, 3 items". Each `<li>` is a card containing a decorative
  icon `<span>` (hidden, see above), an `<h3>` naming the feature, and a `<p>`
  describing it.
- **Attributes.** Classes only; the card grid is pure CSS on the `<ul>`.
- **`<h3>` role.** Third-level headings, children of the h2 section: the outline
  stays strictly nested (h1 > h2 > h3, no jumps).
- **Why not the alternative.** Three sibling `<div class="card">` elements would
  render identically but lose the item count, the list navigation, and the heading
  outline entries. `ul + li + h3` costs nothing extra and gives all three.
- **RGAA.** **9.3** (lists), **9.1** (headings).

---

## 5. The footer

### `<footer class="site-footer">`

```html
<footer class="site-footer">
  <div class="site-footer__inner">
    <p>learn-dev ... an interactive programming learning platform.</p>
    <p><a href="https://github.com/ebouchut/learn-dev">Source on GitHub</a></p>
  </div>
</footer>
```

- **Role.** Footer for its nearest sectioning context; as a direct child of
  `<body>` it gets the implicit ARIA role **`contentinfo`**: the site-information
  landmark (copyright, colophon, secondary links).
- **Attributes.** `class` only.
- **Position.** Last region of `<body>`. Contains a layout `<div>` and two `<p>`,
  one of which holds the external GitHub link.
- **Why not the alternative.** Same argument as `<header>` vs `<div>`: identical
  pixels, but only the semantic element exposes the landmark.
- **RGAA.** **9.2**, **12.2** (consistent placement across pages).

---

## 6. Recap table

| Tag | Implicit ARIA role | Key attributes here | Works with | Main purpose on this page |
|---|---|---|---|---|
| `<!DOCTYPE html>` | n/a | none | whole document | Standards-mode parsing |
| `html` | document | `lang="en"` | everything | Root; page language (RGAA 8.3) |
| `head` | n/a | none | meta, title, link | Metadata container |
| `meta` | n/a | `charset`, `name="viewport"`, `content` | parser, mobile browsers | Encoding; responsive viewport |
| `title` | n/a | none | browser tab, screen reader | Unique page title (RGAA 8.5) |
| `link` | n/a | `rel="preconnect"`/`"stylesheet"`, `href`, `crossorigin` | external CSS/fonts | Styles and font-loading performance |
| `body` | n/a | none | all rendered content | Rendered document |
| `a.skip-link` | link | `href="#main"` | `main#main` | Skip repeated blocks (RGAA 12.7) |
| `header` | banner | `class` | div, nav | Site masthead landmark (RGAA 9.2) |
| `div` | none | `class` | any | Pure layout box, no semantics |
| `nav` | navigation | `aria-label="Main"` | ul > li > a | Named navigation landmark (RGAA 12.2) |
| `ul` | list | `class` | li | Menu and card collections (RGAA 9.3) |
| `li` | listitem | `class` | parent ul | One menu entry / one card |
| `a` | link | `href`, `aria-current="page"` | li, p, div | Navigation; current-page state |
| `main` | main | `id="main"` | skip link | Unique content landmark |
| `section` | region (when named) | `aria-labelledby` | its heading's id | Named page regions (RGAA 9.2) |
| `h1` | heading level 1 | `id="hero-title"` | section | Page outline root (RGAA 9.1) |
| `h2` | heading level 2 | `id="features-title"` | section | Section heading (RGAA 9.1) |
| `h3` | heading level 3 | `class` | li card | Card headings (RGAA 9.1) |
| `p` | paragraph | `class` | text content | Prose blocks |
| `span` | none | `class`, `aria-hidden="true"` | inline text | Styling hooks; hidden emoji icons |
| `code` | code | `class` | code-card div | Code fragment (decorative here) |
| `footer` | contentinfo | `class` | div, p, a | Site info landmark (RGAA 9.2) |
