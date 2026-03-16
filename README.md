# Learn-dev an Interactive Programming Learning Platform

## Presentation

> An interactive programming learning platform with automated assessment and hands-on practice.  

This project aims to enable students to learn programming through hands-on practice
with automated assessment and real-time feedback.

It is also my capstone project for
the [Web and Web Mobile Developer REAC certification](https://www.francecompetences.fr/recherche/rncp/37674/)
I am currently undergoing at [La Plateforme_](https://laplateforme.io).


## Goals

- Provide an interactive environment for learning programming concepts
- Implement automated code assessment and feedback
- Support multiple user roles (such as Student, Instructor, Admin)
- Demonstrate full-stack development skills using industry standards


## Tech Stack

This project is built with [Java](https://en.wikipedia.org/wiki/Java_(programming_language))/[Spring Boot](https://spring.io/projects/spring-boot) backend
and [React](https://react.dev/) frontend.

### Backend

- Java 21 
- [Spring Boot](https://spring.io/projects/spring-boot) 3.x: Java Framework used to build (Web) Applications.
    and REST endpoints
- [Spring Security](https://spring.io/projects/spring-security): Authentication and authorization framework
- [PostgreSQL](https://www.postgresql.org/about/) 15+: Database
- [Maven](https://maven.apache.org/what-is-maven.html): Build and dependency management tool

### Frontend

TODO


### Development Tools

- JetBrains **IntelliJ IDEA**: **IDE** 
- **Git**: Version control
- [**Maven**](https://en.wikipedia.org/wiki/Apache_Maven): Build and dependency management
- **GitHub Actions**: CI/CD pipeline
- **Docker**: Containerization
- **Swagger**: API documentation


## Project Structure

Learn-dev uses a **monorepo structure** which offers the following advantages:

- **Consistent versioning** that applies both to backend and frontend.  
  A single project for the frontend, backend, and documentation makes it easy to manage all of them.

```txt
learn-dev/
├── backend/           # Spring Boot application
│   ├── src/           # Source code
│   └── pom.xml        # Maven configuration file
├── frontend/          # React application
├── docs/              # Project documentation
│   ├── backend        # Backend documentation
│   └── frontend       # tonend documentation
├── README.md          # Project documentation entry point
└── .github/workflows/ # GitHub Actions workflows
```


## Getting Started

### Prerequisites

See the [Tech Stack](#tech-stack) section.

### Installation

TODO


## Project Status

See the [GitHub Project](https://github.com/users/ebouchut/projects/7/views/3) for up-to-date information.


## Contributing

This is a capstone project for educational purposes.   
While not actively seeking contributions for now,
feedback and suggestions are welcome through [GitHub issues](https://github.com/ebouchut/learn-dev/issues).


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

- 💙A big thank you to **our instructors** for their involvement and help:
  - [Alejandro Seijo](https://www.linkedin.com/in/alejandro-f-seijo-1541aa189/),
  - [Jean-César Bazin](https://www.linkedin.com/in/jean-c%C3%A9sar-bazin-a7bab9176/),
  - [Aubry Capitone](https://www.linkedin.com/in/a-capitone/)
  - [Esteban Bare](https://www.linkedin.com/in/esteban-bare-337927284/),
- [REAC Developpeur Web et Web mobile](https://www.francecompetences.fr/recherche/rncp/37674/)
- [La Plateforme_](https://laplateforme.io)
