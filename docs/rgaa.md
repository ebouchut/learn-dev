# RGAA: accessibility of learn-dev

How learn-dev addresses the RGAA, the French accessibility framework, with
the criteria the DWWM certification expects, how each one is fulfilled, and
where. Tracked by issue
[#84 RGAA (Accessibilité)](https://github.com/ebouchut/learn-dev/issues/84).

## What the RGAA is

The **RGAA** (Référentiel général d'amélioration de l'accessibilité,
version 4.x) is the official French method for evaluating web
accessibility. It is the French application of **WCAG 2.1 level AA**: the
same requirements, restated as **106 testable criteria grouped in 13
themes** (images, frames, colors, multimedia, tables, links, scripts,
mandatory elements, structure, presentation, forms, navigation,
consultation). Public-sector sites must comply by law (loi du 11 février
2005, décret 2019-768); for everyone else it is the reference quality bar.

## Why it matters for the DWWM

The REAC for the *Développeur Web et Web Mobile* title expects a certified
developer to **integrate accessibility recommendations into the interfaces
they build and to verify them with tools**. The jury does not expect a full
106-criteria audit of a capstone project; it expects the essentials to be
implemented by construction, the candidate to know *why* each one exists,
and a credible verification method. This document provides exactly that
trail.

## How accessibility is built in (by construction, not retrofitted)

- The design phase produced WCAG-checked tokens first: every text/background
  pair carries a computed contrast ratio in
  [design/theme-exploration.md](design/theme-exploration.md); light-mode
  accents that failed 4.5:1 were darkened before any page was styled.
- The HTML mockups ([design/mockups/](design/mockups/)) encode the
  accessibility wiring (landmarks, labels, error associations) that the
  Thymeleaf templates then reproduce with live data; the study docs
  ([design/mockups-explained.md](design/mockups-explained.md), 🇫🇷
  [design/mockups-explained-fr.md](design/mockups-explained-fr.md)) explain
  every choice.
- The shared chrome lives in one place
  ([templates/fragments/layout.html](../src/main/resources/templates/fragments/layout.html)),
  so the skip link, landmarks, and navigation semantics are correct on
  every page by definition.

## Criteria map

For each criterion: the requirement (official French wording summarized),
how learn-dev fulfills it, and where.

| RGAA | Requirement | How | Where |
|---|---|---|---|
| **1.2** | Chaque image de décoration est ignorée par les technologies d'assistance | `aria-hidden="true"` on the decorative code card and feature emoji | [home.html](../src/main/resources/templates/home.html) |
| **3.1** | L'information n'est pas donnée uniquement par la couleur | Errors: red border **and** bold message **and** `aria-invalid`; current nav item: color **and** bold **and** underline **and** `aria-current` | [register.html](../src/main/resources/templates/register.html), [fragments/layout.html](../src/main/resources/templates/fragments/layout.html), [base.css](../src/main/resources/static/css/base.css) |
| **3.2** | Contraste texte/fond >= 4.5:1 (3:1 en grande taille) | Every token pair computed by script; worst shipped pair 4.73:1 | [design/theme-exploration.md](design/theme-exploration.md), [theme-catppuccin.css](../src/main/resources/static/css/theme-catppuccin.css) |
| **6.1** | Chaque lien est explicite | Link texts meaningful out of context ("Create your account", "Privacy policy (RGPD)") | all templates |
| **8.3/8.4** | Langue par défaut présente et pertinente | `lang="en"` on pages; `lang="fr"` on the French policy | [privacy.html](../src/main/resources/templates/privacy.html) |
| **8.5/8.6** | Titre de page présent et pertinent | Unique, patterned `<title>` per page via the `head(title)` fragment | [fragments/layout.html](../src/main/resources/templates/fragments/layout.html) |
| **8.7** | Changements de langue indiqués | English chrome marked `lang="en"` inside the French page | [fragments/layout.html](../src/main/resources/templates/fragments/layout.html) |
| **9.1** | Information structurée par des titres pertinents | One `h1` per page describing the page (inside `main`), ordered `h2`/`h3`, no skipped level | all templates |
| **9.2** | Structure du document cohérente | `header`/`nav`/`main`/`footer` landmarks on every page | [fragments/layout.html](../src/main/resources/templates/fragments/layout.html) |
| **9.3** | Listes correctement structurées | Navigation and features as `ul`/`li` | [fragments/layout.html](../src/main/resources/templates/fragments/layout.html), [home.html](../src/main/resources/templates/home.html) |
| **10.4** | Texte agrandissable à 200% | All sizes in rem/em, unitless line-height 1.6 | [base.css](../src/main/resources/static/css/base.css) |
| **10.7** | Focus visible | Global `:focus-visible` 2px outline, never removed | [base.css](../src/main/resources/static/css/base.css) |
| **10.11** | Contenu consultable en reflow | rem breakpoint (46rem), `auto-fit` grids, internal overflow for the code card; verified at 375/768/1440 px | [base.css](../src/main/resources/static/css/base.css) |
| **11.1** | Chaque champ a une étiquette | `label for`/`id` on every input | [login.html](../src/main/resources/templates/login.html), [register.html](../src/main/resources/templates/register.html) |
| **11.10** | Contrôle de saisie pertinent | `type="email"`, `required`, server-side Bean Validation as source of truth | [register.html](../src/main/resources/templates/register.html), [RegisterForm](../src/main/java/com/ericbouchut/learndev/auth/dto/RegisterForm.java) |
| **11.11** | Erreurs identifiées avec suggestions | `role="alert"` summary + per-field `th:errors` message wired to its input with `aria-describedby` and `aria-invalid` | [register.html](../src/main/resources/templates/register.html) |
| **11.13** | Finalité des champs déductible | `autocomplete="username"/"email"/"new-password"/"current-password"` | [login.html](../src/main/resources/templates/login.html), [register.html](../src/main/resources/templates/register.html) |
| **12.7** | Lien d'évitement vers le contenu principal | Skip link, first focusable element, target `main#main`, visible on focus | [fragments/layout.html](../src/main/resources/templates/fragments/layout.html), [base.css](../src/main/resources/static/css/base.css) |
| **13.8** | Contenu en mouvement contrôlable | All animation disabled under `prefers-reduced-motion` | [base.css](../src/main/resources/static/css/base.css) |

## Verification method

- **Computed, not eyeballed**: contrast ratios come from a script applying
  the WCAG relative-luminance formula (method in
  [design/theme-exploration.md](design/theme-exploration.md)).
- **Accessibility tree**: pages are checked in a headless browser for the
  landmark roles, accessible names of form fields, the `alert` role, and
  the computed `aria-describedby` associations.
- **Responsive**: screenshots at 375, 768, and 1440 px; no horizontal page
  scroll (`scrollWidth == viewport`).
- **Remaining manual passes** (tracked in
  [#84](https://github.com/ebouchut/learn-dev/issues/84)): keyboard
  walkthrough, Lighthouse/axe on each page, a VoiceOver smoke test, and a
  contrast re-run after any palette change.
