# Ignore existing files with the same name as phony targets
.PHONY: help diagrams mcd mld mpd ddl clean

# Default make target used if none specified
.DEFAULT_GOAL := help

# Display the syntax with available targets
help:
	@echo "Available targets:"
	@echo "  make diagrams  — generate MCD, MLD, and MPD"
	@echo "  make mcd       — generate MCD"
	@echo "  make mld       — generate MLD"
	@echo "  make mpd       — generate MPD"
	@echo "  make ddl       — regenerate DDL (SQL with Postgres database structure)"
	@echo "  make clean     — remove generated diagrams"

# Generate all database diagrams (MCD, MLD, MPD)
diagrams: mcd mld mpd
	@echo "All diagrams generated (MCD, MLD, MPD)"

# Generate MCD from Mocodo source
mcd:
	@echo "Generating MCD..."
	mocodo --input docs/database/merise/learn-dev.mcd --output_dir docs/database/merise --colors brewer+1
	@echo "MCD generated: docs/database/merise/learn-dev.svg"

# Generate MLD (2 step process):
# - transform MCD source into a MLD source: docs/database/merise/learn-dev_mld.mcd
# - Render the MLD source
mld:
	@echo "Generating MLD..."
	mocodo --input docs/database/merise/learn-dev.mcd     --output_dir docs/database/merise --transform mld diagram
	mocodo --input docs/database/merise/learn-dev_mld.mcd --output_dir docs/database/merise --colors ocean
	@echo "MLD generated: docs/database/merise/learn-dev_mld.svg"

# Generate MPD from the PostgreSQL database
mpd:
	@echo "Generating MPD from PostgreSQL Database..."
	@test -n "$(TBLS_DSN)" || (echo "TBLS_DSN is required (e.g., postgres://user:pass@host:5432/dbname)" >&2; exit 1)
	tbls doc "$(TBLS_DSN)" docs/database/merise --force
	@echo "MPD generated in docs/database/merise/"

# Generate a SQL file to create the database structure (tables, associations)
# (with Postgres DDL syntax)
ddl:
	@echo "Generating DDL (Postgres SQL syntax)..."
	@mkdir -p docs/database/ddl
	mocodo --input docs/database/merise/learn-dev.mcd --output_dir docs/database/ddl -t postgres
	@echo "DDL generated in docs/database/ddl/"

# Clean up generated diagram files
clean:
	@echo "Cleaning up generated diagrams..."
	rm -f docs/database/merise/learn-dev.svg
	rm -f docs/database/merise/learn-dev.md
	rm -f docs/database/merise/learn-dev_geo.json

	rm -f docs/database/merise/learn-dev_mld.svg
	rm -f docs/database/merise/learn-dev_mld.md
	rm -f docs/database/merise/learn-dev_geo_mld.json

	rm -f docs/database/ddl/*
	@echo "Cleaned"
