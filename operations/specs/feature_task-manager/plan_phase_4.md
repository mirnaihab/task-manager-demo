# Phase 4 Plan: Hosting, Release, and Docs

# Document Control
- **Version**: 1.0
- **Date Created**: 2026-11-02
- **Last Updated**: 2026-11-02
- **Status**: Planned. Task TASK-P4-001 (provider decision) blocks all other tasks.
- **Parent**: `operations/specs/task-manager-hld.md` (section 9 phase 4, ADR-001, risks R-1, R-2, R-4, R-5)

---

# Executive Summary

- **Goal**: Host the API and the database online, point both apps at the hosted URL, prove it works on another device, and write the README.
- **Scope**: Deployment and documentation. Small code change (two config values).
- **Domains**: infrastructure, documentation.
- **Architecture**: The Phase 1 Docker image runs on a hosting provider. A hosted PostgreSQL gives `DATABASE_URL`. The apps call `https://<host>`. The apps never reach the database.
- **Tech Stack**: Docker image from Phase 1, provider web service plus provider PostgreSQL, `render.yaml` if the provider is Render.
- **Entry Condition**: Phases 1, 2 and 3 are complete and merged. The owner has chosen the provider (TASK-P4-001).
- **Exit Condition**: `GET https://<host>/health` returns `200`. A task made on the Android app shows on the iOS app. The root `README.md` lets a new developer run all three parts.

---

# Requirements

## Functional Requirements

- **REQ-401**: THE SYSTEM SHALL run the backend on a public HTTPS URL with a hosted PostgreSQL database.
- **REQ-402**: WHEN the container starts THE SYSTEM SHALL apply pending migrations before it accepts traffic (Phase 1 Dockerfile).
- **REQ-403**: THE SYSTEM SHALL read `DATABASE_URL` from a provider secret. The value SHALL NOT appear in the repository, the image, logs or the README.
- **REQ-404**: WHEN a request reaches `/health` THE SYSTEM SHALL confirm the database connection. The provider health check SHALL use this path.
- **REQ-405**: THE SYSTEM SHALL ship the Android release build and the iOS Release configuration with the hosted HTTPS URL.
- **REQ-406**: WHEN a user creates a task on one app THE SYSTEM SHALL show it on the other app after a refresh.
- **REQ-407**: THE SYSTEM SHALL include a root `README.md` with: overview, architecture, prerequisites, local setup for the three parts, config table, test commands, deploy steps, known limits.
- **SEC-401**: THE SYSTEM SHALL serve the API over HTTPS only. The apps SHALL NOT accept cleartext in release builds (Phase 2 SEC-201, Phase 3 SEC-301).
- **SEC-402**: THE SYSTEM SHALL use a database connection with TLS (`sslmode=require` in the URL).
- **SEC-403**: THE SYSTEM SHALL use a non-guessable service URL because the API has no authentication (ADR-007, R-1). The README SHALL state this limit.
- **CON-401**: No authentication, no custom domain, no CI/CD pipeline in this phase. Deploy by a manual push or the provider Git integration.
- **CON-402**: Docker is not installed on the dev machine. The provider builds the image.
- **GUD-401**: Prefer the provider free tier for the demo. Record its limits in the README (R-2).

### Acceptance scenarios (Gherkin)

```gherkin
Feature: Hosted deployment
  Scenario: Health
    When anyone sends GET https://<host>/health
    Then the status is 200 and the body is {"status":"ok"}
  Scenario: Migration ran
    When the client sends GET https://<host>/api/summary on a new database
    Then the body is {"total":0,"todo":0,"inProgress":0,"done":0,"overdue":0}
  Scenario: Cross-device
    Given the Android release app and the iOS Release app use the hosted URL
    When the user creates "Demo task" on Android and refreshes iOS
    Then iOS shows "Demo task"
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

| ID | Name | Type | Version | Purpose |
|----|------|------|---------|---------|
| DEP-401 | Hosting provider account | service | n/a | Web service and PostgreSQL. Owner creates the account. |
| DEP-402 | Git remote | service | n/a | Provider Git integration pulls the repository. Needs a hosted Git repository. |
| DEP-403 | Phase 1 `Dockerfile` | artifact | Phase 1 | The deploy image. |
| DEP-404 | Real device or second machine | hardware | n/a | Cross-device check. iOS device needs a signing team (R-4). |

---

# Plan Details

## Design: How it should happen

### Impact on system design

```text
 Android release ──┐  HTTPS   ┌───────────────────────────┐  TLS   ┌──────────────────┐
                   ├─────────▶│ Provider web service      │───────▶│ Provider         │
 iOS Release ──────┘          │ Docker image (Phase 1)    │        │ PostgreSQL       │
                              │ migrate -> start, /health │        │ DATABASE_URL     │
                              └───────────────────────────┘        └──────────────────┘
```

### Decisions

| ID | Decision | Rationale | Alternatives Considered | Side effect (which modules?) |
|----|----------|-----------|------------------------|-------------------|
| DES-P4-001 | The provider is chosen by the owner in TASK-P4-001. Recommendation: Render web service (Docker) with Render PostgreSQL. | One provider, one dashboard, Docker supported, TLS included. | Railway, Fly.io, Neon for the database plus any web host | all tasks |
| DES-P4-002 | Free tier limits (checked on the Render docs, 2026-11-02): a free web service sleeps after 15 minutes idle and takes about one minute to wake. A free PostgreSQL database expires 30 days after creation, has 1 GB, no backups, and is deleted after a 14 day grace period. Only one free database per workspace. | A demo that must "work on another device" later will fail after 30 days on a free Render database. The owner must accept this or pay for the smallest paid database. | Use a free database from another provider | TASK-P4-001 |
| DES-P4-003 | Deploy with the Phase 1 Dockerfile. Migrations run at container start. | Works on any provider. No provider-specific job feature (free tiers often lack one-off jobs). | Pre-deploy command | `backend/Dockerfile` |
| DES-P4-004 | If the provider is Render, add `render.yaml` at the repository root (Blueprint): one web service (`runtime: docker`, `dockerfilePath: backend/Dockerfile`, `dockerContext: backend`, `healthCheckPath: /health`) and one database. `DATABASE_URL` comes from the database `connectionString`. | Repeatable setup in one file. | Click through the dashboard | repo root |
| DES-P4-005 | Hosted URL is set in two files only: `android/app/build.gradle.kts` (release `API_BASE_URL`) and `ios/TaskManager.xcodeproj/project.pbxproj` (Release `INFOPLIST_KEY_API_BASE_URL`). | Phase 2 and 3 already isolate this value. | A shared config file | android, ios |
| DES-P4-006 | Android: install the release build as a debug-signed APK for the demo. The `release` build type gets `signingConfig = signingConfigs.getByName("debug")` for the demo only. State this in the README. No keystore is created. | A real keystore needs secret handling and a store account. Out of scope for a demo. | Create a keystore | `app/build.gradle.kts` |
| DES-P4-007 | iOS real device: the owner sets the signing team in Xcode (Signing and Capabilities). Do not commit the team ID change unless the owner agrees. Fallback: run on the simulator and use a second Mac. | R-4. | TestFlight (needs paid program) | project settings |

### Constraints & Assumptions

| Type | ID | Description | Impact if Wrong | Handling Strategy |
|------|----|-------------|-----------------|-------------------|
| Constraint | CON-P4-001 | Phase 4 needs account creation and secrets only the owner can do. | The agent cannot finish alone. | TASK-P4-001 to 004 are owner steps with exact instructions. The agent verifies results with `curl`. |
| Constraint | CON-P4-002 | The repository has no Git remote or hosted repository yet (check with `git remote -v`). | The provider cannot pull. | TASK-P4-002 covers this. |
| Assumption | ASM-P4-001 | The Phase 1 image builds on the provider. | Deploy fails. | Read the provider build log. Fix in `backend/`. |
| Assumption | ASM-P4-002 | `sslmode=require` in the URL works with `pg` 8 (Phase 1 ASM-P1-004). | API cannot connect. | Add `DATABASE_SSL` handling in `db.ts` with a test. |
| Assumption | ASM-P4-003 | The API stays public with no auth (ADR-007). | Anyone with the URL edits data. | SEC-403. README states it. |

### Edge cases

| ID | Scenario | Expected Behavior | Handling Strategy |
|----|----------|-------------------|-------------------|
| EDGE-P4-001 | First request after sleep | Takes up to a minute, then works | Apps use a 15 s read timeout, so the first call can fail. Add one automatic retry on `Network` error for the initial list load only. Record the choice in the README. |
| EDGE-P4-002 | Free database expires | API returns `500`, `/health` returns `503` | README lists the date to watch. Owner upgrades or re-creates the database and re-runs the migration at the next start. |
| EDGE-P4-003 | Migration fails on start | Container exits, provider shows a failed deploy | The old version keeps serving. Read the log, add a new forward migration. |
| EDGE-P4-004 | Wrong URL in a release build | App shows the network error state | Cross-device check VER-P4-004 catches it. |
| EDGE-P4-005 | Time zone offset | A user near midnight sees a one-day shift for "today" (A-2, R-5) | README known limits. |

### Directory Map

```text
(repo root)
├── README.md                                (create)
├── render.yaml                              (create, only if provider is Render)
├── AGENTS.md                                (modify: Commands and hosting notes)
├── backend/Dockerfile                       (modify only if the provider build fails)
├── android/app/build.gradle.kts             (modify: release API_BASE_URL, demo signing)
├── ios/TaskManager.xcodeproj/project.pbxproj (modify: Release INFOPLIST_KEY_API_BASE_URL)
├── android/.../ui/TaskListViewModel.kt      (modify: first-load retry, EDGE-P4-001)
└── ios/TaskManager/Features/TaskListViewModel.swift (modify: first-load retry, EDGE-P4-001)
```

### Directory Specifications

| ID | Task | File(s) | Responsibility or Expected Behaviors | Validations/Error handling | Key Configurations |
|----|------|---------|--------------------------------------|----------------------------|--------------------|
| TASK-P4-001 | Decide the provider (owner) | none | Owner confirms ADR-001 and accepts DES-P4-002 limits. Record the decision in `operations/specs/task-manager-hld.md` ADR-001. | Stop the phase until this is done. | Provider name |
| TASK-P4-002 | Prepare the repository | Git remote | Owner creates a hosted Git repository. Run `git remote add origin <url>` and push `main`. | `git ls-files \| grep -i '\.env$'` returns nothing before the push. | none |
| TASK-P4-003 | Create hosted services | `render.yaml` (Render) or provider dashboard | Create the PostgreSQL database and the Docker web service from the `backend/` folder. Set health check path `/health`. Set `NODE_ENV=production`. The provider gives `PORT`. | Choose the region closest to the database. Put both in the same region. | `DATABASE_URL` as secret or `fromDatabase` link |
| TASK-P4-004 | First deploy and smoke test | none | Trigger the deploy. Read the log: migration applied, server listening. Run the smoke commands in VER-P4-001 to 003. | If `/health` returns `503`, check `DATABASE_URL` and `sslmode`. | none |
| TASK-P4-005 | First-load retry | Android `TaskListViewModel.kt`, iOS `TaskListViewModel.swift` and their tests | Retry the initial load once after 3 seconds when the error is `Network`. Do not retry mutations. | Unit test with the fake or mock API that fails once and then succeeds. | none |
| TASK-P4-006 | Set the hosted URL in both apps | `android/app/build.gradle.kts`, `ios/.../project.pbxproj` | Replace both `https://example.invalid` placeholders with `https://<host>` (no trailing path on iOS, trailing slash on Android). Add demo signing for Android release (DES-P4-006). | `grep -rn "example.invalid" android ios` returns nothing afterward. | Hosted URL |
| TASK-P4-007 | Build release apps | none | Build `./gradlew assembleRelease` and `xcodebuild build -scheme TaskManager -configuration Release ...`. Install on devices. | Both build with no error. | none |
| TASK-P4-008 | Cross-device check | none | On two devices, run US-01 to US-05 against the hosted API. Create on Android, refresh on iOS, then the reverse. | Record the result in the PR text. | none |
| TASK-P4-009 | Write README | `README.md` | Sections: Overview, Architecture (diagram from the HLD), Prerequisites, Local setup (database, backend, Android, iOS), Config table (every env variable and URL setting), Commands (from `AGENTS.md`), Deploy (steps from TASK-P4-003), Known limits (no auth, free tier sleep and expiry, UTC date, iOS 17 minimum), Troubleshooting. | Follow the STE rules in `AGENTS.md`. No secrets, no real `DATABASE_URL`. | none |
| TASK-P4-010 | Update agent notes | `AGENTS.md` | Add the hosted URL (public, not secret), the deploy method and the real iOS test command. Mark `backend/` as created. | Edit only the changed facts. | none |
| TASK-P4-011 | Fresh-clone check | none | Clone the repository into a temp folder. Follow the README for the backend and run `npm ci`, `npm run build`, `npm test`. Fix any README gap. | The README commands run as written. | none |

Order: 001, 002, 003, 004, 005, 006, 007, 008, 009, 010, 011. Tasks 005 and 009 can start before 001 completes. Do not merge them until 004 passes.

### Schemas

No new API schema. The health response is `{"status":"ok"}` (Phase 1).

### Data Models & flows

<settings>

| Variable or key | Where | Value |
|---|---|---|
| `DATABASE_URL` | Provider secret | Provider connection string with `sslmode=require`. Never committed. |
| `PORT` | Provider sets it | Read by the app |
| `NODE_ENV` | Provider env | `production` |
| Android release `API_BASE_URL` | `app/build.gradle.kts` | `https://<host>/` |
| iOS Release `API_BASE_URL` | `project.pbxproj` | `https://<host>` |
</settings>

<Data-Models>

No change. The migration `1730500000000_create-tasks.sql` runs on the hosted database at the first start.
</Data-Models>

<Data-flow>

```text
git push ──▶ provider build (Dockerfile) ──▶ container start
   └─ npm run migrate up (reads DATABASE_URL, TLS) ──▶ node dist/server.js ──▶ /health 200 ──▶ traffic
Rollback: redeploy the previous image from the provider dashboard. Schema changes are forward-only.
```
</Data-flow>

---
#### **Process**

- Runtime Considerations: One instance. Pool max 5. Free tier sleeps (EDGE-P4-001).
- Deployment Impact: Deploy is manual or by Git push. No pipeline in scope (CON-401). Backups: none on a free database. State this in the README.

---

# Risks

| ID | Risk | Likelihood | Impact | Mitigation |
|----|------|-----------|--------|------------|
| RISK-401 | Free database expires after 30 days. | High (Render) | High | DES-P4-002. Owner picks a plan in TASK-P4-001. |
| RISK-402 | Anyone can read and change tasks. | High | Med | Accepted scope (ADR-007). README states it. Add an API key later if public. |
| RISK-403 | iOS signing team missing for a real device. | Med | Med | DES-P4-007 fallback. |
| RISK-404 | First request timeout after sleep. | High (free tier) | Low | TASK-P4-005 retry. README note. |
| RISK-405 | `DATABASE_URL` leaks in logs or the repository. | Low | High | Phase 1 SEC-004. VER-P4-005 scan. |
| RISK-406 | Release build still calls the placeholder URL. | Low | Med | VER-P4-006 grep. |

---

# Verification

| ID | Check | Command | Pass condition |
|----|-------|---------|----------------|
| VER-P4-001 | Health | `curl -i https://<host>/health` | `200` and `{"status":"ok"}` |
| VER-P4-002 | Migration | `curl -s https://<host>/api/summary` | All counts are `0` on a new database |
| VER-P4-003 | Write path | `curl -s -X POST https://<host>/api/tasks -H 'content-type: application/json' -d '{"title":"smoke","priority":"LOW","dueDate":"<today>"}'`, then `GET /api/tasks`, then `DELETE` | `201`, the list shows it, `204` |
| VER-P4-004 | Cross-device | Manual run from TASK-P4-008 | A task made on one app shows on the other |
| VER-P4-005 | No secret | `git ls-files \| grep -i '\.env$'` and `git grep -n "postgres://"` | No match (only `.env.example` with a placeholder host is allowed) |
| VER-P4-006 | No placeholder | `grep -rn "example.invalid" android ios --include=*.kts --include=*.pbxproj` | No match |
| VER-P4-007 | Cleartext blocked | Read the release merged manifest and the built Release `Info.plist` | Android release has no cleartext config. iOS has no ATS exception. |
| VER-P4-008 | App tests still pass | `./gradlew test` and the `xcodebuild test` command from `AGENTS.md` | Pass |
| VER-P4-009 | README | Fresh-clone check (TASK-P4-011) | Commands run as written |

Review step: a reviewer reads the README as a new developer and checks the Known limits list against RISK-401 to RISK-404.

### Requirement trace

| Requirement | Tasks | Verification |
|---|---|---|
| REQ-401, 402, 404 | 003, 004 | VER-P4-001, 002 |
| REQ-403, SEC-402 | 003 | VER-P4-005 |
| REQ-405, SEC-401 | 006, 007 | VER-P4-006, 007 |
| REQ-406 | 008 | VER-P4-004 |
| REQ-407 | 009, 011 | VER-P4-009 |
| SEC-403, GUD-401 | 001, 009 | Review step |

---

# Code Guidelines

- **Clean code**: change only the config values in the two app files. Keep the retry in the view model, not in the network layer.
- **Security**: keep `DATABASE_URL` in a provider secret. Run the VER-P4-005 scan before every push. Do not add the hosted URL to a place that also holds a secret.
- **Docs**: write the README in simple technical English (`AGENTS.md`). Short sentences. One instruction per step. No marketing words.
- **DevOps**: keep the deploy repeatable. If the provider supports a Blueprint file, commit it. Use conventional commit messages (`feat:`, `fix:`, `docs:`, `chore:`).
