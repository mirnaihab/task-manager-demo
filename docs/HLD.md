# Task Manager: High Level Design and Roadmap

- **Version**: 1.1
- **Date**: 2026-11-03
- **Source**: `docs/user-stories.md` (US-01 to US-05), `AGENTS.md`
- **Status**: Phases 1 to 3 built and reviewed. Phase 4 (hosting) waits for ADR-001.

## 1. Executive Summary

| Item | Value |
|---|---|
| Objective | A single-user task manager with a hosted backend and two native mobile apps. |
| Problem | The user needs to create, track, and prioritize tasks from a phone. |
| Solution | One REST API over PostgreSQL. Two thin native clients call it over HTTPS. |
| Success criteria | 1. All five user stories pass their acceptance criteria on Android and iOS. 2. Both apps work on a device other than the developer machine. 3. Backend unit and API integration tests pass. 4. Each app has unit tests and basic UI tests that pass. 5. README lets a new developer run all three parts. |
| Out of scope | Authentication, multiple users, offline mode, push notifications, external integrations. |

## 2. Simplest-Approach Decisions

The design uses the fewest parts that meet the stories.

- **One backend, no ORM.** Plain SQL with `pg`. The data model has one table.
- **Server owns the rules.** The server validates input and computes `overdue` and the dashboard counts. The apps do not repeat business logic. This keeps Android and iOS identical.
- **No local cache.** The apps always read from the API. Offline mode is out of scope.
- **No dependency injection framework.** Manual wiring on both apps.
- **Hosting-neutral backend.** The backend reads `DATABASE_URL` and `PORT`. It runs in one Docker image on any host.

## 3. Architecture

```text
┌──────────────────┐   HTTPS/JSON   ┌────────────────────────┐   SQL   ┌──────────────┐
│ Android app      │───────────────▶│ Backend (Node + TS)    │────────▶│ PostgreSQL   │
│ Compose          │                │ Express, zod, pg       │         │ table: tasks │
└──────────────────┘                │ routes → service → repo│         └──────────────┘
┌──────────────────┐   HTTPS/JSON   │ /api/tasks, /api/summary│
│ iOS app          │───────────────▶│ /health                │
│ SwiftUI          │                └────────────────────────┘
└──────────────────┘          Apps never connect to the database.
```

### Environments

| Environment | Backend URL | Database |
|---|---|---|
| Local | Android emulator `http://10.0.2.2:3000`. iOS simulator `http://localhost:3000`. | Local PostgreSQL 17, `taskmanager` |
| Test | In-process (supertest) | `taskmanager_test` on the local server |
| Hosted | `https://<host>` (provider open, see ADR-001) | Hosted PostgreSQL |

### Component Map

| Component | Tech | Folder | Layers |
|---|---|---|---|
| Backend | Node.js 22+, TypeScript, Express 5, zod, pg, node-pg-migrate | `backend/` | `routes` (HTTP) → `service` (rules) → `repository` (SQL) |
| Android | Kotlin, Jetpack Compose, Material 3, Retrofit, kotlinx.serialization, ViewModel + StateFlow | `android/` | `data` (API, repository) → `ui` (ViewModel, screens) |
| iOS | Swift, SwiftUI, URLSession async/await, `@Observable` | `ios/` | `Data` (API client) → `Features` (view model, views) |

## 4. Data Model

One table. One user, so no user column.

```sql
CREATE TABLE tasks (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title       VARCHAR(100) NOT NULL CHECK (length(btrim(title)) > 0),
  description TEXT NULL,
  priority    TEXT NOT NULL CHECK (priority IN ('LOW','MEDIUM','HIGH')),
  status      TEXT NOT NULL DEFAULT 'TODO' CHECK (status IN ('TODO','IN_PROGRESS','DONE')),
  due_date    DATE NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX tasks_status_priority_idx ON tasks (status, priority);
```

Overdue is not stored. It is computed: `due_date < today AND status <> 'DONE'`.

## 5. API Contract

Base path `/api`. JSON only. No authentication. IDs are UUIDs. Dates are `YYYY-MM-DD`.

| Method | Path | Purpose | Story |
|---|---|---|---|
| GET | `/health` | Liveness and database check. | Deploy |
| POST | `/api/tasks` | Create a task. Returns `201` and the task. | US-01 |
| GET | `/api/tasks?status=&priority=` | List tasks. Both filters optional and combinable. Sort by `dueDate` ascending, then `createdAt`. | US-02 |
| PATCH | `/api/tasks/:id` | Partial update. Used for status change and for edit. | US-03, US-04 |
| DELETE | `/api/tasks/:id` | Delete a task. Returns `204`. | US-04 |
| GET | `/api/summary` | Counts for the dashboard. | US-05 |

### Schemas

```yaml
Task:
  id: uuid
  title: string          # 1..100 chars after trim
  description: string|null
  priority: LOW|MEDIUM|HIGH
  status: TODO|IN_PROGRESS|DONE
  dueDate: "YYYY-MM-DD"
  overdue: boolean       # computed by server
  createdAt: ISO-8601
  updatedAt: ISO-8601
CreateTaskRequest: { title, description?, priority, dueDate }
UpdateTaskRequest: { title?, description?, priority?, status?, dueDate? }   # at least one field
Summary: { total, todo, inProgress, done, overdue }                         # all integers, 0 when empty
Error:   { error: { code: "VALIDATION_ERROR"|"NOT_FOUND"|"INTERNAL", message: string, fields?: { [name]: string } } }
```

### Conventions
- `400` validation error with a `fields` map. `404` unknown ID. `500` hides internal detail.
- An invalid `status` or `priority` filter value returns `400`.
- Unknown JSON fields are rejected.
- No pagination. A single-user list is small. Add it later if needed.
- No API versioning prefix. One client set ships with the API.

## 6. Business Rules

| ID | Rule | Source |
|---|---|---|
| BR-1 | Title is required, trimmed, and at most 100 characters. | US-01 |
| BR-2 | Description is optional. | US-01 |
| BR-3 | Priority is one of Low, Medium, High. | US-01 |
| BR-4 | On create, `dueDate` cannot be before today. | US-01 |
| BR-5 | On edit, apply the same rules to every field the request changes. | US-04 |
| BR-6 | New tasks start as `TODO`. Any status can change to any other status. | US-03 |
| BR-7 | A task is overdue when `dueDate < today` and status is not `DONE`. | US-05 |
| BR-8 | The delete confirmation lives in the apps. The API deletes without a prompt. | US-04 |

### Assumptions to confirm (see findings)
- **A-1** Priority and due date are required on create. The stories only mark description as optional.
- **A-2** "Today" is the server UTC date. A user near midnight in another time zone can see a one-day difference. This is acceptable for a demo. Both apps also block past dates in the date picker.
- **A-3** On edit, the past-date check applies only when `dueDate` changes. Without this, a user cannot rename an overdue task. This is a reading of BR-5.

## 7. Client Design (both apps)

Same screens and same behavior on both platforms.

```text
┌─────────────── Task list (home) ───────────────┐
│ Summary card: Total | To Do | In Progress | Done│
│ Overdue count (red)                             │
│ Filter chips: Status [All|To Do|In Prog|Done]   │
│               Priority [All|Low|Med|High]       │
│ ┌ Task row: title, priority, status, due date ┐ │
│ │ overdue rows use a red accent + "Overdue"   │ │
│ │ tap row → status menu / edit; swipe or      │ │
│ │ button → delete (confirm dialog)            │ │
│ └─────────────────────────────────────────────┘ │
│ Empty state when no task matches the filters    │
│ [+] button → Create/Edit form                   │
└─────────────────────────────────────────────────┘
Create/Edit form: title, description, priority, due date picker, Save/Cancel.
Inline field errors from the API `fields` map. Success shows a snackbar or toast.
```

| Concern | Decision |
|---|---|
| State | One list view model. It holds the filters, the task list, the summary, loading, and error state. |
| Refresh | After each create, update, or delete, call the API again for the list and the summary. Status changes update the row at once, then reconcile. |
| Config | Android: `BuildConfig.API_BASE_URL` per build type. iOS: `API_BASE_URL` build setting read from `Info.plist`. |
| Local HTTP | Android debug allows cleartext only for `10.0.2.2`. iOS debug allows local networking. Release builds use HTTPS only. |
| Errors | Show a retry state for network errors. Show field errors on the form. |
| Accessibility | Labels on controls. Do not use color alone for overdue. Show the "Overdue" text too. |

## 8. Testing Strategy

| Part | Unit | Integration or UI |
|---|---|---|
| Backend | Vitest: validation and service rules (overdue, date checks). | Vitest + supertest against `taskmanager_test`: every endpoint, filters, errors. |
| Android | JUnit: ViewModel with a fake repository. | Compose UI test: create task, filter, empty state, delete confirm. Uses a fake repository, not the network. |
| iOS | Swift Testing or XCTest: view model with a mock API client. | XCUITest: create task, filter, delete confirm. Uses a launch argument that selects a mock client. |

Commands are in `AGENTS.md`. Each phase must pass its build, lint (where it exists), and test commands.

## 9. Roadmap

Four phases. Each phase is reviewable on its own. Phases 2 and 3 only need the contract from phase 1 and can run in parallel after it.

| Phase | Title | Scope | Approx. files | Stories | Depends on | Status |
|---|---|---|---|---|---|---|
| 1 | Backend API and database | `backend/` project, migration, routes, service, repository, tests, Dockerfile, `.env.example`. | 18-22 | US-01..05 (server side) | none | Done 2026-11-02 |
| 2 | Android app | Data layer, ViewModel, list/summary/form screens, config, unit and UI tests. | 14-18 | US-01..05 | Phase 1 contract | Done 2026-11-02 |
| 3 | iOS app | API client, view model, views, config, unit and UI tests. | 12-16 | US-01..05 | Phase 1 contract | Done 2026-11-03 |
| 4 | Hosting, release, and docs | Host DB and API, run migration, set hosted URL in both apps, root README, cross-device check. | 4-8 | Deployment requirement | Phases 1-3 | Planned. Blocked by ADR-001 |

### Phase 1: Backend API and database
- Create `backend/` with `package.json` scripts `build`, `lint`, `test`, `migrate`, `start`.
- Add the migration for `tasks`. Add the `/health` route.
- Implement the endpoints in section 5 and the rules in section 6.
- Config comes from `DATABASE_URL`, `PORT`, and `NODE_ENV`. Hosted PostgreSQL needs SSL, so honor `sslmode` in the URL.
- Add a multi-stage `Dockerfile` that runs migrations, then starts the server.
- **Exit check**: `npm run build`, `npm run lint`, `npm test` pass. A manual curl run covers each endpoint.

### Phase 2: Android app
- Keep package `com.demo.taskmanager`. `minSdk` 26 stays.
- Add dependencies: Retrofit, OkHttp, kotlinx.serialization, lifecycle-viewmodel-compose, navigation-compose (or a single-screen state if simpler).
- Build the screens in section 7.
- **Exit check**: `./gradlew assembleDebug`, `./gradlew lint`, `./gradlew test` pass. The app runs on the emulator against the local backend.

### Phase 3: iOS app
- Replace the template `ContentView` in `ios/TaskManager`.
- Build the screens in section 7 with SwiftUI. No third-party packages.
- **Exit check**: the `xcodebuild build` and `xcodebuild test` commands in `AGENTS.md` pass. The app runs on the simulator against the local backend.

### Phase 4: Hosting, release, and docs
- Create the hosted database and the hosted API on the chosen provider (ADR-001).
- Run the migration on the hosted database. Set `DATABASE_URL` as a provider secret.
- Set the hosted URL in the Android release config and the iOS release config.
- Install both apps on a real device or a second machine. Run all five stories against the hosted API.
- Write the root `README.md`: overview, local setup, test commands, deploy steps, config table.
- **Exit check**: `GET https://<host>/health` returns `200`. A task made on one app shows on the other app.

## 10. Architecture Decisions

| ID | Decision | Alternatives | Reason |
|---|---|---|---|
| [ADR-001](ADRs/ADR-001-hosting-provider.md) | **Open.** Hosting provider for API and database. Recommended: Render web service (Docker) with a hosted PostgreSQL. Neon is an option for the database. | Railway, Fly.io, Render Postgres, Neon + Render | Provider-neutral design means this choice only blocks Phase 4. Check free-tier limits (database expiry, API sleep time) before you choose. |
| [ADR-002](ADRs/ADR-002-express-zod-pg-no-orm.md) | Express 5 + zod + `pg`, no ORM. | Fastify, Prisma, Drizzle | One table. Fewer moving parts. zod gives one validation source. |
| [ADR-003](ADRs/ADR-003-node-pg-migrate-sql-migrations.md) | `node-pg-migrate` for schema. | Hand-run SQL, ORM migrations | Same command works locally and on the host. |
| [ADR-004](ADRs/ADR-004-server-computes-overdue-and-counts.md) | Server computes `overdue` and counts. | Compute in each app | One rule set. No drift between Android and iOS. |
| [ADR-005](ADRs/ADR-005-patch-for-update-and-status.md) | `PATCH` for status change and edit. | Separate `/status` endpoint | One endpoint, one validation path. |
| [ADR-006](ADRs/ADR-006-uuid-ids.md) | UUID IDs generated by the database. | Serial integers | No ID guessing. No client coordination needed. |
| [ADR-007](ADRs/ADR-007-no-authentication.md) | No authentication. The hosted API is public. | API key header | Stated scope is no auth. See risk R-1. |
| [ADR-008](ADRs/ADR-008-pin-supported-toolchain-versions.md) | Pin the newest toolchain versions that the ecosystem supports. | Newest of each package | TypeScript 7 and newer Android libraries break lint or the Kotlin build. |
| [ADR-009](ADRs/ADR-009-base-url-per-build-type.md) | API base URL per build type. Plain HTTP in debug builds only. | Compile-time constant, build setting | Release builds stay HTTPS only. |
| [ADR-010](ADRs/ADR-010-dates-as-strings.md) | Dates are `YYYY-MM-DD` strings end to end. | Date objects | No time zone shift in data. |
| [ADR-011](ADRs/ADR-011-manual-wiring-single-view-model.md) | Manual wiring and one list view model per app. | Hilt, Koin, Navigation Compose | Two screens. Fewer parts. |

## 11. Risks and Open Items

| ID | Risk or open item | Mitigation |
|---|---|---|
| R-1 | The hosted API has no auth. Anyone with the URL can read and change tasks. | Accepted by scope. Use a non-guessable URL. Add a shared API key later if the demo goes public. |
| R-2 | Free hosting tiers can sleep or expire the database. | Pick the provider with this in mind. Document the first-request delay in the README. |
| R-3 | iOS deployment target in the project was 26.5. Many devices cannot install it. | **Closed.** Phase 3 lowered it to iOS 17.0. The README states the minimum. |
| R-4 | iOS real-device install needs a signing team. | Owner provides the team in Phase 4. Simulator and TestFlight are the fallbacks. |
| R-5 | Time zone drift for "today". | See A-2. Document it in the README. |
| R-6 | Android emulator and iOS simulator use different local hosts. | Per-platform base URL config in section 7. |

## 12. Quality Gates (all phases)

1. The phase build, lint (if it exists), and test commands pass in a fresh run.
2. Each acceptance criterion in `docs/user-stories.md` maps to at least one automated test or one manual check in the phase notes.
3. No `.env` file, secret, or database URL is committed.

## 13. Build Insights (phases 1 to 3)

| Area | Result |
|---|---|
| Backend | Routes, service and repository layers as planned. 55 tests pass. `createApp({ pool, clock })` lets tests fix the date. A test database name must end in `_test`. |
| Android | `data` and `ui` layers. 41 unit tests and 15 Compose UI tests. Cleartext only in the debug build for `10.0.2.2`. |
| iOS | `Data` and `Features` layers. 37 unit tests and 11 UI tests. Run tests with `-parallel-testing-enabled NO`. The mock API is chosen by the `-UITestMock` launch argument. |
| Deviations | See `operations/archives/2026-11-03-task-manager-phases-1-3/summary.md`. |

### Known limits
- The Dockerfile is not built (Docker missing on the dev machine). Phase 4 builds it.
- The pool has no `error` listener yet (TD-001).
- Release builds point at `https://example.invalid` until phase 4.
- All items: `docs/technical-debt.md`.
