# Archive Summary: Fix TD-001 (pool error listener)

- **Archived**: 2026-11-03
- **Source**: `operations/specs/fix_td-001-pool-error-listener/`
- **Scope**: Backend only. Phase 4 is not started.

## Archived files

| File | Result |
|---|---|
| `plan.md` | Done 2026-11-03. |

## Outcome

| Item | Result |
|---|---|
| Change | `createPool` in `backend/src/db.ts` attaches `pool.on('error')`. The listener calls `logger.error({ err }, 'Unexpected error on idle database client')`. The signature is unchanged. |
| Test | `backend/test/db.test.ts` has 3 tests. They failed before the fix (RED) and pass after it. They check one listener, one log entry with no throw, and no database URL in the log. |
| Evidence | In `backend/`, `npm run build`, `npm run lint` and `npm test` pass. 58 of 58 tests pass. |
| Review | APPROVE. 0 critical, 0 high, 0 medium, 3 low findings. |
| Not run | Static scan (TD-005) and Docker build (TD-002). |

## Decision records

No new ADR. The change follows [ADR-002](../../../docs/ADRs/ADR-002-express-zod-pg-no-orm.md) and adds no new choice.

## Debt

TD-001 is fixed. See [docs/technical-debt.md](../../../docs/technical-debt.md).
