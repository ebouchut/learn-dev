# Ignore existing files with the same name as phony targets
.PHONY: help diagrams mcd mld mpd javadoc clean check-schema-drift test run

# Default make target used if none specified
.DEFAULT_GOAL := help

# Display the syntax with available targets
help:
	@echo "Available targets:"
	@echo "  make diagrams  — generate Merise MCD, MLD, and MPD diagrams"
	@echo "  make mcd       — generate MCD"
	@echo "  make mld       — generate MLD"
	@echo "  make mpd       — generate MPD"
	@echo "  make javadoc   — generate the Java API reference (Javadoc)"
	@echo "  make clean     — remove generated diagrams"
	@echo "  make check-schema-drift — fail if a Liquibase column is missing from the MCD"
	@echo "  make test      — run the test suite via Testcontainers"
	@echo "  make run       — start the databases and run the Spring Boot app"

# Generate all database diagrams (MCD, MLD, MPD)
diagrams: mcd mld mpd
	@echo "All diagrams generated (MCD, MLD, MPD)"

# Generate the Java API reference (Javadoc) from the /** */ comments.
# The plugin is not bound to any lifecycle phase, so this is the only way it
# runs: a normal build (make test, mvn package) never pays for it.
javadoc:
	@echo "Generating the Javadoc..."
	./mvnw -q -B javadoc:javadoc
	@echo "Javadoc generated in target/reports/apidocs/index.html"

# Run the test suite. Tests use Testcontainers (a real PostgreSQL), so a
# container engine must be running. Under Podman, point Testcontainers at the
# Podman socket and disable Ryuk. Under Docker, run the Maven wrapper directly.
test:
	@echo "Running tests..."
	@SOCK=""; \
	if command -v podman >/dev/null 2>&1; then \
		SOCK=$$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}' 2>/dev/null); \
	fi; \
	if [ -n "$$SOCK" ]; then \
	  echo "Podman detected, socket: $$SOCK"; \
	  DOCKER_HOST="unix://$$SOCK" TESTCONTAINERS_RYUK_DISABLED=true ./mvnw test; \
	else \
	  ./mvnw test; \
	fi

# Fail if a Liquibase table column is missing from the MCD diagram source.
# Heuristic (column-name presence only); CI-friendly (non-zero exit on drift).
check-schema-drift:
	python3 scripts/check_schema_drift.py

# Run the Spring Boot app locally. Container-engine agnostic: if Podman is
# installed, start its machine when the socket is unreachable; otherwise assume
# Docker. Then bring up the Postgres + Mongo containers and run the app in the
# foreground (Ctrl+C to stop). Run from the project root to load the ./.env file.
run:
	@if command -v podman >/dev/null 2>&1; then \
	  podman info >/dev/null 2>&1 || podman machine start; \
	fi
	@echo "Starting databases..."
	docker compose up -d
	@echo "Starting the app (http://localhost:8080/ , Ctrl+C to stop)..."
	./mvnw spring-boot:run

# Generate MCD from Mocodo source
mcd:
	@echo "Generating MCD..."
	mocodo --input docs/database/merise/learn-dev.mcd --output_dir docs/database/merise --colors brewer+1
	@echo "MCD generated: docs/database/merise/learn-dev.svg"

# IMPORTANT: requires mocodo version 4.3.3+
# Generate the MLD (Modele Logique des Donnees) from the SINGLE source of truth:
# the conceptual MCD (learn-dev.mcd). Generated artifacts (do NOT edit by hand):
#   1. learn-dev_mld.mcd — renderable logical model, auto-derived via `-t diagram`.
#   2. learn-dev_mld.md  — relational schema as Markdown, via `-t mld`.
#   3. learn-dev_mld.svg — the MLD diagram, rendered from learn-dev_mld.mcd.
mld:
	@echo "Generating MLD..."
	mocodo --input docs/database/merise/learn-dev.mcd --output_dir docs/database/merise --colors brewer+1 -t diagram mld
	mocodo --input docs/database/merise/learn-dev_mld.mcd --output_dir docs/database/merise --colors ocean
	@echo "MLD generated: docs/database/merise/learn-dev_mld.svg"

# Generate MPD from the PostgreSQL database.
# Percent-encode the password because it may contain URL-reserved characters.
mpd:
	@echo "Generating MPD from PostgreSQL Database..."
	@if [ -e .env ]; then \
	  while IFS= read -r line; do \
	    case "$$line" in [A-Za-z_]*=*) export "$$line";; esac; \
	  done < .env; \
	fi; \
	if [ -n "$$TBLS_DSN" ]; then \
	  DSN="$$TBLS_DSN"; \
	elif [ -n "$$LEARNDEV_DB_USER" ]; then \
	  ENC_LEARNDEV_DB_PASSWORD=$$(printf '%s' "$$LEARNDEV_DB_PASSWORD" | python3 -c 'import sys,urllib.parse; print(urllib.parse.quote(sys.stdin.read(), safe=""))'); \
	  DSN="postgres://$$LEARNDEV_DB_USER:$$ENC_LEARNDEV_DB_PASSWORD@$$POSTGRES_HOST:$$POSTGRES_PORT/$$LEARNDEV_DB_NAME?sslmode=disable"; \
	else \
	  echo "TBLS_DSN is required (or define LEARNDEV_DB_* / POSTGRES_* in .env)" >&2; exit 1; \
	fi; \
	tbls doc "$$DSN" docs/database/merise/mpd --force
	@echo "MPD generated in docs/database/merise/mpd/"

# Clean up generated diagram files
clean:
	@echo "Cleaning up generated diagrams..."
	rm -f docs/database/merise/learn-dev.svg
	rm -f docs/database/merise/learn-dev.md
	rm -f docs/database/merise/learn-dev_geo.json

	rm -f docs/database/merise/learn-dev_mld.mcd
	rm -f docs/database/merise/learn-dev_mld.svg
	rm -f docs/database/merise/learn-dev_mld.md
	rm -f docs/database/merise/learn-dev_mld_geo.json
	@echo "Cleaned"
