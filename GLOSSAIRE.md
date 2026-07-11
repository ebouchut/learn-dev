# Glossaire

Définitions des termes métier et techniques utilisés dans le projet learn-dev.
Pour les outils concrets et leurs versions, voir [docs/tech-stacks.md](docs/tech-stacks.md) ;
pour l'articulation des composants, voir [ARCHITECTURE.md](ARCHITECTURE.md) ;
pour la justification des décisions de conception, voir les [ADR](docs/adr/README.md).

> [!NOTE]
> 🇬🇧 English version: [GLOSSARY.md](GLOSSARY.md).
> Les deux fichiers sont la traduction l'un de l'autre : toute entrée ajoutée,
> modifiée ou supprimée dans l'un doit l'être aussi dans l'autre.

## Termes métier

- **Archive (archiver)** — Dépublier un cours ou une leçon pour qu'il ne soit
  plus accessible aux étudiants, sans le supprimer.
- **Course (cours)** — Une unité de contenu pédagogique appartenant à un
  formateur ; contient des leçons.
- **Deactivate (désactiver)** — Neutraliser un compte (formateur ou étudiant,
  par exemple) pour qu'il ne puisse plus être utilisé, sans le supprimer.
  Voir aussi *compte désactivé*.
- **Drop a course (abandonner un cours)** — Le retrait d'un étudiant d'un cours
  avant de l'avoir terminé.
- **Enrollment (inscription)** — La relation qui lie un étudiant à un cours
  qu'il a rejoint.
- **Lesson (leçon)** — Un élément de contenu individuel au sein d'un cours.
- **Role (rôle)** — Un ensemble nommé de permissions accordées à un
  utilisateur. Les rôles fournis par défaut sont `STUDENT`, `INSTRUCTOR` et
  `ADMIN` ; `SUPERADMIN` est prévu (voir l'issue #65).

## Authentification et sécurité

- **Authority (autorité)** — Dans Spring Security, une permission unitaire
  détenue par un utilisateur authentifié. Les rôles sont représentés comme des
  autorités préfixées par `ROLE_` (le rôle `ADMIN` devient l'autorité
  `ROLE_ADMIN`).
- **BCrypt** — Une fonction de hachage de mots de passe adaptative. Les mots de
  passe sont stockés sous forme de hachés BCrypt, jamais en clair.
- **CSRF (Cross-Site Request Forgery)** — Une attaque qui pousse le navigateur
  d'un utilisateur connecté à soumettre une requête à son insu. Contrée par un
  jeton par formulaire (injecté par Thymeleaf) et l'attribut de cookie
  `SameSite`.
- **Compte désactivé (disabled account)** — Un compte qui existe mais n'est pas
  autorisé à s'authentifier (issu du drapeau `is_active = false`). À distinguer
  d'un *compte verrouillé*.
- **HttpOnly** — Un attribut de cookie qui masque le cookie au JavaScript côté
  client, ce qui limite le vol de session via XSS.
- **IDOR (Insecure Direct Object Reference)** — Une faille de contrôle d'accès
  où un identifiant fourni par le client est utilisé sans vérification
  d'autorisation. Les clés primaires UUID des utilisateurs limitent
  l'énumération (voir [ADR-0003](docs/adr/0003-uuid-pk-for-users-bigint-elsewhere.md)).
- **Compte verrouillé (locked account)** — Un compte temporairement empêché de
  s'authentifier (par exemple après trop d'échecs de connexion), issu du
  drapeau `is_locked`. À distinguer d'un *compte désactivé*.
- **Principal** — L'entité actuellement authentifiée (en général l'utilisateur)
  dans un contexte de sécurité.
- **SameSite** — Un attribut de cookie qui contrôle l'envoi du cookie par le
  navigateur sur les requêtes inter-sites. Positionné sur `Lax` ici comme
  défense en profondeur contre le CSRF.
- **Secure (cookie)** — Un attribut de cookie qui restreint le cookie au HTTPS.
  Activé seulement lorsque l'application sera servie en TLS.
- **Session (côté serveur)** — L'état d'authentification conservé sur le
  serveur et référencé par un cookie de session (`JSESSIONID`), plutôt qu'un
  jeton autoporteur (voir [ADR-0001](docs/adr/0001-use-server-side-sessions-over-jwt.md)).
- **XSS (Cross-Site Scripting)** — L'injection de scripts malveillants dans des
  pages vues par d'autres utilisateurs. Limitée par l'échappement automatique
  de Thymeleaf et par `HttpOnly`.

## Persistance et modélisation des données

- **Changelog / Changeset (Liquibase)** — Un changelog est la liste ordonnée
  des migrations ; un changeset est une migration atomique, identifiée par
  `path::id::author`.
- **ERD (Entity-Relationship Diagram)** — Un diagramme des entités et de leurs
  relations (produit ici avec Mermaid).
- **Hibernate** — L'implémentation JPA (ORM) utilisée pour faire correspondre
  les entités Java aux tables.
- **JPA (Jakarta Persistence API)** — L'API Java standard du mapping
  objet-relationnel ; implémentée par Hibernate.
- **JSESSIONID** — Le nom par défaut du cookie de session servlet.
- **Liquibase** — L'outil de migration du schéma de base de données. Les
  migrations sont des fichiers SQL formatés écrits à la main et appliqués au
  démarrage (voir [ADR-0005](docs/adr/0005-handwrite-liquibase-migrations-over-mcd-ddl.md)).
- **Merise** — Une méthode française de modélisation des données produisant
  trois vues : MCD, MLD, MPD.
- **MCD (Modèle Conceptuel de Données)** — Le modèle conceptuel ; les entités
  et relations, indépendamment de toute base de données.
- **MLD (Modèle Logique des Données)** — Le modèle logique ; le schéma
  relationnel (tables, clés) dérivé du MCD.
- **MPD (Modèle Physique des Données)** — Le modèle physique ; le schéma
  concret tel qu'implémenté dans PostgreSQL.
- **ORM (Object-Relational Mapping)** — La correspondance entre objets Java et
  tables relationnelles ; assurée par Hibernate/JPA.
- **UUID** — Un identifiant sur 128 bits utilisé comme clé primaire des
  utilisateurs pour éviter l'énumération d'identifiants séquentiels.

## Build, tests et outillage

- **ADR (Architecture Decision Record)** — Un document court, numéroté et en
  ajout seul qui capture une décision de conception et ses compromis, au
  format MADR.
- **Bean Validation** — Le standard Jakarta de déclaration de contraintes
  (`@NotBlank`, `@Email`, `@Size`) sur les champs de formulaires/DTO,
  appliquées avec `@Valid`.
- **Checkstyle** — Un outil d'analyse statique qui vérifie les sources Java
  contre un référentiel de style. Exécuté ici avec le référentiel Google
  fourni (`google_checks.xml`) en mode rapport seul (voir
  [ADR-0011](docs/adr/0011-start-ci-quality-checks-as-advisory-reports.md)).
- **Code coverage (couverture de code)** — Le pourcentage de code exercé par
  la suite de tests. Mesuré ici par JaCoCo et publié sur Codecov ; rapporté,
  sans seuil imposé pour l'instant.
- **Codecov** — Un service hébergé qui reçoit les rapports de couverture
  depuis la CI, fournit un tableau de bord et un badge pour le README, et
  commente chaque PR avec la couverture du projet et du patch. Les statuts
  sont informatifs ici (voir
  [ADR-0012](docs/adr/0012-publish-test-coverage-to-codecov.md)).
- **DTO (Data Transfer Object)** — Un objet qui transporte des données à
  travers une frontière, volontairement distinct des entités. Un DTO `...Form`
  porte un formulaire HTML.
- **Failsafe** — Le plugin Maven qui exécute les tests d'intégration `*IT`
  dans la phase `verify`. Ce projet ne l'utilise **pas** (voir
  [ADR-0009](docs/adr/0009-run-tests-under-surefire-not-failsafe.md)).
- **FIFO (tube nommé)** — Un fichier spécial qui transmet les données à la
  lecture. Le `.env` du projet est une FIFO remplie par 1Password ; le
  `source` du shell ne peut pas la lire (taille nulle au `stat`).
- **HikariCP** — Le pool de connexions JDBC fourni avec Spring Boot.
- **Test d'intégration (integration test)** — Un test qui démarre un contexte
  Spring et exerce plusieurs couches ensemble (ici `@SpringBootTest` contre un
  vrai conteneur Postgres).
- **JaCoCo (Java Code Coverage)** — L'outil de couverture de code pour Java.
  Son plugin Maven instrumente les tests (`prepare-agent`) et écrit un rapport
  HTML/XML dans `target/site/jacoco/` pendant la phase `test` ; la CI le
  publie comme artefact de workflow.
- **Linter** — Un outil qui signale les problèmes de style et de qualité dans
  le code source sans l'exécuter (analyse statique). Le linter du projet est
  Checkstyle.
- **Lombok** — Une bibliothèque qui génère le code répétitif (accesseurs,
  constructeurs) à partir d'annotations, à la compilation.
- **MADR (Markdown ADR)** — Le format léger de modèle d'ADR utilisé dans
  `docs/adr/`.
- **Maven Wrapper (`mvnw`)** — Un script de lancement versionné qui télécharge
  et exécute la version de Maven épinglée par le projet, pour que les builds
  ne dépendent pas d'un Maven installé localement (utilisé par la CI :
  `./mvnw -B -ntp ...`).
- **Slice test (test de tranche)** — Un test qui ne charge qu'une couche du
  contexte (par exemple `@DataJpaTest` pour la couche de persistance).
- **Smoke test (test de fumée)** — Un test minimal vérifiant que le contexte
  de l'application démarre (`LearnDevApplicationTests`).
- **Surefire** — Le plugin Maven qui exécute les tests `*Test`/`*Tests`
  (unitaires et d'intégration) dans la phase `test`. Tous les tests du projet
  passent par Surefire.
- **Testcontainers** — Une bibliothèque qui démarre des conteneurs
  Docker/Podman jetables pour les tests ; utilisée pour exécuter un vrai
  PostgreSQL (voir [ADR-0006](docs/adr/0006-test-against-real-postgres-testcontainers.md)).
- **Ryuk** — Le conteneur compagnon de Testcontainers qui nettoie les
  ressources ; désactivé sous Podman dans ce projet.
- **YAGNI (You Aren't Gonna Need It)** — Le principe de ne pas construire une
  fonctionnalité avant d'en avoir réellement besoin (par exemple le report du
  rôle `SUPERADMIN`).

## Infrastructure et processus

- **Advisory check (contrôle consultatif)** — Un contrôle de CI qui signale
  les problèmes sans bloquer la fusion (goal en rapport seul et/ou
  `continue-on-error`). Le lint et la couverture démarrent en mode consultatif
  ici (voir [ADR-0011](docs/adr/0011-start-ci-quality-checks-as-advisory-reports.md)).
- **CI (intégration continue)** — Construire et tester automatiquement chaque
  changement (chaque PR et chaque push) pour détecter les régressions au plus
  tôt. Mise en oeuvre avec GitHub Actions (issues #45 à #48).
- **Docker Compose** — L'orchestration déclarative de plusieurs conteneurs ;
  fait tourner ici Postgres et Mongo. Sur la machine de développement,
  `docker` est Podman.
- **GitButler** — L'outil de gestion de versions qui enveloppe Git ; utilisé
  via la CLI `but` quand la branche courante est `gitbutler/workspace`.
- **GitHub Actions** — Le service de CI de GitHub. Chaque workflow est un
  fichier YAML sous `.github/workflows/` ; ce projet utilise un workflow ciblé
  par préoccupation (voir [ADR-0010](docs/adr/0010-structure-ci-as-focused-workflows-per-concern.md)).
- **Mailpit** — Un faux serveur SMTP pour le développement : il accepte tous
  les e-mails envoyés par l'application, n'en délivre aucun, et les affiche
  dans une interface web (http://localhost:8025) et une API REST. Tourne comme
  service Docker Compose (voir
  [ADR-0004](docs/adr/0004-use-mailpit-as-local-smtp-catcher.md)).
- **Podman** — Un moteur de conteneurs sans démon, utilisé comme remplaçant de
  `docker`.
- **Runner (exécuteur)** — La machine qui exécute un job GitHub Actions
  (`ubuntu-latest` ici) ; elle embarque un démon Docker, que Testcontainers
  utilise directement.
- **SMTP (Simple Mail Transfer Protocol)** — Le protocole d'envoi des
  e-mails. L'application parle SMTP à Mailpit en développement (port 1025) et
  parlerait à un vrai fournisseur en production.
- **Profil Spring (Spring profile)** — Un jeu de configuration nommé (par
  exemple `dev`) qui sélectionne des propriétés spécifiques et des contextes
  Liquibase.
- **Temurin** — La distribution Eclipse Adoptium de l'OpenJDK ; le build
  Java 21 utilisé en local (via SDKMAN) et sur la CI (via
  `actions/setup-java`).
- **Thymeleaf** — Le moteur de templates HTML côté serveur. Son **dialecte**
  Spring Security (espace de noms `sec:`) expose l'utilisateur authentifié aux
  templates.
- **Workflow (GitHub Actions)** — Un fichier YAML qui déclare quand
  (déclencheurs) et comment (jobs, étapes) la CI s'exécute. Ce projet contient
  `build.yml`, `test.yml`, `lint.yml` et `schema-drift.yml`.
- **Workflow artifact (artefact de workflow)** — Un fichier ou dossier publié
  depuis une exécution de workflow et téléchargeable depuis la page de
  l'exécution (ici : le rapport XML de Checkstyle et les rapports JaCoCo).

## Certification

- **CCP (Certificat de Compétences Professionnelles)** — Un bloc de
  compétences d'un Titre Professionnel français ; le DWWM comprend un CCP
  front-end et un CCP back-end.
- **DWWM (Développeur Web et Web Mobile)** — Le Titre Professionnel français
  visé par ce projet de fin de formation.
- **REAC (Référentiel Emploi Activités Compétences)** — Le référentiel
  officiel de compétences qui définit ce que la certification évalue.
