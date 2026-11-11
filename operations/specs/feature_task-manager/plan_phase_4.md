# Phase 4 Plan: Hosting, Cold Starts, Release, and Docs

# Document Control
- **Version**: 2.0 (replaces 1.0 of 2026-11-02)
- **Date Created**: 2026-11-02
- **Last Updated**: 2026-11-03
- **Status**: Planned. Hosting is decided: Render web service (Docker, free) and Neon PostgreSQL (free).
- **Parent**: `docs/HLD.md` (section 9 phase 4, ADR-001, risks R-1, R-2, R-4, R-5)
- **Changes from 1.0**: Neon replaces Render PostgreSQL (no 30 day expiry). Cold starts are now a design topic (Part C). The iOS release URL lives in `ios/Config/Info-Release.plist`, not in `project.pbxproj` (ADR-009).

---

# Executive Summary

- **Goal**: Host the API on Render and the database on Neon. Point both apps at the hosted URL. Make both apps work when the free API is asleep. Write the deploy docs.
- **Scope**: Deployment config, a small backend change (pool timeout), a client change in both apps (cold start handling), a `DESIGN.md` addition, docs.
- **Domains**: infrastructure, backend, Android, iOS, documentation.
- **Architecture**: The Phase 1 Docker image runs on a Render free web service. Neon gives `DATABASE_URL`. The apps call `https://<host>`. The apps never reach the database.
- **Entry Condition**: Phases 1, 2 and 3 are complete and pushed. The owner has a Render account and a Neon account.
- **Exit Condition**: `GET https://<host>/health` returns `200`. A task made on Android shows on iOS. After 16 idle minutes, both apps show a "waking" message and then load the list with no manual action. The root `README.md` lets a new developer run all three parts and deploy.

---

# Requirements

## Functional Requirements

- **REQ-401**: THE SYSTEM SHALL run the backend on a public HTTPS URL (Render) with a hosted PostgreSQL database (Neon).
- **REQ-402**: WHEN the container starts THE SYSTEM SHALL apply pending migrations before it accepts traffic (Phase 1 Dockerfile).
- **REQ-403**: THE SYSTEM SHALL read `DATABASE_URL` from a Render secret. The value SHALL NOT appear in the repository, the image, logs or the README.
- **REQ-404**: WHEN a request reaches `/health` THE SYSTEM SHALL confirm the database connection. Render SHALL use this path as the health check.
- **REQ-405**: THE SYSTEM SHALL ship the Android release build and the iOS Release configuration with the hosted HTTPS URL.
- **REQ-406**: WHEN a user creates a task on one app THE SYSTEM SHALL show it on the other app after a refresh.
- **REQ-407**: THE SYSTEM SHALL include a root `README.md` with: overview, architecture, prerequisites, local setup, config table, test commands, deploy steps, cold start behavior, known limits.
- **REQ-408 (cold start, apps)**: WHEN a list load runs while the API is asleep THE APP SHALL wait for the API to wake and then show the list, with no user action, for up to 90 seconds.
- **REQ-409 (cold start, hint)**: WHEN a list load takes longer than 4 seconds THE APP SHALL show the text `Waking up the server. This can take up to a minute.` under the loading indicator.
- **REQ-410 (cold start, write)**: WHEN a create, edit, status change or delete runs while the API is asleep THE APP SHALL wait up to 90 seconds for the answer. It SHALL NOT send the same write twice by itself.
- **REQ-411 (cold start, give up)**: IF the API does not answer within the budget THEN THE APP SHALL show the existing error state `Cannot reach the server.` with `Try again`.
- **REQ-412 (cold start, database)**: WHEN Neon has scaled to zero THE API SHALL wait up to 10 seconds for a database connection before it fails the request.
- **SEC-401**: THE SYSTEM SHALL serve the API over HTTPS only. Release builds SHALL NOT accept cleartext (Phase 2 SEC-201, Phase 3 SEC-301).
- **SEC-402**: THE SYSTEM SHALL connect to Neon with TLS and certificate verification (`sslmode=verify-full`).
- **SEC-403**: THE SYSTEM SHALL use a non-guessable service name because the API has no authentication (ADR-007, R-1). The README SHALL state this limit.
- **CON-401**: No authentication, no custom domain, no CI/CD pipeline, no keep-alive pinger.
- **CON-402**: Docker is not installed on the dev machine. Render builds the image. The first Render build is the first Docker build (TD-002).
- **GUD-401**: Use free tiers only. Record their limits in the README.

### Acceptance scenarios (Gherkin)

```gherkin
Feature: Hosted deployment and cold starts
  Scenario: Health
    When anyone sends GET https://<host>/health
    Then the status is 200 and the body is {"status":"ok"}
  Scenario: Migration ran
    When the client sends GET https://<host>/api/summary on a new database
    Then the body is {"total":0,"todo":0,"inProgress":0,"done":0,"overdue":0}
  Scenario: Cross-device
    Given both release apps use the hosted URL
    When the user creates "Demo task" on Android and refreshes iOS
    Then iOS shows "Demo task"
  Scenario: Cold start on first load
    Given the API was idle for 16 minutes
    When the user opens the app
    Then the app shows the loading indicator
    And after 4 seconds the app shows "Waking up the server. This can take up to a minute."
    And the list appears with no user action
  Scenario: Gateway answers 503 while the API wakes
    Given the first list call returns 503
    When the app retries every 5 seconds
    Then the list appears when the API answers 200
  Scenario: API never wakes
    Given every call fails for 90 seconds
    Then the app shows "Cannot reach the server." and "Try again"
  Scenario: Offline device
    Given the device has no network
    When the user opens the app
    Then the app shows the error state at once with no retry loop
  Scenario: Cleartext blocked
    When a release build calls an http:// URL
    Then the platform blocks the call
  Scenario: Fresh developer
    Given a new machine with Node, PostgreSQL, Android Studio and Xcode
    When the developer follows README.md
    Then the backend, the Android app and the iOS app run against the local database
```

---

# Dependencies

| ID | Name | Type | Purpose |
|----|------|------|---------|
| DEP-401 | Render account (free) | service | Docker web service. Owner creates it. |
| DEP-402 | Neon account (free) | service | PostgreSQL 17 project. Owner creates it. |
| DEP-403 | GitHub repo `origin/main` | service | Render pulls from it. The repo is private (anonymous request returns 404). The owner gives the Render GitHub app access to it. |
| DEP-404 | Phase 1 `backend/Dockerfile` | artifact | The deploy image. |
| DEP-405 | Real device or second machine | hardware | Cross-device check. An iOS device needs a signing team (R-4). |

---

# Plan Details

## Design: How it should happen

### Impact on system design

```text
 Android release ──┐  HTTPS   ┌───────────────────────────┐  TLS    ┌──────────────────┐
                   ├─────────▶│ Render free web service   │────────▶│ Neon free        │
 iOS Release ──────┘          │ Docker image (Phase 1)    │ direct  │ PostgreSQL 17    │
   wait up to 90 s            │ migrate -> start          │ string  │ scales to zero   │
   for a sleeping API         │ /health/live (no DB)      │         │ after 5 min idle │
                              │ /health (DB check)        │         └──────────────────┘
                              └───────────────────────────┘
 Region: Render oregon + Neon aws-us-west-2 (same coast, low latency)
```

### Cold start model

Two things sleep. Render sleeps the API after 15 minutes without traffic and needs about one minute to wake. Neon suspends the database after 5 minutes idle and wakes in about a second on the first query. The API wake is the slow part and the app must handle it.

```text
 t=0s   app sends GET /api/tasks and /api/summary
 ┌───────────────────────────────────────────────────────────────────────────┐
 │ Case A: Render holds the request until the API is up (expected)           │
 │   request waits ... 60 s ... 200 OK            -> read timeout is 90 s    │
 │ Case B: Render answers 502/503/504 while it wakes                         │
 │   502 -> wait 5 s -> retry -> 503 -> wait 5 s -> ... 200 OK               │
 │   -> client maps 502/503/504 to ApiError.Unavailable and the VM retries   │
 │ Case C: no network / DNS / refused (device problem, not a sleeping API)   │
 │   -> ApiError.Network -> error state at once, no retry                    │
 └───────────────────────────────────────────────────────────────────────────┘
 t=4s   still loading -> show "Waking up the server. This can take up to a minute."
 t=90s  budget used  -> error state "Cannot reach the server." + Try again
```

TASK-P4-006 measures which case Render shows on a real idle service. The design handles A and B. The constants live in one place per app, so a measurement can tune them.

### Decisions

| ID | Decision | Rationale | Alternatives Considered | Side effect (which modules?) |
|----|----------|-----------|------------------------|-------------------|
| DES-P4-001 | Hosting is Render free web service (Docker, region `oregon`) plus Neon free PostgreSQL 17 (region `aws-us-west-2`). Record as ADR-001 Accepted. | Owner decision. Neon free has no 30 day expiry (Render free Postgres expires after 30 days). Both are free. | Render Postgres, Railway, Fly.io | `docs/ADRs/ADR-001`, `docs/HLD.md` |
| DES-P4-002 | Free tier limits (checked 2026-11-03). Render: web service sleeps after 15 min without inbound traffic, wakes in about 1 min, 750 free hours per month, ephemeral disk. Neon: 100 CU-hours per project per month, 1 GB storage, scale to zero after 5 min (cannot disable), 6 hour history, 1 manual snapshot, no backups. | The README must state these. | none | `README.md` |
| DES-P4-003 | Use the Neon **direct** connection string (host without `-pooler`) for the API and for migrations. | `node-pg-migrate` takes a session advisory lock. Neon PgBouncer runs in transaction mode and does not support it. The API pool is max 5, far under the 97 connection limit of a 0.25 CU compute. | Pooled string for the API, direct for migrations (two variables, more config) | `DATABASE_URL` |
| DES-P4-004 | `DATABASE_URL` uses `sslmode=verify-full`. Remove `channel_binding=require` from the Neon string if present. | `pg` 8 with `pg-connection-string` 2.14 treats `require` like `verify-full` and prints a deprecation warning. Neon uses a public CA, so `verify-full` passes. `pg` does not use the `channel_binding` option. | `sslmode=require` | Render secret |
| DES-P4-005 | Add `connectionTimeoutMillis: 10000` to the pool. | The default is no limit. A request must fail in a known time when the database does not answer. Neon wake is about 1 s. | Default | `backend/src/db.ts` |
| DES-P4-006 | Add `GET /health/live` (no database access). Render uses it as `healthCheckPath`. Keep `GET /health` with the database check for manual smoke tests. Skip request logging for both paths. | Render probes every few seconds. A `SELECT 1` on each probe keeps Neon awake all month. 24/7 at 0.25 CU is about 182 CU-hours and the free plan has 100. Migrations run before the server starts, so a started server has proven the database link. | Probe `/health` and accept the cost; no health check | `backend/src/health`, `backend/src/app.ts`, `render.yaml` |
| DES-P4-007 | Change the Docker `CMD` to `sh -c "npm run migrate up && exec node dist/server.js"`. | Without `exec`, `sh` keeps the PID and `SIGTERM` from Render never reaches Node. The graceful shutdown in `server.ts` does not run. | Keep as is | `backend/Dockerfile` |
| DES-P4-008 | Use a `render.yaml` Blueprint with the web service only. `DATABASE_URL` has `sync: false`. The database is created in Neon by hand. | Neon is outside Render. `sync: false` makes Render ask for the value in the dashboard so it never enters Git. | Dashboard clicks | repo root |
| DES-P4-009 | Name the Render service `tm-api-<8 random letters>`. | The URL is `https://<name>.onrender.com`. A random name is hard to guess (SEC-403). | `task-manager-api` | `render.yaml` |
| DES-P4-010 | New `ApiError.Unavailable` (Android) and `APIError.unavailable` (iOS) for HTTP 502, 503 and 504. The UI maps it to the same text as `Network`. | These codes mean the gateway has no running API behind it. This is different from a device with no network. Only the first case should retry. | Treat as `Server` (no retry) or `Network` (retry offline too) | both apps, data layer |
| DES-P4-011 | The view model retries a **load** that shows the loading or error state (`reload()` and `retry()`) on `Unavailable`. Delay 5 s. Total budget 90 s. After the budget, the load fails as `Network`. Refresh after a change keeps the old single attempt. No retry on `Network`, `Server`, `Validation`, `NotFound`. | Keeps the retry in the view model as plan 1.0 said, with one exception: the status mapping lives in the data layer. Offline fails at once. | Retry in the network layer; retry all errors | `TaskListViewModel` (both apps) |
| DES-P4-012 | HTTP timeouts: Android `connectTimeout` 10 s (unchanged), `readTimeout` 90 s (was 15 s). iOS `timeoutIntervalForRequest` 90 s (was 15 s) and `timeoutIntervalForResource` 100 s. | In case A the proxy holds the request while the API wakes. A 15 s timeout would end it early. This also covers writes (REQ-410). | Longer timeout for GET only | `AppContainer.kt`, `URLSessionTaskAPI.swift` |
| DES-P4-013 | Show `Waking up the server. This can take up to a minute.` after 4 s on the loading screen only (phase `Loading`). A new `isWaking` flag in the view model state drives it. | A silent 60 s spinner looks like a hang. Other load paths already show the thin refresh indicator. | Toast; none | view models, list screens, `DESIGN.md` |
| DES-P4-014 | Writes never retry by themselves. A write waits up to 90 s. A `503` on a write shows the existing network message. | A retried `POST` can create a duplicate. The user usually loads the list first, so the API is awake. | Auto retry with idempotency key (needs backend change) | none |
| DES-P4-015 | No keep-alive pinger, no paid plan. | Out of scope (CON-401). A pinger would also burn Neon CU-hours. | cron-job pinger | README |
| DES-P4-016 | Set the hosted URL in `android/app/build.gradle.kts` (release `API_BASE_URL`, trailing `/`) and `ios/Config/Info-Release.plist` (`API_BASE_URL`, same shape as the placeholder). | Phase 2 and 3 isolate this value (ADR-009). The 1.0 plan named `project.pbxproj` for iOS. That is wrong. | none | android, ios |
| DES-P4-017 | Android release uses debug signing for the demo. No keystore. | Out of scope for a demo. State it in the README. | New keystore | `android/app/build.gradle.kts` |
| DES-P4-018 | iOS real device: the owner sets the signing team in Xcode. Do not commit the team ID unless the owner agrees. Fallback: simulator and a second Mac. | R-4. | TestFlight | project settings |

### Constraints & Assumptions

| Type | ID | Description | Impact if Wrong | Handling Strategy |
|------|----|-------------|-----------------|-------------------|
| Constraint | CON-P4-001 | Account creation, secrets, `git push` and commits need the owner. The agent never commits (`AGENTS.md`). | The agent cannot finish alone. | Owner steps have exact instructions. The agent checks results with `curl`, `psql` and the build commands. |
| Constraint | CON-P4-002 | The repo `mirnaihab/task-manager-demo` is private (anonymous request returns 404). The local `gh` login is a different account. | Render cannot pull the code. | Owner connects the GitHub account that owns the repo to Render and grants this repo (TASK-P4-004). |
| Assumption | ASM-P4-001 | The Phase 1 image builds on Render. It never ran in Docker (TD-002). | First deploy fails. | Read the Render build log. Fix in `backend/`. |
| Assumption | ASM-P4-002 | `pg` ignores `channel_binding` and `sslmode=verify-full` works with Neon. | API cannot connect. | Preflight (TASK-P4-003) runs the migration from the dev machine against Neon before Render. |
| Assumption | ASM-P4-003 | Render health probes do not count as inbound traffic, so the service still sleeps. | Service never sleeps. Uses all 750 hours. Still free, but no cold start to test. | TASK-P4-006 checks the Render log for a spin-down event. |
| Assumption | ASM-P4-004 | Render either holds a request while it wakes (A) or returns 502/503/504 (B). | Another code (for example 404 page) breaks the retry. | TASK-P4-006 records the exact status, body and time. |
| Assumption | ASM-P4-005 | The API stays public with no auth (ADR-007). | Anyone with the URL edits data. | SEC-403. README states it. |

### Edge cases

| ID | Scenario | Expected Behavior | Handling Strategy |
|----|----------|-------------------|-------------------|
| EDGE-P4-001 | App opens while Render sleeps | Loading indicator, hint after 4 s, list within about 70 s | DES-P4-011, 012, 013 |
| EDGE-P4-002 | Render returns 502/503/504 many times | Retry every 5 s up to 90 s, then error state | Budget test |
| EDGE-P4-003 | Device offline | Error state at once. No hint, no retry loop | `Network` is never retried |
| EDGE-P4-004 | The load works on try 3 | Hint disappears, list shows. No error flash | State reset on success |
| EDGE-P4-005 | User taps `Try again` during a wake | A new wake budget starts. The old load cancels (newest load wins) | Existing `loadJob.cancel()` |
| EDGE-P4-006 | User changes a filter while the list is waking | The new load replaces the old one. Budget restarts | Same |
| EDGE-P4-007 | User opens the app, API asleep, and the phase is `Content` (filter change or pull to refresh after 20 min) | The thin refresh indicator shows. No hint. Filter change may end in the error state after 90 s. Pull to refresh keeps data and shows a message | Known limit in `docs/technical-debt.md` |
| EDGE-P4-008 | Write sent while the API sleeps and the proxy holds it | Waits up to 90 s. If the client times out first, the write may still apply later | README known limit. No auto retry |
| EDGE-P4-009 | Neon suspended, API awake | First query adds about 1 s. Pool connect limit is 10 s | DES-P4-005 |
| EDGE-P4-010 | Neon compute quota used up (100 CU-hours) | API returns 500. `/health` returns 503 | README: check usage. Liveness path avoids waste |
| EDGE-P4-011 | Migration fails at start | Container exits. Render keeps the previous deploy serving | Read log. Add a forward migration |
| EDGE-P4-012 | Idle `pg` client dropped by Neon | Pool logs the error and continues (TD-001 fix) | none |
| EDGE-P4-013 | Time zone near midnight | One day shift for "today" (A-2, R-5) | README known limits |
| EDGE-P4-014 | Wrong URL in a release build | Error state after the budget | VER-P4-006 grep, VER-P4-004 |

### Directory Map

```text
(repo root)
├── README.md                                     (modify: deploy, cold start, config, limits)
├── render.yaml                                   (create)
├── AGENTS.md                                     (modify: plan status, hosting facts)
├── DESIGN.md                                     (modify: slow loading hint, done in planning)
├── docs/ADRs/ADR-001-hosting-provider.md         (modify: Accepted)
├── docs/ADRs/ADR-012-handle-free-tier-cold-starts.md  (create)
├── docs/ADRs/ADR-013-neon-direct-connection-and-liveness-path.md (create)
├── docs/HLD.md, docs/PRD.md, docs/technical-debt.md   (modify: status)
├── backend/
│   ├── Dockerfile                                (modify: exec)
│   ├── src/db.ts                                 (modify: connectionTimeoutMillis)
│   ├── src/app.ts                                (modify: skip log for /health*)
│   ├── src/health/health.routes.ts               (modify: add /live)
│   ├── test/db.test.ts, test/health.api.test.ts  (modify: new tests)
├── android/app/build.gradle.kts                  (modify: release URL, debug signing)
├── android/app/src/main/java/com/demo/taskmanager/
│   ├── AppContainer.kt                           (modify: readTimeout)
│   ├── data/TaskRepository.kt                    (modify: ApiError.Unavailable)
│   ├── data/ApiTaskRepository.kt                 (modify: map 502/503/504)
│   ├── ui/TaskListViewModel.kt                   (modify: wake retry, isWaking)
│   ├── ui/UiState.kt                             (modify: isWaking)
│   └── ui/TaskListScreen.kt                      (modify: hint text)
├── android/app/src/main/res/values/strings.xml   (modify: waking_message)
├── android/app/src/{test,sharedTest,androidTest}/...  (modify: tests and fake)
├── ios/Config/Info-Release.plist                 (modify: release URL)
└── ios/TaskManager/
    ├── Data/TaskAPI.swift                        (modify: .unavailable)
    ├── Data/URLSessionTaskAPI.swift              (modify: map 502/503/504, timeouts)
    ├── Data/MockTaskAPI.swift                    (modify: fail N times)
    ├── Features/TaskListViewModel.swift          (modify: wake retry, isWaking)
    └── Features/TaskListView.swift               (modify: hint text)
    (tests: ios/TaskManagerTests/...)
```

### Directory Specifications

Owner tasks (O) need an account or a secret. Agent tasks (A) change files. Do the work in four parts. Parts A and B are independent. Part C and D wait for the hosted URL only at TASK-P4-012.

**Part A. Backend changes (agent)**

| ID | Task | File(s) | Responsibility or Expected Behaviors | Validations/Error handling | Key Configurations |
|----|------|---------|--------------------------------------|----------------------------|--------------------|
| TASK-P4-001 | Pool connect timeout | `backend/src/db.ts`, `backend/test/db.test.ts` | Add `connectionTimeoutMillis: 10_000` as a named constant. Test that `createPool` sets it (read `pool.options`). | Test must not print the URL. | none |
| TASK-P4-002 | Liveness path and log filter | `backend/src/health/health.routes.ts`, `backend/src/app.ts`, `backend/test/health.api.test.ts` | Add `GET /health/live` that returns `200 {"status":"ok"}` and does not touch the pool. Test with a dead pool: `/health/live` is `200`, `/health` is `503`. `requestLog` skips any path that starts with `/health`. | Keep the `/health` behavior and the existing tests. | none |
| TASK-P4-003 | Dockerfile `exec` | `backend/Dockerfile` | `CMD ["sh", "-c", "npm run migrate up && exec node dist/server.js"]`. | `npm run build`, `npm run lint`, `npm test` pass in `backend/`. | none |

**Part B. Accounts and first deploy (owner, then agent verifies)**

| ID | Task | File(s) | Responsibility or Expected Behaviors | Validations/Error handling | Key Configurations |
|----|------|---------|--------------------------------------|----------------------------|--------------------|
| TASK-P4-004 | Create the Neon database (O) | Neon console | Create a project: Postgres 17, region AWS us-west-2, database `taskmanager`. Open Connect, turn **Connection pooling off**, copy the string. Change `sslmode=require` to `sslmode=verify-full` and delete `&channel_binding=require` if present. Keep it in a password manager. | The host has no `-pooler`. | `DATABASE_URL` |
| TASK-P4-005 | Preflight against Neon (A with the owner's URL in the shell) | none | In `backend/`, with `DATABASE_URL` set only in the shell: run `npm run build`, then `npm run migrate up`, then start `node dist/server.js` and `curl localhost:3000/health`. This proves ASM-P4-002 before Render. Then run VER-P4-002 and VER-P4-003 on localhost. | Never write the URL to a file, a log or chat. Unset it after. Delete the smoke task. | none |
| TASK-P4-006 | Repo access (O) | GitHub, Render | Check that `origin/main` has the Part A commits (the owner commits and pushes after review). Connect the GitHub account that owns the repo to Render and grant the repo. | `git ls-files \| grep -i '\.env$'` returns nothing. `git grep -n "postgres://"` shows only placeholders. | none |
| TASK-P4-007 | `render.yaml` and Render service (A writes, O deploys) | `render.yaml` | Blueprint: one `type: web`, `runtime: docker`, `name: tm-api-<random>`, `region: oregon`, `plan: free`, `dockerfilePath: ./backend/Dockerfile`, `dockerContext: ./backend`, `healthCheckPath: /health/live`, `autoDeployTrigger: commit`. `envVars`: `NODE_ENV=production`, `DATABASE_URL` with `sync: false`. Owner: Render dashboard, New, Blueprint, pick the repo, paste `DATABASE_URL` when asked. Do not set `PORT` (Render sets it and the app reads it). | Run `render blueprints validate` if the CLI is available, or paste into the schema check at `https://render.com/schema/render.yaml.json`. | `DATABASE_URL` secret |
| TASK-P4-008 | Smoke test the hosted API (A) | none | Run VER-P4-001 to VER-P4-003 against `https://<host>`. Read the Render log: migration applied (or "No migrations to run"), `Server started`. | If `/health` is `503`, check the Render `DATABASE_URL`. If the build fails, fix `backend/` and ask the owner to push. | none |
| TASK-P4-009 | Measure the real cold start (A with the owner) | `docs/technical-debt.md` or ADR-012 note | Wait 16 minutes with no traffic. Confirm in the Render log that the service spun down. Run `curl -s -o /dev/null -w "%{http_code} %{time_total}\n" https://<host>/health/live`. Record status and seconds. Repeat once with `/api/summary` to see the Neon wake cost. | Use the result to confirm case A or B in the cold start model. Change `WAKE_RETRY_*` constants only if the result needs it. | none |

**Part C. Cold start handling in the apps (agent)**

Constants, one place per app: `WAKE_HINT_DELAY` 4 s, `WAKE_RETRY_DELAY` 5 s, `WAKE_MAX_RETRIES` 18 (about 90 s). Inject the delay function so tests do not wait.

| ID | Task | File(s) | Responsibility or Expected Behaviors | Validations/Error handling | Key Configurations |
|----|------|---------|--------------------------------------|----------------------------|--------------------|
| TASK-P4-010 | Android data layer | `data/TaskRepository.kt`, `data/ApiTaskRepository.kt`, `AppContainer.kt`, `ApiTaskRepositoryTest.kt` | Add `ApiError.Unavailable`. Map 502, 503, 504 to it before the generic `Server` rule. Set `readTimeout(90, SECONDS)`. Tests with MockWebServer: 502, 503 and 504 map to `Unavailable`. 500 stays `Server`. A response with a `VALIDATION_ERROR` 400 body is unchanged. | Do not log bodies (SEC-202). | `readTimeout` 90 s |
| TASK-P4-011 | Android view model and UI | `ui/TaskListViewModel.kt`, `ui/UiState.kt`, `ui/TaskListScreen.kt`, `res/values/strings.xml`, `FakeTaskRepository.kt`, `TaskListViewModelTest.kt`, `TaskAppTest.kt` | In `launchLoad(showErrorState = true)` only: catch `Unavailable`, `delay(WAKE_RETRY_DELAY)`, load again, up to `WAKE_MAX_RETRIES`, then set `Phase.Error`. A second coroutine sets `isWaking = true` after `WAKE_HINT_DELAY` and a successful or failed load clears it. `UiState.isWaking` default `false`. In `Phase.Loading`, show the text under the indicator with test tag `waking-hint` and string `waking_message` = `Waking up the server. This can take up to a minute.` `Unavailable` maps to `UiMessage.NetworkError` and `Cannot reach the server.` Extend the fake with `failNext(error, times)`. | A cancelled load must not touch state (use `ensureActive` or let the cancellation pass). Do not catch `CancellationException`. | strings, tag |
| TASK-P4-012 | iOS data layer | `Data/TaskAPI.swift`, `Data/URLSessionTaskAPI.swift`, `URLSessionTaskAPITests.swift` | Add `APIError.unavailable`. Map 502, 503, 504 to it in `send`. Set `timeoutIntervalForRequest = 90` and `timeoutIntervalForResource = 100`. Tests with the existing `URLProtocol` stub: 502, 503, 504 map to `.unavailable`. 500 stays `.server`. | No body logging. | timeouts |
| TASK-P4-013 | iOS view model and UI | `Features/TaskListViewModel.swift`, `Features/TaskListView.swift`, `Data/MockTaskAPI.swift`, `TaskListViewModelTests.swift`, `TaskManagerUITests.swift` | Same rules as TASK-P4-011. Add `private(set) var isWaking = false`. Inject `sleep: @Sendable (Duration) async -> Void` with the default `Task.sleep` (init parameter with a default, so `TaskManagerApp` is unchanged). In `.loading`, show a `Text` with `accessibilityIdentifier("waking-hint")` under the `ProgressView`. `.unavailable` maps to `.networkError`. `MockTaskAPI.failNext(_:times:)`. | Respect `Task.isCancelled` after each sleep. | strings |
| TASK-P4-014 | `DESIGN.md` | `DESIGN.md` | Add the slow loading hint to section 9 and a Do rule. Done in this plan step. | Keep tokens only. | none |

**Part D. Release URL, build, verify, docs**

| ID | Task | File(s) | Responsibility or Expected Behaviors | Validations/Error handling | Key Configurations |
|----|------|---------|--------------------------------------|----------------------------|--------------------|
| TASK-P4-015 | Set the hosted URL (A) | `android/app/build.gradle.kts`, `ios/Config/Info-Release.plist` | Replace both `https://example.invalid` values. Android: `"https://<host>/"`. iOS: `https://<host>` (no trailing slash, as the placeholder). Add `signingConfig = signingConfigs.getByName("debug")` to the Android `release` build type (DES-P4-017). | VER-P4-006 grep returns nothing. | hosted URL (public) |
| TASK-P4-016 | Build release apps (A) | none | `./gradlew assembleRelease` and `xcodebuild build -scheme TaskManager -configuration Release -destination 'generic/platform=iOS Simulator'`. Check the merged release manifest and the built `Info.plist` (VER-P4-007). | Both build with no error. | none |
| TASK-P4-017 | Cross-device and cold start check (O with the agent) | none | On two devices run US-01 to US-05 against the hosted API. Create on Android, refresh iOS, then reverse. Then idle 16 minutes and open each app: hint shows, list loads. Record the seconds in the PR text. This closes TD-006. | A failure goes back to TASK-P4-009 constants. | none |
| TASK-P4-018 | Docs (A) | `README.md`, `AGENTS.md`, `docs/ADRs/ADR-001`, `ADR-012`, `ADR-013`, `docs/HLD.md`, `docs/PRD.md`, `docs/technical-debt.md` | README: Status, Deploy (Neon steps, Render steps, env table, rollback), Cold starts (what the user sees, 90 s budget, writes do not retry), Known limits (no auth, free tier sleep, Neon scale to zero and 100 CU-hours, no backups, UTC date, iOS 17, debug-signed APK). Config table: add release URL rows and the `render.yaml` secret. ADR-001 Accepted. ADR-012: cold start strategy (DES-P4-010 to 014). ADR-013: direct string and `/health/live`. Update the `Not built` lines about Docker once Render built it. Close or update TD-002, TD-005, TD-006, TD-007. Add EDGE-P4-007 and EDGE-P4-008 as debt. | Follow the STE rules in `AGENTS.md`. No secrets. Do not claim a pass without evidence. | none |
| TASK-P4-019 | Fresh-clone check (A) | none | Clone into a temp folder. Follow the README for the backend: `npm ci`, `npm run build`, `npm test`. Fix any README gap. | The README commands run as written. | none |

Order: A (001 to 003) and C (010 to 013) can start now. Then B (004 to 009). Then D (015 to 019). Do not push the release URL commit before TASK-P4-008 passes. The owner commits and pushes. The agent never commits.

### Schemas

New route: `GET /health/live` returns `200 {"status":"ok"}`. No other API change. No new error codes. The apps read only the HTTP status for 502, 503 and 504.

### Data Models & flows

<settings>

| Variable or key | Where | Value |
|---|---|---|
| `DATABASE_URL` | Render secret (`sync: false`) | Neon **direct** string, `sslmode=verify-full`. Never committed. |
| `PORT` | Render sets it | Read by the app |
| `NODE_ENV` | `render.yaml` | `production` |
| Android release `API_BASE_URL` | `app/build.gradle.kts` | `https://<host>/` |
| iOS Release `API_BASE_URL` | `ios/Config/Info-Release.plist` | `https://<host>` |
| `WAKE_HINT_DELAY`, `WAKE_RETRY_DELAY`, `WAKE_MAX_RETRIES` | One constant block per app | 4 s, 5 s, 18 |
| HTTP read timeout | `AppContainer.kt`, `URLSessionTaskAPI.swift` | 90 s |
| Pool `connectionTimeoutMillis` | `backend/src/db.ts` | 10000 |
</settings>

<Data-Models>

No change. Migration `1730500000000_create-tasks.sql` runs on Neon at the first start.
</Data-Models>

<Data-flow>

```text
git push ──▶ Render build (Dockerfile) ──▶ container start
   └─ npm run migrate up (Neon direct, TLS) ──▶ exec node dist/server.js ──▶ /health/live 200 ──▶ traffic
Rollback: Render dashboard, Events, Rollback (two most recent deploys). Schema changes are forward-only.

App load while asleep:
 VM.reload ─▶ repo.list ─▶ [wait ≤90 s | 502/503/504 ─▶ delay 5 s ─▶ repo.list ... ×18] ─▶ Content | Error
      └─ after 4 s: isWaking = true ─▶ hint text (cleared when the load ends)
```
</Data-flow>

---
#### **Process**

- Runtime Considerations: One instance. Pool max 5. The free tier sleeps (EDGE-P4-001).
- Deployment Impact: Auto deploy on push to the linked branch. No pipeline (CON-401). Backups: none on Neon free (6 hour history only). State this in the README.

---

# Risks

| ID | Risk | Likelihood | Impact | Mitigation |
|----|------|-----------|--------|------------|
| RISK-401 | Render wake behavior differs from the model (other status code or HTML page). | Med | Med | TASK-P4-009 measures it. Constants are in one place. |
| RISK-402 | Anyone can read and change tasks. | High | Med | Accepted (ADR-007). Random service name. README states it. |
| RISK-403 | iOS signing team missing for a real device. | Med | Med | DES-P4-018 fallback. |
| RISK-404 | Neon free CU-hours used up. | Low | High | DES-P4-006 liveness path. README tells the owner where to check usage. |
| RISK-405 | `DATABASE_URL` leaks in logs, repo or chat. | Low | High | `sync: false`. Existing tests check no URL in logs. VER-P4-005 scan before push. |
| RISK-406 | Release build keeps the placeholder URL. | Low | Med | VER-P4-006 grep. |
| RISK-407 | First Docker build fails (TD-002). | Med | Low | Read the Render log. Fix `backend/`. |
| RISK-408 | Private repo not readable by Render. | Med | Low | TASK-P4-006. |
| RISK-409 | A write times out on the client but applies later (EDGE-P4-008). | Low | Low | Users load first. README note. |
| RISK-410 | Retry loop hides a real outage for 90 s. | Low | Low | Fixed budget. Then the error state shows. |

---

# Verification

| ID | Check | Command | Pass condition |
|----|-------|---------|----------------|
| VER-P4-001 | Health | `curl -i https://<host>/health` and `/health/live` | `200` and `{"status":"ok"}` on both |
| VER-P4-002 | Migration | `curl -s https://<host>/api/summary` | All counts `0` on a new database |
| VER-P4-003 | Write path | `curl -s -X POST https://<host>/api/tasks -H 'content-type: application/json' -d '{"title":"smoke","priority":"LOW","dueDate":"<today>"}'`, then `GET /api/tasks`, then `DELETE /api/tasks/<id>` | `201`, the list shows it, `204` |
| VER-P4-004 | Cross-device | Manual run (TASK-P4-017) | A task made on one app shows on the other |
| VER-P4-005 | No secret | `git ls-files \| grep -i '\.env$'` and `git grep -n "postgres://"` and `git grep -n "neon.tech"` | No match except placeholders |
| VER-P4-006 | No placeholder | `grep -rn "example.invalid" android ios backend render.yaml README.md docs` | No match in `android/app/build.gradle.kts` and `ios/Config`. Other hits are only docs that say the old value |
| VER-P4-007 | Cleartext blocked | Read `android/app/build/intermediates/merged_manifests/release` output and the built Release `Info.plist` | Android release has `cleartextTrafficPermitted="false"`. iOS has no ATS exception |
| VER-P4-008 | Backend tests | In `backend/`: `npm run build && npm run lint && npm test` | Pass, with the new tests |
| VER-P4-009 | Android tests | `JAVA_HOME=... ./gradlew test` and `./gradlew lint` | Pass, with wake tests |
| VER-P4-010 | iOS tests | `xcodebuild test` command from `AGENTS.md` | Pass, with wake tests |
| VER-P4-011 | Android UI tests | `./gradlew connectedDebugAndroidTest` (needs an emulator) | Pass, including `waking-hint` |
| VER-P4-012 | Cold start (real) | `curl` timing after 16 idle minutes (TASK-P4-009) and the app check (TASK-P4-017) | Status and seconds recorded. Apps show the hint and then the list |
| VER-P4-013 | Release builds | `./gradlew assembleRelease` and the `xcodebuild -configuration Release` command | Both succeed |
| VER-P4-014 | README | Fresh clone (TASK-P4-019) | Commands run as written |

Review step: a reviewer reads the README as a new developer and checks Known limits against RISK-401 to RISK-410. A reviewer also checks that no write path retries.

### Requirement trace

| Requirement | Tasks | Verification |
|---|---|---|
| REQ-401, 402, 404 | 002, 003, 007, 008 | VER-P4-001, 002 |
| REQ-403, SEC-402 | 004, 005, 007 | VER-P4-005 |
| REQ-405, SEC-401 | 015, 016 | VER-P4-006, 007, 013 |
| REQ-406 | 017 | VER-P4-004 |
| REQ-407 | 018, 019 | VER-P4-014 |
| REQ-408, 410, 411 | 010 to 013 | VER-P4-009, 010, 012 |
| REQ-409 | 011, 013, 014 | VER-P4-009, 010, 011 |
| REQ-412 | 001 | VER-P4-008 |
| SEC-403, GUD-401 | 007, 018 | Review step |

---

# Code Guidelines

- **Clean code**: keep the wake logic in the view model and the status mapping in the data layer. Keep the three constants in one block per app. Do not add a retry to the network layer. Do not change the Phase 2 and 3 layering.
- **Security**: `DATABASE_URL` stays in the Render secret and the owner shell. Never print it. Run VER-P4-005 before every push. Writes never retry by themselves.
- **Testing**: inject the delay function. Tests must not sleep. Write the failing test first for each wake rule (offline does not retry, 503 retries, budget ends in the error state, newest load wins).
- **Docs**: write in simple technical English (`AGENTS.md`). Short sentences. One instruction per step.
- **DevOps**: keep `render.yaml` in Git. Use conventional commit messages (`feat:`, `fix:`, `docs:`, `chore:`). The owner commits.
