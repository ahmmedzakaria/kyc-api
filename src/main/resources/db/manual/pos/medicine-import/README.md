# POS Medicine Import Scripts

These scripts are manual PostgreSQL/psql import helpers for the Bangladesh medicine dataset.

They are intentionally kept outside `src/main/resources/db/migration` because they are not Flyway migrations:

- `02_staging_import.sql` uses psql `\copy` commands.
- CSV paths are local developer paths and must exist on the machine running `psql`.
- The current tables are exploratory medicine lookup tables and do not yet follow the final POS `pos_` table prefix and audit-column rules.

Run order:

```bash
./upload-csv-to-db.sh
```

Optional database overrides:

```bash
DB_HOST=localhost DB_PORT=5433 DB_USER=postgres DB_NAME=medicine_db ./upload-csv-to-db.sh
```

Before promoting these scripts to Flyway, convert local `\copy` usage, add the POS table prefix, and include the required audit columns.
