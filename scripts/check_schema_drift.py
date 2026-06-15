#!/usr/bin/env python3
"""Detect drift between the Liquibase schema and the Merise MCD diagram source.

Forward check only: report every column defined in a Liquibase `CREATE TABLE`
changeset whose name does not appear anywhere in the MCD source. This catches
the common mistake of adding a column to a migration but forgetting to update
`learn-dev.mcd` (so the generated SVG/MLD diagrams omit it).

Heuristic, not a full schema diff: it checks column-NAME presence only, not the
owning entity, type, nullability, or order. Exit code 1 on drift, 0 when clean.

Usage: python3 scripts/check_schema_drift.py
"""

import glob
import re
import sys

CHANGESETS_GLOB = "src/main/resources/db/changelog/changes/V*-create-*-table.sql"
MCD_PATH = "docs/database/merise/learn-dev.mcd"

# Leading tokens that start a constraint clause rather than a column definition.
NON_COLUMN_KEYWORDS = {"primary", "constraint", "foreign", "unique", "check"}


def changeset_columns() -> dict[str, list[str]]:
    """Map each table name to the list of column names in its CREATE TABLE."""
    tables: dict[str, list[str]] = {}
    for path in sorted(glob.glob(CHANGESETS_GLOB)):
        match = re.search(r"CREATE TABLE (\w+)\s*\((.*?)\);", open(path).read(), re.S)
        if not match:
            continue
        table, body = match.group(1), match.group(2)
        columns = []
        for line in body.splitlines():
            token = re.match(r"\s*([a-z_]+)\b", line)
            if token and token.group(1).lower() not in NON_COLUMN_KEYWORDS:
                columns.append(token.group(1))
        tables[table] = columns
    return tables


def mcd_tokens() -> set[str]:
    """All lowercase identifier tokens present in the MCD source."""
    return set(re.findall(r"[a-z_]+", open(MCD_PATH).read()))


def main() -> int:
    tables = changeset_columns()
    if not tables:
        print(f"ERROR: no changesets matched {CHANGESETS_GLOB}", file=sys.stderr)
        return 2
    known = mcd_tokens()

    drift = [
        (table, column)
        for table, columns in sorted(tables.items())
        for column in columns
        if column not in known
    ]

    if not drift:
        n = sum(len(c) for c in tables.values())
        print(f"OK: MCD represents all {n} columns across {len(tables)} tables.")
        return 0

    print("DRIFT: columns in the schema but missing from the MCD "
          f"({MCD_PATH}):", file=sys.stderr)
    for table, column in drift:
        print(f"  - {table}.{column}", file=sys.stderr)
    print("\nAdd the column(s) to the matching entity in the MCD, then "
          "`make mcd && make mld`.", file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main())
