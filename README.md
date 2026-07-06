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
- Database:
  - [PostgreSQL](https://www.postgresql.org/about/) version 17
- Build and dependency management tool:
  - [Maven](https://maven.apache.org/what-is-maven.html)
- Containerization:
  - [Podman](https://en.wikipedia.org/wiki/Podman) (preferred over [Docker](https://en.wikipedia.org/wiki/Docker_(software))) 
    to containerize parts of the application as container images that can run as autonomous containers. 


### Frontend

The frontend is **server-rendered**: there is no separate frontend application.

- [Thymeleaf](https://www.thymeleaf.org/) templates rendered by the backend
  (home, login, register, and dashboard pages)
- [thymeleaf-extras-springsecurity6](https://github.com/thymeleaf/thymeleaf-extras-springsecurity)
  to display authentication data (such as the logged-in username) in the pages
- Server-side form handling with bean validation (no JavaScript framework yet)
- Plain HTML and CSS


### Development Tools

- JetBrains **IntelliJ IDEA**: **IDE** 
- **Git**: Version control
- [**Maven**](https://en.wikipedia.org/wiki/Apache_Maven): Build and dependency management
- **Podman**: Containerization
- **Swagger**: API documentation


## Getting Started


### Prerequisites


See the [Tech Stack](#tech-stack) section.


### Installation

- Clone the `ebouchut/learn-dev` Git Repository
- Install Docker, Docker Desktop, and Docker Compose

#### Clone the Git repository

```shell
#
git clone git@github.com:ebouchut/learn-dev.git
# git clone https://github.com/ebouchut/learn-dev.git

cd learn-dev
```

#### Docker Setup

From the project root folder.
Install _Docker_ and _Docker Compose_:

- on macOS (read [this for Windows or Linux install](https://docs.docker.com/get-started/get-docker/)):  
  ```shell
  brew install docker docker-compose docker-desktop
  ```
- on [Windows and Linux](https://docs.docker.com/get-started/get-docker/)

### Python Setup

This is **optional** if you only need **to run the application**.

This is necessary in order to regenerate the MERISE database diagrams 
(MCD, MLD and MPD) after any changes have been made to the database design.  

You will need to install *Python* and:
- **`mocodo`**: a CLI tool to generate the MCD and MLD database diagrams 
  from a text-file description of the conceptual data model.
- **`tbls`**: a CLI tool to reverse engineer the live database to generate the MPD. 

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

- Edit `.env`
  - [ ] Set a value for the variables `POSTGRES_PASSWORD`, `LEARNDEV_DB_PASSWORD`, `MONGO_ROOT_PASSWORD`


## Run the application


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
Then `docker compoose up -d`  starts all the application Docker services 
 as declared in [docker-compose.yaml](docker-compose.yaml)
(the *Docker Compose* configuration file), like this.
For each service (`postgres` and `mongo`):

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


## Stop the Application

This command stops all the application services containers 
declared in the Docker Compose file (`docker-compose.yaml`):

  ```shell
  docker compose down
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
> See the `Remove all Posgres Databases` section for details.    


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


## Project Status

For up-to-date information about the status of the project, 
visit [this link](https://github.com/users/ebouchut/projects/7/views/3). 


## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) — how the pieces fit together (layers, request flow, authentication, data, testing).
- [docs/tech-stacks.md](docs/tech-stacks.md) — catalogue of tools, languages, and frameworks with versions used in the project.
- [GLOSSARY.md](GLOSSARY.md) — definitions of the domain and technical terms used across the project.
- [Architecture Decision Records](docs/adr/README.md) — A list of design decisions and their trade-offs.


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
