<!-- GitHub Badges -->

[![build status][build-image]][build-url]
[![test status][test-image]][test-url]
[![code coverage][coverage-image]][coverage-url]
[![lint status][lint-image]][lint-url][![lint report][lint-report-image]][lint-report-url]
[![schema drift status][schema-drift-image]][schema-drift-url]
[![github issues][github-issues-image]][github-issues-url]

# Learn-dev: An Interactive Programming Learning Platform

## Presentation

> An interactive programming learning platform.  

This project aims to enable students to learn programming.

It is also my capstone project for the [Web and Web Mobile Developer REAC certification](https://www.francecompetences.fr/recherche/rncp/37674/), which I am currently undergoing at [La Plateforme_](https://laplateforme.io).




## Goals

- Provide an interactive environment for learning programming concepts
- Support multiple user roles (such as Student, Instructor, Admin)
- Demonstrate full-stack development skills using industry standards


## Tech Stack

This project is built with [Java](https://en.wikipedia.org/wiki/Java_(programming_language))/[Spring Boot](https://spring.io/projects/spring-boot) backend
and [Thymeleaf](https://en.wikipedia.org/wiki/Thymeleaf) frontend.


### Backend

- Language: Java 21 
- Frameworks: 
  - Java Framework used to build (Web) Applications and REST endpoints.
    - [Spring Boot](https://spring.io/projects/spring-boot) 3.x:
    - Thymeleaf
-  Authentication and authorization framework:
  - [Spring Security](https://spring.io/projects/spring-security):
- Databases:
  - [PostgreSQL](https://www.postgresql.org/about/) version 17 (relational core)
  - [MongoDB](https://www.mongodb.com/) version 8, provisioned (Docker) for
    future content storage; not yet wired to a feature
- Database schema migrations:
  - [Liquibase](https://www.liquibase.com/) (migrations applied at application startup)
- Build and dependency management tool:
  - [Maven](https://maven.apache.org/what-is-maven.html)
- Containerization:
  - [Podman](https://en.wikipedia.org/wiki/Podman) (preferred over [Docker](https://en.wikipedia.org/wiki/Docker_(software))) 
    to containerize parts of the application as container images that can run as autonomous containers. 


### Frontend

The frontend is **server-rendered**: there is no separate frontend application.

- [Thymeleaf](https://www.thymeleaf.org/) templates rendered by the backend:
  public pages (home, login, register, privacy), the student area (dashboard,
  course catalogue, course and lesson pages), the instructor authoring area
  (courses, lessons, roster), the admin area (accounts, course moderation),
  and styled error pages
- [thymeleaf-extras-springsecurity6](https://github.com/thymeleaf/thymeleaf-extras-springsecurity)
  to display authentication data (such as the logged-in username) in the pages
- Server-side form handling with bean validation (no JavaScript framework yet)
- Plain HTML and CSS


### Development Tools

- JetBrains **IntelliJ IDEA**: **IDE** 
- **Git**: Version control
- [**Maven**](https://en.wikipedia.org/wiki/Apache_Maven): Build and dependency management
- **Podman**: Containerization


## Getting Started


### Prerequisites


See the [Tech Stack](#tech-stack) section.


### Installation

- Clone the `ebouchut/learn-dev` Git Repository
- Install a container engine: **Podman** (recommended) or Docker, plus Docker Compose

#### Clone the Git repository

```shell
#
git clone git@github.com:ebouchut/learn-dev.git
# git clone https://github.com/ebouchut/learn-dev.git

cd learn-dev
```

#### Container Engine Setup

You need a container engine and _Docker Compose_ to run the databases.

**Option 1 (recommended): Podman**

We recommend [Podman](https://podman.io/) for **security reasons**:
it runs containers **rootless** by default and does not need a
privileged, always-on daemon.

- on macOS:
  ```shell
  brew install podman podman-compose podman-desktop
  podman machine init   # Do it once: create the Linux VM
  podman machine start  # Start the VM (needed after each reboot)
  ```
- on [Windows and Linux](https://podman.io/docs/installation)

`podman` understands the Docker CLI syntax, and `docker compose` works
against the Podman socket, so every `docker compose ...` command in this
README works unchanged.

**Option 2: Docker**

If you prefer Docker anyway:

- on macOS (read [this for Windows or Linux install](https://docs.docker.com/get-started/get-docker/)):  
  ```shell
  brew install --cask docker   # Docker Desktop (includes Docker Compose)
  ```
- on [Windows and Linux](https://docs.docker.com/get-started/get-docker/)

### Python Setup

This is **optional** if you only need **to run the application**.

This is necessary in order to regenerate the MERISE database diagrams 
(MCD, MLD and MPD) after any changes have been made to the database design.  

You will need to install *Python* and:
- **`mocodo`**: a CLI tool to generate the MCD and MLD database diagrams 
  from a text-file description of the conceptual data model.
- **`tbls`**: a CLI tool used to generate the MPD. 

Here is the procedure:

- [ ] Install Python
    - Install Python on macOS  
      ```shell
      brew install python@3.14
      ```
    - Install Python on other OSes:  
      https://docs.python-guide.org/en/latest/starting/installation/
- [ ] Create a Python Virtual Environment:
  ```shell
  # cd to the folder where you cloned the repository
  python3 -m venv venv       # Do it once
  source venv/bin/activate  # Run this line each time you open a new shell/terminal/window/tab
  ```
- [ ] Install **[mocodo](https://laowantong.github.io/mocodo/doc/fr_refman.html#Installation-et-lancement-du-programme)**
  ```shell
  # mocodo >= 4.3.3 is required (the `make mld` target relies on its `-t diagram` template)
  pip install 'mocodo[svg,clipboard]>=4.3.3'
  ```
- [ ] Install **[tbls](https://github.com/k1LoW/tbls#install)**
  ```shell
  # On macOS
  brew install tbls
  
  # or any OS with Go installed
   go install github.com/k1LoW/tbls@latest
  ```

## Configuration

A fresh clone has no `.env` file (it is gitignored because it holds secrets).
Create it from the provided template, then fill in the secrets:

- Create `.env` from the template (do it once):
  ```shell
  cp .env.example .env
  ```
- Edit `.env`
  - [ ] Set a value for the variables `POSTGRES_PASSWORD`, `LEARNDEV_DB_PASSWORD`, `MONGO_ROOT_PASSWORD`


## Run the application

The quickest way is the Make target, from the project root:

```shell
make run
```

It starts the container machine if needed (Podman only), starts the
database services, then runs the app on http://localhost:8080/
(stop it with `Ctrl+C`).

Alternatively, run the underlying commands yourself:

  ```shell
  # Make sure the required versions of Java and Maven are active for this shell
  sdk env 
  
  # Ensure the Podman "machine" is up and running 
  podman info >/dev/null 2>&1 || podman machine start
  
  # Start the "Docker" services for the application 
  docker compose up -d
  
  # Run the app from the project root
  ./mvnw spring-boot:run
  ```

The first command starts the Podman machine if it is not already running.  
Then `docker compose up -d`  starts all the application Docker services 
 as declared in [docker-compose.yaml](docker-compose.yaml)
(the *Docker Compose* configuration file), like this.
For each service (`postgres`, `mongo`, and `mailpit`):

1. Download the Docker image for this service as specified in `docker-compose.yaml` 
  from the [Docker Hub](https://hub.docker.com/) public registry, only if the Docker
  image is not already cached locally.
2. Store the downloaded image in the local Docker image cache.
3. Start a Docker container (if it is not already running) based on this image 
  and the configuration in `docker-compose.yaml`.


> [!NOTE]  
> A Docker init script automatically **creates the database user and the application database**
> when the **`postgres`** service is run **for the first time**.
> It does not create the database structure or populate the database.

> [!NOTE]
> TODO: Explain how the database is created in MongoDB and when. 

> [!NOTE]
> For Docker or Podman to run on macOS and Windows they need a Linux OS.  
> 
> **Why?**  
> Containers rely on Linux kernel features (*namespaces* and *cgroups*).  
> Windows and macOS do not have a *Linux* kernel.  
> This is why Docker Desktop and Podman run a lightweight *Linux* VM 
> behind the scenes.
> The containers run inside that hidden *VM*, not directly on macOS/Windows.


## Package for production (Docker)

The [Dockerfile](Dockerfile) builds a self-contained application image in
two stages (Maven build, then a minimal JRE runtime running as a non-root
user, `prod` profile active by default):

```shell
docker build -t learn-dev .
docker run --rm -p 8080:8080 --env-file .env.prod learn-dev
```

The runtime configuration comes entirely from environment variables (see
[.env.example](.env.example)): the PostgreSQL coordinates and credentials,
and the SMTP relay (`SMTP_*`) used by the password-reset and
email-verification emails. The prod profile assumes a TLS-terminating
reverse proxy in front of the app: it honors `X-Forwarded-*` headers and
marks the session cookie `Secure`.

> [!NOTE]
> Packaging is local only: the image is not pushed to a registry, and no
> Maven artifact is deployed to any public repository (the `pom.xml`
> deliberately has no `distributionManagement`).

## Stop the Application

This command stops all the application services containers 
declared in the Docker Compose file (`docker-compose.yaml`):

  ```shell
  docker compose stop
  ```


### Docker Terminology

I use **Docker Compose** (a CLI tool) to describe and handle the lifecycle of services that comprise my application.

A **service** is basically a component of the application packaged as a Docker container.
It specifies the Docker image and version, configuration, and the network and Docker volume(s) if any.

A **Docker image** is pre-packaged piece of software that can work as a standalone on Linux. 
**Docker Hub** is a  public registry that hosts and serves public Docker images.


### Postgres Service

Once the `postgres` service container and its named data volume 
have been created with `docker compose up -d`,
you can stop then restart the `postgres` service container individually.
Make sure you stopped the application beforehand.

#### Stop Postgres


```shell
docker compose stop postgres
```

This command stops the `postgres` service container.
It does NOT remove its data volume (its databases).

#### Start Postgres

This command **restarts the existing stopped** `postgres` service container.  
If the service container does not already exist, use `docker compose up -d` to create it.

```shell
docker compose start postgres
```  


Now, check that `postgres` is running:

```shell
docker compose ps | grep postgres
```

> To recreate the database, and start from scratch you need to stop the `postgres` container 
> and remove the (data) volumes.   
> See the `Remove the Postgres Databases` section for details.    


#### Remove the Postgres Databases

Stops and **remove** the `postgres` service **container and its data volumes** (meaning all its databases).

> [!CAUTION]
> This **destructive command** will:
> - stop and remove the `postgres` service container, 
> - **remove ALL its databases: structure and content**,
>   (i.e., everything created by Postgres running in the container).

```shell
docker compose down -v postgres
```


### Mongo Service

Once the `mongo`service container has been created with `docker compose up -d`,
you can stop then restart the `mongo` service container individually.


#### Stop MongoDB

```shell
docker compose stop mongo
```
This command stops the `mongo` service container.
It does NOT remove its data volume (i.e., the MongoDB databases created in this container).


#### Start MongoDB

This command **restarts the existing stopped** `mongo` service container.  
If the service container does not already exist, use `docker compose up -d` to create it.

```shell
docker compose start mongo
```

Now, check that `mongo` is running:

```shell
docker compose ps | grep mongo
```


#### Remove MongoDB and its Databases

> [!CAUTION]
> This **destructive command** will:
> - stop and remove the `mongo` service container, 
> - **remove** its data **volumes** (i.e., **ALL** the **databases** created by MongoDB running in the container).

```shell
docker compose down -v mongo 
```

Where:
- `-v` request Compose to remove the named data volumes created for this service


### Mailpit Service (fake SMTP)

**Mailpit** catches every email the application sends in development
(for example the password-reset email): nothing leaves your machine.
It starts with the other services (`docker compose up -d`).

- **Web UI (browse the caught emails):** http://localhost:8025
- SMTP endpoint used by the app (dev profile): `localhost:1025`

See [ADR-0004](docs/adr/0004-use-mailpit-as-local-smtp-catcher.md) for why
Mailpit was chosen.

#### Stop Mailpit

```shell
docker compose stop mailpit
```

This command stops the `mailpit` service container.
The application keeps running without it, but any email it tries to send
(for example the password reset email) is lost until Mailpit is started again.

#### Start Mailpit

This command **restarts the existing stopped** `mailpit` service container.

```shell
docker compose start mailpit
```

Now, check that `mailpit` is running:

```shell
docker compose ps | grep mailpit
```

#### Remove the Mailpit Data

There is **nothing to remove**: unlike `postgres` and `mongo`, the `mailpit`
service has **no data volume**. Mailpit keeps the caught emails **in memory
only**, so they disappear as soon as the container stops.

To clear the caught emails without stopping Mailpit:

- delete them from the [Web UI](http://localhost:8025), or
- restart the service:

```shell
docker compose restart mailpit
```


## Project Status

For up-to-date information about the status of the project, 
visit [this link](https://github.com/users/ebouchut/projects/7/views/3). 


## Documentation

- [API reference (Javadoc)](https://www.ericbouchut.com/learn-dev/javadoc/index.html) —
  the code reference rendered from the Javadoc comments, republished from `dev` on
  each merge by the [Lint workflow](https://github.com/ebouchut/learn-dev/actions/workflows/lint.yml);
  build it locally with `make javadoc` (see
  [CONTRIBUTING](CONTRIBUTING.md#generating-the-documentation)).
- [ARCHITECTURE.md](ARCHITECTURE.md) — how the pieces fit together (layers, request flow, authentication, data, testing).
- [docs/tech-stacks.md](docs/tech-stacks.md) — catalogue of tools, languages, and frameworks with versions used in the project.
- [GLOSSARY.md](GLOSSARY.md) — definitions of the domain and technical terms used across the project
  (🇫🇷 French version: [GLOSSAIRE.md](GLOSSAIRE.md)).
- [Architecture Decision Records](docs/adr/README.md) — A list of design decisions and their trade-offs.
- [docs/rgaa.md](docs/rgaa.md) — accessibility (RGAA) criteria map: what is expected for the DWWM,
  how and where each criterion is fulfilled;
  [docs/rgaa-audit.md](docs/rgaa-audit.md) is the tooled self-audit report
  (Lighthouse, axe-core, keyboard walkthrough).
- [Mockups and wireframes (Figma)](https://www.figma.com/design/2q1Rt5NGbQ1w8gRtRGoF4A) —
  read-only Figma file with the high-fidelity mockups (Catppuccin theme) and the
  low-fidelity wireframes of the frontend pages; the browsable HTML mockups and
  their study docs live in [docs/design/](docs/design/theme-exploration.md)
  (see [mockups-explained.md](docs/design/mockups-explained.md),
  🇫🇷 [mockups-explained-fr.md](docs/design/mockups-explained-fr.md)).
- [Test coverage (Codecov)](https://app.codecov.io/gh/ebouchut/learn-dev) —
  live coverage dashboard fed by CI
  (see [ADR-0012](docs/adr/0012-publish-test-coverage-to-codecov.md));
  the raw JaCoCo report is also published as the `jacoco-coverage-report`
  artifact of each [Tests workflow run](https://github.com/ebouchut/learn-dev/actions/workflows/test.yml),
  and locally `make test` writes it to `target/site/jacoco/index.html`
  (see [CONTRIBUTING](CONTRIBUTING.md#test-coverage-report-jacoco) for details).
- [Code quality report (Checkstyle)](https://www.ericbouchut.com/learn-dev/checkstyle/checkstyle.html) —
  the latest report, rendered online via GitHub Pages (refreshed on each merge to `dev`);
  also published as the `checkstyle-report` artifact of each
  [Lint workflow run](https://github.com/ebouchut/learn-dev/actions/workflows/lint.yml);
  locally, `./mvnw checkstyle:checkstyle` writes `target/reports/checkstyle.html`
  (see [CONTRIBUTING](CONTRIBUTING.md#code-style-and-formatting) for the code style).


## Contributing

**[CONTRIBUTING.md](CONTRIBUTING.md)** contains:

- How to help
- [Code of Conduct](https://github.com/ebouchut/learn-dev?tab=contributing-ov-file#code-of-conduct)
- [Architecture overview](https://github.com/ebouchut/learn-dev?tab=contributing-ov-file#architecture-overview)
- [Architecture Decision Records](https://github.com/ebouchut/learn-dev?tab=contributing-ov-file#architecture-decision-records-adr) (ADRs)
- Codebase:
    - Documentation
    - [MonoRepo](https://github.com/ebouchut/learn-dev?tab=contributing-ov-file#monorepo)
    - [Directory structure](https://github.com/ebouchut/learn-dev?tab=contributing-ov-file#directory-structure)
    - [Feature-based package layout](https://github.com/ebouchut/learn-dev?tab=contributing-ov-file#feature-based-package-layout)
    - [File naming conventions](https://github.com/ebouchut/learn-dev?tab=contributing-ov-file#file-naming-convention)
- **Database**:
    - [Database Naming Conventions](https://github.com/ebouchut/learn-dev?tab=contributing-ov-file#database-naming-conventions)
    - Database schema:
      - **[MCD](https://github.com/ebouchut/learn-dev?tab=contributing-ov-file#mcd-diagram)**,
      - **[MLD](https://github.com/ebouchut/learn-dev?tab=contributing-ov-file#mld-diagram)**, 
      - **[MPD](https://github.com/ebouchut/learn-dev?tab=contributing-ov-file#mpd-diagram)**, 
      - **[ERD](https://github.com/ebouchut/learn-dev?tab=contributing-ov-file#erd-diagram)**.
    - [Database migrations](https://github.com/ebouchut/learn-dev?tab=contributing-ov-file#database-migrations-liquibase)
- Git:
    - Git [branching strategy](https://github.com/ebouchut/learn-dev?tab=contributing-ov-file#git-branching-strategy)
    - Git [commit message convention](https://github.com/ebouchut/learn-dev?tab=contributing-ov-file#git-commit-message-convention)
- Dependencies:
    - Adding dependencies
    - Installing dependencies
- Running tests
- Submitting Pull Requests
- TODO: ...


## License

This project is **dual-licensed**  
under **AGPL v3** for open source use    
and a **commercial license** for proprietary use.

See [LICENSE](LICENSE) for details.


## Authors

**Eric Bouchut**:

- LinkedIn: https://linkedin.com/in/ebouchut
- GitHub: [@ebouchut](https://github.com/ebouchut)
- Blog: https://EricBouchut.com


## Resources

- 💙 A big thank you to **our instructors** for their involvement and help:
  - [Alejandro Seijo](https://www.linkedin.com/in/alejandro-f-seijo-1541aa189/),
  - [Jean-César Bazin](https://www.linkedin.com/in/jean-c%C3%A9sar-bazin-a7bab9176/),
  - [Aubry Capitone](https://www.linkedin.com/in/a-capitone/)
  - [Esteban Bare](https://www.linkedin.com/in/esteban-bare-337927284/),
- [REAC Developpeur Web et Web mobile](https://www.francecompetences.fr/recherche/rncp/37674/)

<!-- GitHub Badges: image and link definitions -->

[build-image]: https://github.com/ebouchut/learn-dev/actions/workflows/build.yml/badge.svg?branch=dev&event=push
[build-url]:   https://github.com/ebouchut/learn-dev/actions/workflows/build.yml
[test-image]:  https://github.com/ebouchut/learn-dev/actions/workflows/test.yml/badge.svg?branch=dev&event=push
[test-url]:    https://github.com/ebouchut/learn-dev/actions/workflows/test.yml
[coverage-image]: https://codecov.io/gh/ebouchut/learn-dev/branch/dev/graph/badge.svg
[coverage-url]:   https://app.codecov.io/gh/ebouchut/learn-dev
[lint-image]:  https://github.com/ebouchut/learn-dev/actions/workflows/lint.yml/badge.svg?branch=dev&event=push
[lint-url]:    https://github.com/ebouchut/learn-dev/actions/workflows/lint.yml
[lint-report-image]: https://img.shields.io/badge/-report-blue
[lint-report-url]:   https://www.ericbouchut.com/learn-dev/checkstyle/checkstyle.html
[schema-drift-image]: https://github.com/ebouchut/learn-dev/actions/workflows/schema-drift.yml/badge.svg?branch=dev&event=push
[schema-drift-url]:   https://github.com/ebouchut/learn-dev/actions/workflows/schema-drift.yml
[github-issues-image]: https://img.shields.io/github/issues/ebouchut/learn-dev
[github-issues-url]: https://github.com/ebouchut/learn-dev/issues
