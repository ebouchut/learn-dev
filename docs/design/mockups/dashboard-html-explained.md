# dashboard.html, tag by tag

An educational walkthrough of `dashboard.html`, the authenticated home of the
learn-dev mockups. The mockup freezes the state of a logged-in user ("carol")
with three enrolled courses. Two elements deserve special attention on this
page and get their own sections: the **`<progress>` element** and the **logout
POST form**.

The document shell (doctype, `html lang`, `head` metadata, font and stylesheet
`link`s, skip link, `header`, `footer`) is identical to the home page and is
explained in depth in [home-html-explained.md](home-html-explained.md).
Section 1 lists it and covers the one real difference: the header now belongs
to an authenticated context.

---

## 1. Shared shell, with an authenticated twist

| Tag | Reminder |
|---|---|
| `<!DOCTYPE html>`, `html lang="en"` | Standards mode; page language (RGAA 8.3) |
| `meta charset` / `meta viewport` | UTF-8; responsive viewport |
| `title` | `Dashboard ... learn-dev` (RGAA 8.5) |
| `link` preconnect / stylesheet | Font-origin warm-up; theme then base CSS |
| `a.skip-link` + `main#main` | Skip repeated blocks (RGAA 12.7) |
| `header` > `div` > brand `a` + `nav aria-label="Main"` | Banner + navigation landmarks (RGAA 9.2, 12.2) |
| `footer` > `div` > `p` | Contentinfo landmark |

The nav content changes because the user is signed in: the public Home / Log in /
Sign up entries are replaced by Dashboard (current page) and a Log out control.

```html
<ul class="nav__list">
  <li><a class="nav__link" href="dashboard.html" aria-current="page">Dashboard</a></li>
  <li>
    <!-- Logout is a POST in the real app (CSRF); styled as a ghost button -->
    <form action="#" method="post">
      <button class="button button--ghost" type="submit">Log out</button>
    </form>
  </li>
</ul>
```

`aria-current="page"` marks Dashboard as the current location, as on every page
(see [home-html-explained.md](home-html-explained.md)). The second `<li>` is the
interesting one.

---

## 2. The logout POST form (why not a plain link)

```html
<form action="#" method="post">
  <button class="button button--ghost" type="submit">Log out</button>
</form>
```

Visually this is one ghost button in the menu; structurally it is a **whole form
whose only control is its submit button**. That is deliberate, and being able to
justify it is worth real points in a DWWM security discussion.

- **Logout changes server state.** It invalidates the session. The HTTP
  semantics are clear: **GET must be safe** (no state change, cacheable,
  prefetchable, repeatable); state changes belong to **POST** (or other unsafe
  methods). Logout is a state change, so it is a POST.
- **Prefetch danger of a GET logout.** Browsers, link-prefetching extensions,
  proxies, and crawlers feel free to *pre-fetch* GET links to speed up
  navigation, precisely because GET promises to be safe. A `<a href="/logout">`
  can therefore be silently fetched the moment the page renders, logging the
  user out without any click. This is not theoretical; accelerator extensions
  and some browser preloading features have caused exactly this bug in
  production apps.
- **CSRF.** A GET logout is trivially forgeable from any third-party page
  (`<img src="https://app.example/logout">` suffices) to log users out against
  their will (a nuisance form of Cross-Site Request Forgery). A POST protected
  by a CSRF token is not. In the real app, Thymeleaf's `th:action` on this form
  makes Spring Security inject a hidden `_csrf` input automatically, and Spring
  Security's `LogoutFilter` **only accepts POST** for `/logout` when CSRF
  protection is on (its default). The mockup's `action="#"` is the placeholder
  for that.
- **`<button>` not `<a>`.** The rule from the other pages, applied in reverse:
  logging out is an *action*, not a navigation, so it is a `<button
  type="submit">` inside a form. It is styled with the shared
  `button button--ghost` classes so it sits quietly in the menu, proof that the
  visual layer and the element choice are independent decisions. A JavaScript
  `onclick` on a link could send a POST too, but would break without JS and
  misrepresent the control's role to assistive technology.
- **Position.** The form lives inside the nav's `<li>`, keeping the menu a
  proper list (RGAA **9.3**) even though one entry is a form rather than a link.

---

## 3. Page heading and intro

```html
<h1 class="page-title reveal">Welcome back, <strong>carol</strong> 👋</h1>
<p class="reveal reveal--2">Here is where you left off.</p>
```

- **`<h1>`.** The unique top-level heading (RGAA **9.1**), personalized with the
  username the server will interpolate.
- **`<strong>`.** Genuine strong importance, not just bold styling: the username
  is the key information in the sentence (whose dashboard is this?). Screen
  readers may convey the emphasis. A `<span class="bold">` would style the same
  but carry no meaning; `<b>` is the styling-only fallback. When the weight *is*
  the message, `<strong>` is right.
- The waving-hand emoji sits in plain text here (unlike the decorative icons on
  the home page, which are wrapped in `aria-hidden` spans); it will be read by
  screen readers as "waving hand". Defensible either way; hiding it would be the
  stricter choice.

---

## 4. The stats strip

```html
<h2 class="visually-hidden" id="stats-title">Your numbers</h2>
<ul class="stats reveal reveal--2" aria-labelledby="stats-title">
  <li class="stat-card">
    <span class="stat-card__value">3</span>
    <span class="stat-card__label">Courses enrolled</span>
  </li>
  ...
</ul>
```

- **`<h2 class="visually-hidden">`.** The stat cards are visually
  self-explanatory, so a visible "Your numbers" heading would be clutter; but
  removing the heading entirely would leave a hole in the heading outline and an
  unnamed list. The compromise: keep the heading in the DOM, hide it with the
  `visually-hidden` CSS technique (clip/1px trick), which hides it from sighted
  users but **keeps it in the accessibility tree**. This is the exact opposite
  tool to `aria-hidden="true"` (visible but not announced): visually-hidden is
  *invisible but announced*. Screen reader users get the outline entry and can
  jump to the stats by heading (RGAA **9.1**).
- **`aria-labelledby="stats-title"` on the `<ul>`.** Names the list after the
  hidden heading, so it is announced as "Your numbers, list, 3 items". Same
  naming-by-reference pattern as the `<section>` elements
  (see [home-html-explained.md](home-html-explained.md)).
- **`<ul>`/`<li>`.** Three cards of the same kind = a list (RGAA **9.3**),
  giving assistive tech the "3 items" count for free.
- **The two `<span>`s.** Value and label are inline, meaning-free wrappers
  stacked by CSS. Reading order in the DOM is value then label ("3, Courses
  enrolled"), which reads naturally. Neither is a heading: "Courses enrolled" is
  a *caption of a number*, not a section title, and making every card label a
  heading would pollute the outline.

---

## 5. The courses section

```html
<section aria-labelledby="courses-title" class="reveal reveal--3">
  <h2 id="courses-title">Your courses</h2>
  <ul class="course-list">
    <li class="course-card">
      <h3 class="course-card__title">Java fundamentals</h3>
      <p class="course-card__meta">12 of 16 lessons completed</p>
      <progress class="course-card__progress" max="16" value="12"
                aria-label="Java fundamentals progress: 12 of 16 lessons"></progress>
    </li>
    ...
  </ul>
</section>
```

- **`<section aria-labelledby="courses-title">` + `<h2 id="courses-title">`.**
  The named-region pattern: the section becomes a `region` landmark named "Your
  courses" by its own visible heading (RGAA **9.2**, **9.1**). Full discussion
  in [home-html-explained.md](home-html-explained.md).
- **`<ul class="course-list">` / `<li class="course-card">`.** A list of course
  cards (RGAA **9.3**), same reasoning as the features grid on the home page.
- **`<h3>`.** Each course name is a third-level heading under the "Your courses"
  h2: the outline stays strictly nested (h1 > h2 > h3) and a screen reader user
  can jump course to course by heading.
- **`<p class="course-card__meta">`.** The human-readable progress sentence
  ("12 of 16 lessons completed"). Note that the information exists **as text**
  before the graphical bar even appears; the bar could disappear and nothing
  would be lost. That redundancy is the backbone of the accessibility story here
  (RGAA **3.1**: never by presentation alone).

### `<progress>`, in depth

```html
<progress class="course-card__progress" max="16" value="12"
          aria-label="Java fundamentals progress: 12 of 16 lessons"></progress>
```

- **Role.** The native HTML element for *task completion*. It maps to the ARIA
  **`progressbar` role automatically**, and, crucially, it derives and exposes
  the ARIA value properties from its attributes with zero extra work:
  `aria-valuemin` (0), `aria-valuemax` (from `max`), `aria-valuenow` (from
  `value`). A screen reader announces something like "progress bar, 75%". One
  element, correct semantics, no ARIA bookkeeping.
- **`max="16"` and `value="12"`.**
  - `max`: the total amount of work, a positive float; here the number of
    lessons in the course. Defaults to 1 if omitted.
  - `value`: the completed amount, between 0 and `max`.
  - The pair expresses the real domain numbers (12 of 16) rather than a
    pre-chewed percentage (`max="100" value="75"`). Both are valid; keeping the
    domain values means the markup states the truth and the browser does the
    division. In the Thymeleaf template these become
    `th:attr="max=${course.totalLessons}, value=${course.completedLessons}"` or
    equivalent.
  - Omitting `value` entirely would switch the element to its
    **indeterminate** state (animated "busy" bar, no `aria-valuenow`), which is
    for unknown durations, not for known fractions like these.
- **`aria-label="Java fundamentals progress: 12 of 16 lessons"`.** A
  progressbar exposes *values* but has no inherent *name*: three bare bars in a
  row would each announce only "progress bar, 75%", "64%", "33%", leaving a
  screen reader user to guess which course each belongs to. The `aria-label`
  gives each bar a self-contained accessible name that repeats the course and
  the raw numbers. Each of the three bars has its own label naming its own
  course. An alternative wiring would be `aria-labelledby` pointing at the
  card's `<h3>` id; the literal label was chosen so the announcement includes
  the exact lesson counts without depending on reading order.
- **Empty content.** The element has no fallback content between its tags;
  every current browser renders `<progress>` natively, and the adjacent `<p>`
  already carries the text version.
- **Position and partners.** Inside the course `<li>`, after the `<h3>` (which
  identifies the course) and the meta `<p>` (which states the numbers in
  prose). The three express the same fact through three channels: outline,
  text, and graphic/ARIA values.
- **Why not the alternative (`div` bar).** The ubiquitous
  `<div class="bar"><div class="fill" style="width:75%"></div></div>` is, to
  assistive technology, **two empty boxes**: no role, no values, no name. To
  fix it you must hand-write `role="progressbar"`, `aria-valuemin`,
  `aria-valuemax`, `aria-valuenow`, and a name, then keep `aria-valuenow` in
  sync with the width forever. The native element does all of that by existing.
  The honest tradeoff: `<progress>` is harder to *style* consistently across
  browsers (it needs vendor pseudo-elements like `::-webkit-progress-value`),
  which is why so many sites reach for divs; these mockups accept the styling
  cost to keep the semantics. Also worth distinguishing: `<meter>` looks
  similar but means a *measurement within a known range* (disk usage, score),
  not *progress toward completing a task*; course completion is task progress,
  hence `<progress>`.

---

## 6. Recap table

| Tag | Implicit ARIA role | Key attributes here | Works with | Main purpose on this page |
|---|---|---|---|---|
| shell tags | see home-html-explained.md | `aria-current="page"` on Dashboard | shell | Shared frame, authenticated nav |
| `form` (in nav) | none | `action="#"`, `method="post"` | its submit button | Logout as state-changing POST (CSRF, no prefetch) |
| `button` | button | `type="submit"` | logout form | The logout action (button = act, link = navigate) |
| `main` | main | `id="main"` | skip link | Unique content landmark |
| `h1` | heading level 1 | `class` | outline | Personalized page title (RGAA 9.1) |
| `strong` | strong | none | h1 text | Genuine importance of the username |
| `h2.visually-hidden` | heading level 2 | `id="stats-title"`, hidden by CSS | ul `aria-labelledby` | Outline entry without visual clutter |
| `ul.stats` | list | `aria-labelledby="stats-title"` | hidden h2, li | Named list of stat cards (RGAA 9.3) |
| `li.stat-card` | listitem | `class` | spans | One statistic |
| `span` | none | `class` | stat cards | Value / label styling hooks |
| `section` | region (named) | `aria-labelledby="courses-title"` | h2 id | Courses landmark (RGAA 9.2) |
| `h2` / `h3` | heading 2 / 3 | `id` (h2) | section, cards | Outline: section then one h3 per course |
| `ul.course-list` / `li.course-card` | list / listitem | `class` | h3, p, progress | List of courses (RGAA 9.3) |
| `p.course-card__meta` | paragraph | `class` | progress | Progress as plain text (RGAA 3.1) |
| `progress` | progressbar | `max`, `value`, `aria-label` | h3, meta p | Native completion bar with derived ARIA values |
| `footer` | contentinfo | `class` | p | Site info landmark |
