# Task Manager

A single-user task manager. One Node.js API stores tasks in PostgreSQL. A native Android app and a native iOS app call the API.

## Status

| Part | Status |
|---|---|
| Backend API (`backend/`) | Done. Tested locally. |
| Android app (`android/`) | Done. Tested on the emulator against the local backend. |
| iOS app (`ios/`) | Done. Tested on the simulator. Manual run against the real API is partial. |
| Hosting and release | Planned. Waits for the hosting decision ([ADR-001](docs/ADRs/ADR-001-hosting-provider.md)). |

The release builds of both apps point at the placeholder `https://example.invalid`. They do not work on a device until the hosted URL is set.

## Architecture

```text
Android app (Compose) ─┐  HTTPS/JSON   ┌───────────────────────┐   SQL   ┌────────────┐
                       ├──────────────▶│ Backend (Node + TS)   │────────▶│ PostgreSQL │
iOS app (SwiftUI) ─────┘               │ routes → service → repo│         │ table:tasks│
                                       └───────────────────────┘         └────────────┘
```

The apps never connect to the database. The server computes `overdue` and the dashboard counts, so both apps behave the same. See [docs/HLD.md](docs/HLD.md).

## Prerequisites

- Node.js 22.12 or newer
- PostgreSQL 17
- Android Studio (Android) with an emulator
- Xcode with an iOS 17 or newer simulator (iOS)

## Local setup

### 1. Database

1. Run `createdb taskmanager`.
2. Run `createdb taskmanager_test`.

### 2. Backend

1. Run `cd backend && npm ci`.
2. Run `export DATABASE_URL=postgresql://localhost:5432/taskmanager`.
3. Run `npm run migrate up`.
4. Run `npm run dev`.
5. Run `curl localhost:3000/health`. The answer is `{"status":"ok"}`.

The server does not read `.env` files. Export the variables in your shell. `backend/.env.example` lists them.

### 3. Android

1. Set `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
2. Start the backend.
3. Run `cd android && ./gradlew assembleDebug`.
4. Install the app on an emulator. The debug build calls `http://10.0.2.2:3000/`.

### 4. iOS

1. Start the backend.
2. Open `ios/TaskManager.xcodeproj` in Xcode.
3. Select the `TaskManager` scheme and an iOS simulator.
4. Run the app. The debug build calls `http://localhost:3000`.

## Configuration

| Setting | Where | Value |
|---|---|---|
| `DATABASE_URL` | Backend environment. Required. | `postgresql://localhost:5432/taskmanager` |
| `PORT` | Backend environment. | `3000` (default) |
| `NODE_ENV` | Backend environment. | `development` (default) |
| `TEST_DATABASE_URL` | Backend tests. The database name must end in `_test`. | `postgresql://localhost:5432/taskmanager_test` (default) |
| `API_BASE_URL` (Android) | `buildConfigField` in `android/app/build.gradle.kts`. | Debug `http://10.0.2.2:3000/`. Release placeholder. |
| `API_BASE_URL` (iOS) | `ios/Config/Info-Debug.plist` and `Info-Release.plist`. | Debug `http://localhost:3000`. Release placeholder. |

Never commit a `.env` file or a real database URL.

## Commands

Set `JAVA_HOME` before you run Gradle (see Android setup).

| Part | Build | Lint | Test |
|---|---|---|---|
| backend | `npm run build` | `npm run lint` | `npm test` |
| android | `cd android && ./gradlew assembleDebug` | `./gradlew lint` | `./gradlew test`. UI tests: `./gradlew connectedDebugAndroidTest` (needs an emulator) |
| ios | `cd ios && xcodebuild build -scheme TaskManager -destination 'generic/platform=iOS Simulator'` | n/a | `xcodebuild test -scheme TaskManager -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -parallel-testing-enabled NO` |

Run the backend commands in `backend/`. The backend tests need the `taskmanager_test` database.

## API

| Method | Path | Purpose |
|---|---|---|
| GET | `/health` | Liveness and database check. |
| POST | `/api/tasks` | Create a task. Returns `201`. |
| GET | `/api/tasks?status=&priority=` | List tasks. Both filters are optional. |
| PATCH | `/api/tasks/:id` | Partial update. Used for edit and status change. |
| DELETE | `/api/tasks/:id` | Delete a task. Returns `204`. |
| GET | `/api/summary` | Counts: `total`, `todo`, `inProgress`, `done`, `overdue`. |

Statuses: `TODO`, `IN_PROGRESS`, `DONE`. Priorities: `LOW`, `MEDIUM`, `HIGH`. Dates use `YYYY-MM-DD`. Errors use `{ "error": { "code", "message", "fields?" } }`.

## Known limits

- The API has no authentication ([ADR-007](docs/ADRs/ADR-007-no-authentication.md)).
- "Today" is the server UTC date. A user near midnight in another time zone can see a one-day difference.
- The minimum iOS version is 17.0. The minimum Android version is API 26.
- The Dockerfile in `backend/` is not built or tested yet.
- Full list: [docs/technical-debt.md](docs/technical-debt.md).

## Documentation

| File | Content |
|---|---|
| [docs/user-stories.md](docs/user-stories.md) | Requirements and acceptance criteria. |
| [docs/PRD.md](docs/PRD.md) | Feature status and roadmap. |
| [docs/HLD.md](docs/HLD.md) | Architecture and design. |
| [docs/ADRs/](docs/ADRs/) | Architecture decisions. |
| [docs/technical-debt.md](docs/technical-debt.md) | Known issues. |
| [DESIGN.md](DESIGN.md) | UI rules for both apps. |
| [operations/archives/](operations/archives/) | Finished plans and phase summaries. |
| [operations/specs/](operations/specs/) | Open plans. |
