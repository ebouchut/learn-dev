# Learn-dev: An Interactive Programming Learning Platform

## Presentation

> An interactive programming learning platform with automated assessment and hands-on practice.  

This project aims to enable students to learn programming through hands-on practice
with automated assessment and real-time feedback.

It is also my capstone project for the [Web and Web Mobile Developer REAC certification](https://www.francecompetences.fr/recherche/rncp/37674/), which I am currently undergoing at [La Plateforme_](https://laplateforme.io).




## Goals

- Provide an interactive environment for learning programming concepts
- Implement automated code assessment and feedback
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

TODO


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

This starts all the Docker services for the application:

  ```shell
  docker compose up -d
  ```

**For each service** (`postgres`, `mongo`)
declared in the *Docker Compose* configuration file
(`docker-compose.yaml`), *Docker Compose*:

1. downloads the Docker image (if not cached yet) from the Docker Hub registry,
2. stores the downloaded image in the local Docker image cache,
3. starts a Docker container based on this image (if it is not already running).

## Stop the Application

This stops all the Docker services for the application:

  ```shell
  docker compose down
  ```


### Docker Terminology

I use ** Docker Compose** (a CLI tool) to describe and handle the lifecycle of services that comprise my application.

A **service** is basically a component of the application packaged as a Docker container.
It specifies the Docker image and version, configuration, and the network and Docker volume(s) if any.

A Docker image is pre-packaged piece of software that can work as a standalone on Linux. 
**Docker Hub** is a  public registry that hosts and serves public Docker images.


### Postgres Service

#### Start Postgres

Running the app using `docker compose up -d` 
starts **all** the application services, including `postgres`.

To only start the `postgres` service:

```shell
docker compose start -d postgres
docker compose logs postgres
```  
> [!NOTE]
> 
> The above command downloads, installs the `postgres` Docker image
> specified by the `postgres` service in `docker-compose.yaml`.
> Then it runs a Docker container with this image.

> [!NOTE]  
> A Docker init script automatically **creates the database user and the application database**
> when the **`postgres`** service is run **for the first time**.

Now, check that `postgres` is running:

```shell
docker compose ps | grep postgres
```

> To recreate the database, and start from scratch you need to stop the `postgres` container 
> and remove the (data) volumes.   
> See the `Remove all Posgres Databases` section for details.    

#### Stop Postgres

```shell
docker compose stop postgres
```

#### Remove all Postgres Databases

Stops and remove the `postgres` container and its data volume.

> [!CAUTION]
> This is a **destructive command** that will **remove all the databases 
> (structure and content)** created by Postgres running in the container.

```shell
docker compose stop postgres  # Stop the container
docker rm           postgres  # Remove the container
docker volume rm    pg_data   # Remove the named volume 
```


### Mongo Service

#### Start MongoDB

```shell
docker compose start mongo
```

Now, check that `mongo` is running:

```shell
docker compose ps | grep mongo
```

#### Stop MongoDB

```shell
docker compose stop mongo
```

#### Remove MongoDB and its Databases

> [!CAUTION]
> This **destructive command** will stop and remove the container, then **remove** its data **volume** 
> (all the databases created by MongoDB running in the container).

```shell
docker compose stop mongo # Stop container
docker  rm          mongo # Remove the stopped container
docker volume rm    learn-dev_mongo_data # Remove the named volume   
```


## Project Status

See the [GitHub Project](https://github.com/users/ebouchut/projects/7/views/3) for up-to-date information.


## Contributing

See the [CONTRIBUTING.md](CONTRIBUTING.md) file for how to help out.    
It contains detailed guidelines, including:

- Architecture overview
- Code:
    - Documentation
    - Directory structure
    - Naming conventions
- Database:
    - Database schema, ERD (Entity Relationships Diagram)
    - Running database migrations
- Git:
    - Git branching strategy
    - Git commit message conventions
- Dependencies:
    - Adding dependencies
    - Installing dependencies
- Running tests
- Submitting pull requests
- ...


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
