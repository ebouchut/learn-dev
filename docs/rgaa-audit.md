# Auto-audit d'accessibilité RGAA

Rapport d'auto-audit d'accessibilité de **learn-dev**, destiné à figurer en
annexe du dossier projet de la certification DWWM. Il complète
[docs/rgaa.md](rgaa.md) (English), qui explique chaque critère et où il est
implémenté ; le présent document apporte la **preuve outillée** : mesures
Lighthouse et axe-core sur chaque page, et parcours clavier manuel.

- **Date de l'audit** : 12 juillet 2026
- **État audité** : branche `dev`, commit `8c82a93` (application démarrée
  localement, profil `dev`)
- **Périmètre** : les 7 pages de l'application, y compris la page
  authentifiée, plus l'état d'erreur du formulaire d'inscription
- **Référentiel** : RGAA 4.x (déclinaison française de WCAG 2.1 niveau AA)

## Méthode et outils

| Outil | Version | Rôle |
|-------|---------|------|
| Lighthouse | 13.4.0 | Score d'accessibilité par page (Chrome headless) |
| axe-core | 4.12.1 | Analyse statique du DOM rendu, jeu de règles WCAG 2.1 A/AA + bonnes pratiques, injecté dans le navigateur sur la page réelle |
| Parcours clavier | manuel | Ordre de tabulation, visibilité du focus, lien d'évitement, connexion au clavier seul |

Trois angles indépendants : Lighthouse note la page, axe-core détaille les
règles (y compris celles que Lighthouse ne couvre pas), et le parcours
clavier vérifie ce qu'aucun outil automatique ne sait prouver
(l'utilisabilité réelle au clavier). La page authentifiée `/dashboard` a été
auditée avec une vraie session (compte créé pour l'audit), pas une copie
statique.

**Exemple : reproduire la mesure Lighthouse d'une page**

```shell
npx lighthouse "http://localhost:8082/auth/login" \
    --only-categories=accessibility \
    --output=json --output-path=lh-login.json \
    --chrome-flags="--headless=new"
```

Où :

- `--only-categories=accessibility` limite l'audit à la catégorie
  accessibilité (les autres catégories ne concernent pas ce rapport) ;
- `--chrome-flags="--headless=new"` exécute Chrome sans interface ;
- le score se lit dans `categories.accessibility.score` du JSON (1 = 100 %).

**Exemple : reproduire l'analyse axe-core dans le navigateur**

```js
// Dans la console du navigateur, sur la page à auditer,
// après avoir chargé axe.min.js (npm install axe-core) :
const r = await axe.run(document, {
  runOnly: { type: 'tag',
             values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'best-practice'] }
});
console.log(r.violations);   // [] attendu
```

## Résultats : mesures automatiques

| Page | État | Lighthouse | axe : violations | axe : règles passées |
|------|------|-----------:|-----------------:|---------------------:|
| `/` (accueil) | anonyme | **100** | **0** | 32 |
| `/auth/login` | anonyme | **100** | **0** | 37 |
| `/auth/register` | anonyme | **100** | **0** | 38 |
| `/auth/register` | erreurs de saisie | n/a (voir ci-dessous) | n/a | markup vérifié |
| `/auth/forgot-password` | anonyme | **100** | **0** | 38 |
| `/auth/reset-password` | lien invalide | **100** | **0** | 35 |
| `/privacy` (`lang="fr"`) | anonyme | **100** | **0** | 37 |
| `/dashboard` | **authentifié** | **100** | **0** | 33 |

**État d'erreur du formulaire d'inscription** : une soumission vide (POST
anonyme avec jeton CSRF valide) re-rend la page (HTTP 200) avec 3 champs
marqués `aria-invalid="true"`, chacun relié à son message par
`aria-describedby` (par exemple `aria-describedby="username-hint
username-error"`, message « must not be blank »). Ce câblage est aussi
verrouillé par un test de non-régression MockMvc
(`AuthFlowTest.register_form_renders_and_shows_field_errors`).

### Points « incomplete » d'axe-core

Sur `/auth/login`, axe-core classe 8 nœuds en `incomplete` (ni réussite ni
échec) pour la règle `color-contrast` : le **lien d'évitement**, positionné
hors écran au-dessus de l'en-tête tant qu'il n'a pas le focus, chevauche
géométriquement d'autres éléments, et l'outil renonce à déterminer leur
couleur de fond. Vérification manuelle : tous les couples texte/fond de la
charte sont calculés par script avec la formule de luminance relative WCAG
(méthode et valeurs dans
[design/theme-exploration.md](design/theme-exploration.md)) ; le pire couple
livré est à **4,73:1**, au-dessus du seuil AA de 4,5:1. Aucune action requise.

## Résultats : parcours clavier

Réalisé sur `/auth/login` en mode sombre (`prefers-color-scheme: dark`),
puis étendu par sondage aux autres pages (même gabarit `layout.html`, mêmes
styles de focus globaux).

### Ordre de tabulation constaté

| Tab | Élément | Focus visible |
|----:|---------|---------------|
| 1 | Lien d'évitement « Skip to main content » | contour 2 px |
| 2 | Marque « learn-dev » (retour accueil) | contour 2 px |
| 3 | Navigation : « Home » | contour 2 px |
| 4 | Navigation : « Log in » | contour 2 px |
| 5 | Navigation : « Sign up » | contour 2 px |
| 6 | Champ « Username » | contour 2 px |
| 7 | Champ « Password » | contour 2 px |
| 8 | Bouton « Log in » | contour 2 px |
| 9 | Lien « Create one » | contour 2 px |
| 10 | Lien « Forgot your password? » | contour 2 px |
| 11 | Pied de page : « Privacy policy (RGPD) » | contour 2 px |
| 12 | Pied de page : « Source on GitHub » | contour 2 px |

L'ordre suit la lecture visuelle, aucun piège au clavier, et **chacun des 12
arrêts** affiche l'indicateur de focus global (`outline: 2px solid`, couleur
du jeton bleu, mesuré `rgb(137, 180, 250)` en mode sombre). Aucun
`outline: none` non compensé.

### Lien d'évitement (RGAA 12.7)

- Premier élément focusable de chaque page ; visible dès qu'il a le focus.
- Cible `href="#main"` présente (`<main id="main">`).
- **Preuve d'utilité** : après activation du lien, la tabulation suivante
  atterrit directement sur le champ « Username », en sautant les 4 liens
  d'en-tête.

### Connexion au clavier seul

Le parcours complet de connexion a été réalisé sans souris : lien
d'évitement, saisie de l'identifiant, tabulation, saisie du mot de passe,
activation du bouton de soumission, arrivée sur `/dashboard`. Le formulaire
est un `<form method="post">` avec un `<button type="submit">`, ce qui
garantit aussi la soumission implicite par la touche Entrée dans un champ.

## Correspondance avec la grille de critères

Les mesures ci-dessus apportent la preuve outillée des critères suivis dans
[docs/rgaa.md](rgaa.md#criteria-map) ; en particulier :

| Critère RGAA | Preuve dans cet audit |
|--------------|------------------------|
| 3.2 (contrastes) | Lighthouse et axe sans violation ; ratios calculés, pire couple 4,73:1 |
| 8.3/8.4 (langue) | `lang` vérifié par axe sur chaque page, `lang="fr"` sur `/privacy` |
| 8.5/8.6 (titre de page) | Titres uniques constatés sur les 7 pages |
| 9.1/9.2 (titres, structure) | Règles axe heading-order et landmarks passées partout |
| 10.7 (focus visible) | 12/12 arrêts avec contour visible au parcours clavier |
| 11.1 (étiquettes) | Règle axe `label` passée sur les 4 formulaires |
| 11.11 (erreurs identifiées) | État d'erreur : `aria-invalid` + `aria-describedby` + message explicite |
| 12.7 (lien d'évitement) | Premier focusable, cible valide, utilité prouvée |

## Constats et recommandations

1. **Aucune violation** détectée par les deux outils sur les 7 pages, y
   compris la page authentifiée et l'état « lien invalide » de la
   réinitialisation ; score Lighthouse 100/100 partout.
2. **Recommandation (robustesse)** : ajouter `tabindex="-1"` sur
   `<main id="main">`. Aujourd'hui l'activation du lien d'évitement
   repose sur le « sequential focus navigation starting point » du
   navigateur (comportement correct des navigateurs modernes) ; avec
   `tabindex="-1"`, le focus serait déplacé explicitement sur `main`, ce
   qui est plus robuste avec des technologies d'assistance anciennes.
3. **Reste à faire** (suivi dans
   [#84](https://github.com/ebouchut/learn-dev/issues/84)) : test de fumée
   avec un lecteur d'écran (VoiceOver), et re-passage de cet audit après
   tout changement de palette ou ajout de page (les commandes de la section
   « Méthode et outils » rendent le re-passage reproductible).
