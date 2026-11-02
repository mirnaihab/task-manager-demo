# ADR-004: The server computes overdue and dashboard counts

- **Status**: Accepted
- **Date**: 2026-11-02

## Context
Android and iOS must show the same overdue state and the same counts.

## Decision
The server sets `overdue` on each Task. It also returns `GET /api/summary` with the counts. The rule is `dueDate < today AND status <> DONE`. "Today" is the server UTC date.

## Options considered
Compute in each app.

## Consequences
There is one rule set and no drift between the apps. The apps call the API again after each change (DES-P2-009, DES-P3-011). A user near midnight in another time zone can see a one-day difference (assumption A-2, risk R-5).

## Links
- `backend/src/tasks/task.service.ts`
- `backend/src/tasks/task.repository.ts`
- Source: `../../operations/archives/2026-11-03-task-manager-phases-1-3/plan_phase_1.md` DES-P1-002, DES-P1-003
