# ADR-005: PATCH for edit and status change

- **Status**: Accepted
- **Date**: 2026-11-02

## Context
The apps change status and edit fields. Both need validation.

## Decision
Use `PATCH /api/tasks/:id` with a partial body. At least one field is required. The past-date rule applies only when `dueDate` changes (assumption A-3).

## Options considered
A separate status endpoint.

## Consequences
One endpoint and one validation path. A user can rename an overdue task. A PATCH that sets `description` to null clears it.

## Links
- `backend/src/tasks/task.routes.ts`
- `backend/test/tasks.api.test.ts`
- Source: HLD section 10
