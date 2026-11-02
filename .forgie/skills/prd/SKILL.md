---
name: prd
description: Product Requirements Document (PRD) template for capturing the problem statement, goals, success criteria, target users, requirements, scope, user flows, UI/UX direction, data requirements, dependencies, risks, release strategy, open questions, user stories, and references. Use this template when the user asks for a PRD, product requirements, or a detailed specification of a feature or project. Use before planning implementation or creating a design document. Use this template to ensure all necessary information is captured and organized in a structured manner. Use this template to communicate the product vision, requirements, and design decisions to stakeholders, developers, and other team members. Use this template to facilitate collaboration, feedback, and alignment on the product requirements and design. Use this template to ensure that the product requirements are clear, complete, and actionable for the development team. Use this template to document the product requirements and design decisions for future reference, maintenance, and iteration. Use this template to ensure that the product requirements are aligned with the business goals, user needs, and technical constraints
---

# Product Requirements Document Template
Fill the template below with the relevant information for your project or feature. Use this template to capture the problem statement, goals, success criteria, target users, requirements, scope, user flows, UI/UX direction, data requirements, dependencies, risks, release strategy, open questions, user stories, and references. Use this template to ensure that all necessary information is captured and organized in a structured manner. Use this template to communicate the product vision, requirements, and design decisions to stakeholders, developers, and other team members. Use this template to facilitate collaboration, feedback, and alignment on the product requirements and design. Use this template to ensure that the product requirements are clear, complete, and actionable for the development team. Use this template to document the product requirements and design decisions for future reference, maintenance, and iteration. Use this template to ensure that the product requirements are aligned with the business goals, user needs, and technical constraints.

---

# 1. Document Control

- Version:
- Date Created:
- Last Updated:
- Status: Draft | In Review | Approved | Superseded
- Author:
- Reviewers:
- Approval:

---

# 2. Problem Statement

## 2.1 Background
[What is the current state? What context does the reader need to understand
why this document exists?]

## 2.2 Problem
[What specific problem are we solving? Who experiences it? What is the
impact of not solving it?]

## 2.3 Evidence
[How do we know this is a real problem? Data, user feedback, support
tickets, business metrics, competitive pressure, etc.]

---

# 3. Goals and Success Criteria

## 3.1 Goals
- G-001:
- G-002:

## 3.2 Non-Goals
[What are we explicitly choosing NOT to solve in this effort?]
-
-

## 3.3 Success Metrics
| Metric | Current State | Target | How Measured |
|--------|--------------|--------|--------------|
| | | | |

> Guidance:
> - Goals should be outcomes, not outputs
> - "Build a tracking page" is an output
> - "Users can self-serve order status without contacting support" is an outcome

---

# 4. Target Users

## 4.1 Primary Users
[Who is the main user of this feature? Role, context, technical proficiency.]

## 4.2 Secondary Users
[Other users who interact with or are affected by this feature.]

## 4.3 User Segments and Considerations
[Any segmentation that affects behavior: geography, permissions, plan tier,
device type, etc.]

---

# 5. Requirements

## 5.1 Functional Requirements

| ID | Requirement | Priority | Notes |
|----|------------|----------|-------|
| FR-001 | | Must / Should / Could | |
| FR-002 | | Must / Should / Could | |
| FR-003 | | Must / Should / Could | |

> Guidance:
> - Requirements should be testable and unambiguous
> - Use MoSCoW prioritization (Must / Should / Could / Won't)
> - Each requirement should map to at least one user story

## 5.2 Non-Functional Requirements (If Applicable)

| ID | Category | Requirement | Target |
|----|----------|------------|--------|
| NFR-001 | Performance | | |
| NFR-002 | Security | | |
| NFR-003 | Accessibility | | |

## 5.3 Constraints

| ID | Constraint | Rationale |
|----|-----------|-----------|
| CON-001 | | |

---

# 6. Scope

## 6.1 In Scope
-
-

## 6.2 Out of Scope
-
-

## 6.3 Future Considerations
[Things we may want to do later but are explicitly deferring. This prevents
scope creep and documents intent.]
-

---

# 7. User Flows

## 7.1 Primary Flow
[Step-by-step description of the main user journey through this feature.]

1.
2.
3.

## 7.2 Alternative Flows
[Variations on the primary flow: different entry points, optional steps,
branching paths.]

## 7.3 Error / Edge Flows
[What happens when things go wrong from the user's perspective?]

> Guidance:
> - Flows should be written from the user's perspective, not the system's
> - Each flow should be traceable to one or more functional requirements
> - These flows directly feed into user story scenarios

---

# 8. UI / UX Direction (If Applicable)

## 8.1 Wireframes / Mockups
[Link to design files or embed key screens]

## 8.2 Interaction Notes
[Any specific interaction behavior: animations, transitions, loading
states, empty states, responsive behavior.]

## 8.3 Content / Copy
[Any specific text, labels, error messages, or translations required.]

> Guidance:
> - This section provides direction, not pixel-perfect specification
> - The developer's design document will detail the implementation

---

# 9. Data Requirements

## 9.1 Data Inputs
[What data does this feature need to function? Where does it come from?]

## 9.2 Data Outputs
[What data does this feature produce or modify?]
[what are the schema, models, formats and storage requirements?]

## 9.3 Data Dependencies
[External data sources, APIs, or datasets required.]

## 9.4 Privacy and Compliance
[Any PII handling, data retention, consent, or regulatory considerations.]

---

# 10. Dependencies and Assumptions

## 10.1 Dependencies

| ID | Dependency | Owner | Status | Impact if Delayed |
|----|-----------|-------|--------|-------------------|
| DEP-001 | | | | |

## 10.2 Assumptions

| ID | Assumption | Impact if Wrong |
|----|-----------|-----------------|
| ASM-001 | | |

---

# 11. Risks

| ID | Risk | Likelihood | Impact | Mitigation |
|----|------|-----------|--------|------------|
| R-001 | | Low / Med / High | Low / Med / High | |

---

# 12. Release Strategy

## 12.1 Rollout Plan
[Big bang, phased rollout, feature flag, A/B test, beta group, etc.]

## 12.2 Rollback Plan
[How do we revert if something goes wrong post-release?]

## 12.3 Communication
[Who needs to be informed? Support teams, documentation, marketing, etc.]

---

# 13. Open Questions

| ID | Question | Owner | Status | Resolution |
|----|----------|-------|--------|------------|
| OQ-001 | | | Open / Resolved | |

> Guidance:
> - Features should not pass Definition of Ready with critical open
>   questions unresolved
> - Non-critical questions can be marked "developer decides" with boundaries

---

# 14. User Stories

[Link to or list the user stories derived from this PRD.
Each story should reference the functional requirements it fulfills.]

- US-001: [Title] --> FR-001, FR-003
- US-002: [Title] --> FR-002
- US-003: [Title] --> FR-004, FR-005

---

# 15. References

| Document | Description | Link |
|----------|------------|------|
| | | |

---

# Appendix (Optional)

[Supplementary material: competitive analysis, research data, technical
spikes, prior art, related PRDs.]

---
