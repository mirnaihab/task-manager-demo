# Phase 3 Plan: iOS App

# Document Control
- **Version**: 1.0
- **Date Created**: 2026-11-02
- **Last Updated**: 2026-11-03
- **Status**: Done
- **Parent**: `docs/HLD.md` (sections 3, 7, 8, roadmap phase 3)
- **Design**: `DESIGN.md` (colors, type, components, screens). This plan does not repeat it.
- **Peer**: `plan_phase_2.md` (Android). Both apps must show the same behavior. Use the same requirement meaning and the same UI text.

---

# Executive Summary

- **Goal**: Build the native iOS task manager that calls the Phase 1 API.
- **Scope**: MVP, iOS app only.
- **Domains**: frontend (mobile).
- **Architecture**: Two groups in `ios/TaskManager`. `Data` holds `TaskAPI` (protocol), `URLSessionTaskAPI`, models and `APIError`. `Features` holds one `@Observable TaskListViewModel` and the SwiftUI views. `AppEnvironment` picks the real or the mock API at launch. No third-party packages.
- **Tech Stack**: Swift 5 mode (existing project setting), SwiftUI, `@Observable` (iOS 17), URLSession async/await, XCTest, XCUITest.
- **Entry Condition**: Phase 1 is complete. The Xcode project builds. Phase 2 is not required. Both phases only need the Phase 1 contract.
- **Exit Condition**: The `xcodebuild build` and `xcodebuild test` commands from `AGENTS.md` pass (unit and UI tests). A manual run on the simulator against the local backend passes all five stories.

> **Build note (2026-11-03)**: `INFOPLIST_KEY_API_BASE_URL` did not reach the built plist (RISK-301). The DES-P3-006 fallback applies: `ios/Config/Info-Debug.plist` and `ios/Config/Info-Release.plist` set `API_BASE_URL`. Color assets `Primary` and `Secondary` are named `AppPrimary` and `AppSecondary` because the names clash with SwiftUI symbols. Test commands use `-parallel-testing-enabled NO`.

---

# Requirements

## Functional Requirements

Requirements REQ-301 to REQ-314 match REQ-201 to REQ-214 in `plan_phase_2.md` one to one. The text is repeated here so this plan stands alone.

- **REQ-301**: WHEN the list screen opens THE SYSTEM SHALL load the task list and the summary from the API and show a loading indicator until both arrive.
- **REQ-302**: THE SYSTEM SHALL show for each task: title, priority, status and due date (US-02).
- **REQ-303**: WHEN the user selects a status chip or a priority chip THE SYSTEM SHALL reload the list with those filters. Both filters SHALL combine (US-02).
- **REQ-304**: IF no task matches THEN THE SYSTEM SHALL show the empty state text from `DESIGN.md`. WHEN filters are active it SHALL also show `Clear filters`.
- **REQ-305**: WHEN the user taps the add button THE SYSTEM SHALL open the form with an empty title, empty description, priority Medium and due date today.
- **REQ-306**: WHEN the user taps Save on a valid form THE SYSTEM SHALL send the request, close the form, show a toast `Task created` or `Task updated`, and refresh the list and summary (US-01, US-04).
- **REQ-307**: IF the API returns `VALIDATION_ERROR` THEN THE SYSTEM SHALL keep the form open and show each `fields.<name>` message under its field.
- **REQ-308**: THE SYSTEM SHALL check the title on the device before it sends: required after trim, at most 100 characters. The API remains the authority.
- **REQ-309**: THE SYSTEM SHALL disable past dates in the date picker (`in: today...`).
- **REQ-310**: WHEN the user picks a status in a row menu THE SYSTEM SHALL update the row at once, send `PATCH`, and refresh the summary. IF the request fails THEN the row SHALL revert and a toast SHALL show the error (US-03).
- **REQ-311**: WHEN the user taps the delete button THE SYSTEM SHALL show the confirmation dialog. Cancel SHALL change nothing. Delete SHALL call the API, remove the row and refresh the summary (US-04).
- **REQ-312**: THE SYSTEM SHALL show total, to do, in progress, done and overdue counts from `/api/summary`, with zeros when empty (US-05).
- **REQ-313**: THE SYSTEM SHALL mark each task with `overdue = true` using the red row fill, the warning icon and the text `Overdue` (US-05).
- **REQ-314**: IF a network call fails on load THEN THE SYSTEM SHALL show the error state with `Try again`. IF a mutation fails THEN it SHALL show a toast and keep the user's data.
- **REQ-315**: THE SYSTEM SHALL read the API base URL from the Info.plist key `API_BASE_URL`. Debug is `http://localhost:3000`. Release is the hosted HTTPS URL set in Phase 4. The Phase 3 placeholder for release is `https://example.invalid`.
- **REQ-316**: WHEN the app starts with the launch argument `-UITestMock` THE SYSTEM SHALL use `MockTaskAPI` and SHALL NOT use the network.
- **SEC-301**: THE SYSTEM SHALL allow plain HTTP only in the Debug configuration and only for local networking (`NSAllowsLocalNetworking`). Release SHALL keep the App Transport Security defaults.
- **SEC-302**: THE SYSTEM SHALL NOT log request or response bodies.
- **SEC-303**: THE SYSTEM SHALL use an ephemeral `URLSession` configuration (no disk cache, no cookies).
- **CON-301**: Lower `IPHONEOS_DEPLOYMENT_TARGET` from `26.5` to `17.0` for the app, unit test and UI test targets (risk R-3 in the HLD). `@Observable` needs iOS 17.
- **CON-302**: Keep bundle ID `com.demo.TaskManager`. Add no capability and no entitlement.
- **CON-303**: The app holds no business rules except the title check (REQ-308) and the optimistic status change. `overdue` and counts come from the server.
- **GUD-301**: Follow `DESIGN.md` tokens exactly. Define colors as named colors in `Assets.xcassets` with light and dark values.
- **PAT-301**: `TaskAPI` is a protocol. `URLSessionTaskAPI` and `MockTaskAPI` implement it. Unit tests use `MockTaskAPI`.

### Acceptance scenarios (Gherkin)

The scenarios are the same as in `plan_phase_2.md`, with "snackbar" read as "toast". The XCUITest suite covers:

```gherkin
Feature: iOS task manager (launch argument -UITestMock)
  Scenario: Create a task (US-01)
    Given the list is empty
    When the user taps add, types "Pay rent", taps Save
    Then the form closes, a toast shows "Task created" and the row "Pay rent" is visible
  Scenario: Title too long (US-01)
    When the user types 101 characters
    Then the field shows an error and Save is disabled
  Scenario: Filter and empty state (US-02)
    Given tasks (TODO,HIGH) and (DONE,LOW)
    When the user selects "To Do" and "High"
    Then only the (TODO,HIGH) row is visible
    When the user then selects "Low"
    Then "No tasks match these filters." and "Clear filters" are visible
  Scenario: Status change (US-03)
    When the user opens a row status menu and picks "Done"
    Then the row shows "Done" and the Done count rises by 1
  Scenario: Delete cancel and confirm (US-04)
    When the user taps delete then Cancel
    Then the row is still visible
    When the user taps delete then Delete
    Then the row is gone and the Total count falls by 1
  Scenario: Overdue and empty summary (US-05)
    Given a seeded overdue task
    Then the row shows the text "Overdue"
    Given no tasks
    Then the panel shows 0 for every count
```

Unit tests (XCTest, `MockTaskAPI`) cover server error mapping, optimistic revert, filter race, double save and 404 on edit, the same as Android.

---

# Dependencies

| ID | Name | Type | Version | Purpose |
|----|------|------|---------|---------|
| DEP-301 | Phase 1 backend | service | Phase 1 contract | API for manual runs |
| DEP-302 | Xcode | tool | 26.6 (installed) | Build and test |
| DEP-303 | iOS Simulator | tool | iPhone 17 Pro, iOS 26.5 (installed) | Tests and manual run |
| DEP-304 | Third-party packages | none | none | Use only Apple frameworks |

---

# Plan Details

## Design: How it should happen

### Impact on system design

```text
TaskManagerApp ── AppEnvironment.make(arguments) ──▶ TaskAPI
     │                                                  ▲   ▲
     ▼                                                  │   └── MockTaskAPI (-UITestMock, unit tests)
TaskListViewModel (@Observable, @MainActor)             └────── URLSessionTaskAPI ──▶ Backend /api
  │ state: tasks, summary, filters, phase, form, pendingDelete, toast
  ▼
TaskListView ── TaskRowView, SummaryPanelView, FilterChipsView
             └─ TaskFormView (sheet, fullScreenCover) ── DeleteConfirm (.confirmationDialog or alert)
```

### Decisions

| ID | Decision | Rationale | Alternatives Considered | Side effect (which modules?) |
|----|----------|-----------|------------------------|-------------------|
| DES-P3-001 | Lower the deployment target to 17.0 in `project.pbxproj` (6 build configurations). | The 26.5 target blocks most devices (HLD R-3). Phase 4 needs a real device. | Keep 26.5 | `project.pbxproj` |
| DES-P3-002 | Files go into the existing `TaskManager/` folder. The project uses a file-system synchronized group, so Xcode picks up new files with no `pbxproj` edit. | Fewer project edits. | Add files by hand in the project file | all new files |
| DES-P3-003 | Keep `SWIFT_DEFAULT_ACTOR_ISOLATION = MainActor` as the project sets it. Mark `Sendable` model structs `nonisolated` where the network layer uses them off the main actor. | Do not fight the template settings. | Change to nonisolated | Data |
| DES-P3-004 | `Task` uses `String` for `dueDate` (`yyyy-MM-dd`). A `DateFormatter` with `en_US_POSIX` and the current time zone converts to `Date` for the picker only. | Same format as the API. No time zone shift in data. | `Date` everywhere | Data, Features |
| DES-P3-005 | Name the model `TaskItem`, not `Task`. | `Task` clashes with Swift concurrency `Task`. | `Task` in a namespace | Data, Features |
| DES-P3-006 | Base URL comes from Info.plist key `API_BASE_URL`, set through the build setting `INFOPLIST_KEY_API_BASE_URL` per configuration. If Xcode does not write the custom key, use `INFOPLIST_FILE` with `Config/Info.plist` instead (fallback). | HLD section 7 config row. | Compile-time `#if DEBUG` constant | project settings, Data |
| DES-P3-007 | Debug-only ATS exception: an extra plist `ios/Config/Info-Debug.plist` with `NSAppTransportSecurity > NSAllowsLocalNetworking = true`, merged for the Debug configuration only (`INFOPLIST_FILE[config=Debug]`). Keep it outside the synced `TaskManager/` folder so it is not copied as a resource. | `INFOPLIST_KEY_` cannot express a dictionary. Release stays strict. | Allow arbitrary loads | project settings |
| DES-P3-008 | The mock API is chosen by `ProcessInfo.processInfo.arguments.contains("-UITestMock")`. The mock lives in the app target, not the test target. | XCUITest runs the app as a separate process. Only the app can swap its API. | Test-only target hook | App |
| DES-P3-009 | Delete uses `.confirmationDialog`-free `alert` with two buttons (Cancel, Delete destructive). | `DESIGN.md` dialog: title, sentence, two buttons. An alert fits it. | Swipe action only | Features |
| DES-P3-010 | The toast is a small overlay view with a 3 second task, driven by `message: String?`. | SwiftUI has no snackbar. | Third-party toast | Features |
| DES-P3-011 | After each mutation call `refresh()` for list and summary in parallel with `async let`. | Server is the source of truth. | Local count update | ViewModel |

### Constraints & Assumptions

| Type | ID | Description | Impact if Wrong | Handling Strategy |
|------|----|-------------|-----------------|-------------------|
| Constraint | CON-P3-001 | The test command needs an installed simulator name. Use `iPhone 17 Pro`. | Test command fails. | The name was checked with `simctl` on 2026-11-02. Re-check with `xcrun simctl list devices available`. |
| Constraint | CON-P3-002 | Lowering the target and adding build settings changes `project.pbxproj`. Phase 2 does not touch `ios/`, so the two phases do not conflict. | A bad edit breaks the project. | Edit the file with exact text replacement. Run the build right after. |
| Assumption | ASM-P3-001 | The owner accepts iOS 17.0 as the minimum. | Device may be older than 17. | Listed as a finding in the planning report. |
| Assumption | ASM-P3-002 | Generated Info.plist keeps arbitrary `INFOPLIST_KEY_*` keys. | `API_BASE_URL` is missing at run time. | Unit test reads the key from `Bundle.main`. Use the DES-P3-006 fallback. |
| Assumption | ASM-P3-003 | The simulator resolves `localhost` to the Mac. | Manual run fails. | It does. Use `http://localhost:3000`. |

### Edge cases

| ID | Scenario | Expected Behavior | Handling Strategy |
|----|----------|-------------------|-------------------|
| EDGE-P3-001 | Filter changes while a load runs | The newest filter wins | Store the load `Task`, cancel it before a new load. Check `Task.isCancelled` before you write state. |
| EDGE-P3-002 | User rotates the device or the scene changes | State stays | State lives in the `@Observable` model that the app owns with `@State` |
| EDGE-P3-003 | Double tap on Save | One request only | `isSaving` disables Save |
| EDGE-P3-004 | Edited task was deleted elsewhere (404) | Toast `Task no longer exists`, close form, refresh | Map `APIError.notFound` |
| EDGE-P3-005 | Description is only spaces | Sent as is. Server stores `null` | No client rule |
| EDGE-P3-006 | Long title | 2 lines with truncation | `.lineLimit(2)` |
| EDGE-P3-007 | Largest Dynamic Type size | No clipped text | Layout wraps. Chips scroll horizontally |
| EDGE-P3-008 | Status change under an active filter | Row stays until the next refresh, then drops | Refresh after the PATCH settles |
| EDGE-P3-009 | `URLError.cancelled` from a cancelled load | Ignored, no error state | Check for `CancellationError` and `URLError.cancelled` |
| EDGE-P3-010 | Server unreachable on save | Form stays open, toast shows the error | `APIError.network` |

### Directory Map

```text
ios/
├── Config/
│   └── Info-Debug.plist              (create: ATS NSAllowsLocalNetworking, Debug only)
├── TaskManager.xcodeproj/project.pbxproj   (modify: deployment target 17.0, API_BASE_URL, INFOPLIST_FILE[Debug])
├── TaskManager/                      (synchronized group: new files appear in the target)
│   ├── TaskManagerApp.swift          (modify: build AppEnvironment, own the view model)
│   ├── ContentView.swift             (delete after TaskListView replaces it)
│   ├── AppEnvironment.swift          (create: choose real or mock API)
│   ├── Assets.xcassets/              (modify: add named colors from DESIGN.md)
│   ├── Data/
│   │   ├── TaskModels.swift          (create: TaskItem, Priority, Status, Summary, TaskFilters, TaskDraft)
│   │   ├── TaskAPI.swift             (create: protocol + APIError)
│   │   ├── URLSessionTaskAPI.swift   (create)
│   │   └── MockTaskAPI.swift         (create: in-memory, used by UI tests and unit tests)
│   └── Features/
│       ├── Theme.swift               (create: spacing, radius, type, color names)
│       ├── TaskListViewModel.swift   (create)
│       ├── TaskListView.swift        (create)
│       ├── SummaryPanelView.swift    (create)
│       ├── FilterChipsView.swift     (create)
│       ├── TaskRowView.swift         (create)
│       ├── TaskFormView.swift        (create)
│       └── ToastView.swift           (create)
├── TaskManagerTests/
│   ├── TaskManagerTests.swift        (modify: delete the template tests, keep the file for config tests)
│   ├── TaskListViewModelTests.swift  (create)
│   └── URLSessionTaskAPITests.swift  (create: URLProtocol stub)
└── TaskManagerUITests/
    ├── TaskManagerUITests.swift      (modify: replace template with the scenarios)
    └── TaskManagerUITestsLaunchTests.swift  (keep as is)
```

### Directory Specifications

| ID | Task | File(s) | Responsibility or Expected Behaviors | Validations/Error handling | Key Configurations |
|----|------|---------|--------------------------------------|----------------------------|--------------------|
| TASK-P3-001 | Project settings | `project.pbxproj`, `Config/Info-Debug.plist` | Set `IPHONEOS_DEPLOYMENT_TARGET = 17.0` in every configuration. Add `INFOPLIST_KEY_API_BASE_URL`: Debug `http://localhost:3000`, Release `https://example.invalid`. Add `INFOPLIST_FILE[config=Debug] = Config/Info-Debug.plist` for the app target only. | Run `xcodebuild build` right after. It must pass. Confirm Release has no ATS exception: build Release, read the built `Info.plist` with `plutil -p`. | See DES-P3-006, 007 |
| TASK-P3-002 | Models | `Data/TaskModels.swift` | `enum Priority: String, Codable, CaseIterable`, `enum Status: ...`, each with `label` (`To Do`, `In Progress`, `Done`, `Low`, `Medium`, `High`). `struct TaskItem: Codable, Identifiable, Equatable, Sendable` (id, title, description, priority, status, dueDate, overdue). `Summary`, `TaskFilters`, `TaskDraft`. | Decoding an unknown enum value throws `DecodingError`. The API layer maps it to `APIError.server`. | none |
| TASK-P3-003 | API protocol and errors | `Data/TaskAPI.swift` | `protocol TaskAPI: Sendable` with `list(filters:)`, `create(_ draft:)`, `update(id:draft:)`, `setStatus(id:status:)`, `delete(id:)`, `summary()`, all `async throws`. `enum APIError: Error, Equatable` with cases `validation(fields: [String:String], message: String)`, `notFound`, `server`, `network`. | none | none |
| TASK-P3-004 | Real API client | `Data/URLSessionTaskAPI.swift` | Build requests from the base URL. Encode and decode JSON. Query items only for set filters. Map status codes: `200`, `201`, `204` ok. `400` decodes the error envelope to `validation`. `404` to `notFound`. Others to `server`. `URLError` to `network`. | Timeout 15 s. Never put the response body in an error message. Do not log bodies. | `URLSessionConfiguration.ephemeral` |
| TASK-P3-005 | Mock API | `Data/MockTaskAPI.swift` | Actor with an in-memory list. Mirrors server rules: new task is `TODO`, `overdue` from a fixed `today`, filters, summary. Test hooks: seed tasks, `failNext(APIError)`. Launch arguments `-UITestMock` (empty) and `-UITestSeed` (three seeded tasks, one overdue) pick the data. | none | Fixed `today = "2026-11-02"` |
| TASK-P3-006 | Environment and app | `AppEnvironment.swift`, `TaskManagerApp.swift` | `AppEnvironment.make(arguments:bundle:)` returns the API. `TaskManagerApp` holds the view model in `@State` and shows `TaskListView`. Delete `ContentView.swift`. | If `API_BASE_URL` is missing or not a valid URL, the app shows a fatal setup message in debug (`assertionFailure`) and an error state in release. | none |
| TASK-P3-007 | Theme and colors | `Features/Theme.swift`, `Assets.xcassets` | Add named colors `Primary`, `OnPrimary`, `Surface`, `SurfaceVariant`, `OnSurface`, `OnSurfaceVariant`, `Outline`, `OutlineStrong`, `Error`, `ErrorContainer`, `Success`, `Warning`, `Info` with light and dark values from `DESIGN.md`. Set `AccentColor` to Primary. `Theme` holds spacing (4, 8, 16, 24, 32) and radius values. | Use named colors only. No hex in views. | none |
| TASK-P3-008 | View model | `Features/TaskListViewModel.swift` | `@Observable @MainActor final class` with the same actions and rules as Android TASK-P2-008: load, retry, filters, clear filters, open create/edit, close form, save, change status (optimistic), ask/cancel/confirm delete, toast. | `save()` returns early if `isSaving`. Title check: trimmed length 1 to 100. Ignore `CancellationError` and `URLError.cancelled`. | Inject `TaskAPI` |
| TASK-P3-009 | List UI | `Features/TaskListView.swift`, `SummaryPanelView.swift`, `FilterChipsView.swift`, `TaskRowView.swift`, `ToastView.swift` | Build the list screen from `DESIGN.md` section 10: summary panel, two chip rows, `List` of rows (plain style, dividers), add button, empty state, error state, delete alert, toast overlay, pull to refresh. | `accessibilityIdentifier` on add button, rows, chips, status menu, delete button, summary counts. Row uses `.accessibilityElement(children: .combine)` with the label from `DESIGN.md` section 9. Overdue shows text and icon, not only color. | none |
| TASK-P3-010 | Form UI | `Features/TaskFormView.swift` | `fullScreenCover` form: Title with counter, Description (3 lines), Priority segmented `Picker`, Due date `DatePicker` limited to `in: today...` on create and on edit (the field shows an old date unchanged, the picker lower bound is `min(today, current due date)` on edit), Cancel and Save. Show `fields` errors under the field. Save shows a `ProgressView` while saving. | Edit of a past-due task keeps its date valid because the server skips the date check when the date is unchanged (Phase 1 REQ-009). | none |
| TASK-P3-011 | Unit tests | `TaskManagerTests/TaskListViewModelTests.swift`, `URLSessionTaskAPITests.swift`, `TaskManagerTests.swift` | View model tests use `MockTaskAPI`. Cover each unit scenario named in the Acceptance section. API tests use a `URLProtocol` stub: path and query for filters, `200`, `201`, `204`, `400` with fields, `404`, `500`, dropped connection to `network`. Config test reads `API_BASE_URL` from `Bundle.main`. Remove the template `testExample` and `testPerformanceExample`. | Name each test after its REQ. Tests do not use the real network. | XCTest, `@MainActor` where needed |
| TASK-P3-012 | UI tests | `TaskManagerUITests/TaskManagerUITests.swift` | One test per Gherkin scenario. Launch with `-UITestMock` or `-UITestMock -UITestSeed`. Find elements by `accessibilityIdentifier`. | Use `waitForExistence`. No `sleep`. | Device `iPhone 17 Pro` |
| TASK-P3-013 | Docs and commands | `AGENTS.md` | In the iOS row of the Commands table, replace `<installed iPhone>` with `iPhone 17 Pro` after the test command passes. | Edit only that value. | none |
| TASK-P3-014 | Verify | none | Run the Verification table. Run the manual check against the local backend. | Report any step you could not run. | none |

Order: 001, 002, 003, 005 (with 011 first if you follow TDD), 004, 006, 007, 008, 009, 010, 011, 012, 013, 014.

### Schemas

The API JSON schemas are in `plan_phase_1.md` (section "Schemas grouped by domain"). The Swift models mirror them.

```swift
// JSON keys match the API 1:1 (camelCase), so Codable needs no CodingKeys.
struct TaskItem: Codable, Identifiable, Equatable, Sendable {
    let id: String; let title: String; let description: String?
    let priority: Priority; let status: Status
    let dueDate: String; let overdue: Bool
}
struct TaskDraft: Sendable { var title = ""; var description = ""; var priority: Priority = .medium; var dueDate: String }
enum Priority: String, Codable, CaseIterable, Sendable { case low = "LOW", medium = "MEDIUM", high = "HIGH" }
enum Status: String, Codable, CaseIterable, Sendable { case todo = "TODO", inProgress = "IN_PROGRESS", done = "DONE" }
```

Ignore unknown JSON keys (the default for `JSONDecoder`). Send `description` as `""` to clear it, same as Android.

### Data Models & flows

<settings>

| Name | Where | Value |
|---|---|---|
| `API_BASE_URL` (Debug) | build setting `INFOPLIST_KEY_API_BASE_URL` | `http://localhost:3000` |
| `API_BASE_URL` (Release) | same | `https://example.invalid` until Phase 4 |
| ATS exception | `Config/Info-Debug.plist`, Debug only | `NSAllowsLocalNetworking = true` |
| Deployment target | all configurations | `17.0` |
</settings>

<Data-Models>

```text
TaskItem (domain)  id | title | description? | priority | status | dueDate "yyyy-MM-dd" | overdue
FormState          mode: create | edit(id) | draft: TaskDraft | errors: [String:String] | isSaving
```

The app has no local database and no stored data.
</Data-Models>

<Data-flow>

The flows are the same as in `plan_phase_2.md` (optimistic status change, save form). Differences:

```text
Selection of the API   launch args contain -UITestMock ? MockTaskAPI : URLSessionTaskAPI(baseURL from Info.plist)
Concurrency            ViewModel is @MainActor. API calls are async and run off the main actor.
                       refresh() uses `async let tasks = api.list(...)` and `async let summary = api.summary()`.
```
</Data-flow>

---
#### **Process**

- Runtime Considerations: No background work. Ephemeral session. The list reloads on filter change, after mutations and on pull to refresh.
- Deployment Impact: Phase 4 replaces the Release `API_BASE_URL` and sets the signing team for a real device.

---

# Risks

| ID | Risk | Likelihood | Impact | Mitigation |
|----|------|-----------|--------|------------|
| RISK-301 | The custom `INFOPLIST_KEY_API_BASE_URL` is not in the built plist. | Med | Med | Check with `plutil` after the first build. Use the DES-P3-006 fallback. |
| RISK-302 | The Debug-only plist merge does not apply. | Med | Med | Check the built Debug `Info.plist` for `NSAppTransportSecurity`. Check Release has none. |
| RISK-303 | Strict concurrency errors from `MainActor` default isolation. | Med | Low | DES-P3-003. Fix per error. Do not switch the setting off. |
| RISK-304 | XCUITest is slow or flaky on the simulator. | Med | Med | Use `waitForExistence`. Boot the simulator before the run. Report flakes. |
| RISK-305 | A user on iOS older than 17 cannot install. | Low | Med | State the minimum in the README (Phase 4). |
| RISK-306 | iOS and Android show different text. | Med | Low | Both plans use the strings in `DESIGN.md` section 10. A reviewer compares screenshots. |

---

# Verification

Run in `ios/`. Boot `iPhone 17 Pro` first for the UI tests.

| ID | Check | Command | Pass condition |
|----|-------|---------|----------------|
| VER-P3-001 | Build | `xcodebuild build -scheme TaskManager -destination 'generic/platform=iOS Simulator'` | `** BUILD SUCCEEDED **` |
| VER-P3-002 | Tests | `xcodebuild test -scheme TaskManager -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` | `** TEST SUCCEEDED **`. Unit and UI tests both ran. |
| VER-P3-003 | Release config | `xcodebuild build -scheme TaskManager -configuration Release -destination 'generic/platform=iOS Simulator'`, then `plutil -p` on the built `Info.plist` | Builds. `API_BASE_URL` is the HTTPS placeholder. No `NSAppTransportSecurity` key. |
| VER-P3-004 | Debug plist | `plutil -p` on the Debug built `Info.plist` | `API_BASE_URL` is `http://localhost:3000`. `NSAllowsLocalNetworking` is true. |
| VER-P3-005 | Deployment target | `grep -c "IPHONEOS_DEPLOYMENT_TARGET = 17.0" TaskManager.xcodeproj/project.pbxproj` | The count equals the number of configurations (6). No `26.5` remains. |
| VER-P3-006 | Manual run | Start the Phase 1 backend. Run the app in the simulator. Run US-01 to US-05 by hand. | Each story passes. A task created in the app shows in `curl http://localhost:3000/api/tasks`. |
| VER-P3-007 | Secrets | `git status --short` | No `xcuserdata` change staged. No URL with credentials in the diff. |

Documentation: one value in `AGENTS.md` (TASK-P3-013). The README is a Phase 4 task.

Review step: a reviewer checks each requirement against its test and compares screenshots with Android in light and dark mode (RISK-306).

### Requirement trace

| Requirement | Tasks | Tests |
|---|---|---|
| REQ-301, 303, 304 | 008, 009 | ViewModelTests, UITests (filter, empty state) |
| REQ-302, 313 | 009 | UITests (overdue) |
| REQ-305 to 309 | 008, 010 | ViewModelTests, UITests (create, long title) |
| REQ-310 | 008, 009 | ViewModelTests, UITests (status change) |
| REQ-311 | 008, 009 | UITests (delete cancel, confirm) |
| REQ-312 | 009 | UITests (empty summary) |
| REQ-314 | 004, 008, 009 | APITests, ViewModelTests |
| REQ-315, 316, SEC-301 to 303, CON-301 | 001, 005, 006 | VER-P3-003 to 005, config test |

---

# Code Guidelines

**Clean code**
- One view per file when it has its own state. Small helpers may share a file.
- Views take state and closures. They do not call the API.
- Business logic lives in the view model. Views map state to widgets.
- Define the `TaskAPI` protocol before the implementations (GEN-03). Inject through the initializer.
- Read `Bundle.main` only in `AppEnvironment` (DES-01).
- No commented-out code. Remove template comments from files you change.
- Never swallow an error. Map it to `APIError` or rethrow. Ignore only cancellation, with a comment that states why.
- UI text lives in one place per screen. Use the exact strings from `DESIGN.md`.

**Frontend patterns (applied to SwiftUI)**
- Keep state in one `@Observable` model. Use `@Bindable` for form fields.
- Validate external data: `Codable` decoding is the schema check. A decode failure maps to `APIError.server`.
- Build for all states: loading, content, empty, error, saving, disabled.
- Accessibility: identifiers for tests, labels for icon buttons, combined row element, text plus icon for overdue, Dynamic Type support.
- Use the 8-point spacing tokens from `Theme`.

**Security**
- ATS defaults in Release. Local networking exception in Debug only.
- No logging of bodies. No `print` of URLs with credentials.
- No secrets in Info.plist. The API has no key.
- Ephemeral `URLSession`.
