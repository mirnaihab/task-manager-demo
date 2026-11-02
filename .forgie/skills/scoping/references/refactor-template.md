# Refactor Plan Template

<!--
  REFACTOR PLAN — Structured for AI agent execution.

  PURPOSE: Make the current state, target state, impact, risks, and rollback path
  explicit BEFORE any editing begins. A refactor changes HOW code works, not WHAT it does.

  STRUCTURE:
    - Prerequisites & context gathering
    - Current State → Target State analysis
    - Impact Analysis across all 5 pillars
    - Roadmap of phased execution
    - Phases: each flows through three layers:
        A. WHAT  → What needs to change and why (scope, affected code, constraints)
        B. HOW   → Design of the change (patterns, contracts, data)
        C. DO    → Tasks, verification, rollback
    - Risks & stop conditions

  DESIGN PILLARS (use to assess blast radius of the refactor):
    1. PRESENTATION  — Entry points, interfaces, output
    2. BUSINESS LOGIC — Patterns, rules, architecture, data handling
    3. CONTEXT        — Storage, external info, settings
    4. COMMUNICATION  — Protocols, events, human-in-the-loop
    5. PROCESS        — Runtime, deployment, QA, observability

  DEFAULT SEQUENCING (lowest risk → highest risk):
    1. Shared types, schemas, or interfaces
    2. Core implementations
    3. Integrations and adapters
    4. Tests and fixtures
    5. Cleanup, documentation, removal of deprecated paths

  STOP & RESCOPE CONDITIONS:
    - Touches unrelated subsystems with weak coupling
    - Dependency chain too large to validate confidently
    - Rollback is unclear or impossible for a planned phase
    - Work mixes feature expansion with structural cleanup

  RULES:
    - Every task must trace to a refactor objective via IMPLEMENTS.
    - Every phase must have a verification gate before the next phase starts.
    - Every phase must have a concrete rollback path.
    - Save in: operations/refactors/nnn_name/plan.md
-->

# Prerequisites

<RULE>
You must have a context and specs available in: `/archives`, `/docs`, `AGENTS.md`,
a PRD, an HLD, or `README.md`. If not, ask the user to run the analyser agent first or
build these docs. Do NOT proceed without existing context documents.
</RULE>

| Prerequisite | Status | Location |
|-------------|--------|----------|
| Code analysis completed | [ ] | |
| HLD / AGENTS.md available | [ ] | |
| Existing tests pass | [ ] | Command: |
| Codebase searched for impact | [ ] | |

---

# Template

```markdown
# Document Control
- **Title**: Refactor Plan: [name]
- **Date Created**: [YYYY-MM-DD]
- **Last Updated**: [YYYY-MM-DD]
- **Status**: Planned | In Progress | Completed | On Hold | Abandoned

---

# Executive Summary

- **Objective**: [One sentence: what is being refactored and why]
- **Scope**: [module | subsystem | cross-cutting | full codebase]
- **Domains Affected**: [frontend | backend | API | database | infrastructure]
- **Trigger**: [What prompted this refactor: tech debt, performance, maintainability, new requirement]

---

# Current State vs. Target State

## Current State
<!-- How things work NOW. Be specific — reference files, patterns, and data flows. -->

- **Description**:
- **Architecture / Pattern in Use**:
- **Known Problems**: (why this needs refactoring)
  - Problem 1:
  - Problem 2:

## Target State
<!-- How things WILL work after. Must be concrete enough to verify. -->

- **Description**:
- **Architecture / Pattern After**:
- **Improvements Delivered**:
  - Improvement 1:
  - Improvement 2:

## Success Criteria
<!-- How do we know the refactor succeeded? Measurable, verifiable. -->

| ID | Criterion | How to Verify |
|----|-----------|---------------|
| SC-001 | All existing tests pass | `[test command]` |
| SC-002 | No new lint/type errors | `[lint command]` |
| SC-003 | [Behavioral equivalence / performance target] | [method] |

---

# Impact Analysis

<!-- Assess the blast radius of this refactor across each pillar. Mark N/A where not affected. -->

## Affected Files

| File | Change Type | Pillar | Blocks | Blocked By |
|------|-------------|--------|--------|------------|
| `path/to/file` | modify / create / delete / rename | Business Logic | | |

## Impact by Pillar

**Presentation** (entry points, interfaces, output):
- APIs / UI contracts that change:
- Breaking changes to consumers: yes / no
- Deprecation shims needed: yes / no

**Business Logic** (patterns, rules, data handling):
- Patterns being replaced:
- Validation rules affected:
- Data flows that change:

**Context** (storage, external info, settings):
- Schema / migration changes:
- Configuration changes:
- External integration impact:

**Communication** (protocols, events, human-in-loop):
- Contract changes between components:
- Event schema changes:
- Schemas and DTOs
- APIs

**Process** (runtime, QA, deployment):
- Build / CI pipeline changes:
- Deployment considerations:
- Feature flag needed: yes / no

---

# Refactor Objectives

<!-- What this refactor must achieve. Phases trace back to these. -->

- **RFO-001**: [Objective — e.g., "Replace raw SQL with repository pattern"]
- **RFO-002**: [Objective]

# Constraints

| ID | Constraint | Rationale |
|----|-----------|-----------|
| CON-001 | No behavioral changes to public APIs | Backwards compatibility |
| CON-002 | | |

---

# Roadmap

| Phase | Title | Scope | Depends On | Verification Gate | Rollback |
|-------|-------|-------|------------|-------------------|----------|
| 1 | Types & Interfaces | [summary] | — | [command] | [method] |
| 2 | Core Implementation | [summary] | Phase 1 | [command] | [method] |
| 3 | Integrations & Adapters | [summary] | Phase 2 | [command] | [method] |
| 4 | Tests & Fixtures | [summary] | Phase 3 | [command] | [method] |
| 5 | Cleanup & Documentation | [summary] | Phase 4 | [command] | [method] |

<!--
  SEQUENCING RULES:
  - Default order: types → implementations → integrations → tests → cleanup
  - If the codebase follows a different migration style, preserve compatibility and explain.
  - A phase's entry condition must match the previous phase's verification gate.
  - Phases with no shared dependencies MAY run in parallel — note in Depends On.
-->

---

# Phase 1: [Phase Title]

> [One-sentence description of what this phase changes]

- **GOAL-P1**: [What this phase achieves]
- **Implements**: RFO-xxx
- **Entry Condition**: [prerequisite state]
- **Exit Condition**: [verification gate passes]

---

## A · What — *What needs to change*

### A.1 Scope

| ID | Current Code | Problem | Target | Affected Files |
|----|-------------|---------|--------|----------------|
| CHG-P1-001 | [what exists now] | [why it's wrong] | [what it becomes] | `path/to/file` |

### A.2 Constraints & Assumptions

| Type | ID | Description | Impact if Wrong |
|------|----|-------------|-----------------|
| Constraint | CON-P1-001 | | |
| Assumption | ASM-P1-001 | | |

---

## B · How — *Design of the change*

### B.1 Decisions

| ID | Decision | Rationale | Alternatives Considered |
|----|----------|-----------|------------------------|
| DES-P1-001 | | | |

### B.2 Design (by pillar)

<!-- Only fill pillars affected by this phase. Mark others N/A. -->

**Presentation** (entry points, interfaces, output):
- Contract changes:
- Deprecation shims:

**Business Logic** (patterns, rules, data handling):
- Pattern before → after:
- Data flow changes:

**Context** (storage, external info, settings):
- Schema changes:
- Config changes:

**Communication** (protocols, events):
- Contract changes:

**Process** (runtime, QA, deployment):
- Build impact:

### B.3 Data Models

<!-- Only models modified in this phase. -->

| Module | Model | Change (before → after) | Migration Needed |
|--------|-------|------------------------|-----------------|
| | | | yes / no |

### B.4 Interfaces & Contracts

| ID | From → To | Before | After | Breaking? |
|----|-----------|--------|-------|-----------|
| INT-P1-001 | | | | yes / no |

---

## C · Do — *Execute, verify, rollback*

### C.1 Tasks

| ID | Task | Implements | Files | Status |
|----|------|-----------|-------|--------|
| TASK-P1-001 | | CHG-P1-xxx | `path/to/file` | [ ] |
| TASK-P1-002 | | CHG-P1-xxx | `path/to/file` | [ ] |

### C.2 Verification Gate

<!-- The EXACT command or check that proves this phase is safe before moving on. -->

| Check | Command | Expected Result |
|-------|---------|-----------------|
| Types compile | `[command]` | 0 errors |
| Existing tests pass | `[command]` | All pass |
| Lint clean | `[command]` | 0 errors |

### C.3 Rollback Plan

<!-- Concrete steps to undo THIS phase if verification fails. -->

| Step | Action | Restores |
|------|--------|----------|
| 1 | | |
| 2 | | |

- **Irreversible operations in this phase**: [none | list them]
- **Feature flag / compatibility shim**: [describe if applicable]

---

# Phase 2: [Phase Title]

> [One-sentence description]

<!-- Repeat the A · B · C structure. Increment IDs: CHG-P2-xxx, TASK-P2-xxx, etc. -->

## A · What
### A.1 Scope
### A.2 Constraints & Assumptions

## B · How
### B.1 Decisions
### B.2 Design (by pillar)
### B.3 Data Models
### B.4 Interfaces & Contracts

## C · Do
### C.1 Tasks
### C.2 Verification Gate
### C.3 Rollback Plan

---

<!-- Add more phases as needed following the same A · B · C pattern. -->

---

# Risks

| ID | Risk | Likelihood | Impact | Mitigation | Phase |
|----|------|-----------|--------|------------|-------|
| RISK-001 | | Low / Med / High | Low / Med / High | | P1 |

# Documentation Updates

<!-- Capture all doc changes needed after the refactor completes. -->

| Document | Change Needed | Status |
|----------|--------------|--------|
| AGENTS.md | | [ ] |
| README.md | | [ ] |
| API docs | | [ ] |

# Related Specifications & References

| Document | Location | Link |
|----------|----------|------|
| | | |
```
