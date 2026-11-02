# ADR-001: Hosting provider for the API and database

- **Status**: Proposed. Open.
- **Date**: 2026-11-02

## Context
The apps must work on a device other than the developer machine. The API and PostgreSQL must run online. The backend reads `DATABASE_URL` and `PORT`, so it runs on any host that runs a Docker image.

## Decision
Not decided. The recommendation is a Render web service (Docker) with a hosted PostgreSQL. The owner must confirm it and accept the free-tier limits in the phase 4 plan.

## Options considered
- Render web service and Render PostgreSQL.
- Neon for the database and Render for the API.
- Railway.
- Fly.io.

## Consequences
This decision blocks only phase 4 (TASK-P4-001). Free tiers sleep the API after idle time and expire the free database. The README must state these limits (risk R-2).

## Links
- `docs/HLD.md` section 10 and risk R-2
- `operations/specs/feature_task-manager/plan_phase_4.md`
- `backend/Dockerfile`
