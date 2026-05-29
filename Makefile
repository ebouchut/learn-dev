# Ignore existing files with the same name as phony targets
.PHONY: diagrams mcd mld mpd ddl clean

# Default make target used if none specified
.DEFAULT_GOAL := help

# Display the syntax with available targets
help:
	@echo "Available targets:"
	@echo "  make diagrams  — generate MCD, MLD, and MPD"
	@echo "  make mcd       — generate MCD"
	@echo "  make mpd       — generate MPD"
	@echo "  make ddl       — regenerate DDL (SQL with Postgres database structure)"
	@echo "  make clean     — remove generated diagrams"

# Generate all database diagrams (MCD, MLD, MPD)
diagrams: mcd mld mpd
	@echo "All diagrams generated (MCD, MLD, MPD)"

# Generate MCD from Mocodo source
mcd:
	@echo "Generating MCD..."
	mocodo --input docs/database/mcd/learn-dev.mcd --output_dir docs/database/mcd --colors brewer+1
	@echo "MCD generated in docs/database/mcd/"

# Generate MLD (2 step process):
# - transform MCD source into a MLD source: docs/database/mld/learn-dev_mld.mcd
# - Render the MLD source
mld:
	@echo "Generating MLD..."
	mocodo --input docs/database/mcd/learn-dev.mcd     --output_dir docs/database/mld --transform mld diagram
	rm -f docs/database/mld/learn-dev.*
	mocodo --input docs/database/mld/learn-dev_mld.mcd --output_dir docs/database/mld --colors ocean
	@echo "MLD generated in docs/database/mld/learn-dev_mld.svg"

# Generate MPD from the PostgreSQL database
mpd:
	@echo "Generating MPD from PostgreSQL Database..."
	tbls docs/database/mpd --force
	@echo "MPD generated in docs/database/mpd/"

# Generate a SQL file to create the database structure (tables, associations)
# (with Postgres DDL syntax)
ddl:
	@echo "Generating DDL (Postgres SQL syntax)..."
	mocodo --input docs/database/mcd/learn-dev.mcd --output_dir docs/database/ddl -t postgres
	@echo "DDL generated in docs/database/ddl/"

# Clean up generated diagram files (keep source files)
clean:
	@echo "Cleaning up generated diagrams..."
	rm -f docs/database/mcd/learn-dev.svg
	rm -f docs/database/mcd/learn-dev_geo.json
	rm -f docs/database/mld/*
	rm -f  docs/database/mpd/*
	rm -f docs/database/ddl/*
	@echo "Cleaned"
