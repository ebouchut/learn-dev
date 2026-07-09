# Les maquettes de learn-dev expliquées : register.html et home.html

Une lecture commentée de deux fichiers de maquette
([mockups/register.html](mockups/register.html) et
[mockups/home.html](mockups/home.html))
et du CSS qui les met en forme ([mockups/css/base.css](mockups/css/base.css) +
[mockups/css/theme-catppuccin.css](mockups/css/theme-catppuccin.css)).
Pour chaque élément : la balise choisie et pourquoi, le problème qu'elle
résout, l'ARIA impliqué, et les critères RGAA 4.1 satisfaits (le RGAA est la
déclinaison française de WCAG 2.1 AA à laquelle la certification DWWM fait
référence).

> [!NOTE]
> 🇬🇧 English version: [mockups-explained.md](mockups-explained.md).
> Les deux fichiers doivent rester synchronisés.

> Ces fichiers ont été choisis parce qu'à eux deux ils sollicitent tout le système :
> `register.html` est la vitrine de l'accessibilité (formulaires, erreurs, ARIA) ;
> `home.html` est la vitrine de la mise en page (grilles, contenu décoratif, composants).

---

## Partie 1 : structure HTML

### 1.1 Le squelette du document (les deux pages)

```html
<!DOCTYPE html>
<html lang="en">
```

- **`<!DOCTYPE html>`** fait basculer le navigateur en mode standard. Sans
  lui, les navigateurs émulent les bizarreries des années 1990 (modèle de
  boîte cassé, rendu incohérent).
- **`lang="en"`** déclare la langue de la page. Les lecteurs d'écran en
  déduisent leur voix de synthèse vocale ; les moteurs de recherche et les
  traducteurs s'en servent aussi. **RGAA 8.3/8.4** (langue par défaut
  présente et pertinente). C'est pourquoi la décision sur la langue compte
  avant le vrai frontend : l'attribut doit correspondre à la langue réelle
  du contenu.

```html
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Create an account — learn-dev</title>
```

- **`charset`** en premier, pour que l'analyseur ne décode jamais les octets
  de travers.
- **`viewport`** fait que les navigateurs mobiles affichent la page à la
  largeur de l'appareil au lieu d'un canevas dézoomé de 980px ; c'est le
  prérequis de tout comportement responsive
  (**RGAA 10.11**, contenu consultable quelle que soit l'orientation ou la
  largeur).
- **`<title>`** est unique par page et suit un motif constant
  ("Page — site"). C'est la première chose qu'entend un utilisateur de
  lecteur d'écran, et ce qu'affichent les onglets et les marque-pages.
  **RGAA 8.5/8.6** (titre de page présent et pertinent).

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=..." rel="stylesheet">
<link rel="stylesheet" href="css/theme-catppuccin.css">
<link rel="stylesheet" href="css/base.css">
```

- `preconnect` ouvre tôt la connexion TCP/TLS vers l'hébergeur de polices
  (on économise un aller-retour au moment où le CSS des polices est demandé).
- **L'ordre des feuilles de style est le mécanisme de thème** : le fichier
  de thème ne définit que des propriétés personnalisées (custom properties),
  les design tokens ; `base.css` les consomme. Charger un autre fichier de
  thème remplace toutes les couleurs sans le moindre changement dans
  `base.css`.

### 1.2 Le lien d'évitement (les deux pages)

```html
<a class="skip-link" href="#main">Skip to main content</a>
```

- **Problème résolu** : sans lui, les utilisateurs au clavier et de lecteur
  d'écran doivent tabuler à travers tout l'en-tête et la navigation sur
  *chaque page* avant d'atteindre le contenu. Le lien d'évitement est le
  premier élément focalisable et saute directement vers `<main id="main">`.
- C'est une ancre ordinaire vers un fragment ; aucun ARIA nécessaire.
- Le CSS le masque hors écran jusqu'à ce qu'il reçoive le focus (voir la
  partie 2.4) : les utilisateurs voyants à la souris ne le voient jamais,
  les utilisateurs au clavier le voient toujours.
- **RGAA 12.7** (lien d'évitement ou d'accès rapide à la zone de contenu
  principal). Un des critères les plus vérifiés en audit.

### 1.3 Zones de repère : header, nav, main, footer (les deux pages)

```html
<header class="site-header">...</header>
<nav class="site-header__nav" aria-label="Main">...</nav>
<main class="site-main" id="main">...</main>
<footer class="site-footer">...</footer>
```

- Ces quatre balises produisent **des zones de repère (landmarks) ARIA
  gratuitement** : `banner`, `navigation`, `main`, `contentinfo`. Les
  lecteurs d'écran exposent un menu des zones de repère, ce qui permet de
  sauter d'une zone à l'autre sans tabuler. Cela a été vérifié dans l'arbre
  d'accessibilité de la page rendue (les rôles `banner`,
  `navigation "Main"`, `main`, `contentinfo` sont tous présents).
- **`aria-label="Main"` sur `<nav>`** : nomme la zone de navigation.
  Indispensable dès qu'une page peut contenir plus d'un `<nav>` (menu
  principal, menu de pied de page, fil d'Ariane) ; nommer dès le premier
  jour ne coûte rien et passe à l'échelle.
- **Pourquoi pas `<div class="header">`** : une div n'a pas de rôle ; le
  menu des zones de repère serait vide, et RGAA 9.2 / 12.6 échoueraient.
- **RGAA 9.2** (structure du document cohérente : header, main, footer),
  **RGAA 12.6** (zones de regroupement atteignables ou activables).

### 1.4 La liste de navigation et la page courante (les deux pages)

```html
<ul class="nav__list">
  <li><a class="nav__link" href="home.html">Home</a></li>
  <li><a class="nav__link" href="register.html" aria-current="page">Sign up</a></li>
</ul>
```

- **`<ul>/<li>`** : la navigation est une *liste de liens* ; la sémantique
  de liste permet au lecteur d'écran d'annoncer « liste, 3 éléments », ce
  qui donne d'emblée la taille du menu à l'utilisateur. **RGAA 9.3** (listes
  correctement structurées).
- **`aria-current="page"`** marque le lien correspondant à la page courante.
  Un lecteur d'écran annonce « page courante » ; le CSS le met aussi en
  forme (gras + soulignement mauve), donc l'information existe dans les
  *deux* canaux : technologies d'assistance et vision. Cette dualité est le
  cœur de **RGAA 3.1** (l'information n'est pas donnée uniquement par la
  couleur).
- Les intitulés de liens ("Home", "Log in", "Sign up") sont explicites hors
  contexte : **RGAA 6.1** (chaque lien est explicite).

### 1.5 La hiérarchie des titres

`register.html` : un seul `<h1>` ("Create an account").
`home.html` : `<h1>` (titre du hero) puis `<h2>` ("Why learn-dev?") puis un
`<h3>` par carte de fonctionnalité.

- Les utilisateurs de lecteur d'écran naviguent par titres (la touche `H`)
  plus que par tout autre mécanisme. La hiérarchie est strictement
  décroissante, sans niveau sauté, et il y a exactement un `<h1>` par page.
- **RGAA 9.1** (information structurée par des titres pertinents).

### 1.6 Le formulaire d'inscription (register.html), champ par champ

```html
<p class="alert alert--error reveal" role="alert">
  Your registration could not be completed. Check the highlighted field below.
</p>
```

- **`role="alert"`** : transforme le paragraphe en **région live**
  assertive : quand la page s'affiche (ou se réaffiche) avec une erreur, les
  lecteurs d'écran l'annoncent immédiatement, sans que l'utilisateur ait à
  la chercher. Dans le vrai template Thymeleaf, ce bloc sera rendu
  conditionnellement après un POST en échec.
- Placé **avant** le formulaire, pour être rencontré en premier dans l'ordre
  de lecture.
- **RGAA 11.11** (le contrôle de saisie est accompagné de suggestions
  d'erreur), et une partie du comportement « status messages » de
  WCAG 4.1.3.

```html
<form action="#" method="post" novalidate>
```

- **`method="post"`** : l'inscription modifie l'état ; GET ferait fuiter le
  mot de passe dans les URL, les journaux et l'historique.
- **`novalidate`** : réservé à la maquette. Il désactive la validation
  native du navigateur pour pouvoir démontrer l'état d'erreur *rendu côté
  serveur* (c'est ainsi que se comporte l'application Spring/Thymeleaf :
  Bean Validation s'exécute côté serveur et la page se réaffiche avec les
  erreurs).

```html
<label class="form__label" for="username">Username</label>
<span class="form__hint" id="username-hint">3 to 50 characters.</span>
<input class="form__input" type="text" id="username" name="username"
       autocomplete="username" aria-describedby="username-hint" required>
```

- **`<label for>` + `id`** : l'association *programmatique* entre le texte
  et le champ. Cliquer sur l'étiquette donne le focus au champ (cible
  tactile plus grande), et un lecteur d'écran annonce « Username, édition »
  quand le champ reçoit le focus. **RGAA 11.1** (chaque champ a une
  étiquette) : le critère de formulaire le plus audité, et de loin.
- **`<span class="form__hint" id>` + `aria-describedby`** : l'indication de
  saisie est rattachée au champ comme *description accessible* : annoncée
  après l'étiquette, mais sans faire partie du nom. Les utilisateurs voyants
  la voient au-dessus du champ ; les utilisateurs de lecteur d'écran
  l'entendent en contexte. Famille **RGAA 11.4/11.5** (étiquettes et champs
  accolés, indications de saisie).
- **`autocomplete="username"`** : indique aux navigateurs et aux
  gestionnaires de mots de passe la *finalité* du champ, ce qui active le
  remplissage automatique. **RGAA 11.13** (la finalité du champ peut être
  déduite) = WCAG 1.3.5 « Identify Input Purpose ».
- **`required`** : exprime la contrainte dans le balisage (l'arbre
  d'accessibilité expose « obligatoire ») ; le serveur revalide de toute
  façon.

```html
<input class="form__input form__input--invalid" type="email" id="email"
       name="email" autocomplete="email" aria-describedby="email-error"
       aria-invalid="true" value="carol@example.org" required>
<span class="form__error" id="email-error">Email already registered</span>
```

Le motif d'état d'erreur, et le cœur de cette maquette :

- **`type="email"`** apporte les claviers sémantiques sur mobile et la
  vérification native du format (**RGAA 11.10**, contrôle de saisie
  pertinent).
- **`aria-invalid="true"`** signale le champ comme en erreur dans l'arbre
  d'accessibilité : les lecteurs d'écran annoncent « saisie non valide ».
- **`aria-describedby="email-error"`** pointe vers le message d'erreur ;
  donner le focus au champ fait donc lire : « Email, édition, saisie non
  valide, Email already registered ». Le texte d'erreur est *retrouvable
  depuis le champ*, pas seulement proche visuellement. Cela a été vérifié
  dans la page rendue : la description calculée de `#email` est exactement
  "Email already registered".
- Le canal visuel est redondant avec le canal programmatique : bordure rouge
  (classe modificatrice `--invalid`) **et** texte rouge en gras en dessous ;
  jamais la couleur seule (**RGAA 3.1**), avec un contraste de 4.80:1 pour
  la couleur d'erreur sur le fond de la page (**RGAA 3.2**, contraste des
  textes >= 4.5:1).
- **RGAA 11.11** (erreur identifiée + suggestion de correction).

```html
<button class="button button--primary" type="submit">Create my account</button>
```

- Un vrai `<button type="submit">`, pas un `<a>` ni un `<div>` stylés : il
  soumet au clic *et* avec Entrée/Espace, il est focalisable, et il expose
  nativement le rôle `button`. Aucun ARIA nécessaire : la première règle
  d'ARIA est de préférer les éléments natifs.

### 1.7 La carte de code décorative (home.html)

```html
<div class="code-card reveal reveal--4" aria-hidden="true">
  <code class="code-card__code">...</code>
</div>
```

- **`aria-hidden="true"`** : la carte est purement *décorative* : un extrait
  Java stylisé qui signale visuellement « programmation ». Pour un lecteur
  d'écran, ce serait du bruit (entendre du code brut lu à voix haute
  n'ajoute rien au message du hero), donc le sous-arbre est entièrement
  retiré de l'arbre d'accessibilité. Analogue de **RGAA 1.2** (contenu
  décoratif ignoré par les technologies d'assistance).
- **`<div>`** est correct ici précisément *parce que* l'élément ne porte
  aucune sémantique : c'est de la présentation pure.
- **`<code>`** à l'intérieur conserve une sémantique honnête pour le texte
  du code (et reçoit la police mono via une règle
  `code { font-family: var(--font-mono) }`).
- Même raisonnement pour l'emoji des cartes de fonctionnalités,
  `<span class="feature-card__icon" aria-hidden="true">⚡</span>` : le
  `<h3>` adjacent porte déjà le sens ; sans cela, l'emoji serait lu
  « haute tension ».

### 1.8 Les cartes de fonctionnalités sous forme de liste (home.html)

```html
<ul class="features">
  <li class="feature-card">
    <h3 class="feature-card__title">Interactive lessons</h3>
    <p class="feature-card__text">...</p>
  </li>
  ...
</ul>
```

- Trois éléments parallèles = une **liste**, donc `<ul>/<li>` (RGAA 9.3), et
  non trois divs sœurs. « Liste, 3 éléments » indique à l'utilisateur la
  forme du contenu.
- Chaque carte contient un titre + un paragraphe, ce qui donne à la
  navigation par la touche H un arrêt par fonctionnalité.

---

## Partie 2 : mise en page et CSS

### 2.1 Convention de nommage : BEM

Chaque classe suit **BEM** (`block__element--modifier`) :

| Rôle | Syntaxe | Exemples tirés de ces pages |
|---|---|---|
| Block : composant autonome | `.block` | `.site-header`, `.form-card`, `.code-card`, `.alert`, `.button` |
| Element : une partie qui n'a de sens qu'à l'intérieur de son block | `.block__element` | `.site-header__brand`, `.form__input`, `.hero__title`, `.code-card__code` |
| Modifier : une variante ou un état d'un block/element | `.block--modifier`, `.block__element--modifier` | `.button--primary`, `.alert--error`, `.form__input--invalid`, `.site-main--narrow` |

Pourquoi BEM ici :

- **Spécificité plate** : chaque sélecteur est une classe unique, soit une
  spécificité `(0,1,0)`. Pas de chaînes de descendants, donc pas de guerres
  de spécificité : n'importe quelle règle peut être surchargée par une autre
  classe unique placée plus loin dans la cascade.
- **Auto-documenté** : `form__error` dit exactement où il vit ;
  `button--ghost` dit qu'il s'agit d'une variante de `.button`.
- **Facile à rechercher (grep)** : chercher `feature-card` retrouve tout le
  composant.
- Les ID ne servent **jamais au style** (seulement au `label for`, aux
  cibles de fragment comme `#main` et à la tuyauterie `aria-describedby`).
  Un sélecteur d'ID (`#main`, spécificité `(1,0,0)`) écraserait toutes les
  règles de classe et casserait le modèle plat.

### 2.2 Les design tokens (fichier de thème + tokens structurels)

```css
:root {
  --color-bg: #eff1f5;
  --color-text: #4c4f69;
  --color-primary: #8839ef;
  ...
}
@media (prefers-color-scheme: dark) {
  :root { --color-bg: #1e1e2e; ... }
}
```

- **`:root`** est l'élément `<html>` avec une spécificité de pseudo-classe
  `(0,1,0)` ; les propriétés personnalisées qui y sont déclarées **héritent
  dans chaque élément**, ce qui en fait des design tokens globaux.
- **Les propriétés personnalisées `--color-*`** sont consommées avec
  `var(--color-*)` dans `base.css`. Aucun composant ne contient jamais de
  valeur hexadécimale, donc :
  1. remplacer le `<link>` de thème rhabille le site entier ;
  2. le mode sombre est un seul bloc `@media (prefers-color-scheme: dark)`
     qui réaffecte les mêmes noms de tokens : les composants restent
     intacts.
- **`color-scheme: light dark`** indique au navigateur que les deux schémas
  sont pris en charge, donc les widgets natifs (champs, barres de
  défilement) suivent le mouvement.
- Les tokens structurels de `base.css` (`--space-1..6` sur une échelle de
  0.25rem, `--font-size-*` sur une échelle modulaire, `--radius`,
  `--shadow`) jouent le même rôle pour la géométrie. Toutes les tailles sont
  en **rem**, donc tout s'adapte quand l'utilisateur change la taille de
  police de base du navigateur (**RGAA 10.4**, texte agrandissable à 200%).

### 2.3 Reset et typographie

```css
*, *::before, *::after { box-sizing: border-box; }
```

- Sélecteur universel, spécificité `(0,0,0)` : délibérément la règle la plus
  faible du fichier, pour que n'importe quoi puisse la surcharger.
  `border-box` fait que `width` inclut le padding et la bordure : le modèle
  de boîte intuitif (`width: 100%` plus du padding ne déborde plus, ce sur
  quoi `.form__input` s'appuie exactement).

```css
body {
  font-family: var(--font-body);
  font-size: var(--font-size-base);
  line-height: var(--line-height);   /* 1.6, sans unité */
  color: var(--color-text);
  background: var(--color-bg);
}
```

- Défini une fois sur `body`, hérité partout (sélecteur de type, `(0,0,1)`).
- **Le `line-height: 1.6` sans unité** est un multiplicateur qui hérite
  correctement à n'importe quelle taille de police (une valeur avec unité,
  comme des `px`, le figerait). 1.6 satisfait la recommandation de confort
  RGAA/WCAG (>= 1.5, contexte **RGAA 10.12**).
- La police du corps de texte est **Atkinson Hyperlegible**, conçue par le
  Braille Institute pour la lisibilité en basse vision (formes de lettres
  b/d/p/q bien distinctes) : un choix typographique qui *est* une
  fonctionnalité d'accessibilité.

```css
:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}
```

- **La règle d'accessibilité la plus importante du fichier.** Chaque élément
  focalisable reçoit un contour visible de 2px dans la couleur de focus
  (bleu Catppuccin, choisi pour l'exigence de contraste de 3:1 des
  composants d'interface).
- `:focus-visible` (et non `:focus`) se déclenche pour le focus clavier mais
  pas pour les clics de souris : le contour ne gêne donc jamais les
  utilisateurs à la souris et ne disparaît jamais pour les utilisateurs au
  clavier. La feuille de style n'écrit **jamais** `outline: none`.
  **RGAA 10.7** (focus visible).

```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { animation: none !important; transition: none !important; }
}
```

- Les utilisateurs atteints de troubles vestibulaires désactivent les
  animations au niveau du système ; cette règle respecte ce choix en
  supprimant toute animation. C'est le seul `!important` du projet :
  justifié parce que cette préférence doit l'emporter sur *n'importe
  quelle* autre règle. Famille **RGAA 13.x** (contenu en mouvement contrôlé
  par l'utilisateur).

### 2.4 La mécanique du lien d'évitement

```css
.skip-link {
  position: absolute;
  top: -3rem;              /* garé au-dessus de la zone d'affichage */
  transition: top 150ms ease;
}
.skip-link:focus-visible { top: 0; }   /* glisse en vue au focus */
```

- Le positionnement hors écran (pas `display: none` !) garde le lien
  focalisable : `display: none` le retirerait de l'ordre de tabulation et
  ruinerait tout l'intérêt du mécanisme.
- Au focus, `top: 0` le fait apparaître ; le sélecteur composé
  `.skip-link:focus-visible` a une spécificité `(0,2,0)` et bat `.skip-link`
  `(0,1,0)` : les règles d'état doivent peser plus lourd que les règles de
  base, et ici la pseudo-classe fournit exactement le point de spécificité
  supplémentaire qu'il faut.

### 2.5 En-tête et navigation

```css
.site-header { border-bottom: 1px solid var(--color-border); background: var(--color-surface); }
.site-header__inner {
  max-width: var(--content-width);   /* 64rem */
  margin: 0 auto;                    /* l'idiome classique de centrage */
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex-wrap: wrap;
}
.site-header__nav { margin-left: auto; }
```

- Le block peint d'un bord à l'autre (bordure + surface) ; l'élément
  `__inner` contraint le contenu à une colonne lisible et le centre avec des
  marges automatiques. Ce motif à deux couches se répète dans le pied de
  page.
- **Flexbox** pour l'en-tête parce que c'est une rangée unidimensionnelle ;
  `align-items: center` centre verticalement la marque et la navigation ;
  `margin-left: auto` sur la nav absorbe tout l'espace libre et pousse la
  navigation à droite, sans float ni positionnement ; `flex-wrap` laisse la
  navigation passer sur une deuxième ligne sur les écrans étroits au lieu de
  déborder.

```css
.nav__link[aria-current="page"] {
  font-weight: 700;
  color: var(--color-primary);
  box-shadow: inset 0 -2px 0 var(--color-primary);
}
```

- **Un style piloté par l'attribut ARIA lui-même** : l'état vit à un seul
  endroit, dans le balisage, et le CSS le lit ; impossible que l'état visuel
  et l'état annoncé divergent.
- Spécificité `(0,2,0)` (classe + sélecteur d'attribut), qui surcharge donc
  proprement `.nav__link` `(0,1,0)`.
- Le « soulignement » est un box-shadow interne plutôt qu'un
  `text-decoration` : il se place au bord du padding du lien et ne se cumule
  pas avec le soulignement du survol.

### 2.6 Les boutons

```css
.button { /* base : police, padding 0.65em/1.4em, radius, cursor, transition */ }
.button--primary { background: var(--color-primary); color: var(--color-on-primary); }
.button--ghost   { background: transparent; color: var(--color-text); border-color: var(--color-border); }
```

- Le balisage s'abonne avec les deux classes :
  `class="button button--primary"`. La base et le modificateur ont la même
  spécificité `(0,1,0)` ; le modificateur gagne, pour les propriétés qu'il
  redéfinit, uniquement par **ordre dans la source** (il apparaît plus loin
  dans le fichier) : c'est le mécanisme BEM voulu, sans `!important` et sans
  imbrication.
- Le padding en **em** s'adapte à la taille de police du bouton lui-même ;
  l'arrondi et les couleurs viennent des tokens. La paire blanc sur mauve
  mesure 5.41:1 (**RGAA 3.2** ; les boutons sont du texte, donc c'est le
  seuil de 4.5:1 qui s'applique, pas seulement le minimum de 3:1 des
  composants d'interface).
- L'effet de soulèvement au survol (`transform: translateY(-1px)` + ombre)
  est une transition, donc automatiquement désactivé par le bloc
  reduced-motion.

### 2.7 La grille du hero et la carte de code (home.html)

```css
.hero {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: var(--space-5);
  align-items: center;
}
@media (max-width: 46rem) { .hero { grid-template-columns: 1fr; } }
```

- **Grid** parce que le hero est bidimensionnel (des colonnes dont les
  hauteurs doivent s'aligner). Les unités `fr` donnent 55% de l'espace à la
  colonne de texte, 45% à la carte de code, sans nombres magiques en pixels.
- La **media query en rem** (46rem ≈ 736px au zoom par défaut) replie le
  tout sur une seule colonne : des points de rupture en rem respectent les
  réglages de taille de police de l'utilisateur (**RGAA 10.11**, reflow).

```css
.code-card {
  background: var(--code-bg);      /* reste en Mocha sombre, même en mode clair */
  font-family: var(--font-mono);
  rotate: 1.5deg;
  position: relative;
}
.code-card::before {
  content: "";
  position: absolute;
  width: 10px; height: 10px; border-radius: 50%;
  background: var(--color-error);
  box-shadow: 18px 0 0 var(--color-warning), 36px 0 0 var(--color-success);
}
```

- La carte garde volontairement la palette sombre dans les deux modes :
  c'est un motif « fenêtre de terminal », et ses couleurs de syntaxe (les
  tokens `--code-*`) sont les pastels Mocha, qui passent le contraste sur le
  fond sombre.
- **L'astuce `::before` + box-shadow** : un seul pseudo-élément peint les
  trois pastilles « feux tricolores » de macOS ; les deux pastilles
  supplémentaires sont des box-shadows pleins et décalés (de 18px et 36px
  vers la droite). Zéro balisage supplémentaire pour de la pure décoration,
  ce qui est exactement le rôle des pseudo-éléments (et comme il s'agit de
  contenu CSS, il est invisible pour les lecteurs d'écran, en cohérence avec
  l'`aria-hidden` de la carte).
- `position: relative` sur la carte établit le bloc conteneur par rapport
  auquel le pseudo-élément positionné en absolu est placé.
- `rotate: 1.5deg` (la propriété de transformation individuelle moderne)
  donne l'effet d'autocollant posé à la main ;
  `.code-card__code { white-space: pre; overflow-x: auto; }`
  préserve l'indentation du code et fait défiler horizontalement *à
  l'intérieur de la carte* plutôt que de casser la page (**RGAA 10.11**,
  encore).

```css
.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(15rem, 1fr));
  gap: var(--space-4);
}
```

- **L'idiome `auto-fit` + `minmax`** : autant de colonnes égales que
  possible, chacune d'au moins 15rem, chacune s'étirant pour se partager le
  reste. Trois cartes sur ordinateur, deux sur tablette, une sur téléphone :
  responsive **sans aucune media query**.

### 2.8 Le formulaire (register.html)

```css
.site-main--narrow { max-width: var(--form-width); }  /* colonne de 26rem */
```

- Un modificateur sur le block `main` remplace la colonne de contenu de
  64rem par une colonne de 26rem : un formulaire se lit mieux dans une
  mesure étroite. Une seule classe dans le balisage
  (`class="site-main site-main--narrow"`), aucun code de mise en page
  dupliqué.

```css
.form__input {
  width: 100%;
  font: inherit;
  border: 1px solid var(--color-border);
  background: var(--color-bg);
}
```

- **`font: inherit`** est indispensable : les contrôles de formulaire
  n'héritent *pas* des polices par défaut ; sans cela, les champs
  s'affichent dans la police du système à 13px, ce qui casse à la fois le
  design et le comportement au zoom.
- `width: 100%` est sans risque grâce au `border-box` global.

```css
.form__input:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 1px;
  border-color: var(--color-focus);
}
.form__input--invalid { border-color: var(--color-error); }
.form__error { color: var(--color-error); font-weight: 700; font-size: var(--font-size-sm); }
```

- Le focus recolore la bordure *et* dessine le contour (ceinture et
  bretelles pour RGAA 10.7).
- `.form__input` et `.form__input--invalid` sont à égalité à `(0,1,0)` ; le
  modificateur gagne `border-color` par ordre dans la source. Mais
  `.form__input:focus-visible` est à `(0,2,0)`, donc **pendant le focus, la
  couleur de focus bat délibérément la couleur d'erreur** : on voit toujours
  où l'on se trouve ; l'erreur reste portée par le message et par
  `aria-invalid`.
- Le texte d'erreur est en gras *et* rouge *et* lié programmatiquement :
  trois canaux (RGAA 3.1).

```css
.alert { border: 1px solid; border-radius: var(--radius); background: var(--color-surface); }
.alert--error   { color: var(--color-error);   border-color: var(--color-error); }
.alert--success { color: var(--color-success); border-color: var(--color-success); }
```

- La règle de base déclare `border: 1px solid` **sans couleur** : le CSS
  utilise alors `currentColor` pour la bordure, et chaque modificateur n'a
  plus qu'à définir `color` + `border-color`... en fait, définir `color`
  seul suffirait pour la bordure grâce à currentColor ; le `border-color`
  explicite garde l'intention lisible. Le texte de chaque variante passe le
  seuil de 4.5:1 sur le fond de la page (voir les tableaux de l'exploration
  des thèmes).

### 2.9 L'animation d'apparition

```css
@keyframes rise-in {
  from { opacity: 0; translate: 0 10px; }
  to   { opacity: 1; translate: 0 0; }
}
.reveal    { animation: rise-in 500ms ease both; }
.reveal--2 { animation-delay: 100ms; }   /* --3 : 200ms, --4 : 300ms */
```

- Une seule keyframe, étagée par des classes modificatrices de délai : le
  titre du hero, le chapeau, les boutons et la carte de code apparaissent en
  séquence, un unique moment orchestré au chargement de la page plutôt que
  des effets dispersés.
- `animation-fill-mode: both` (le mot-clé `both`) applique l'état `from`
  avant le début de l'animation (pas de flash de l'état final) et conserve
  l'état `to` après.
- 10px de déplacement et 500ms : perceptible, pas théâtral. Et l'ensemble
  est effacé par le bloc `prefers-reduced-motion` pour les utilisateurs qui
  en ont besoin (la page reste pleinement utilisable sans aucune animation).

---

## Correspondance RGAA rapide (ce qu'un auditeur vérifierait sur ces deux pages)

| Critère RGAA (thématique) | Où il est satisfait |
|---|---|
| 3.1 information pas seulement par la couleur | erreur = bordure + texte en gras + `aria-invalid` ; nav courante = gras + soulignement + `aria-current` |
| 3.2 contraste des textes (4.5:1) | chaque paire de tokens calculée dans [theme-exploration.md](theme-exploration.md) (pire paire sur ces pages : 4.73:1) |
| 6.1 liens explicites | "Create your account", "Source on GitHub", "Log in" |
| 8.3/8.4 langue de page | `<html lang="en">` (deviendra `fr` quand la décision sur la langue sera actée) |
| 8.5/8.6 titre de page | `<title>` unique et suivant un motif constant sur chaque page |
| 9.1 titres pertinents | un seul `h1`, `h2`/`h3` ordonnés |
| 9.2 structure cohérente | zones de repère `header`/`nav`/`main`/`footer` |
| 9.3 listes | liste de navigation, liste des fonctionnalités |
| 10.4 agrandissement du texte | toutes les tailles en rem/em, line-height sans unité |
| 10.7 focus visible | contour `:focus-visible` global, jamais retiré |
| 10.11 reflow / responsive | points de rupture en rem, grilles auto-fit, débordement interne |
| 11.1 étiquettes de champs | `<label for>` sur chaque champ |
| 11.10 contrôle de saisie | `type="email"`, `required` |
| 11.11 erreurs et suggestions | résumé `role="alert"` + message par champ via `aria-describedby` |
| 11.13 finalité des champs | `autocomplete="username|email|new-password"` |
| 12.7 lien d'évitement | `.skip-link` vers `#main` |
| 1.2 décoration ignorée | `aria-hidden` sur la carte de code et l'emoji |

---

*Les sources de style faisant autorité sont
[mockups/css/base.css](mockups/css/base.css) et
[mockups/css/theme-catppuccin.css](mockups/css/theme-catppuccin.css) ;
les chiffres de contraste viennent de [theme-exploration.md](theme-exploration.md).
Les documents compagnons de chaque fichier se trouvent dans [mockups/](mockups/).*
