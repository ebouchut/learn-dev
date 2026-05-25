#!/bin/bash
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# Postgres container init script — runs automatically on first container start
# (when /var/lib/postgresql/data is empty).
#
# Creates:
#   - a regular (non-superuser) database role: LEARNDEV_DB_USER
#   - the application database:                LEARNDEV_DB_NAME
#
# Required container env vars (set via --env-file):
#   LEARNDEV_DB_NAME      — name of the application database
#   LEARNDEV_DB_USER      — name of the application role      (database "user" name")
#   LEARNDEV_DB_PASSWORD  — password for the application role (database "user" password)
#
# See .env.example for details
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

# Exit on error
set -e

echo "Creating database '${LEARNDEV_DB_NAME}' and role (database user) '${LEARNDEV_DB_USER}'..."

psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" \
     --dbname   "$POSTGRES_DB"   \
<<-EOSQL
    CREATE USER     "${LEARNDEV_DB_USER}" WITH PASSWORD '${LEARNDEV_DB_PASSWORD}';
    CREATE DATABASE "${LEARNDEV_DB_NAME}" OWNER "${LEARNDEV_DB_USER}";
    GRANT ALL PRIVILEGES ON DATABASE "${LEARNDEV_DB_NAME}" TO "${LEARNDEV_DB_USER}";
EOSQL

echo "SUCCESS: Created database '${LEARNDEV_DB_NAME}' and role (database user) '${LEARNDEV_DB_USER}' "
