# Phase 2 Plan: Android App

# Document Control
- **Version**: 1.0
- **Date Created**: 2026-11-02
- **Last Updated**: 2026-11-02
- **Status**: Done (2026-11-02). Library versions differ from the table: see Implementation notes.
- **Parent**: `docs/HLD.md` (sections 3, 7, 8, roadmap phase 2)
- **Design**: `DESIGN.md` (colors, type, components, screens). This plan does not repeat it.

---

# Executive Summary

- **Goal**: Build the native Android task manager that calls the Phase 1 API.
- **Scope**: MVP, Android app only.
- **Domains**: frontend (mobile).
- **Architecture**: Two layers in package `com.demo.taskmanager`. `data` holds the Retrofit API, DTOs and `TaskRepository`. `ui` holds one `TaskListViewModel` with `StateFlow` and Compose screens. Manual wiring in an `AppContainer`. No navigation library: `MainActivity` shows the list or the form from one UI state.
- **Tech Stack**: Kotlin 2.2, Jetpack Compose, Material 3, Retrofit 3, OkHttp 5, kotlinx.serialization, ViewModel + StateFlow, JUnit 4, kotlinx-coroutines-test, Compose UI test.
- **Entry Condition**: Phase 1 is complete and its API runs on `http://localhost:3000`. The template app in `android/` builds.
- **Exit Condition**: `./gradlew assembleDebug`, `./gradlew lint` and `./gradlew test` pass. The Compose UI tests pass on the `Pixel_8` emulator. A manual run on the emulator against the local backend passes all five stories.

### Dependency versions (checked on 2026-11-02)

| Library | Version | Catalog key |
|---|---|---|
| retrofit | 3.0.0 | `retrofit` |
| retrofit converter-kotlinx-serialization | 3.0.0 | `retrofit-kotlinx-serialization` |
| okhttp | 5.5.1 | `okhttp` |
| okhttp mockwebserver | 5.5.1 | `okhttp-mockwebserver` (test) |
| kotlinx-serialization-json | 1.12.0 | `kotlinx-serialization-json` |
| kotlinx-coroutines-android and -test | 1.11.0 | `kotlinx-coroutines-*` |
| lifecycle-viewmodel-compose, lifecycle-runtime-compose | 2.10.0 | `androidx-lifecycle-*` |
| Kotlin serialization Gradle plugin | same as Kotlin (2.2.10) | `kotlin-serialization` |

Also raise `lifecycleRuntimeKtx` from `2.6.1` to `2.10.0` so all lifecycle libraries match. If Gradle reports a `compileSdk` or AGP conflict, step back one minor version of the lifecycle libraries and record it in the commit message.

---

# Requirements

## Functional Requirements

- **REQ-201**: WHEN the list screen opens THE SYSTEM SHALL load the task list and the summary from the API and show a loading indicator until both arrive.
- **REQ-202**: THE SYSTEM SHALL show for each task: title, priority, status and due date (US-02).
- **REQ-203**: WHEN the user selects a status chip or a priority chip THE SYSTEM SHALL reload the list with those filters. Both filters SHALL combine (US-02).
- **REQ-204**: IF no task matches THEN THE SYSTEM SHALL show the empty state text from `DESIGN.md`. WHEN filters are active it SHALL also show `Clear filters`.
- **REQ-205**: WHEN the user taps the add button THE SYSTEM SHALL open the form with an empty title, empty description, priority Medium and due date today.
- **REQ-206**: WHEN the user taps Save on a valid form THE SYSTEM SHALL send the request, close the form, show a snackbar `Task created` or `Task updated`, and refresh the list and summary (US-01, US-04).
- **REQ-207**: IF the API returns `VALIDATION_ERROR` THEN THE SYSTEM SHALL keep the form open and show each `fields.<name>` message under its field.
- **REQ-208**: THE SYSTEM SHALL check the title on the device before it sends: required after trim, at most 100 characters. The device check is a convenience. The API remains the authority.
- **REQ-209**: THE SYSTEM SHALL disable past dates in the date picker.
- **REQ-210**: WHEN the user picks a status in a row menu THE SYSTEM SHALL update the row at once, send `PATCH`, and refresh the summary. IF the request fails THEN the row SHALL revert and a snackbar SHALL show the error (US-03).
- **REQ-211**: WHEN the user taps the delete icon THE SYSTEM SHALL show the confirmation dialog. Cancel SHALL change nothing. Delete SHALL call the API, remove the row and refresh the summary (US-04).
- **REQ-212**: THE SYSTEM SHALL show total, to do, in progress, done and overdue counts from `/api/summary`, with zeros when empty (US-05).
- **REQ-213**: THE SYSTEM SHALL mark each task with `overdue = true` using the red row fill, the warning icon and the text `Overdue` (US-05).
- **REQ-214**: IF a network call fails on load THEN THE SYSTEM SHALL show the error state with `Try again`. IF a mutation fails THEN it SHALL show a snackbar and keep the user's data.
- **REQ-215**: THE SYSTEM SHALL read the API base URL from `BuildConfig.API_BASE_URL`. Debug uses `http://10.0.2.2:3000/`. Release uses the hosted HTTPS URL set in Phase 4. The Phase 2 placeholder for release is `https://example.invalid/` so a release build never calls the local server.
- **SEC-201**: THE SYSTEM SHALL allow cleartext HTTP only in the debug build and only for host `10.0.2.2`, with a debug-only network security config. Release builds SHALL allow HTTPS only.
- **SEC-202**: THE SYSTEM SHALL NOT log request or response bodies. Add the OkHttp logging interceptor to no build type.
- **SEC-203**: THE SYSTEM SHALL set `android:allowBackup="false"` because the app stores no local data.
- **CON-201**: Keep package `com.demo.taskmanager`, `minSdk 26`. The only permission is `INTERNET`.
- **CON-202**: The app holds no business rules except the title check (REQ-208) and the optimistic status change. `overdue` and counts come from the server.
- **GUD-201**: Follow `DESIGN.md` tokens exactly. Do not call `dynamicLightColorScheme`.
- **PAT-201**: `TaskRepository` is an interface. `ApiTaskRepository` implements it. Tests use `FakeTaskRepository`.

### Acceptance scenarios (Gherkin)

```gherkin
Feature: Android task manager
  Scenario: Create a task (US-01)
    Given the list is empty
    When the user taps add, types "Pay rent", keeps Medium, keeps today, taps Save
    Then the form closes, the snackbar shows "Task created" and the row "Pay rent" is visible
  Scenario: Title too long (US-01)
    When the user types 101 characters
    Then the field shows an error and Save does not send a request
  Scenario: Server error on form (US-01)
    Given the API returns fields.dueDate
    Then the message shows under the Due date field and the form stays open
  Scenario: Filter (US-02)
    Given tasks (TODO,HIGH) and (DONE,LOW)
    When the user selects Status "To Do" and Priority "High"
    Then only the (TODO,HIGH) row is visible
  Scenario: Empty state (US-02)
    When the user selects a filter pair with no match
    Then "No tasks match these filters." and "Clear filters" are visible
  Scenario: Status change (US-03)
    When the user opens a row status menu and picks "Done"
    Then the row shows "Done" and the Done count rises by 1
  Scenario: Status change fails (US-03)
    Given the PATCH call fails
    Then the row shows the old status and a snackbar shows an error
  Scenario: Edit (US-04)
    When the user taps a row, changes the title and saves
    Then the row shows the new title and the snackbar shows "Task updated"
  Scenario: Delete cancel (US-04)
    When the user taps delete then Cancel
    Then the row is still visible
  Scenario: Delete confirm (US-04)
    When the user taps delete then Delete
    Then the row is gone and the Total count falls by 1
  Scenario: Overdue (US-05)
    Given a task with overdue true
    Then the row shows the text "Overdue"
  Scenario: Empty summary (US-05)
    Given no tasks
    Then the panel shows 0 for every count and no error
```

---

# Dependencies

| ID | Name | Type | Version | Purpose |
|----|------|------|---------|---------|
| DEP-201 | Phase 1 backend | service | Phase 1 contract | API for manual runs |
| DEP-202 | Android Studio JBR | tool | bundled | `JAVA_HOME` for Gradle (see `AGENTS.md`) |
| DEP-203 | `Pixel_8` AVD | tool | API 37.1 image | Compose UI tests and manual run |
| DEP-204 | Libraries | Gradle | see table above | Network, JSON, state |

---

# Plan Details

## Design: How it should happen

### Impact on system design

```text
MainActivity ── creates ──▶ AppContainer(repository)
     │                              │
     ▼                              ▼
TaskListViewModel ◀── TaskRepository (interface)
  │  StateFlow<UiState>                 ▲
  ▼                                     │ implements
TaskApp (Compose) ─ list screen ─       ApiTaskRepository ── Retrofit ──▶ Backend /api
                  └ form screen ─       (maps DTO <-> domain, ApiError)
```

```text
UiState
 ├─ filters: TaskFilters(status?, priority?)
 ├─ tasks: List<Task>        ├─ summary: Summary
 ├─ phase: Loading | Content | Error(message)
 ├─ form: FormState? (null = list visible)
 ├─ pendingDelete: Task?     └─ message: String? (one-shot snackbar text)
```

### Decisions

| ID | Decision | Rationale | Alternatives Considered | Side effect (which modules?) |
|----|----------|-----------|------------------------|-------------------|
| DES-P2-001 | One `TaskListViewModel` for list, form and delete dialog. | HLD section 7: one state holder. Two screens do not need a navigation library. | Navigation Compose, a second ViewModel | ui |
| DES-P2-002 | `ViewModelProvider.Factory` gets the repository from `AppContainer`. | No DI framework (HLD section 2). | Hilt, Koin | `MainActivity`, ui |
| DES-P2-003 | Domain `Task` uses `String` for `dueDate` (`yyyy-MM-dd`) and enums for priority and status. Convert to `LocalDate` only in the date picker. | Same format as the API. No time zone bugs. `minSdk 26` has `java.time`. | `LocalDate` everywhere | data, ui |
| DES-P2-004 | Retrofit uses `suspend` functions. Errors map to `ApiError(code, message, fields)` in the repository. Network failures map to `ApiError.Network`. | The ViewModel handles one error type. | `Result` from Retrofit | data |
| DES-P2-005 | Use `Json { ignoreUnknownKeys = true }`. | A new server field does not break an old app. | strict | data |
| DES-P2-006 | The Compose UI tests use `FakeTaskRepository` through the same ViewModel factory. They never use the network. | HLD section 8. Tests are fast and stable. | MockWebServer in UI tests | androidTest |
| DES-P2-007 | The base URL is a `buildConfigField` per build type. | HLD section 7 config row. | Gradle property | `app/build.gradle.kts` |
| DES-P2-008 | Date picker uses `SelectableDates` with `isSelectableDate` checking the UTC day against today. Convert millis with `Instant.ofEpochMilli(m).atZone(UTC).toLocalDate()`. | Material 3 returns UTC midnight. A local zone conversion shifts the date. | Custom picker | ui |
| DES-P2-009 | After each mutation call `refresh()` for both list and summary in parallel with `async`. | Server is the source of truth for overdue and counts. | Update counts locally | ViewModel |

### Constraints & Assumptions

| Type | ID | Description | Impact if Wrong | Handling Strategy |
|------|----|-------------|-----------------|-------------------|
| Constraint | CON-P2-001 | `./gradlew test` runs JVM unit tests only. Compose UI tests need an emulator (`connectedDebugAndroidTest`). | A reviewer may think UI tests ran. | VER-P2-004 lists the emulator command. Add the command to `AGENTS.md` in TASK-P2-016. |
| Constraint | CON-P2-002 | Run Gradle with `JAVA_HOME` set as in `AGENTS.md`. | Build fails. | Every command below sets it. |
| Assumption | ASM-P2-001 | Lifecycle 2.10.0 works with AGP 9.3.3 and `compileSdk 36`. | Build error. | Step back one version (see versions note). |
| Assumption | ASM-P2-002 | Phase 1 API matches the HLD contract. | Parse errors. | Do a manual run before you finish. |
| Assumption | ASM-P2-003 | The `Pixel_8` AVD can boot on this machine. | UI tests cannot run. | Report the blocker. Do not claim the UI tests pass. |

### Edge cases

| ID | Scenario | Expected Behavior | Handling Strategy |
|----|----------|-------------------|-------------------|
| EDGE-P2-001 | Filter changes while a load runs | The newest filter wins | Cancel the previous load job before a new one starts |
| EDGE-P2-002 | User rotates the device | Filters, list and open form stay | State lives in the ViewModel. Form text uses the ViewModel state, not `remember` |
| EDGE-P2-003 | Double tap on Save | One request only | `isSaving` flag disables Save |
| EDGE-P2-004 | Edited task was deleted elsewhere (404) | Snackbar `Task no longer exists`, close form, refresh | Map `NOT_FOUND` in the ViewModel |
| EDGE-P2-005 | Description is only spaces | Sent as is. Server stores `null` | No client rule |
| EDGE-P2-006 | Long title | Row shows 2 lines with ellipsis | `maxLines = 2` |
| EDGE-P2-007 | Font scale 200 percent | No clipped text | Layout wraps. Chips scroll horizontally |
| EDGE-P2-008 | Status change on a task that a filter hides afterward | The row stays until the next refresh, then drops | Refresh after the PATCH settles |
| EDGE-P2-009 | Server unreachable during an edit save | Form stays open with a snackbar | `ApiError.Network` |

### Directory Map

```text
android/
├── gradle/libs.versions.toml                 (modify: add versions, libraries, serialization plugin)
├── build.gradle.kts                          (modify: declare serialization plugin, apply false)
└── app/
    ├── build.gradle.kts                      (modify: plugin, buildConfig, deps, test deps)
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml           (modify: INTERNET, networkSecurityConfig, allowBackup false)
        │   ├── res/values/strings.xml        (modify: UI strings)
        │   └── java/com/demo/taskmanager/
        │       ├── MainActivity.kt           (modify: host TaskApp)
        │       ├── AppContainer.kt           (create)
        │       ├── data/
        │       │   ├── Task.kt               (create: domain types)
        │       │   ├── TaskApi.kt            (create: Retrofit + DTOs)
        │       │   ├── TaskRepository.kt     (create: interface + ApiError)
        │       │   └── ApiTaskRepository.kt  (create)
        │       └── ui/
        │           ├── theme/Color.kt, Theme.kt, Type.kt   (modify: DESIGN.md tokens)
        │           ├── TaskListViewModel.kt  (create: state + actions)
        │           ├── UiState.kt            (create)
        │           ├── TaskApp.kt            (create: list/form switch + snackbar host)
        │           ├── TaskListScreen.kt     (create)
        │           ├── SummaryPanel.kt       (create)
        │           ├── FilterChips.kt        (create)
        │           ├── TaskRow.kt            (create)
        │           ├── TaskFormScreen.kt     (create)
        │           └── DeleteDialog.kt       (create)
        ├── debug/res/xml/network_security_config.xml   (create: cleartext for 10.0.2.2 only)
        ├── release/res/xml/network_security_config.xml (create: no cleartext)
        ├── sharedTest/java/com/demo/taskmanager/
        │   └── FakeTaskRepository.kt         (create: used by test and androidTest)
        ├── test/java/com/demo/taskmanager/
        │   ├── TaskListViewModelTest.kt      (create)
        │   └── ApiTaskRepositoryTest.kt      (create, MockWebServer)
        └── androidTest/java/com/demo/taskmanager/
            └── TaskAppTest.kt                (create: Compose UI tests)
```

Share the fake: put `FakeTaskRepository.kt` in `src/sharedTest/java/` and add that directory to both the `test` and `androidTest` source sets in `app/build.gradle.kts` (TASK-P2-012). Do not point `androidTest` at `src/test/java`, because that would also pull in the unit tests.

### Directory Specifications

| ID | Task | File(s) | Responsibility or Expected Behaviors | Validations/Error handling | Key Configurations |
|----|------|---------|--------------------------------------|----------------------------|--------------------|
| TASK-P2-001 | Add dependencies and plugin | `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts` | Add every library in the version table. Add the `kotlin-serialization` plugin. Enable `buildFeatures { buildConfig = true }`. | Run `./gradlew assembleDebug` after the change. It must pass before the next task. | Debug `API_BASE_URL="http://10.0.2.2:3000/"`. Release `API_BASE_URL="https://example.invalid/"` |
| TASK-P2-002 | Network config and manifest | `AndroidManifest.xml`, `src/debug/res/xml/network_security_config.xml`, `src/release/res/xml/network_security_config.xml` | Add `INTERNET`. Set `android:networkSecurityConfig="@xml/network_security_config"` and `allowBackup="false"`. Debug file allows cleartext for domain `10.0.2.2` only. Release file has `cleartextTrafficPermitted="false"`. | Lint must not flag cleartext in release. | none |
| TASK-P2-003 | Domain types | `data/Task.kt` | `enum Priority {LOW, MEDIUM, HIGH}`, `enum Status {TODO, IN_PROGRESS, DONE}`, `data class Task(id, title, description, priority, status, dueDate: String, overdue)`, `Summary(total, todo, inProgress, done, overdue)`, `TaskFilters(status, priority)`, `TaskDraft(title, description, priority, dueDate)`. Each enum has a `label` for display (`To Do`, `In Progress`, `Done`, `Low`, `Medium`, `High`). | none | none |
| TASK-P2-004 | API interface and DTOs | `data/TaskApi.kt` | Retrofit interface: `listTasks(status?, priority?)`, `createTask(body)`, `updateTask(id, body)`, `deleteTask(id)`, `summary()`. `@Serializable` DTOs match the Phase 1 schemas. `ErrorEnvelope(error: ErrorBody)`. | Update body DTO uses explicit-null for `description` only when the user clears it (`encodeDefaults = false`, use a nullable wrapper). | Paths `api/tasks`, `api/summary` |
| TASK-P2-005 | Repository | `data/TaskRepository.kt`, `data/ApiTaskRepository.kt` | Interface with `list(filters)`, `create(draft)`, `update(id, draft)`, `setStatus(id, status)`, `delete(id)`, `summary()`. `ApiTaskRepository` maps DTOs to domain and throws `ApiError`. `sealed class ApiError(Validation(fields), NotFound, Server, Network)`. | Parse the error body for `4xx`. `IOException` maps to `Network`. Never expose the response body to the UI beyond `message` and `fields`. | OkHttp timeouts: connect 10 s, read 15 s |
| TASK-P2-006 | Container and activity | `AppContainer.kt`, `MainActivity.kt` | `AppContainer` builds OkHttp, Retrofit, repository from `BuildConfig.API_BASE_URL`. `MainActivity` builds the ViewModel through a factory and calls `setContent { TaskManagerTheme { TaskApp(viewModel) } }`. Remove the `Greeting` composable. | none | none |
| TASK-P2-007 | Theme | `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`, `res/values/themes.xml` | Replace the purple tokens with the `DESIGN.md` light and dark tokens. Add custom colors for `outlineStrong`, `success`, `warning`, `info`, `errorContainer` through `CompositionLocal` or extended `ColorScheme` fields. Set `dynamicColor = false`. Type scale from `DESIGN.md`. | Check contrast pairs listed in `DESIGN.md` are used as written. | none |
| TASK-P2-008 | State and ViewModel | `ui/UiState.kt`, `ui/TaskListViewModel.kt` | Actions: `load()`, `retry()`, `setStatusFilter`, `setPriorityFilter`, `clearFilters`, `openCreate`, `openEdit(task)`, `closeForm`, `updateDraft`, `save()`, `changeStatus(task, status)`, `askDelete(task)`, `cancelDelete`, `confirmDelete()`, `consumeMessage()`. Rules for REQ-203, 206 to 208, 210, 211, 214 and EDGE-P2-001 to 004. | `save()` returns early if `isSaving`. Title check: trimmed length 1..100. Catch `ApiError` only. Do not catch `CancellationException`. | `viewModelScope`, injected `CoroutineDispatcher` for tests |
| TASK-P2-009 | List UI | `ui/TaskApp.kt`, `TaskListScreen.kt`, `SummaryPanel.kt`, `FilterChips.kt`, `TaskRow.kt`, `DeleteDialog.kt` | Build the list screen from `DESIGN.md` section 10: summary panel, two chip rows, `LazyColumn` of rows, FAB, empty state, error state, delete dialog, `SnackbarHost`. `TaskApp` shows the form when `form != null`. | Every control has a `testTag` or text for tests. Each icon button has a content description. Row has merged semantics (DESIGN.md section 9). | none |
| TASK-P2-010 | Form UI | `ui/TaskFormScreen.kt` | Title (counter `n/100`), Description, Priority segmented buttons, Due date field with `DatePickerDialog`, Cancel and Save. Show `fields` errors. Save shows a spinner while `isSaving`. | `SelectableDates` blocks past days (DES-P2-008). Field errors use the `error` token. | none |
| TASK-P2-011 | Strings | `res/values/strings.xml` | Move all UI text into string resources. Use the exact texts from `DESIGN.md` section 10. Remove em dashes. | none | none |
| TASK-P2-012 | Fake repository | `src/sharedTest/java/.../FakeTaskRepository.kt`, `app/build.gradle.kts` | In-memory list. Mirrors server rules: new task is `TODO`, `overdue` from a fixed `today`, filters, summary counts. Supports `failNext(ApiError)` for error tests. Register `sharedTest` for `test` and `androidTest`. | none | Fixed `today = "2026-11-02"` |
| TASK-P2-013 | ViewModel unit tests | `src/test/.../TaskListViewModelTest.kt` | Cover every scenario in the Gherkin list that does not need a screen. Use `StandardTestDispatcher` and `runTest`. Name each test after its REQ. | Assert the revert on failed status change and the `isSaving` guard. | `kotlinx-coroutines-test` |
| TASK-P2-014 | Repository unit tests | `src/test/.../ApiTaskRepositoryTest.kt` | Use MockWebServer. Assert the request path and query for filters, the mapping of `200`, `201`, `204`, `400` with `fields`, `404`, `500`, and a dropped connection to `ApiError.Network`. | Assert no body leaks into the `Server` error message. | none |
| TASK-P2-015 | Compose UI tests | `src/androidTest/.../TaskAppTest.kt` | One test per Gherkin scenario with a screen: create, title too long, server field error, filter, empty state, status change, edit, delete cancel, delete confirm, overdue label, empty summary. Use `createComposeRule`, `FakeTaskRepository`. | Tests do not use the network or `Thread.sleep`. Use `waitUntil` or idling. | `Pixel_8` AVD |
| TASK-P2-016 | Docs and commands | `AGENTS.md` | In the Commands table, add the row for UI tests: `./gradlew connectedDebugAndroidTest` (needs a running emulator). | Edit only that row. | none |
| TASK-P2-017 | Verify | none | Run the Verification table. Run the manual check against the local backend. | Report any step you could not run. | none |

Order: 001, 002, 003, 004, 005 (with 012 and 014 first if you follow TDD), 006, 007, 008 (with 013), 009, 010, 011, 015, 016, 017.

### Schemas

The API JSON schemas are in `plan_phase_1.md` (section "Schemas grouped by domain"). The Android DTOs mirror them.

```kotlin
// Update body: only send fields the user changed (edit form sends all four, status change sends one)
@Serializable data class UpdateTaskBody(
    val title: String? = null,
    val description: String? = null,   // send "" to clear: the server stores null
    val priority: Priority? = null,
    val status: Status? = null,
    val dueDate: String? = null,
)
// Json { ignoreUnknownKeys = true; explicitNulls = false }
```

Because the server turns an empty `description` into `null`, the app sends `""` to clear it and never needs a JSON null. `explicitNulls = false` keeps unset fields out of the body.

### Data Models & flows

<settings>

| Name | Where | Value |
|---|---|---|
| `API_BASE_URL` (debug) | `app/build.gradle.kts` `buildConfigField` | `http://10.0.2.2:3000/` |
| `API_BASE_URL` (release) | same | `https://example.invalid/` until Phase 4 |
| Cleartext | debug network security config | `10.0.2.2` only |
</settings>

<Data-Models>

```text
Task (domain)   id: String(uuid) | title | description: String? | priority | status
                dueDate: "yyyy-MM-dd" | overdue: Boolean
FormState       mode: Create | Edit(id) | draft: TaskDraft | errors: Map<String,String> | isSaving
```

The app has no local database and no stored data.
</Data-Models>

<Data-flow>

```text
Status change (optimistic)
 UI pick "Done" ─▶ VM.changeStatus
   1 replace row in uiState.tasks with status DONE        (instant)
   2 repo.setStatus(id, DONE)
        ok   ─▶ 3 refresh list + summary (parallel)
        fail ─▶ restore previous row, message = error text
```

```text
Save form
 Save ─▶ VM.save: title check ─fail─▶ errors[title]
                   │ ok: isSaving=true
                   ▼
              repo.create/update
   ok ─▶ form=null, message="Task created|updated", refresh
   Validation(fields) ─▶ errors = fields, isSaving=false
   Network/Server    ─▶ message = text, form stays
```
</Data-flow>

---
#### **Process**

- Runtime Considerations: No background work. OkHttp handles connection reuse. The list reloads on filter change and after mutations only.
- Deployment Impact: Phase 4 replaces the release `API_BASE_URL`. No signing or store work in this phase.

---

# Risks

| ID | Risk | Likelihood | Impact | Mitigation |
|----|------|-----------|--------|------------|
| RISK-201 | A library version does not work with AGP 9.3.3. | Med | Med | Build after each dependency change (TASK-P2-001). Step back one version. |
| RISK-202 | The emulator does not boot in this environment. | Med | Med | Report it. Unit tests and `assembleDebug` still run. Do not claim UI tests passed. |
| RISK-203 | Date picker returns a wrong day near midnight. | Med | Med | DES-P2-008. Unit test the conversion helper with `2026-11-02T00:00:00Z`. |
| RISK-204 | Cleartext config leaks into release. | Low | High | Separate `debug` and `release` resource folders. Lint check in VER-P2-002. |
| RISK-205 | Material 3 API names differ in the BOM `2026.02.01`. | Med | Low | Read the compile error. Use the equivalent API. Keep the behavior in this plan. |

---

# Verification

Set `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` and run in `android/`.

| ID | Check | Command | Pass condition |
|----|-------|---------|----------------|
| VER-P2-001 | Build | `./gradlew assembleDebug` | `BUILD SUCCESSFUL` |
| VER-P2-002 | Lint | `./gradlew lint` | 0 errors. No cleartext warning for release. |
| VER-P2-003 | Unit tests | `./gradlew test` | All pass. Count of tests is at least the number of VM and repository scenarios. |
| VER-P2-004 | UI tests | Boot `Pixel_8`, then `./gradlew connectedDebugAndroidTest` | All pass |
| VER-P2-005 | Release build | `./gradlew assembleRelease` | `BUILD SUCCESSFUL`. `BuildConfig.API_BASE_URL` in the release output is the HTTPS placeholder. |
| VER-P2-006 | Manual run | Start the Phase 1 backend. Install the debug app on the emulator. Run US-01 to US-05 by hand. | Each story passes. Create on the emulator shows in `curl http://localhost:3000/api/tasks`. |
| VER-P2-007 | Secrets | `git status --short` | No `local.properties` change is staged. No URL with credentials in the diff. |

Documentation: one row in `AGENTS.md` (TASK-P2-016). The README is a Phase 4 task.

Review step: a reviewer checks each requirement against its test and checks `DESIGN.md` conformity by screenshot on the emulator (light and dark mode).

### Requirement trace

| Requirement | Tasks | Tests |
|---|---|---|
| REQ-201, 203, 204 | 008, 009 | ViewModelTest, TaskAppTest (filter, empty state) |
| REQ-202, 213 | 009 | TaskAppTest (overdue) |
| REQ-205 to 209 | 008, 010 | ViewModelTest, TaskAppTest (create, long title, server error) |
| REQ-210 | 008, 009 | ViewModelTest, TaskAppTest (status change, fail) |
| REQ-211 | 008, 009 | ViewModelTest, TaskAppTest (delete cancel, confirm) |
| REQ-212 | 009 | TaskAppTest (empty summary) |
| REQ-214 | 005, 008, 009 | RepositoryTest, ViewModelTest |
| REQ-215, SEC-201 to 203 | 001, 002 | VER-P2-002, VER-P2-005 |

---

# Code Guidelines

**Clean code**
- One composable per file when it has its own state or preview. Small helpers may share a file.
- Composables take state and callbacks. They do not touch the repository.
- Business logic lives in the ViewModel. UI code only maps state to widgets.
- Define `TaskRepository` before `ApiTaskRepository` (GEN-03). Use constructor injection.
- Do not read `BuildConfig` below `AppContainer` (DES-01).
- No commented-out code. Comments state intent only.
- Never swallow an exception. Map it to `ApiError` or rethrow. Rethrow `CancellationException`.
- Strings come from `strings.xml`. No UI text in code.

**Frontend patterns (applied to Compose)**
- Hoist state. Expose one `StateFlow<UiState>`. Collect it with `collectAsStateWithLifecycle`.
- Validate external data: DTO parsing with kotlinx.serialization is the schema check. Unknown enum values map to a parse error, not a crash.
- Build for all states: loading, content, empty, error, saving, disabled (`DESIGN.md` section 4 and 9).
- Accessibility: content descriptions, 48 dp targets, merged row semantics, text plus color for overdue.
- Use the 8-point spacing tokens from `DESIGN.md` as constants in `ui/theme`.

**Security**
- HTTPS only in release. Cleartext only for `10.0.2.2` in debug.
- No logging of bodies or URLs with credentials.
- No secrets in `BuildConfig`. The API has no key.
- `allowBackup="false"`.

---

# Implementation notes (2026-11-02)

- okhttp 5.5.1, kotlinx-serialization 1.12.0 and kotlinx-coroutines 1.11.0 need Kotlin 2.4 and compileSdk 37. The project uses Kotlin 2.2.10 and compileSdk 36.1. The build uses okhttp 5.1.0, kotlinx-serialization-json 1.9.0 and kotlinx-coroutines 1.10.2 instead.
- Added `material-icons-core` (BOM managed) for the icons.
- Raised `espressoCore` to 3.7.0 and `junitVersion` (androidx.test.ext) to 1.3.0. The old versions crash on the API 37 emulator (`InputManager.getInstance`).
- The shared fake source set uses `kotlin.directories` (AGP 9 built-in Kotlin).
- DESIGN.md has no dark values for secondary, on-surface-variant, outline-strong, success, warning and info. The dark theme uses derived values, marked "derived" in `Color.kt`.
