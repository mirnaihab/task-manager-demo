# ADR-002: Express 5, zod and pg with no ORM

- **Status**: Accepted
- **Date**: 2026-11-02

## Context
The data model is one table. The backend needs input validation, SQL access and a small HTTP layer.

## Decision
Use Express 5 for HTTP, zod for validation and `pg` for plain SQL. Code is in three layers: routes, service, repository. `createApp({ pool, clock })` receives its dependencies.

## Options considered
- Fastify.
- Prisma.
- Drizzle.

## Consequences
Fewer parts and one validation source. The team writes SQL by hand. All SQL uses bind parameters. The `UPDATE` column names come from a fixed allow-list (DES-P1-005).

## Links
- `backend/src/app.ts`
- `backend/src/tasks/task.repository.ts`
- `backend/test/tasks.api.test.ts`
- Source: `../../operations/archives/2026-11-03-task-manager-phases-1-3/plan_phase_1.md` DES-P1-005
