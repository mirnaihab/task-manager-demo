# Document Control
- **Version**: 1.0
- **Date Created**: 2026-11-03
- **Last Updated**: 2026-11-03
- **Status**: Done

---

# Executive Summary

- **Goal**: Fix TD-001. Log a dropped idle database connection instead of crashing the server.
- **Scope**: bug fix
- **Domains**: backend
- **Architecture**: `createPool` in `backend/src/db.ts` attaches one `error` listener to the new pool. The listener calls `logger.error` with the error. The process stays up. `pg` removes the dead client and opens a new one on the next query.
- **Tech Stack**: Node.js, TypeScript, `pg` 8, `pino` (`logger`), Vitest.
- **Entry Condition**: Phases 1 to 3 are done. `taskmanager_test` database exists.
- **Exit Condition**: The listener exists. The new test passes. `npm run build`, `npm run lint` and `npm test` pass in `backend/`. TD-001 is marked fixed in `docs/technical-debt.md`.

Out of scope: any change outside `backend/` (except the one TD-001 doc update), phase 4, Docker, hosting.

---

# Requirements

## Functional Requirements
- **REQ-001**: WHEN the pool emits an `error` event, THE SYSTEM SHALL log it with `logger.error` and keep the process running.
- **SEC-001**: THE SYSTEM SHALL NOT write the database URL or password to the log. Log only the error object.
- **CON-001**: Change only files in `backend/`, plus the TD-001 row in `docs/technical-debt.md`.
- **CON-002**: Keep the `createPool(databaseUrl): pg.Pool` signature. No caller changes.
- **PAT-001**: Use the shared `logger` from `backend/src/logger.ts`.

```gherkin
Feature: Pool error handling
  Scenario: Idle connection drops
    Given a pool created by createPool
    When the pool emits an "error" event with an Error
    Then no exception is thrown
    And logger.error is called once with that error
    And the error message does not contain the database URL

  Scenario: Pool has a listener
    Given a pool created by createPool
    Then pool.listenerCount("error") is 1
```

---

# Dependencies

| ID | Name | Type | Version | Purpose |
|----|------|------|---------|---------|
| DEP-001 | pg | package | existing | Pool and its `error` event |
| DEP-002 | pino logger | module | existing | `logger.error` |
| DEP-003 | vitest | package | existing | `vi.spyOn` on the logger |

No new packages.

---

# Plan Details

## Design

### Impact on system design

```text
idle client <-X- database restart
      |
      v
 pg.Pool emits "error"
      |
      v
 listener in createPool  --> logger.error(...)   (process stays up)
      |
      v
 pg drops the dead client; next query opens a new one
```

Without a listener, Node throws on an unhandled `error` event and the process exits.

### Decisions

| ID | Decision | Rationale | Alternatives Considered | Side effect |
|----|----------|-----------|------------------------|-------------|
| DES-001 | Attach the listener inside `createPool`. | One place. Every pool (server and tests) is covered. | Attach in `server.ts`. Rejected: tests and future callers would miss it. | `db.ts` imports `logger`. |
| DES-002 | Log with `logger.error({ err }, 'Unexpected error on idle database client')`. | pino serializes `err` as a standard error. Matches `messageKey: 'message'`. | Log `err.message` only. Rejected: loses the code and stack. | None |
| DES-003 | Do not exit and do not reconnect by hand. | `pg` handles reconnect on the next query. `/health` already reports a failed query. | Exit and let the host restart. Rejected: ticket asks to stay up. | None |

### Constraints & Assumptions

| Type | ID | Description | Impact if Wrong | Handling Strategy |
|------|----|-------------|-----------------|-------------------|
| Constraint | CON-P1-001 | Backend only. | Scope creep. | Reviewer checks the diff. |
| Assumption | ASM-P1-001 | pino `err` serializer keeps the message free of the URL. A connection error from `pg` does not include the password. | Secret in log. | Test asserts the logged text has no URL (EDGE-001). |
| Assumption | ASM-P1-002 | `logger` is `silent` in tests, so tests must spy on `logger.error`. | Test passes without testing. | Use `vi.spyOn(logger, 'error')`. |

### Edge cases

| ID | Scenario | Expected Behavior | Handling Strategy |
|----|----------|-------------------|-------------------|
| EDGE-001 | Error text contains the database URL. | URL not in the log call. | Test uses a URL with a fake password and checks `JSON.stringify` of the spy call arguments. The error passed in does not contain it. |
| EDGE-002 | Two errors in a row. | Each one is logged. Process stays up. | Listener has no state. |
| EDGE-003 | Pool ended with `pool.end()` in tests. | No extra log or throw. | Test calls `pool.end()` in `afterEach`. |

### Directory Map

```text
backend/
├── src/
│   └── db.ts                 (modify)
└── test/
    └── db.test.ts            (create)
docs/
└── technical-debt.md         (modify, TD-001 row only)
```

### Directory Specifications

| ID | Task | File(s) | Responsibility or Expected Behaviors | Validations/Error handling | Key Configurations/settings |
|----|------|---------|--------------------------------------|----------------------------|-----------------------------|
| TASK-P1-001 | IMPLEMENTS REQ-001 SEC-001 PAT-001. Write the failing test first (RED). | `backend/test/db.test.ts` | Create pool with `createPool('postgresql://user:secret@localhost:1/none')`. Spy on `logger.error` with an empty mock implementation. Test 1: `pool.listenerCount('error')` is 1. Test 2: `pool.emit('error', new Error('connection terminated'))` does not throw and `logger.error` is called once with an object whose `err` is that error. Test 3: `JSON.stringify` of the call arguments does not contain `secret`. | `afterEach`: `pool.end()` and `vi.restoreAllMocks()`. | None |
| TASK-P1-002 | IMPLEMENTS REQ-001 CON-002. Make the test pass (GREEN). | `backend/src/db.ts` | Import `logger` from `./logger`. In `createPool`, build the pool, call `pool.on('error', (err) => logger.error({ err }, 'Unexpected error on idle database client'))`, return the pool. Add a one line comment: without a listener Node exits on the event. | Signature unchanged. | None |
| TASK-P1-003 | Verify. | `backend/` | Run `npm run build`, `npm run lint`, `npm test`. All must pass with 0 failures. Confirm the new test fails if the `pool.on` line is removed (RED check done in TASK-P1-001). | Fix any lint error in the new files only. | `taskmanager_test` database must exist. |
| TASK-P1-004 | Update the debt record. | `docs/technical-debt.md` | Mark TD-001 as fixed (move it out of the High table or add "Fixed 2026-11-03"). Update the High count from 3 to 2 and Total from 13 to 12. Do not edit `AGENTS.md`. Report that its Open items line for TD-001 is now stale. | Do not edit other rows. | None |

### Schemas
None. No API, DTO or response change.

### Data Models & flows
- Settings: none. No new environment variable.
- Data models: none. No migration.

```text
Test: emit('error', err)
  -> listener -> logger.error({ err }, msg)  [spy]
  -> assert called once, no throw, no URL in args
```

---
#### **Process**
- Runtime Considerations: Log line is JSON at level `error` with `message: "Unexpected error on idle database client"`. Hosted log search can alert on it.
- Deployment Impact: None. Phase 4 deploys the fixed code.

---

# Risks

| ID | Risk | Likelihood | Impact | Mitigation |
|----|------|-----------|--------|------------|
| RISK-001 | Logged error leaks connection details. | Low | Med | SEC-001 and EDGE-001 test. Log the error object only, never the config. |
| RISK-002 | Test passes without the fix (false green). | Low | Med | Run the test first and see it fail (RED). |
| RISK-003 | `docs/technical-debt.md` edit conflicts with CON-001 wording. | Low | Low | The edit is one row. The ticket names this file. |

---

# Code Guidelines
- **clean-code**: Keep the change to a few lines. One name per thing. No dead code or commented code. Short comment only to say why.
- **clean-code**: Test names say the behavior. One assertion theme per test. Clean up in `afterEach`.
- **security**: Never log the database URL, password or config object. Log the error object only. No secrets in the test file beyond a fake password string.
- **security**: Do not add a new dependency. Do not change the TLS or pool options.
- **Repo patterns**: Use the shared `logger`. Keep `createPool` signature. Follow Vitest style in `backend/test/`.
- **Commit rule**: Do not commit. The owner has not asked (TD-003).
