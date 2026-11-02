# ADR-003: node-pg-migrate with raw SQL migrations

- **Status**: Accepted
- **Date**: 2026-11-02

## Context
The schema must be created the same way on a local machine, in tests and on the host.

## Decision
Use `node-pg-migrate` with SQL files that have `-- Up Migration` and `-- Down Migration` blocks. It is a runtime dependency. The Docker image runs `npm run migrate up` before the server starts.

## Options considered
- Hand-run SQL.
- JavaScript migrations.
- ORM migrations.

## Consequences
One command works everywhere. The test setup runs the migration on `taskmanager_test`. The setup refuses a database name that does not end in `_test`. `npm audit` reports 3 moderate advisories in the `node-pg-migrate` tree (see technical debt TD-003).

## Links
- `backend/migrations/1730500000000_create-tasks.sql`
- `backend/test/globalSetup.ts`
- Source: `../../operations/archives/2026-11-03-task-manager-phases-1-3/plan_phase_1.md` DES-P1-007
