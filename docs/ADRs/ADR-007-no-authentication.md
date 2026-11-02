# ADR-007: No authentication

- **Status**: Accepted
- **Date**: 2026-11-02

## Context
The scope is single user with no authentication (`docs/user-stories.md`). The hosted API will be public.

## Decision
Add no authentication. Use a non-guessable service URL. The README must state the limit.

## Options considered
A shared API key header.

## Consequences
Anyone with the URL can read and change tasks (risk R-1). Add an API key if the demo becomes public. Android debug and iOS debug builds allow plain HTTP only for local hosts.

## Links
- `docs/HLD.md` risk R-1
- `operations/specs/feature_task-manager/plan_phase_4.md` SEC-403
