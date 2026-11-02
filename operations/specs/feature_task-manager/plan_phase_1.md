# Phase 1 Plan: Backend API and Database

# Document Control
- **Version**: 1.0
- **Date Created**: 2026-11-02
- **Last Updated**: 2026-11-02
- **Status**: Planned
- **Parent**: `operations/specs/task-manager-hld.md` (sections 3 to 6, roadmap phase 1)

---

# Executive Summary

- **Goal**: Build the Node.js + TypeScript REST API and the PostgreSQL schema for the task manager.
- **Scope**: MVP, backend only.
- **Domains**: backend, API, database.
- **Architecture**: Express 5 app in three layers: `routes` (HTTP and zod), `service` (rules), `repository` (SQL with `pg`). The app is built by a `createApp(deps)` factory so tests use the same code as production. The server computes `overdue` and the dashboard counts.
- **Tech Stack**: Node.js 22+, TypeScript 6.0, Express 5, zod 4, pg 8, node-pg-migrate 9, pino, helmet, Vitest, supertest, ESLint 10 with typescript-eslint.
- **Entry Condition**: `backend/` does not exist. Local PostgreSQL 17 runs. Database `taskmanager` exists.
- **Exit Condition**: `npm run build`, `npm run lint` and `npm test` pass in `backend/`. Every endpoint works against the local database with `curl`. The error and filter rules in this plan have a passing test.

### Pinned versions (checked on npm on 2026-11-02)

| Package | Range | Note |
|---|---|---|
| express | `^5.3.0` | |
| zod | `^4.6.7` | |
| pg | `^8.23.1` | |
| node-pg-migrate | `^9.0.0` | Runtime dependency. The Docker image runs it. |
| pino | `^10.4.0` | |
| helmet | `^8.3.0` | |
| typescript | `~6.0.3` | Do not use 7.x. typescript-eslint 8.72 supports `<6.1.0` only. |
| eslint | `^10.14.0` | |
| typescript-eslint | `^8.72.1` | |
| vitest | `^5.0.5` | Needs Node `^22.12`. |
| supertest | `^7.3.1` | |
| tsx | `^4.23.15` | Dev only. |
| @types/node | `^22` | Match `engines`. |
| @types/express, @types/pg, @types/supertest | latest | Dev only. |

---

# Requirements

## Functional Requirements

- **REQ-001**: WHEN a client sends `POST /api/tasks` with a valid body THE SYSTEM SHALL insert a task with status `TODO` and return `201` with the Task.
- **REQ-002**: THE SYSTEM SHALL reject a create body when `title` is empty after trim or longer than 100 characters, when `priority` is not `LOW`, `MEDIUM` or `HIGH`, when `dueDate` is missing or not a real `YYYY-MM-DD` date, or when the body has an unknown field.
- **REQ-003**: IF `dueDate` on create is before the server UTC date THEN THE SYSTEM SHALL return `400` with `fields.dueDate`.
- **REQ-004**: WHEN a client sends `GET /api/tasks` THE SYSTEM SHALL return all tasks sorted by `dueDate` ascending, then `createdAt` ascending, then `id`.
- **REQ-005**: WHEN `status` or `priority` query parameters are present THE SYSTEM SHALL return only matching tasks, and SHALL combine both filters with AND.
- **REQ-006**: IF a filter value is not in the allowed set THEN THE SYSTEM SHALL return `400` with the invalid parameter in `fields`.
- **REQ-007**: WHEN a client sends `PATCH /api/tasks/:id` with at least one valid field THE SYSTEM SHALL update only the sent fields, set `updated_at`, and return the Task.
- **REQ-008**: IF a PATCH body has no field THEN THE SYSTEM SHALL return `400`.
- **REQ-009**: WHEN a PATCH sets `dueDate` to a value different from the stored value THE SYSTEM SHALL apply the past-date rule. A PATCH that leaves `dueDate` unchanged or omits it SHALL NOT apply the past-date rule (assumption A-3).
- **REQ-010**: WHEN a client sends `DELETE /api/tasks/:id` for an existing task THE SYSTEM SHALL delete it and return `204`.
- **REQ-011**: IF a task ID is unknown or is not a valid UUID THEN THE SYSTEM SHALL return `404` with code `NOT_FOUND` for PATCH and DELETE.
- **REQ-012**: WHEN a client sends `GET /api/summary` THE SYSTEM SHALL return `{ total, todo, inProgress, done, overdue }`. All values are integers. All values are `0` when there are no tasks.
- **REQ-013**: THE SYSTEM SHALL set `overdue` to `true` on a Task when `dueDate` is before the server UTC date and `status` is not `DONE`. The summary `overdue` count SHALL use the same rule.
- **REQ-014**: WHEN a client sends `GET /health` THE SYSTEM SHALL run `SELECT 1` and return `200 {"status":"ok"}`. IF the query fails THEN THE SYSTEM SHALL return `503 {"status":"unavailable"}`.
- **REQ-015**: THE SYSTEM SHALL read `DATABASE_URL`, `PORT` (default `3000`) and `NODE_ENV` from the environment, and SHALL exit with a clear message when `DATABASE_URL` is missing.
- **REQ-016**: THE SYSTEM SHALL store `description` as `null` when the client sends an empty or whitespace-only string.
- **SEC-001**: THE SYSTEM SHALL use parameterized SQL only. Column names in dynamic `UPDATE` come from a fixed allow-list.
- **SEC-002**: THE SYSTEM SHALL return `500 INTERNAL` with a generic message for any unexpected error and SHALL log the detail server-side only.
- **SEC-003**: THE SYSTEM SHALL limit the JSON body to 16 KB, send `helmet` headers, and disable `x-powered-by`.
- **SEC-004**: THE SYSTEM SHALL NOT log request bodies, `DATABASE_URL` or any credential.
- **CON-001**: No authentication, no ORM, no pagination, no URL version prefix (HLD sections 2 and 5).
- **CON-002**: Dates leave the API as `YYYY-MM-DD` strings. The `pg` DATE parser SHALL return strings, not JS `Date` objects.
- **GUD-001**: Config is read once in `config.ts` and passed down. Low-level modules never read `process.env`.
- **PAT-001**: `createApp({ pool, clock })` receives its dependencies. Tests pass a fixed clock.

### Acceptance scenarios (Gherkin)

```gherkin
Feature: Create task (US-01)
  Scenario: Valid create
    Given the tasks table is empty
    When the client POSTs {"title":"Pay rent","priority":"HIGH","dueDate":"<today>"} to /api/tasks
    Then the status is 201
    And the body has status "TODO", overdue false and a UUID id
  Scenario: Title too long
    When the client POSTs a title of 101 characters
    Then the status is 400 and fields.title is present
  Scenario: Past due date
    When the client POSTs dueDate "<yesterday>"
    Then the status is 400 and fields.dueDate is present
  Scenario: Whitespace title
    When the client POSTs title "   "
    Then the status is 400 and fields.title is present

Feature: List and filter (US-02)
  Scenario: Combined filters
    Given tasks with (TODO,HIGH), (TODO,LOW), (DONE,HIGH)
    When the client GETs /api/tasks?status=TODO&priority=HIGH
    Then exactly the (TODO,HIGH) task is returned
  Scenario: Bad filter
    When the client GETs /api/tasks?status=BLOCKED
    Then the status is 400 and fields.status is present
  Scenario: No match
    When no task matches the filters
    Then the status is 200 and the body is []

Feature: Update and delete (US-03, US-04)
  Scenario: Status change persists
    Given a TODO task
    When the client PATCHes {"status":"DONE"}
    Then a later GET returns that task with status "DONE"
  Scenario: Rename an overdue task
    Given a task whose dueDate is in the past
    When the client PATCHes {"title":"New"} only
    Then the status is 200
  Scenario: Move due date to the past
    When the client PATCHes dueDate "<yesterday>" on a task due today
    Then the status is 400
  Scenario: Delete unknown id
    When the client DELETEs a random UUID
    Then the status is 404 with code NOT_FOUND

Feature: Summary (US-05)
  Scenario: Empty list
    Given no tasks
    When the client GETs /api/summary
    Then the body is {"total":0,"todo":0,"inProgress":0,"done":0,"overdue":0}
  Scenario: Overdue excludes DONE
    Given one past-due TODO task and one past-due DONE task
    Then summary.overdue is 1
```

---

# Dependencies

| ID | Name | Type | Version | Purpose |
|----|------|------|---------|---------|
| DEP-001 | PostgreSQL | service | 17 (local) | Storage. Needs `gen_random_uuid()` (built in since 13). |
| DEP-002 | Node.js | runtime | 22.12+ | Runtime, `engines` field. |
| DEP-003 | `taskmanager_test` database | local setup | n/a | Isolated database for tests. Created in TASK-P1-001. |
| DEP-004 | Packages | npm | see Pinned versions | Build, run, test. |

---

# Plan Details

## Design: How it should happen

### Impact on system design

```text
HTTP request
   │
   ▼
┌───────────────┐   zod parse    ┌───────────────┐   SQL ($1..)   ┌────────────┐
│ routes        │───────────────▶│ service       │───────────────▶│ repository │──▶ PostgreSQL
│ tasks/summary │  typed input   │ rules, clock  │  rows → Task   │ pg Pool    │
│ health        │◀───────────────│ AppError      │◀───────────────│            │
└───────┬───────┘   JSON         └───────────────┘                └────────────┘
        │ throws AppError / ZodError / unknown
        ▼
┌───────────────┐
│ errorHandler  │──▶ { error: { code, message, fields? } }
└───────────────┘
```

### Decisions

| ID | Decision | Rationale | Alternatives Considered | Side effect (which modules?) |
|----|----------|-----------|------------------------|-------------------|
| DES-P1-001 | CommonJS output. `tsconfig` uses `module: nodenext` with no `"type":"module"`. | Extensionless imports. Fewer config lines. Works with Vitest. | ESM with `.js` import suffixes | all `src` |
| DES-P1-002 | Service takes `clock: () => Date` and derives `today` as a UTC `YYYY-MM-DD` string. | Tests fix the date. One place defines "today" (A-2). | `CURRENT_DATE` in SQL | service, repository |
| DES-P1-003 | `overdue` on list and create/update responses is computed in the service from the row. The summary `overdue` is a SQL `COUNT(*) FILTER` with `today` as a bind parameter. | One SQL for counts, no second query. Both use the same rule (BR-7). | Two queries | service, repository |
| DES-P1-004 | `summary` uses one query with `COUNT(*) FILTER (WHERE ...)`. | One table scan. | Five `COUNT` queries | repository |
| DES-P1-005 | Dynamic `UPDATE` builds `SET col = $n` from a fixed map of API field to column. | Partial update with no string concatenation of input. | `COALESCE` per column (cannot clear `description`) | repository |
| DES-P1-006 | Malformed UUID in the path returns `404`, not `400`. | Client cannot tell a bad ID from a missing task. No `pg` cast error. | `400` | routes |
| DES-P1-007 | Migrations are raw SQL files with `-- Up Migration` and `-- Down Migration` blocks. | The schema is plain SQL from the HLD. | JS migrations | `migrations/` |
| DES-P1-008 | HLD index `tasks_status_priority_idx` stays as written. | Matches the HLD. List queries on one user are small, so the index is cheap. | Drop it | migration |
| DES-P1-009 | SSL for hosted PostgreSQL uses `sslmode` in `DATABASE_URL`. No extra env variable. | HLD section 9 phase 1. Fewer settings. | `DATABASE_SSL` flag | db, Phase 4 |
| DES-P1-010 | Request log is one small middleware that logs method, path, status and duration with pino. No body. | SEC-004. No extra dependency. | pino-http | app |

### Constraints & Assumptions

| Type | ID | Description | Impact if Wrong | Handling Strategy |
|------|----|-------------|-----------------|-------------------|
| Constraint | CON-P1-001 | Docker is not installed on the dev machine. | The Dockerfile cannot be built here. | Review the Dockerfile by reading. Phase 4 builds it on the host. |
| Constraint | CON-P1-002 | Only `backend/` and `.gitignore` entries change. No file outside `backend/` is edited. | n/a | n/a |
| Assumption | ASM-P1-001 | A-1: `priority` and `dueDate` are required on create. | Apps send them anyway. A relaxed rule is a small change. | Not yet confirmed by the owner. Listed as a finding in the planning report. |
| Assumption | ASM-P1-002 | A-2: "today" is the UTC date. | A user near midnight sees a one-day shift. | Documented in README (Phase 4). |
| Assumption | ASM-P1-003 | A-3: past-date check on PATCH applies only when `dueDate` changes. | Users could not rename overdue tasks. | REQ-009 and a test. |
| Assumption | ASM-P1-004 | `pg` 8.23 honors `sslmode=require` in the URL. | Hosted connection fails. | Phase 4 checks against the real host. Fallback: add `DATABASE_SSL`. |

### Edge cases

| ID | Scenario | Expected Behavior | Handling Strategy |
|----|----------|-------------------|-------------------|
| EDGE-P1-001 | Title is exactly 100 characters | Accepted | zod `max(100)` after trim |
| EDGE-P1-002 | Title has 100 characters plus spaces | Accepted (trim first) | `.trim()` before `.min/.max` |
| EDGE-P1-003 | `dueDate` is `2026-02-30` | `400` | `z.iso.date()` rejects it |
| EDGE-P1-004 | `dueDate` equals today | Accepted, not overdue | Compare strings with `<` |
| EDGE-P1-005 | PATCH `{"description":null}` | Clears the description | zod `nullable()` on update |
| EDGE-P1-006 | PATCH `{"description":"  "}` | Stored as `null` | Same normalizer as create |
| EDGE-P1-007 | PATCH `{"status":"DONE"}` on a past-due task | `200`, no date check | REQ-009 |
| EDGE-P1-008 | Malformed JSON body | `400 VALIDATION_ERROR` | Error handler maps `SyntaxError` with `type: entity.parse.failed` |
| EDGE-P1-009 | Body larger than 16 KB | `413`, mapped to `400 VALIDATION_ERROR` | Error handler maps `entity.too.large` |
| EDGE-P1-010 | Database down | `/health` gives `503`. Other routes give `500 INTERNAL` | Error handler, no leak |
| EDGE-P1-011 | Two filters sent twice, `?status=TODO&status=DONE` | `400` | zod string schema rejects arrays |
| EDGE-P1-012 | Unknown route | `404 NOT_FOUND` | Final `app.use` |

### Directory Map

```text
backend/
├── package.json              (scripts: build, lint, test, migrate, start, dev)
├── tsconfig.json
├── tsconfig.build.json
├── eslint.config.mjs
├── vitest.config.ts
├── Dockerfile
├── .dockerignore
├── .env.example
├── migrations/
│   └── 1730500000000_create-tasks.sql
├── src/
│   ├── server.ts             entry: load config, build pool, listen, shutdown
│   ├── app.ts                createApp({ pool, clock })
│   ├── config.ts             read + validate env (only file that reads process.env)
│   ├── db.ts                 createPool(databaseUrl), DATE type parser
│   ├── errors.ts             AppError, error codes
│   ├── errorHandler.ts       Express error middleware + 404 handler
│   ├── tasks/
│   │   ├── task.schemas.ts   zod schemas + inferred types
│   │   ├── task.routes.ts    router for /api/tasks
│   │   ├── task.service.ts   rules, overdue, today
│   │   └── task.repository.ts  SQL
│   ├── summary/
│   │   └── summary.routes.ts   GET /api/summary (calls service)
│   └── health/
│       └── health.routes.ts
└── test/
    ├── helpers.ts            test app, truncate, seed
    ├── globalSetup.ts        run migrations on taskmanager_test
    ├── task.service.test.ts  unit (fake repository, fixed clock)
    ├── task.schemas.test.ts  unit
    ├── config.test.ts        unit
    ├── tasks.api.test.ts     integration (supertest + real DB)
    ├── summary.api.test.ts   integration
    └── health.api.test.ts    integration
```

### Directory Specifications

| ID | Task | File(s) | Responsibility or Expected Behaviors | Validations/Error handling | Key Configurations |
|----|------|---------|--------------------------------------|----------------------------|--------------------|
| TASK-P1-001 | Create the test database | shell only | Run `createdb taskmanager_test`. Skip if it exists. | Verify with `psql -l`. | `postgresql://localhost:5432/taskmanager_test` |
| TASK-P1-002 | Scaffold the project | `backend/package.json`, `tsconfig.json`, `tsconfig.build.json`, `.env.example`, root `.gitignore` | Add scripts: `build` (`tsc -p tsconfig.build.json`), `lint` (`eslint .`), `test` (`vitest run`), `migrate` (`node-pg-migrate`), `start` (`node dist/server.js`), `dev` (`tsx watch src/server.ts`). Set `engines.node >=22.12`. Add `.gitignore` lines: `.env`, `node_modules`, `dist`. | `.env.example` holds only variable names and a local example URL with no password. | `strict: true`, `noUncheckedIndexedAccess: true`, `target ES2023`, `module nodenext`, `outDir dist`, `rootDir src` |
| TASK-P1-003 | Write the migration | `migrations/1730500000000_create-tasks.sql` | Create `tasks` table and `tasks_status_priority_idx` as in HLD section 4. Add `-- Down Migration` that drops the index and table. | CHECK constraints stay in SQL (defense in depth). | `node-pg-migrate -m migrations --migration-file-language sql` |
| TASK-P1-004 | Add config and db | `src/config.ts`, `src/db.ts` | `loadConfig(env)` returns `{ databaseUrl, port, nodeEnv }`. `createPool(url)` returns a `pg.Pool` (max 5). Register `types.setTypeParser(1082, v => v)` so DATE stays a string. | `databaseUrl` missing: throw an error with the message `DATABASE_URL is required`. `PORT` must be an integer 1 to 65535. | `PORT=3000`, `NODE_ENV=development` |
| TASK-P1-005 | Add errors | `src/errors.ts`, `src/errorHandler.ts` | `AppError(code, status, message, fields?)`. Handler maps `AppError`, `ZodError` (to `VALIDATION_ERROR` with `fields` map), body-parser errors, unknown to `INTERNAL`. `notFoundHandler` returns `NOT_FOUND`. | Log unknown errors with pino including stack. Response never has stack or SQL text. | none |
| TASK-P1-006 | Define schemas | `src/tasks/task.schemas.ts` | Export `createTaskSchema`, `updateTaskSchema`, `listQuerySchema`, `Task`, `CreateTask`, `UpdateTask`, `TaskFilters`, `Summary`, `PRIORITIES`, `STATUSES`. All object schemas use `.strict()`. | Rules in REQ-002, REQ-006, REQ-008, EDGE-P1-001..003, 005, 006, 011. Update schema uses `.refine` for "at least one field". | Title max 100. |
| TASK-P1-007 | Write the repository | `src/tasks/task.repository.ts` | Interface `TaskRepository` with `create`, `list(filters)`, `findById`, `update(id, patch)`, `remove(id)`, `summary(today)`. Class `PgTaskRepository(pool)` implements it. Maps rows to `TaskRow` (camelCase, dates as strings, timestamps as ISO strings). | All values bound as `$n`. Update column names come from a fixed map. `remove` returns a boolean. `update` returns `null` when no row matches. | `ORDER BY due_date, created_at, id` |
| TASK-P1-008 | Write the service | `src/tasks/task.service.ts` | `createTaskService(repo, clock)` returns `{ create, list, update, remove, summary }`. Adds `overdue`. Applies BR-4 and A-3. Normalizes blank description to `null`. Throws `AppError` `NOT_FOUND`. | `update` loads the current task first to compare `dueDate` (REQ-009). Unknown ID gives `NOT_FOUND`. | `today = clock().toISOString().slice(0,10)` |
| TASK-P1-009 | Write the routes | `src/tasks/task.routes.ts`, `src/summary/summary.routes.ts`, `src/health/health.routes.ts` | `createTaskRouter(service)`, `createSummaryRouter(service)`, `createHealthRouter(pool)`. Routes parse input with zod, call the service, send JSON. Express 5 forwards async errors. | UUID check with `z.uuid()`. A bad UUID throws `NOT_FOUND` (DES-P1-006). Health catches the DB error and returns `503`. | none |
| TASK-P1-010 | Write the app and server | `src/app.ts`, `src/server.ts` | `createApp({ pool, clock })` wires `helmet`, `express.json({ limit: '16kb' })`, request log, routers, 404, error handler. `server.ts` loads config, creates the pool, listens, and closes server and pool on `SIGTERM` and `SIGINT`. | `app.disable('x-powered-by')`. The request log skips `/health`. | `trust proxy` set to `1` for hosted proxies |
| TASK-P1-011 | Add lint and test config | `eslint.config.mjs`, `vitest.config.ts`, `test/globalSetup.ts`, `test/helpers.ts` | ESLint flat config with typescript-eslint recommended. Vitest uses `globalSetup` to run migrations on `taskmanager_test`, `fileParallelism: false`, and sets `DATABASE_URL` for tests from `TEST_DATABASE_URL` (default local test URL). Helper truncates `tasks` before each test. | Global setup refuses to run when the URL does not end in `_test`. This protects the dev database. | `TEST_DATABASE_URL` |
| TASK-P1-012 | Write unit tests | `test/task.service.test.ts`, `test/task.schemas.test.ts`, `test/config.test.ts` | Service tests use a fake in-memory repository and a fixed clock. Cover BR-4, A-3, `overdue`, description normalizer, `NOT_FOUND`. Schema tests cover EDGE-P1-001..006, 011. Config tests cover REQ-015 (missing URL, bad port, default port). | Each test names the requirement ID. | Fixed clock `2026-11-02T10:00:00Z` |
| TASK-P1-013 | Write integration tests | `test/tasks.api.test.ts`, `test/summary.api.test.ts`, `test/health.api.test.ts` | Cover every Gherkin scenario above plus EDGE-P1-008, 009, 012, and the `/health` `503` path (pool pointed at a closed port). | Assert error body shape on every `4xx`. Assert no `stack` key on `500`. | Use `createApp` with the test pool |
| TASK-P1-014 | Write the Dockerfile | `Dockerfile`, `.dockerignore` | Two stages. Build stage: `npm ci`, `npm run build`. Runtime stage: `node:22-alpine`, `npm ci --omit=dev`, copy `dist` and `migrations`, run as user `node`, `CMD ["sh","-c","npm run migrate up && node dist/server.js"]`. | `.dockerignore` lists `node_modules`, `dist`, `.env*`, `test`. | `EXPOSE 3000`, `ENV NODE_ENV=production` |
| TASK-P1-015 | Verify end to end | none | Run the exit commands. Run the manual `curl` script. Run `git status` to confirm no `.env` file is staged. | See Verification table. | none |

Order: 001, 002, 003, 004, 005, 006, 007, 008, 009, 010, 011, 012, 013, 014, 015. Write each unit test file before its target file when you follow TDD (see `test-driven-development` skill).

### Schemas grouped by domain

```yaml
# Task domain (input)
CreateTaskRequest:               # .strict()
  title:       string, trim, 1..100          # required
  description: string|null, optional         # blank string -> null
  priority:    LOW|MEDIUM|HIGH               # required
  dueDate:     "YYYY-MM-DD" real date        # required, >= today (service)
UpdateTaskRequest:               # .strict(), at least one key
  title?: string trim 1..100
  description?: string|null
  priority?: LOW|MEDIUM|HIGH
  status?: TODO|IN_PROGRESS|DONE
  dueDate?: "YYYY-MM-DD"          # past-date rule only if value changes
ListQuery:                        # .strict()
  status?: TODO|IN_PROGRESS|DONE
  priority?: LOW|MEDIUM|HIGH

# Task domain (output)
Task:
  id: uuid
  title: string
  description: string|null
  priority: LOW|MEDIUM|HIGH
  status: TODO|IN_PROGRESS|DONE
  dueDate: "YYYY-MM-DD"
  overdue: boolean
  createdAt: ISO-8601
  updatedAt: ISO-8601
Summary: { total: int, todo: int, inProgress: int, done: int, overdue: int }

# Errors
ErrorResponse:
  error:
    code: VALIDATION_ERROR | NOT_FOUND | INTERNAL
    message: string
    fields?: { [fieldName]: string }
```

| Code | Status | Example |
|---|---|---|
| `VALIDATION_ERROR` | 400 | `{"error":{"code":"VALIDATION_ERROR","message":"Invalid request","fields":{"title":"Title is required"}}}` |
| `NOT_FOUND` | 404 | `{"error":{"code":"NOT_FOUND","message":"Task not found"}}` |
| `INTERNAL` | 500 | `{"error":{"code":"INTERNAL","message":"Something went wrong"}}` |

Field error messages are plain English. The apps in Phases 2 and 3 show `fields.<name>` as-is.

### Data Models & flows

<settings>

| Variable | Purpose | Secret? | Default |
|---|---|---|---|
| `DATABASE_URL` | PostgreSQL URL. Add `?sslmode=require` on a hosted database. | Yes | none, required |
| `PORT` | HTTP port | No | `3000` |
| `NODE_ENV` | `development`, `test`, `production` | No | `development` |
| `TEST_DATABASE_URL` | Used by tests only | No | `postgresql://localhost:5432/taskmanager_test` |

`backend/.env.example` holds the first three names. Never commit `.env`.
</settings>

<Data-Models>

```text
tasks
┌─────────────┬──────────────┬───────────────────────────────────────────┐
│ id          │ uuid PK      │ default gen_random_uuid()                 │
│ title       │ varchar(100) │ NOT NULL, CHECK length(btrim(title)) > 0  │
│ description │ text         │ NULL                                      │
│ priority    │ text         │ NOT NULL, CHECK LOW|MEDIUM|HIGH           │
│ status      │ text         │ NOT NULL default TODO, CHECK 3 values     │
│ due_date    │ date         │ NOT NULL                                  │
│ created_at  │ timestamptz  │ NOT NULL default now()                    │
│ updated_at  │ timestamptz  │ NOT NULL default now()                    │
└─────────────┴──────────────┴───────────────────────────────────────────┘
index tasks_status_priority_idx (status, priority)
```

The code sets `updated_at = now()` in every `UPDATE`. No trigger is needed.
The DB `tasks` column names use `snake_case`. The API uses `camelCase`. Only `task.repository.ts` knows both.
</Data-Models>

<Data-flow>

```text
PATCH /api/tasks/:id {"dueDate":"2026-11-01"}      (today = 2026-11-02)
 routes   : parse id (uuid) -> parse body (updateTaskSchema)
 service  : current = repo.findById(id)            -> null? NOT_FOUND
            patch.dueDate != current.dueDate and patch.dueDate < today -> AppError 400 {dueDate}
            repo.update(id, patch)
 repo     : UPDATE tasks SET due_date=$1, updated_at=now() WHERE id=$2 RETURNING ...
 service  : add overdue = dueDate < today && status != DONE
 routes   : 200 Task
```

The `findById` then `update` pair is not atomic. This is acceptable for one user. The `UPDATE` itself is one statement.
</Data-flow>

---
#### **Process**

- Runtime Considerations: One Node process. Pool max 5. Graceful shutdown on `SIGTERM`.
- Deployment Impact: The Dockerfile runs `npm run migrate up` before start. Phase 4 sets `DATABASE_URL` as a host secret. The host health check path is `/health`.

---

# Risks

| ID | Risk | Likelihood | Impact | Mitigation |
|----|------|-----------|--------|------------|
| RISK-001 | TypeScript 7 is the npm `latest`. typescript-eslint does not support it. | High | Med | Pin `typescript@~6.0.3`. |
| RISK-002 | `node-pg-migrate` 9 is ESM only. | Low | Low | Only the CLI uses it. The app code never imports it. |
| RISK-003 | Docker is not installed. The image cannot be tested here. | High | Med | Keep the Dockerfile short. Phase 4 builds it on the host. |
| RISK-004 | Tests could run against the dev database and erase data. | Low | High | Global setup refuses a URL that does not end in `_test`. |
| RISK-005 | A DATE returned as a JS `Date` shifts by time zone. | Med | High | CON-002 type parser. A test asserts the string form. |
| RISK-006 | Package versions change before the build. | Med | Low | Use `npm install` once, commit `package-lock.json`, then use `npm ci`. |

---

# Verification

Run all commands in `backend/` unless noted.

| ID | Check | Command | Pass condition |
|----|-------|---------|----------------|
| VER-P1-001 | Build | `npm run build` | Exit code 0, `dist/server.js` exists |
| VER-P1-002 | Lint | `npm run lint` | 0 errors, 0 warnings |
| VER-P1-003 | Tests | `npm test` | All pass. Every Gherkin scenario above has a test. |
| VER-P1-004 | Migration up and down | `DATABASE_URL=<test url> npm run migrate up`, then `down`, then `up` | Each exits 0 |
| VER-P1-005 | Manual API run | Start `npm run dev` with the local `DATABASE_URL`. `curl` create, list with both filters, patch status, patch bad date, delete, summary, `/health`. | Status codes match REQ-001 to REQ-014 |
| VER-P1-006 | No secret committed | `git status --short` and `git ls-files backend \| grep -i '\.env$'` | No `.env` file listed |
| VER-P1-007 | Error leak check | `curl` with malformed JSON. Stop PostgreSQL connection by using a wrong `DATABASE_URL` and call `/api/tasks`. | Body has `code` and `message` only. No stack. No SQL. |

Documentation for this phase: `backend/.env.example` and comments only where intent is not clear. The root README is a Phase 4 task.

Review step: a reviewer checks the Code Guidelines below and the requirement-to-task trace.

### Requirement trace

| Requirement | Tasks | Tests |
|---|---|---|
| REQ-001, 002, 003, 016 | 006, 007, 008, 009 | tasks.api, task.service, task.schemas |
| REQ-004, 005, 006 | 006, 007, 009 | tasks.api |
| REQ-007, 008, 009 | 006, 007, 008, 009 | tasks.api, task.service |
| REQ-010, 011 | 007, 008, 009 | tasks.api |
| REQ-012, 013 | 007, 008, 009 | summary.api, task.service |
| REQ-014 | 009 | health.api |
| REQ-015 | 004 | config.test |
| SEC-001 to 004 | 005, 007, 010 | tasks.api (error shape), VER-P1-007 |

---

# Code Guidelines

**Clean code**
- Keep each file to one job. `routes` never contain SQL. `repository` never contains rules.
- Pass dependencies in (`createApp`, `createTaskService`). Only `config.ts` reads `process.env` (DES-01).
- Define `TaskRepository` as an interface first, then `PgTaskRepository` (GEN-03).
- No flag arguments. No commented-out code. Comments explain intent only.
- Use one error type (`AppError`). Do not swallow errors. Log unexpected errors once with context (LOG-01).
- Log shape: `timestamp`, `level`, `message`, plus `method`, `path`, `status`, `duration_ms`. Past-tense messages.

**Security**
- Validate every external input with zod at the route edge. Use `.strict()` on objects.
- Parameterize all SQL. Use the field-to-column map for dynamic `UPDATE`.
- Return generic `500` bodies. Keep stack traces in logs only.
- Do not log bodies, URLs with credentials, or `DATABASE_URL`.
- Run `npm audit --omit=dev` once at the end. Report high or critical findings.
- Container runs as non-root user `node`.

**Database**
- Use `timestamptz`, `uuid`, CHECK constraints, `snake_case` names. No `SELECT *`. List columns.
- Schema change and data change live in separate migrations. This phase has only a schema migration.
- Do not edit a migration after it runs on the hosted database. Add a new one.
