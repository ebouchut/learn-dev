# login.html, tag by tag

An educational walkthrough of `login.html`, the sign-in page of the learn-dev
mockups. The mockup freezes a specific state: **the user has just registered
successfully and lands here with a confirmation message**.

The document shell (doctype, `html lang`, `head` metadata, font and stylesheet
`link`s, skip link, `header`/`nav`, `footer`) is identical to the home page and is
explained in depth in [home-html-explained.md](home-html-explained.md). Section 1
below only lists it; everything from section 2 on is specific to this page.

---

## 1. Shared shell (see home-html-explained.md)

| Tag | Reminder |
|---|---|
| `<!DOCTYPE html>`, `html lang="en"` | Standards mode; page language (RGAA 8.3) |
| `meta charset` / `meta viewport` | UTF-8; responsive viewport |
| `title` | `Log in ... learn-dev`: page name first, so tabs are distinguishable (RGAA 8.5) |
| `link` preconnect / stylesheet | Font-origin warm-up; theme then base CSS |
| `a.skip-link` + `main#main` | Skip repeated blocks (RGAA 12.7) |
| `header` > `div` > brand `a` + `nav aria-label="Main"` > `ul` > `li` > `a` | Banner + navigation landmarks (RGAA 9.2, 9.3, 12.2) |
| `footer` > `div` > `p` > `a` | Contentinfo landmark |

One shell detail changes per page: `aria-current="page"` sits on the **Log in**
nav link here, marking this page as the current one for assistive technology and
driving the highlighted style from the same attribute.

```html
<li><a class="nav__link" href="login.html" aria-current="page">Log in</a></li>
```

The `<main>` element also gains a modifier class, `site-main--narrow`, purely
CSS: it caps the width for a single-column form layout.

```html
<main class="site-main site-main--narrow" id="main">
```

---

## 2. The success alert

```html
<!-- Mockup state: arriving from a successful registration -->
<p class="alert alert--success reveal" role="status">
  Account created, please log in.
</p>
```

- **The comment.** `<!-- ... -->` is an HTML comment: invisible to users and
  assistive tech, it documents *which application state* this static file freezes.
  The real Thymeleaf template will render this element conditionally.
- **`<p>` as the base element.** The message is one sentence of prose, so a
  paragraph is the honest container; the interesting part is the `role`.
- **`role="status"`.** This ARIA role makes the element a **live region** with
  polite semantics (equivalent to `aria-live="polite"` plus `aria-atomic="true"`).
  When content appears inside it dynamically, screen readers announce it *after*
  finishing what they are currently saying, without stealing focus. A status role
  fits here because the news is good and non-blocking: nothing needs immediate
  action.
- **Why not `role="alert"`.** `alert` is assertive: it interrupts the user
  mid-sentence. That aggressiveness is reserved for problems that block the task,
  which is exactly what `register.html` uses it for (a failed submission). Success
  confirmation does not justify interrupting.
- **Live-region caveat worth knowing.** On a full page load (as in this
  server-rendered app), the element is present from the start, so the "announce on
  change" mechanism is not what fires; the role still correctly identifies the
  message as a status, and matters as soon as the message is injected client-side
  or the template re-renders a fragment.
- **`class="alert alert--success"`.** BEM block + modifier: the success variant is
  green-tinted, but the meaning is carried by the *text*, not only the color, which
  supports RGAA **3.1** (information is not given by color alone).
- **Position.** First child of `<main>`, before the form card, matching visual
  order to DOM order so screen reader and keyboard order match the layout.

---

## 3. The form card

```html
<div class="form-card reveal reveal--2">
  <h1 class="form-card__title">Log in</h1>
  <form action="#" method="post" novalidate>...</form>
  <p class="form__footer">
    No account yet? <a href="register.html">Create one</a>.
  </p>
</div>
```

- **`<div class="form-card">`.** A purely visual card (border, padding, shadow)
  around the heading, the form, and the footer link. No semantics intended, so
  `<div>` is the right tool (see the div discussion in
  [home-html-explained.md](home-html-explained.md)).
- **`<h1>`.** The unique top-level heading of this page: "Log in". On content
  pages the h1 is the hero title; on task pages it names the task. One h1 per
  page keeps the outline rooted (RGAA **9.1**).
- **`<p class="form__footer">` with `<a>`.** The escape hatch to registration is a
  *link* because it navigates; sentence context ("No account yet?") makes the link
  purpose explicit (RGAA **6.1**).

---

## 4. The form itself

### `<form action="#" method="post" novalidate>`

```html
<form action="#" method="post" novalidate>
```

- **Role.** Groups the controls and defines how they are submitted. A `<form>`
  maps to the ARIA `form` landmark only when it has an accessible name; unnamed,
  it is still the functional grouping that makes Enter-to-submit work.
- **Attributes.**
  - `action="#"`: mockup placeholder. In the real app this will be the Spring
    Security processing URL (typically `/login`), rendered by Thymeleaf with
    `th:action` (which also injects the CSRF hidden field automatically).
  - `method="post"`: credentials must travel in the **request body**, never the
    URL. With GET they would land in the query string, hence in browser history,
    server access logs, and `Referer` headers. POST is non-negotiable for
    passwords.
  - `novalidate` (boolean attribute, no value): disables the browser's *native*
    validation bubbles. Two reasons. First, native bubbles are inconsistent
    across browsers, vanish on their own, and are hard to style or translate.
    Second, and decisive for this project: the source of truth for validation is
    the **server** (Jakarta Bean Validation on the Spring side), and the mockups
    demonstrate exactly that server-rendered error state (see `register.html`).
    `novalidate` lets the form submit so the server can answer with accessible,
    persistent, styled messages. The HTML attributes that *describe* the rules
    (`required`, `type="email"`) are kept anyway: they feed the accessibility
    tree and CSS `:required` even when native enforcement is off.
- **Position.** Inside the form card; children are two `div.form__group` field
  rows and the submit `<button>`.

### `<div class="form__group">`

```html
<div class="form__group">
  <label class="form__label" for="username">Username</label>
  <input class="form__input" type="text" id="username" name="username"
         autocomplete="username" required>
</div>
```

- **Role.** One field row: label above input, stacked by CSS. Pure layout, hence
  `<div>`. A `<fieldset>` would be justified for a *group of related controls
  sharing one caption* (radio sets, address blocks); a single label + input pair
  does not need one.

### `<label for="...">`

```html
<label class="form__label" for="username">Username</label>
```

- **Role.** The caption of a form control. The **`for`/`id` pair** is the single
  most important accessibility wiring in any form: `for="username"` points at the
  `id="username"` of the input, which gives the input its **accessible name**.
  Screen readers announce "Username, edit text" when the field gains focus; and
  clicking the label focuses the input, a real usability win on touch screens.
- **Attributes.** `for` (the id reference), `class` (styling).
- **Why not the alternative.** `placeholder` text is not a label: it disappears on
  input, has low contrast, and is not reliably announced. Wrapping the input
  inside the label (implicit association) works but the explicit `for`/`id` form
  is more robust with CSS layouts and is what the Thymeleaf `th:field` pattern
  pairs naturally with. `aria-label` would work for screen readers but leaves no
  visible caption, failing sighted users.
- **RGAA.** **11.1** (each form field has a label) and **11.2** (the label is
  relevant).

### `<input>` (username and password)

```html
<input class="form__input" type="text" id="username" name="username"
       autocomplete="username" required>

<input class="form__input" type="password" id="password" name="password"
       autocomplete="current-password" required>
```

- **Role.** Single-line text entry; implicit ARIA role `textbox` for
  `type="text"`. A void element: no closing tag, no content.
- **Attributes, one by one.**
  - `type="text"` vs `type="password"`: the password type masks echoed
    characters, blocks copy of the masked value, and signals password managers.
  - `id`: target of the label's `for` (accessibility wiring, per-document unique).
  - `name`: the key under which the value is submitted to the server
    (`username=...&password=...` in the POST body). **`id` is for the DOM and
    accessibility; `name` is for the HTTP submission.** They often match, but
    they serve different masters; Spring Security's default form login expects
    exactly the parameter names `username` and `password`.
  - `autocomplete="username"` / `autocomplete="current-password"`: standardized
    *purpose tokens* from the HTML spec. They tell browsers and password managers
    exactly which stored credential slot each field is, so autofill works
    reliably. `current-password` (as opposed to `new-password` on the register
    page) means "the password the user already has": the password manager fills
    it rather than proposing a generated one. Beyond convenience this is an
    accessibility feature: users with memory or motor difficulties depend on
    autofill.
  - `required` (boolean): declares the field mandatory. Even with `novalidate`
    switching off native bubbles, `required` still sets `aria-required` semantics
    in the accessibility tree ("required, edit text") and enables the CSS
    `:required` hook. The server re-validates regardless; client hints never
    replace server checks.
- **Works with.** Its `<label>` via `for`/`id`; the register page extends this
  pattern with hint/error `<span>`s via `aria-describedby`.
- **RGAA.** **11.1** (label association), **11.13** (the purpose of the field can
  be deduced, via `autocomplete`).

### `<button type="submit">`

```html
<button class="button button--primary" type="submit">Log in</button>
```

- **Role.** A real action: submit the form. Implicit ARIA role `button`.
  Activatable with both Enter and Space, focusable by default.
- **Attributes.**
  - `type="submit"`: explicit, even though it is the default for a button inside
    a form. Writing it is a defensive habit: the day a second button (e.g. "show
    password") lands in the form, forgetting `type="button"` on it would make it
    submit too. Explicit types make intent auditable.
  - `class="button button--primary"`: shares the exact visual style with the
    `<a class="button">` links elsewhere; same look, correct element for each
    behavior.
- **Why not the alternative.** An `<a href="#">` with a click handler cannot
  submit a form natively, is announced as a link, and does not respond to Space.
  `<input type="submit">` works but cannot contain markup and is less styleable.
  **Link = navigate, button = act**; logging in is an act.

---

## 5. Recap table

| Tag | Implicit ARIA role | Key attributes here | Works with | Main purpose on this page |
|---|---|---|---|---|
| shell tags | see home-html-explained.md | `aria-current="page"` on Log in | shell | Shared frame |
| `main` | main | `id="main"`, `site-main--narrow` | skip link | Narrow single-column task page |
| `p.alert` | status (via `role`) | `role="status"` | future dynamic rendering | Polite success announcement |
| `div.form-card` | none | `class` | h1, form, p | Visual card, no semantics |
| `h1` | heading level 1 | `class` | page outline | Names the task (RGAA 9.1) |
| `form` | none (unnamed) | `action="#"`, `method="post"`, `novalidate` | inputs, button | POST submission; server-side validation strategy |
| `div.form__group` | none | `class` | label + input | One field row |
| `label` | n/a | `for="username"` / `for="password"` | matching input `id` | Accessible name (RGAA 11.1) |
| `input` | textbox | `type`, `id`, `name`, `autocomplete`, `required` | label, form | Credential entry (RGAA 11.13) |
| `button` | button | `type="submit"` | form | Submit action (button = act) |
| `p.form__footer` + `a` | paragraph / link | `href="register.html"` | register page | Cross-link to the other auth flow |
