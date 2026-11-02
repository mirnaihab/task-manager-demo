---
name: user-story
description: A template for creating a user story to capture the design, requirements, and implementation plan for a new feature. Use when the user wants to create a user story, outline the design and requirements for a new feature, or communicate the design to stakeholders.
---

# 1. Story Statement

As a [role/persona],
I want to [action/capability],
So that [outcome/value].

---

# 2. Context

## 2.1 Background
[Brief context for why this story exists. 2-3 sentences maximum.
Reference the PRD for full detail.]

## 2.2 Assumptions
-
-

## 2.3 Constraints
-
-

## 2.4 Dependencies
-
-

---

# 3. Acceptance Criteria (Gherkin Scenarios)

**REQ: FR-001**: <name>, refers to: <PRD requirement ID>

Feature: <Feature name or short description>

  Scenario: <Happy path — what needs to happen>
    Given: <context or prerequisite>
    When: <description of what happens>
    And: <further condition>
    Then: <action that should happen>
    And: <side effects>

---

**REQ: FR-002**: <name>, refers to: <PRD requirement ID>

Feature: <Feature name or short description>

  Scenario: <Alternative path>
    Given: <context or prerequisite>
    When: <description of what happens>
    Then: <action that should happen>

---

**REQ: FR-003**: <name>, refers to: <PRD requirement ID>

Feature: <Feature name or short description>

  Scenario: <Error / edge case>
    Given: <context or prerequisite>
    When/If: <unwanted or unexpected condition>
    Then: <system response>
    And: <user feedback or recovery action>

---

> Guidance:
> - Write one scenario per distinct behavior, not per requirement
> - A single requirement may have multiple scenarios (happy, alternative, error)
> - Keep Given/When/Then statements atomic and testable
> - Avoid implementation details in scenarios (no "clicks button", no "calls API")
>   Focus on behavior: "user submits the form", "system validates the input"

---

# 4. Scope

## 4.1 In Scope
-
-

## 4.2 Out of Scope
-
-

---

# 5. UI / UX Notes (If Applicable)

[Reference wireframes, mockups, or describe expected behavior.
Link to design assets rather than duplicating them.]

-
-

---

# 6. Data Requirements (If Applicable)

- Input Data:
- Output Data:
- Validation Rules:

---

# 7. Technical Notes (Optional)

[Any technical context the developer should be aware of before starting
the design document. This is not a design — it is a heads-up.]

-
-

---

# 8. Open Questions

| ID | Question | Owner | Status |
|----|----------|-------|--------|
| | | | Open / Resolved |

---

# 9. Sizing

- Estimate: S / M / L / XL
- Rationale:

---

# Definition of Ready Checklist

- [ ] Problem and outcome are clear
- [ ] Acceptance criteria are specific and testable
- [ ] Scope boundaries defined
- [ ] Dependencies identified and availability confirmed
- [ ] No unresolved blockers
- [ ] Design assets available (if applicable)
- [ ] Reviewer / point of contact identified