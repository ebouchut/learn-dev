# Course Management by Role Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the web layer (controllers, services, templates, security rules) that lets each role use the shipped course domain: **students** browse, register for, read, and quit courses; **instructors** author courses and lessons, archive them, and handle their students; **admins** create instructor accounts, archive accounts, and archive any course or lesson.

**Architecture:** Everything sits on the existing domain (PR #88 lifecycles, PR #89 schema + entities, PR #90 Markdown rendering). Feature-based packages: the web layer goes into the existing `course` package plus a new `admin` package. Role-gated URL prefixes (`/instructor/**`, `/admin/**`) follow the CONTRIBUTING rule that a feature package owns its URL prefix. Server-rendered Thymeleaf pages reuse the shared layout fragments and the RGAA wiring documented in [docs/rgaa.md](../rgaa.md).

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Security (session auth per [ADR-0001](../adr/0001-use-server-side-sessions-over-jwt.md)), Spring Data JPA, Thymeleaf, commonmark-java + jsoup (per [ADR-0013](../adr/0013-render-markdown-with-commonmark-and-jsoup.md)), Testcontainers PostgreSQL.

**Out of scope:** lesson-level progress tracking (course-level `EnrollmentStatus` only), exercises, MongoDB content storage, `SUPERADMIN` (issue #65), instructor self-registration (admin creates instructor accounts), destructive deletes (archiving only, by design).

---

## Version Control (GitButler)

This repository is on the `gitbutler/workspace` branch, so **use GitButler (`but`), not raw `git`** (per CLAUDE.md):

- Execute every commit step as `but commit <branch> -m "<msg>"` (with `-c` to create the branch). Do **not** run `git add` / `git commit`.
- **NEVER push** — no `but push`, no `git push`. The user reviews in GitButler and pushes manually.
- Implement as **stacked branches**, one per phase below, in dependency order:
  1. `feat/course-security-foundation` (security rules + shared service seams)
  2. `feat/student-course-catalog` (stacked on 1)
  3. `feat/instructor-course-authoring` (stacked on 2)
  4. `feat/admin-account-management` (stacked on 3)
- One atomic commit per task; tests land in the same commit as the code they prove.

---

## Data model impact: none

The Merise models (MCD, MLD, [MPD](../database/merise/mpd/README.md)) and the Liquibase schema already cover every capability below; **no migration and no diagram regeneration is needed**. Verified mapping:

| Capability | Existing schema support |
|---|---|
| Instructor authors courses/lessons | `courses.user_id` (instructor FK), `lessons.course_id`, `position` |
| Archive a course or lesson | `status` column, `PublicationStatus.ARCHIVED` (no destructive delete in v1) |
| Student registers for a course | `enrollments(user_id, course_id)` composite PK, `enrolled_at`, `EnrollmentStatus.ENROLLED` |
| Student quits a course | `EnrollmentStatus.DROPPED` + `dropped_at` |
| Student reads lessons | `lessons.content_markdown` rendered by `MarkdownRenderer` (sanitized, cached) |
| Instructor handles students | `enrollments` roster per course; status transitions |
| Admin creates instructor accounts | `users` + `user_roles` with seeded `INSTRUCTOR` role |
| Admin archives accounts | `users.is_active` (already mapped to Spring Security `disabled`) |
| Role gates | Seeded `STUDENT` / `INSTRUCTOR` / `ADMIN` roles, `ROLE_*` authorities |

The lifecycle state diagrams in [CONTRIBUTING.md](../../CONTRIBUTING.md#domain-lifecycles) remain the reference for allowed transitions; services below must enforce them.

---

## Phase 1: Security foundation (`feat/course-security-foundation`)

- [ ] Extend `SecurityConfig`: `/courses/**` requires authentication (catalogue is for logged-in users; revisit if the catalogue goes public), `/instructor/**` requires `ROLE_INSTRUCTOR`, `/admin/**` requires `ROLE_ADMIN`; enable method security for ownership checks.
- [ ] Add a `403` error page (`error/403.html`) using the shared layout, with the RGAA wiring (`<main id="main" tabindex="-1">`, one `h1`).
- [ ] Role-aware navigation in `fragments/layout.html`: "Instructor" and "Admin" nav entries rendered with `sec:authorize`, `aria-current` wiring as on existing entries.
- [ ] Tests: MockMvc security-rule tests (anonymous, student, instructor, admin access matrices for the three prefixes).
- [ ] Commit: `feat(security): Gate course, instructor, and admin URL prefixes by role`

## Phase 2: Student experience (`feat/student-course-catalog`)

Routes owned by the `course` package.

- [ ] `CourseService` (read side): published-course catalogue, course detail with ordered published lessons, visibility rule (students see `PUBLISHED` only; an enrolled student keeps read access if the course is later archived — decide and document in the service Javadoc).
- [ ] `EnrollmentService`: `enroll(user, course)` (idempotent, re-activates a `DROPPED` row per the lifecycle diagram), `drop(user, course)` (sets `DROPPED` + `dropped_at`), `myCourses(user)`.
- [ ] `CourseController`:
  - [ ] `GET /courses` — catalogue of published courses with enrollment state per card.
  - [ ] `GET /courses/{courseId}` — detail: description, lesson list, Register / Quit button per state.
  - [ ] `POST /courses/{courseId}/enroll` — register, redirect back with a flash confirmation.
  - [ ] `POST /courses/{courseId}/drop` — quit, POST + confirmation (never a GET; state change).
  - [ ] `GET /courses/{courseId}/lessons/{lessonId}` — lesson page, `MarkdownRenderer` output in the template (`th:utext` on the already-sanitized HTML), enrolled students only, prev/next lesson links by `position`.
- [ ] Templates: `courses/catalog.html`, `courses/detail.html`, `courses/lesson.html` — shared layout fragments, RGAA criteria from [docs/rgaa.md](../rgaa.md) (headings order, explicit links, `main#main` with `tabindex="-1"`).
- [ ] Dashboard: replace the placeholder content with "My courses" (status per course, continue/quit actions).
- [ ] Tests: unit (services, lifecycle transitions incl. re-enroll after drop), slice (repository queries for catalogue and roster), MockMvc journey (browse, enroll, read lesson, drop).
- [ ] Commits (one per bullet group): `feat(course): Add the published course catalogue`, `feat(course): Let students enroll in and drop a course`, `feat(course): Render lessons for enrolled students`, `feat(dashboard): List the student's courses`

## Phase 3: Instructor experience (`feat/instructor-course-authoring`)

Routes under `/instructor/**`, owned by the `course` package (instructor side of the same feature).

- [ ] `CourseService` (write side): create (starts `DRAFT`), update, `publish` (sets `published_at`), `archive`; ownership rule: an instructor manages only their own courses (`@PreAuthorize` or service-level check; admin bypass in Phase 4).
- [ ] `LessonService`: create/update lessons, publish, archive, reorder (`position` swap within a course).
- [ ] `InstructorCourseController`:
  - [ ] `GET /instructor/courses` — the instructor's courses, all statuses.
  - [ ] `GET/POST /instructor/courses/new` — create form (Bean Validation on a `CourseForm`).
  - [ ] `GET/POST /instructor/courses/{courseId}/edit` — edit form.
  - [ ] `POST /instructor/courses/{courseId}/publish` and `POST .../archive` — lifecycle actions with confirmation flash.
  - [ ] `GET/POST /instructor/courses/{courseId}/lessons/new`, `GET/POST .../lessons/{lessonId}/edit`, `POST .../lessons/{lessonId}/publish|archive`, `POST .../lessons/{lessonId}/move-up|move-down`.
- [ ] Handle students: `GET /instructor/courses/{courseId}/students` — roster (username, status, enrolled/completed/dropped dates); `POST .../students/{userId}/drop` — remove a student from the course (sets `DROPPED`; audit-logged).
- [ ] Templates: `instructor/courses.html`, `instructor/course-form.html`, `instructor/lesson-form.html`, `instructor/students.html` — error wiring as on the register form (`aria-invalid`, `aria-describedby`, `role="alert"` summary).
- [ ] Audit: record publish/archive/roster-drop actions through the existing `AuditService`.
- [ ] Tests: unit (ownership + lifecycle guards: no publish from `ARCHIVED`, etc.), MockMvc (authoring journey; a second instructor gets 403 on someone else's course).
- [ ] Commits: `feat(course): Let instructors author and publish courses`, `feat(course): Let instructors author, order, and archive lessons`, `feat(course): Give instructors a per-course student roster`

## Phase 4: Admin experience (`feat/admin-account-management`)

New `admin` package owning `/admin/**`.

- [ ] `AccountAdminService`: create an instructor account (unique username/email checks reuse `RegistrationService` seams, assigns `INSTRUCTOR` role, BCrypt password; initial password delivered via the existing mail seam or a forced first-login reset — pick one and record it in the commit message), archive (`is_active = false`) and reactivate an account; guard: an admin cannot archive their own account.
- [ ] Content moderation: archive/reactivate **any** course or lesson (reuses Phase 3 services with an admin bypass of the ownership rule).
- [ ] `AdminController`:
  - [ ] `GET /admin/users` — accounts list with role and active flags.
  - [ ] `GET/POST /admin/users/new-instructor` — creation form.
  - [ ] `POST /admin/users/{userId}/archive` and `POST .../reactivate`.
  - [ ] `GET /admin/courses` — all courses, any status, any instructor; `POST /admin/courses/{courseId}/archive`; `POST /admin/courses/{courseId}/lessons/{lessonId}/archive`.
- [ ] Audit: record account creation, account archive/reactivate, and admin content archiving through `AuditService`.
- [ ] Templates: `admin/users.html`, `admin/user-form.html`, `admin/courses.html`.
- [ ] Tests: unit (self-archive guard, role assignment), MockMvc (admin journey; instructor gets 403 on `/admin/**`; an archived account can no longer log in — `disabled` already maps from `is_active`).
- [ ] Commits: `feat(admin): Let admins create instructor accounts`, `feat(admin): Let admins archive and reactivate accounts`, `feat(admin): Let admins archive any course or lesson`

## Cross-cutting checklist (every phase)

- [ ] RGAA: run the axe/Lighthouse commands from [docs/rgaa-audit.md](../rgaa-audit.md) on each new page before the phase's PR.
- [ ] Checkstyle passes (`./mvnw checkstyle:check`).
- [ ] Update [GLOSSARY.md](../../GLOSSARY.md)/[GLOSSAIRE.md](../../GLOSSAIRE.md) together if a phase introduces a new domain term (glossary sync rule).
- [ ] No schema change expected; if one becomes necessary, stop and update the Merise MCD source + regenerate MLD/MPD first (schema-drift CI enforces this).
