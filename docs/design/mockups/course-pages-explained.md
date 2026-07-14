# The course and instructor mockups, explained

Unlike the original four mockups (which preceded the Thymeleaf templates),
these nine pages were added *after* the course UI shipped: they are the
shipped templates frozen as static HTML with sample data, so the prototype
stays a faithful, browsable snapshot of the real interface. The sync happens
at stable UI milestones only, not per merged pull request.

What each page snapshots, and the one thing worth noticing on it:

- **courses.html** (student catalogue): each `course-card` carries the
  "You are enrolled" marker inside `course-card__meta`, the state the
  controller computes per card.
- **course.html** (course detail): shown in the just-enrolled state, so the
  success alert (`role="status"`) and the "Quit this course" form are both
  visible. Enroll/quit are POST forms, never links: they change state.
- **lesson.html**: the `<article>` holds what `MarkdownRenderer` outputs.
  The instructor authored `# Why lists matter`, but it arrives as an `h2`:
  headings are demoted one level at render time so the page keeps exactly
  one `h1` (RGAA 9.1, [ADR-0014](../../adr/0014-demote-markdown-headings-in-lesson-rendering.md)).
- **dashboard.html** (updated): the earlier stats-and-progress concept is
  gone; the shipped dashboard lists the student's active enrollments with
  status and enrollment date. Stats may return later; the mockup tracks what
  is real.
- **instructor-courses.html**: the authoring entry point lists all statuses
  (Draft, Published, Archived), unlike the student catalogue.
- **instructor-course-editor.html**: one page hosts the edit form, the
  publication lifecycle actions (only the transitions the current status
  allows are rendered), and lesson management. The per-lesson buttons carry a
  `visually-hidden` suffix ("Move up: Loops without tears") so a screen
  reader hears which lesson each button acts on, while sighted users read
  the row.
- **instructor-lesson-editor.html**: `textarea.form__input` is the Markdown
  editor; the hint reminds authors that output is sanitized (ADR-0013).
- **instructor-students.html**: the roster uses the `.table` block with a
  `visually-hidden` caption; the Remove action only renders for enrollments
  that can still be dropped.
- **error.html**: the styled 404 that replaced the Whitelabel page; 403 and
  500 use the same skeleton with different copy.

Shared wiring on every page, inherited from the app templates: skip link
targeting `main#main` with `tabindex="-1"` (explicit focus move), one `h1`,
`aria-current="page"` on the active nav entry, and role-aware nav (the
Instructor entry appears only on instructor pages, where an instructor is
logged in).
