# Header Username Implementation Plan

**Goal:** Show the signed-in username in the site header (issue #126). Authenticated users currently see Dashboard, Courses, and Log out, but nothing tells them which account they are acting as; the username only appears in the dashboard greeting. This matters when switching between the student, instructor, and admin roles.

**Approach:** Reuse the existing display pattern from the dashboard greeting (`sec:authentication="name"`, Thymeleaf Spring Security extras already imported by the layout fragment). The username is user-typed data, so it is rendered escaped (inherent to the attribute processor) and truncated with CSS so it can never distort or spoof the header chrome. No ADR: this is a presentation change, no architectural decision involved.

**Out of scope:** a profile page or menu behind the username, and any change to what identifies a user (the `username` column stays the display name).

---

## Version Control (GitButler)

- Commit with `but commit feat/header-username -m "<msg>"` from the main repository.
- **NEVER push.** The user reviews in GitButler and pushes manually.

---

## Tasks

- [ ] `docs(plan): Add the header username plan` (this document)
- [ ] `feat(frontend): Show the signed-in username in the header` (closes #126)
  - `fragments/layout.html`, authenticated `ul.nav__list`, before the Log out control:
    `<li class="nav__user"><span class="visually-hidden">Signed in as </span><span class="nav__user-name" sec:authentication="name">username</span></li>`
    (the `.visually-hidden` utility already exists in `base.css`; screen readers get the sentence, sighted users just see the name)
  - `base.css`: `.nav__user` as non-interactive chrome (`color: var(--color-text-muted)`, `font-size: var(--font-size-sm)`); `.nav__user-name` with `max-width: 12rem`, `overflow: hidden`, `text-overflow: ellipsis`, `white-space: nowrap`, `display: inline-block` so a 50-character username (the database cap) truncates instead of wrapping the header
  - MockMvc assertions: an authenticated page renders "Signed in as" plus the principal's username in the header; an anonymous page contains no `nav__user`
- [ ] `fix(frontend): Separate the account group from the nav and wrap on mobile`
  (review feedback, issue #126 reopened: the username read as just another
  nav item, and `.nav__list` never wrapped, so the 12rem username pushed
  the Log out button off-screen on narrow viewports; the username and Log
  out move out of the nav into `div.site-header__account` behind a border
  divider, `.nav__list` gains `flex-wrap: wrap`, and the username width
  caps at `40vw` under the 46rem breakpoint)

## Verification

- Full test suite and Checkstyle green.
- Browser pane, both themes: username visible next to Log out, muted; axe (WCAG 2.1 A/AA) reports no new violations; a 50-character username truncates with an ellipsis and causes no horizontal scroll.
- Demo data cleaned up afterwards; nothing pushed.
