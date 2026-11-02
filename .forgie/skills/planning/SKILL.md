---
name: planning
description: Use for feature discovery, scoping, context mapping, requirements, technical specifications, and implementation plans. Use before any code changes to plan implementation. Use for when you need to create documentation, design decisions, or implementation plans or refactor plans. Use this template for creating specs and feature or implementation plans.
---

# Universal Quality Standards of the planning skill

- Use precise, explicit, unambiguous language.
- Prefer structured sections, bullets, tables, and examples over prose-only descriptions.
- Keep the document self-contained rather than relying on surrounding context.
- Keep tasks small, explicit, and independently verifiable.
- Prefer simple design based on the requirements rather than over-engineering for potential future needs.
- Do not rely on placeholders such as `TBD`, `TODO`, or “handle appropriately”.
- Before defining tasks, map:
  - Which files will be created or modified
  - What each file is responsible for
  - Which responsibilities belong together
  - Which tests and docs belong with each area
- Favor focused files and explicit interfaces. 
- Favor shallow wide interfaces and deep implementations.
- In existing codebases, follow established conventions unless the current shape directly blocks the requested work.
- Every task must:
  - Use explicit action verbs (create, modify, validate, test).
  - Be independently verifiable.
  - Avoid vague language ("improve", "handle").

**important rule** The simplest way to do the delivery is paramount to the plan. Always prefer the simplest approach that meets the requirements and constraints. If the simplest approach is not possible, gradually layer in complexity, and clearly document the rationale for each added layer of complexity. 
**important rule** Use diagrams, mermaid, asciidoc, sequence diagrams, data flow, relationships, tables and other visualizations to clarify complex relationships, flows, and dependencies.
**important rule**  This creates an implementation focused plan for a single phase of a scope or a single feature. multiple versions of this shall be created for each phase of a complex scope or feature. When the scope is small enough that multiple phases can be merged into a single plan, then a single plan is sufficient. When the scope is large enough that multiple phases are needed, then multiple plans shall be created for each phase. Each plan shall be self contained and independently verifiable. Each plan shall have its own set of tasks, files, and responsibilities. Each plan shall have its own set of tests and documentation. Each plan shall have its own set of diagrams and visualizations. Each plan shall have its own set of requirements and constraints. Each plan shall have its own set of risks and unknowns. Each plan shall have its own set of assumptions and dependencies. Each plan shall have its own set of verification steps. Each plan shall have its own set of documentation steps. Each plan shall have its own set of review steps. Each plan shall have its own set of approval steps. Each plan shall have its own set of delivery steps. 


## Plan Self-Review
After drafting the plan:
1. Check every requirement in the spec and confirm a task implements it.
2. Search for placeholders, vague language, or references to undefined functions or types.
3. Confirm later tasks use the same names, types, and signatures introduced earlier.
4. Add any missing verification or documentation steps.

## What Good Looks Like
A good plan lets another engineer or agent execute the work with minimal interpretation, predictable verification, and clear file-by-file responsibility.


# Implementation & Low-Level Design Plan Template

<!--
  IMPLEMENTATION & LLD PLAN — Structured for AI agent execution.

  RULES:
    - Requirements follow EARS & gherkin notation (see below).
    - Requirements must be Testable, Unambiguous, Necessary, Feasible, and Traceable.
    - Every task must trace back to a requirement via IMPLEMENTS.
    - Every test must trace back to an acceptance criterion.
    - Save in: operations/specs/xxx_name/plan.md
    - Valid prefixes: upgrade, refactor, feature, data, infrastructure, process, architecture, design
-->

## Requirements Quality Standard

```diff
# Vague (BAD)
- The search should be fast and return relevant results.
- The UI must look modern and be easy to use.

# Concrete (GOOD)
+ The search must return results within 200ms for a 10k record dataset.
+ The search algorithm must achieve >= 85% Precision@10 in benchmark evals.
+ The UI must follow the chosen design system and achieve 100% Lighthouse Accessibility score.
```

### EARS Notation
- **Ubiquitous:** `THE SYSTEM SHALL [expected behavior]`
- **Event-driven:** `WHEN [trigger event] THE SYSTEM SHALL [expected behavior]`
- **State-driven:** `WHILE [in specific state] THE SYSTEM SHALL [expected behavior]`
- **Unwanted behavior:** `IF [unwanted condition] THEN THE SYSTEM SHALL [required response]`
- **Optional:** `WHERE [feature is included] THE SYSTEM SHALL [expected behavior]`

---

# Template

```markdown
# Document Control
- **Version**:
- **Date Created**: [YYYY-MM-DD]
- **Last Updated**: [YYYY-MM-DD]
- **Status**: Planned | In Progress | Completed | On Hold | Deprecated

---

# Executive Summary

- **Goal**: [Concise title describing the plan's goal]
- **Scope**: [bug fix | refactor | POC | MVP | production feature | small feature]
- **Domains**: [frontend | backend | API | database | infrastructure | full-stack]
- **Architecture**: [2–3 sentences on the approach]
- **Tech Stack**: [Key technologies and libraries]
- **Entry Condition**: [What must be true before this plan starts]
- **Exit Condition**: [What must be true for this plan to be considered done]
---

# Requirements

## Functional Requirements
- **REQ-001**: [EARS-formatted requirement one liner]
- **SEC-001**: [Security requirement]
- **CON-001**: [Constraint]
- **GUD-001**: [Guideline]
- **PAT-001**: [Pattern to follow]
<!-- bdd scenario gherkin format for each one above -->
Feature: <Feature-name-or-short-description>
  Scenario: <what-needs-to-happen>
    Given: <context-or-prerequisite>
    When/if: <description-of-what-happens> eg: user enters "x" in field "y"
    And: <further-condition>
    Then: <action-that-should-happen>
    And: <side-effects>

---

# Dependencies

| ID | Name | Type | Version | Purpose |
|----|------|------|---------|---------|
| DEP-001 | | package / module / integration / service | | |

---

# Plan Details

## Design — *How it should happen*

### Impact on system design
<add system or project design impact if needed, use visuals>

### Decisions

| ID | Decision | Rationale | Alternatives Considered | Side effect (which modules?) |
|----|----------|-----------|------------------------|-------------------|
| DES-P1-001 | | | | |

### Constraints & Assumptions

| Type | ID | Description | Impact if Wrong | Handling Strategy |
|------|----|-------------|-----------------|-------------------|
| Constraint | CON-P1-001 | | | |
| Assumption | ASM-P1-001 | | | |

### Edge cases
  | ID | Scenario | Expected Behavior | Handling Strategy |
  |----|----------|-------------------|-------------------|
  | EDGE-P1-001 | | | |


### Directory Map
  ```text
  <repository-root>/ 
  ├── <directory-name>/ 
  │   ├── <file-name.ext> 
  │   ├── <file-name.ext>
  │   └── <subdirectory-name>/
  │       └── <file-name.ext>
  ├── <directory-name>/
  │   └── <file-name.ext>
  └── <file-name.ext>
  ```
### Directory Specifications

<rule> everything in this table is a concise 1-3 lines comma separate the points if many <rule>

| ID | Task | File(s)| responsibility or Expected Behaviors (include side effect if any)  | Validations/Error handling | Key Configurations/settings |
|----|------|--------|-------------------------------------------------------------------|----------------------------|-----------------------------|
| TASK-P1-001 | | | | | | | | |

### Schemas grouped reasonably by domain or module
  - add input schemas like APIs (Yaml/JSON), DTOs, ...etc
  - add output schemas like expected returns, DTOs responses, errors thrown...etc
  - use objects/dictionaries to express the contract/schema

### Data Models & flows
<settings>
  **Add here any changes to config files, settings or env variables**
</settings>

<Data-Models> 
<!-- Only models introduced or modified in this plan. -->
  **Add here the ERD and data model changes needed.**
  - **Note**: Prepare visual using ASCII diagram, or ERD as tables or JSON schema if the visual would be too complex
</Data-Models>

<Data-flow>
<!-- Describe here the data flow diagram -->
  **Add here sequence diagrams or data flow blocks explaining what needs to happen in this plan**
  - **Note**: you can use code blocks if you need
</data-flow>

---
#### **Process** (runtime, QA, deployment):
**only fill this section if applicable on the design level not on every implementation detail level**
- Runtime Considerations:
- Deployment Impact: <add here changes that need to be considered in deplyment files or instances that need to be created or even git ops>
---

# Risks

| ID | Risk | Likelihood | Impact | Mitigation |
|----|------|-----------|--------|------------|
| RISK-001 | | Low / Med / High | Low / Med / High | |


# Code Guidelines
- *always applicable* load the `clean-code` skill and extract relevant rules/patterns and add it to this section.
- *always applicable* load the `security` skill and extract relevant rules/patterns and add it to this section.
- *if applicable* load the `frontend-patterns` skill and extract relevant rules/patterns and add it to this section.
- *if applicable* load the `database` skill and extract relevant rules/patterns and add it to this section.

---
```
