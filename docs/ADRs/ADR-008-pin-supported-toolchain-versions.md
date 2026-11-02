# ADR-008: Pin toolchain versions that the ecosystem supports

- **Status**: Accepted
- **Date**: 2026-11-02

## Context
The newest package version is not always usable. Build steps found three cases.

## Decision
Pin to the newest version that the rest of the toolchain supports. Backend: TypeScript `~6.0.3` (npm latest is 7.0.2, but typescript-eslint 8.72 supports only versions below 6.1). Android: okhttp 5.1.0, kotlinx-serialization-json 1.9.0 and kotlinx-coroutines 1.10.2 (newer versions need Kotlin 2.4 and compileSdk 37, the project uses Kotlin 2.2.10 and compileSdk 36.1). Android test libraries: espresso 3.7.0 and androidx.test.ext junit 1.3.0 (older versions crash on the API 37 emulator).

## Options considered
- Upgrade Kotlin and compileSdk first.
- Use the newest version of each package.

## Consequences
Builds pass today. Upgrades must move together (TypeScript with typescript-eslint, Kotlin with the Android libraries). See technical debt TD-004.

## Links
- `backend/package.json`
- `android/gradle/libs.versions.toml`
- Source: `../../operations/archives/2026-11-03-task-manager-phases-1-3/plan_phase_1.md` pinned versions, `../../operations/archives/2026-11-03-task-manager-phases-1-3/plan_phase_2.md` implementation notes
