# Technical Debt

- **Updated**: 2026-11-03
- **Scan**: A search for `TODO`, `FIXME` and `HACK` in `backend/`, `android/app/src` and `ios/` found no comment markers. The matches were the `TODO` task status value.
- **Source**: Three review steps and the build notes in `operations/archives/2026-11-03-task-manager-phases-1-3/`.
- **Issue links**: No GitHub issues exist for these items.

Effort: S is under 1 hour. M is under 1 day. L is more than 1 day.

## Critical

None.

## High

| ID | Item | Effort | Mitigation |
|---|---|---|---|
| TD-002 | `backend/Dockerfile` is written but never built. Docker is not installed on the dev machine. | M | Build the image on the host in phase 4 (TASK-P4). Run `/health` against the container. |
| TD-003 | Fixed 2026-11-03. Phases 1 to 3 are committed in `2caef5b`. | S | None. |

## Medium

| ID | Item | Effort | Mitigation |
|---|---|---|---|
| TD-004 | `npm audit --omit=dev` reports 3 moderate advisories in `node-pg-migrate`, `glob` and `minimatch`. The suggested fix is a breaking downgrade. | M | Track a `node-pg-migrate` release that updates `glob`. Run the migration only from the Docker start command, not from user input. |
| TD-005 | No static scan ran (SonarQube and CodeQL tools were not available). The reviews rely on build, lint, tests and a secret scan. | M | Run a scan in phase 4 or add a CI job. |
| TD-006 | iOS manual run is partial. Create, status change and delete were not driven by hand against the real API. UI tests cover them against the mock API. | S | Do the manual run during the phase 4 cross-device check. |
| TD-007 | Both release builds use the placeholder `https://example.invalid`. Android also needs a signing choice for a demo install (DES-P4-006). | S | Set the hosted URL in phase 4 (`android/app/build.gradle.kts`, `ios/Config/Info-Release.plist`). |
| TD-008 | Assumption A-1 is not confirmed: `priority` and `dueDate` are required on create. | S | Ask the owner. A relaxed rule is a small change in `task.schemas.ts`. |

## Low

| ID | Item | Effort | Mitigation |
|---|---|---|---|
| TD-009 | Version pins hold back TypeScript (`~6.0.3`) and three Android libraries (see ADR-008). | M | Upgrade Kotlin and compileSdk together. Upgrade TypeScript when typescript-eslint supports it. |
| TD-010 | The Android dark theme uses derived values for six tokens that `DESIGN.md` does not define. Android dark mode was not checked by hand. iOS copies the same values. | S | Add the dark values to `DESIGN.md`, then update `Color.kt` and the iOS color assets. |
| TD-011 | "Today" is the server UTC date (A-2, R-5). A user near midnight in another time zone can see a one-day difference. | M | Document it in the README. Send the client date if users complain. |
| TD-012 | No CI pipeline. Each part runs by hand from the commands in `AGENTS.md`. | M | Add a GitHub Actions workflow after phase 4. |
| TD-013 | The hosted API has no authentication (ADR-007, R-1). | M | Accepted by scope. Add a shared API key if the demo becomes public. |

## Count

| Severity | Items |
|---|---|
| Critical | 0 |
| High | 1 |
| Medium | 5 |
| Low | 5 |
| Total | 11 |

Fixed: TD-003 on 2026-11-03 (commit `2caef5b`). TD-001 on 2026-11-03. `createPool` now logs pool errors with `logger.error`. Test: `backend/test/db.test.ts`.
