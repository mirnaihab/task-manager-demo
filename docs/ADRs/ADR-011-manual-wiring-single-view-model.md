# ADR-011: Manual wiring and one list view model per app

- **Status**: Accepted
- **Date**: 2026-11-02

## Context
The apps have two screens. A DI framework or a navigation library adds parts with no gain.

## Decision
Android uses an `AppContainer` and a `ViewModelProvider.Factory`. iOS passes the API through `AppEnvironment`. Each app has one `TaskListViewModel` for the list, form and delete dialog. After each change the view model calls the API for the list and summary in parallel. UI tests use a fake or mock API through the same wiring. iOS selects the mock with the `-UITestMock` launch argument.

## Options considered
- Hilt or Koin.
- Navigation Compose.
- Separate view models per screen.

## Consequences
Tests are fast and stable and never use the network. The mock API lives in the iOS app target, not the test target (DES-P3-008). Grow into a DI framework only if the app gets more screens.

## Links
- `android/app/src/main/java/com/demo/taskmanager/ui/TaskListViewModel.kt`
- `ios/TaskManager/Features/TaskListViewModel.swift`
- `ios/TaskManager/Data/MockTaskAPI.swift`
- Source: `../../operations/archives/2026-11-03-task-manager-phases-1-3/plan_phase_2.md` DES-P2-001, DES-P2-002, DES-P2-006, `../../operations/archives/2026-11-03-task-manager-phases-1-3/plan_phase_3.md` DES-P3-008
