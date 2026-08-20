# learn-dev application image (multi-stage).
# Build:  docker build -t learn-dev .
# Run:    docker run --rm -p 8080:8080 --env-file <prod env file> learn-dev
# The image is self-contained and is NOT published anywhere; no Maven
# artifact is deployed to any public repository either (the pom has no
# distributionManagement).

# ---- Stage 1: build the executable jar ------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Warm the dependency cache first so code changes do not re-download the world.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package

# ---- Stage 2: minimal runtime ----------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a non-root user.
RUN useradd --system --home /app learndev
USER learndev

COPY --from=build /workspace/target/*.jar app.jar

# Configuration comes from environment variables (see .env.example):
# POSTGRES_HOST/PORT, LEARNDEV_DB_*, SMTP_*, ...
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
