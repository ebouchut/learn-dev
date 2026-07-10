# register.html, tag by tag

An educational walkthrough of `register.html`, the account-creation page of the
learn-dev mockups. The mockup freezes the page's most instructive state: **the form
was submitted and came back from the server with one field error** (email already
registered). Every accessibility decision on this page exists to make that error
state work for everyone; the error wiring gets its own section below.

The document shell (doctype, `html lang`, `head` metadata, font and stylesheet
`link`s, skip link, `header`/`nav`, `footer`) is identical to the home page and is
explained in depth in [home-html-explained.md](home-html-explained.md). Section 1
only lists it. The form basics (`form method="post" novalidate`, `label`/`for`,
`input` fundamentals, `button type="submit"`) are shared with the login page and
explained in [login-html-explained.md](login-html-explained.md); this document
focuses on what register adds: hints, the error state, and `new-password`.

---

## 1. Shared shell (see home-html-explained.md)

| Tag | Reminder |
|---|---|
| `<!DOCTYPE html>`, `html lang="en"` | Standards mode; page language (RGAA 8.3) |
| `meta charset` / `meta viewport` | UTF-8; responsive viewport |
| `title` | `Create an account ... learn-dev` (RGAA 8.5) |
| `link` preconnect / stylesheet | Font-origin warm-up; theme then base CSS |
| `a.skip-link` + `main#main` | Skip repeated blocks (RGAA 12.7) |
| `header` > `div` > brand `a` + `nav aria-label="Main"` > `ul` > `li` > `a` | Banner + navigation landmarks (RGAA 9.2, 9.3, 12.2) |
| `footer` > `div` > `p` > `a` | Contentinfo landmark |

Per-page shell details: `aria-current="page"` sits on the **Sign up** nav link,
and `<main>` carries the `site-main--narrow` modifier for the single-column form.

```html
<li><a class="nav__link" href="register.html" aria-current="page">Sign up</a></li>
```

---

## 2. The error alert

```html
<!-- Mockup state: submission came back with one field error.
     The alert summarizes; the field error is tied to its input
     with aria-describedby, exactly as the Thymeleaf template will. -->
<p class="alert alert--error reveal" role="alert">
  Your registration could not be completed. Check the highlighted field below.
</p>
```

- **The comment** documents the frozen application state and states the design
  intent: a page-level summary plus a field-level message wired to its input.
- **`role="alert"`.** The assertive live-region role (equivalent to
  `aria-live="assertive"` + `aria-atomic="true"`). When such an element appears,
  screen readers interrupt what they are saying to announce it. That interruption
  is justified here and *only* here among the mockups: the user's task has
  **failed** and nothing else matters until they know it. Compare `role="status"`
  on the login page's success message, which politely waits its turn. Choosing
  between the two is a judgment call the DWWM jury can ask about: **alert =
  blocking problem, status = FYI**.
- **Two-level error pattern.** The alert is the *summary* ("something failed,
  look below"); it deliberately does not repeat the specific message. The
  specifics live next to the field (`#email-error`), where the user will fix
  them. Summary at the top + detail at the field is the classic accessible
  server-side validation pattern.
- **Color independence.** The red styling comes from `alert--error`, but the
  *text* alone carries the full meaning, satisfying RGAA **3.1** (information is
  not conveyed by color alone).
- **Position.** First child of `<main>`, before the form card: first in reading
  order, matching its urgency.

---

## 3. The form card and heading

```html
<div class="form-card reveal reveal--2">
  <h1 class="form-card__title">Create an account</h1>
  <form action="#" method="post" novalidate>...</form>
  <p class="form__footer">
    Already registered? <a href="login.html">Log in</a>.
  </p>
</div>
```

Same trio as the login page: a presentational `<div>` card, the page's single
`<h1>` naming the task (RGAA **9.1**), and a footer `<p>` whose `<a>` links to
the opposite auth flow (link because it navigates; RGAA **6.1**).

`<form action="#" method="post" novalidate>` is explained fully in
[login-html-explained.md](login-html-explained.md). The short version: POST keeps
credentials out of URLs; `novalidate` hands validation to the server (Bean
Validation in Spring) so errors come back as the styled, persistent, accessible
messages this very page demonstrates; `action="#"` is the mockup placeholder for
the future `th:action`, which will also inject the CSRF token.

---

## 4. The field rows: hint, error, and the id wiring

This is the heart of the page. Three `div.form__group` rows, three variations of
one pattern.

### The healthy field with a hint (username)

```html
<div class="form__group">
  <label class="form__label" for="username">Username</label>
  <span class="form__hint" id="username-hint">3 to 50 characters.</span>
  <input class="form__input" type="text" id="username" name="username"
         autocomplete="username" aria-describedby="username-hint"
         value="carol" required>
</div>
```

- **`<label for="username">`.** The accessible *name* of the field, via the
  `for`/`id` pair (RGAA **11.1**). Name answers "what is this field?".
- **`<span class="form__hint" id="username-hint">`.** The format constraint,
  visible to everyone at all times (not hidden in a `placeholder` or a `title`
  tooltip). It is a `<span>` because it is a short inline annotation attached to
  a control, not standalone prose; what makes it functional is its `id`.
- **`aria-describedby="username-hint"`.** The input points at the hint's id. This
  makes the hint the field's accessible **description**: after announcing
  "Username, edit text, required", a screen reader adds "3 to 50 characters."
  Name (label) answers *what*, description (describedby) answers *how*.
  `aria-describedby` accepts a **space-separated list of ids**, so a field can
  accumulate a hint and an error at once if needed.
- **`value="carol"`.** The mockup replays the server round trip faithfully: after
  a failed submission, the server re-renders the form **with the previously
  entered values**, so the user only fixes the broken field instead of retyping
  everything. Losing user input on error is a classic usability failure the
  Thymeleaf template (via `th:field`) will avoid; the mockup shows the target
  state.
- `type`, `id`, `name`, `autocomplete="username"`, `required` are as on the login
  page (see [login-html-explained.md](login-html-explained.md)); `required` also
  supports RGAA **11.10** (mandatory fields are indicated).

### The field in error (email)

```html
<div class="form__group">
  <label class="form__label" for="email">Email</label>
  <input class="form__input form__input--invalid" type="email" id="email" name="email"
         autocomplete="email" aria-describedby="email-error" aria-invalid="true"
         value="carol@example.org" required>
  <span class="form__error" id="email-error">Email already registered</span>
</div>
```

Every attribute earns its place:

- **`type="email"`.** Semantically an email field: mobile keyboards show the `@`
  key, and browsers *would* check the shape natively if `novalidate` were absent.
  Kept even with `novalidate` because the semantics (and the keyboard layout)
  survive; only the native bubbles are off.
- **`class="form__input form__input--invalid"`.** The BEM modifier paints the red
  border. Visual channel only; the machine-readable channel is the next
  attribute.
- **`aria-invalid="true"`.** The standardized way to flag a control as failing
  validation. Screen readers announce the field as "invalid entry". The CSS
  could even target `[aria-invalid="true"]` instead of the modifier class so the
  two channels can never disagree. Only set it when the field is *actually*
  invalid after a submission; never ship `aria-invalid="true"` on pristine
  fields.
- **`aria-describedby="email-error"`.** Points at the error `<span>` below, so
  the *reason* travels with the field: focus the input and hear "Email, edit
  text, invalid entry, required... Email already registered". Without this link,
  a screen reader user tabbing straight to the field would know it is invalid
  but not **why**, and would have to hunt around the page for the reason.
- **`value="carol@example.org"`.** The rejected value is preserved and shown, so
  the user can see what the server refused.
- **`<span class="form__error" id="email-error">`.** The visible error text. Its
  `id` is its whole reason for being findable; its position right after the
  input mirrors the reading order a sighted user follows. The message states
  the actual problem ("Email already registered"), not a generic "invalid
  field", satisfying RGAA **11.11** (error messages describe the error and,
  where possible, how to fix it). Because it is plain visible text (not only a
  red border or an icon), RGAA **3.1** holds too.

**Why not the alternatives.**
- *Native validation bubbles* (drop `novalidate`): cannot express server-only
  rules like "already registered", vanish after a moment, and are unstylable.
- *An `alert()` or toast*: disappears, is disconnected from the field, and
  leaves nothing for `aria-describedby` to point at.
- *Error text inside the `<label>`*: pollutes the field's *name*; name and
  description have distinct jobs and the ARIA attributes keep them separate.

### The new-password field with a hint (password)

```html
<div class="form__group">
  <label class="form__label" for="password">Password</label>
  <span class="form__hint" id="password-hint">At least 12 characters.</span>
  <input class="form__input" type="password" id="password" name="password"
         autocomplete="new-password" aria-describedby="password-hint" required>
</div>
```

- Same hint pattern as username. Note the password field has **no `value`**: the
  server must never echo a password back into the page source, even after a
  failed submission. The user retypes it; that asymmetry with the other two
  fields is deliberate and worth being able to explain.
- **`autocomplete="new-password"`.** The register-page counterpart of the login
  page's `current-password`. It tells password managers "do not fill the stored
  password here; this is where a **new** one is created", which triggers their
  password *generator* instead. Getting this pair right
  (`new-password` on register and password-change forms, `current-password` on
  login) is exactly what RGAA **11.13** (input purpose can be deduced) checks.

### The id wiring, in one diagram

Three independent id links make this form work. Arrows point from the attribute
to the element it references:

```text
label[for="username"] -----------> input#username
                                     |  aria-describedby="username-hint"
                                     +---------------> span#username-hint  ("3 to 50 characters.")

label[for="email"] --------------> input#email  [aria-invalid="true"]
                                     |  aria-describedby="email-error"
                                     +---------------> span#email-error    ("Email already registered")

label[for="password"] -----------> input#password
                                     |  aria-describedby="password-hint"
                                     +---------------> span#password-hint  ("At least 12 characters.")
```

Rules of the mechanism:

- `for` gives the input its **name**; `aria-describedby` gives it a
  **description**. Both resolve by document-wide unique `id`.
- The hint/error element does not know it is referenced; all wiring lives on the
  input. Reordering the DOM does not break it; renaming an `id` without updating
  its references does. In the Thymeleaf template these ids will be generated
  from the field names, keeping them in sync mechanically.
- When the email field becomes valid again, the server flips three things at
  once: drop `aria-invalid`, drop (or retarget) `aria-describedby`, remove the
  error `<span>`. State and message always move together.

### `<button type="submit">`

```html
<button class="button button--primary" type="submit">Create my account</button>
```

As on the login page: a real `<button>` because submitting is an action, with an
explicit `type="submit"` and a label that names the outcome ("Create my
account") rather than a vague "Submit".

---

## 5. Recap table

| Tag | Implicit ARIA role | Key attributes here | Works with | Main purpose on this page |
|---|---|---|---|---|
| shell tags | see home-html-explained.md | `aria-current="page"` on Sign up | shell | Shared frame |
| `main` | main | `id="main"`, `site-main--narrow` | skip link | Narrow task page |
| `p.alert` | alert (via `role`) | `role="alert"` | field error below | Assertive failure summary |
| `div.form-card` | none | `class` | h1, form, p | Visual card |
| `h1` | heading level 1 | `class` | outline | Names the task (RGAA 9.1) |
| `form` | none (unnamed) | `method="post"`, `novalidate`, `action="#"` | fields, button | Server-validated POST |
| `div.form__group` | none | `class` | label + hint/error + input | One field row |
| `label` | n/a | `for` = input `id` | input | Accessible name (RGAA 11.1) |
| `span.form__hint` | none | `id="...-hint"` | input `aria-describedby` | Format help as description |
| `span.form__error` | none | `id="email-error"` | input `aria-describedby` | Specific error text (RGAA 11.11, 3.1) |
| `input` | textbox | `type`, `id`, `name`, `autocomplete`, `required`, `value`, `aria-describedby`, `aria-invalid` | label, hint/error spans | The error-state pattern (RGAA 11.10, 11.13) |
| `button` | button | `type="submit"` | form | Submit action |
| `p.form__footer` + `a` | paragraph / link | `href="login.html"` | login page | Cross-link to the other auth flow |
