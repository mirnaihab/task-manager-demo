# Task Manager: Product Requirements

## 1. Document Control

- **Version**: 1.0
- **Last Updated**: 2026-11-03
- **Status**: Approved scope. Phases 1 to 3 done. Phase 4 planned.
- **Sources**: `docs/user-stories.md`, `docs/HLD.md`

## 2. Overview

A single user creates, tracks and prioritizes tasks from a phone. One REST API stores the tasks in PostgreSQL. Two native apps call the API. The API and database must run online so the apps work on any device.

**Goals**
- G-001: A user manages tasks on Android and on iOS with the same behavior.
- G-002: The apps work on a device other than the developer machine.

**Non-goals**: authentication, multiple users, offline mode, push notifications, external integrations.

## 3. Feature Status

| ID | Feature | Backend | Android | iOS | Completed |
|---|---|---|---|---|---|
| US-01 | Create a task | Done | Done | Done | 2026-11-03 |
| US-02 | View and filter tasks | Done | Done | Done | 2026-11-03 |
| US-03 | Update task status | Done | Done | Done | 2026-11-03 |
| US-04 | Edit and delete a task | Done | Done | Done | 2026-11-03 |
| US-05 | Dashboard and overdue tasks | Done | Done | Done | 2026-11-03 |
| DEP | Hosted API and database | Planned | Planned | Planned | n/a |
| DOC | Root README with setup steps | Done (local setup) | n/a | n/a | 2026-11-03 |

"Done" means the phase build and test commands pass. The apps run against the local backend only. The hosted run is open.

## 4. Requirements

### Functional

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| FR-001 | Create a task with a title (1 to 100 characters), optional description, priority and a due date that is not in the past. The app shows a confirmation and lists the task. | Done | Backend API tests. Android and iOS UI tests. |
| FR-002 | List tasks with title, priority, status and due date. Filter by status and priority. Filters combine. Show an empty state. | Done | Same. |
| FR-003 | Change status between To Do, In Progress and Done. The change persists. | Done | Same. |
| FR-004 | Edit title, description, priority and due date with the create rules. | Done | Same. |
| FR-005 | Delete a task after a confirmation. Cancel leaves the task unchanged. | Done | Same. |
| FR-006 | Show counts for total, To Do, In Progress and Done. Show zero counts for an empty list. | Done | Same. |
| FR-007 | Mark a task overdue when the due date is before today and the status is not Done. Make it visually distinct. | Done | Same. |

### Non-functional

| ID | Requirement | Status |
|---|---|---|
| NFR-001 | Unit tests, API integration tests and basic UI tests pass. | Done. Backend 55, Android 41 unit and 15 UI, iOS 37 unit and 11 UI. |
| NFR-002 | The backend URL is configurable per environment. | Done. Release URL is a placeholder until phase 4. |
| NFR-003 | The apps reach the hosted backend over HTTPS. | Open (phase 4). |
| NFR-004 | The API and the database run online. | Open (phase 4, ADR-001). |
| NFR-005 | The README gives setup instructions for all three parts. | Done for local setup. Deploy steps open. |
| NFR-006 | The apps never connect to the database. | Done. |

## 5. Roadmap

| Phase | Title | Status | Plan |
|---|---|---|---|
| 1 | Backend API and database | Done 2026-11-02 | `operations/archives/2026-11-03-task-manager-phases-1-3/plan_phase_1.md` |
| 2 | Android app | Done 2026-11-02 | `operations/archives/2026-11-03-task-manager-phases-1-3/plan_phase_2.md` |
| 3 | iOS app | Done 2026-11-03 | `operations/archives/2026-11-03-task-manager-phases-1-3/plan_phase_3.md` |
| 4 | Hosting, release, docs | Planned. Blocked by [ADR-001](ADRs/ADR-001-hosting-provider.md). | `operations/specs/feature_task-manager/plan_phase_4.md` |

**Next priorities**
1. The owner decides the hosting provider (ADR-001).
2. Fix TD-001 (pool error listener) before the first hosted deploy.
3. Build the Docker image (TD-002), deploy, set the hosted URL in both apps.
4. Run the cross-device check and complete the deploy section of the README.

## 6. Requirement Changes and Assumptions

| ID | Item | Status |
|---|---|---|
| A-1 | Priority and due date are required on create. | Not confirmed by the owner (TD-008). |
| A-2 | "Today" is the server UTC date. | In use. See TD-011. |
| A-3 | The past-date check on edit applies only when the due date changes. | In use (ADR-005). |
| CH-1 | The iOS deployment target fell from 26.5 to 17.0. | Done. The minimum iOS version is 17.0. |

## 7. Risks

See `docs/HLD.md` section 11 and `docs/technical-debt.md`.
