# ADR-010: Dates are YYYY-MM-DD strings end to end

- **Status**: Accepted
- **Date**: 2026-11-02

## Context
A due date has no time. A time zone conversion can shift the day.

## Decision
The database column is `DATE`. The `pg` DATE parser returns strings. The API sends `YYYY-MM-DD`. Both apps keep `dueDate` as a string. They convert to a date only for the date picker. The Android picker converts UTC midnight with `Instant.ofEpochMilli(m).atZone(UTC)`.

## Options considered
Use date objects in the apps and in Node.

## Consequences
No day shift in data. Each app needs one tested conversion helper (`DateConversionTest` on Android, config and date tests on iOS).

## Links
- `backend/src/db.ts`
- `android/app/src/main/java/com/demo/taskmanager/ui/DateConversion.kt`
- Source: `../../operations/archives/2026-11-03-task-manager-phases-1-3/plan_phase_1.md` DES-P1-002, `../../operations/archives/2026-11-03-task-manager-phases-1-3/plan_phase_2.md` DES-P2-003, DES-P2-008, `../../operations/archives/2026-11-03-task-manager-phases-1-3/plan_phase_3.md` DES-P3-004
