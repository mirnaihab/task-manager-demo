# ADR-009: API base URL per build type, plain HTTP in debug builds only

- **Status**: Accepted
- **Date**: 2026-11-03

## Context
The apps call a local API in development and a hosted HTTPS API in release. The Android emulator reaches the host at `10.0.2.2`. The iOS simulator uses `localhost`.

## Decision
Android sets `BuildConfig.API_BASE_URL` per build type. Cleartext traffic is allowed in the debug build only, for `10.0.2.2`. iOS reads `API_BASE_URL` from `ios/Config/Info-Debug.plist` and `Info-Release.plist`. Only the Debug plist sets `NSAllowsLocalNetworking`. The release value is `https://example.invalid` until phase 4 sets the hosted URL.

## Options considered
- Gradle property or compile-time constant.
- `INFOPLIST_KEY_API_BASE_URL` build setting. It did not reach the built plist (RISK-301), so the plist files are used.

## Consequences
Release builds cannot call plain HTTP. Phase 4 must replace the placeholder URL in both release configs.

## Links
- `android/app/build.gradle.kts`
- `ios/Config/Info-Debug.plist`
- `ios/Config/Info-Release.plist`
- `ios/TaskManager/AppEnvironment.swift`
- Source: `../../operations/archives/2026-11-03-task-manager-phases-1-3/plan_phase_2.md` DES-P2-007, `../../operations/archives/2026-11-03-task-manager-phases-1-3/plan_phase_3.md` DES-P3-006, DES-P3-007
