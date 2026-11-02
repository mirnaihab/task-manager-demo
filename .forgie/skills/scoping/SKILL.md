---
name: scoping
description: This skill captures the high level design decisions, project scope, and architecture overview for the project template. It serves as a reference template for creating a high level design or review report for the codebase for onboarding or explanation or exploration of scope. It is based on project context, goals, and constraints. Use this skill when you need to create or update the AGENTS.md file for project overview, context, goals, and constraints. Use this when the user wants to explore a full project scope or a prototype or just wants to think through their design decisions. Use this skill when the user wants to create a refactor plan or full codebase analysis report. 
---

**important rule** The simplest way to do the delivery is paramount to the plan. Always prefer the simplest approach that meets the requirements and constraints. If the simplest approach is not possible, gradually layer in complexity, and clearly document the rationale for each added layer of complexity. 
**important rule** Use diagrams, mermaid, asciidoc, sequence diagrams, data flow, relationships, tables and other visualizations to clarify complex relationships, flows, and dependencies.


# The Behavior
- **Visual** - Use ASCII diagrams liberally when they'd help clarify thinking
- **Adaptive** - Follow interesting threads, pivot when new information emerges
- **Patient** - Don't rush to conclusions, let the shape of the problem emerge
- **Grounded** - clarify the actual codebase when relevant, don't just theorize

---

# What You Might Do

Depending on what the user brings, you might:

- **clarify the problem space**
- **Investigate the codebase**
- **Compare options**
- **Surface risks and unknowns**
- **Visualize** - use ASCII diagrams, mermaid, asciidoc, sequence diagrams, data flow, relationships, tables and other visualizations to clarify complex relationships, flows, and dependencies.

 ```text
     ┌─────────────────────────────────────────┐
     │     Use ASCII diagrams liberally        │
     ├─────────────────────────────────────────┤
     │                                         │
     │   ┌────────┐         ┌────────┐        │
     │   │ State  │────────▶│ State  │        │
     │   │   A    │         │   B    │        │
     │   └────────┘         └────────┘        │
     │                                         │
     │   System diagrams, state machines,      │
     │   data flows, architecture sketches,    │
     │   dependency graphs, comparison tables  │
     │                                         │
     └─────────────────────────────────────────┘
  ```
---

# Handling Different Entry Points

**User brings a vague idea:**
```
User: I'm thinking about adding real-time collaboration

You: Real-time collab is a big space. Let me think about this...

      COLLABORATION SPECTRUM
      ════════════════════════════════════════════

      Awareness          Coordination         Sync
          │                   │                 │
          ▼                   ▼                 ▼
      ┌────────┐         ┌────────┐        ┌────────┐
      │Presence│         │Cursors │        │  CRDT  │
      │  "3    │         │ Multi  │        │Conflict│
      │online" │         │ select │        │  free  │
      └────────┘         └────────┘        └────────┘
          │                   │                 │
       trivial            moderate           complex

      Where's your head at?
```

**User brings a specific problem:**
```
User: The auth system is a mess

You: [reads codebase]

     ┌─────────────────────────────────────────────┐
     │              CURRENT AUTH FLOW              │
     └─────────────────────────────────────────────┘
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
     ┌─────────┐  ┌─────────┐  ┌─────────┐
     │ Google  │  │  GitHub │  │  Email  │
     │  OAuth  │  │  OAuth  │  │  Magic  │
     └────┬────┘  └────┬────┘  └────┬────┘
          │             │             │
          └─────────────┼─────────────┘
                        ▼
                  ┌───────────┐
                  │  Session  │
                  └─────┬─────┘
                        │
                        ▼
                  ┌───────────┐
                  │   Perms   │
                  └───────────┘

     I see three tangles. Which one's burning?
```

**User is stuck mid-implementation:**
```
User: add-auth-system
      The OAuth integration is more complex than expected

You: [reads change artifacts]

     You're on task 4: "Implement OAuth flow"

     Let me trace what's involved...

     [draws diagram, clarifys options, suggests paths]

     Want to update the design to reflect this?
     Or add a spike task to investigate?
```

**User wants to compare options:**
```
User: Should we use Postgres or SQLite?

You: Generic answer is boring. What's the context?

User: A CLI tool that tracks local dev environments

You: That changes everything.

     ┌─────────────────────────────────────────────────┐
     │         CLI TOOL DATA STORAGE                  │
     └─────────────────────────────────────────────────┘

     Key constraints:
     • No daemon running
     • Must work offline
     • Single user

                  SQLite          Postgres
     Deployment   embedded ✓      needs server ✗
     Offline      yes ✓           no ✗
     Single file  yes ✓           no ✗

     SQLite. Not even close.

     Unless... is there a sync component?
```

<!--
  HLD TEMPLATE — Structured for AI agent consumption throughout the project lifecycle. 

  TONE: Generally minimalist and guidance oriented. Not verbose unless it is necessary. 

  STRUCTURE: Five pillars that collectively define the full system design:
    1. PRESENTATION  — How the system is triggered, exposed, and delivers output
    2. BUSINESS LOGIC — What the system does: requirements, architecture, patterns, data
    3. CONTEXT        — Where data lives and what external information the system depends on
    4. COMMUNICATION  — How components, services, and humans exchange information
    5. PROCESS        — How the system is built, shipped, monitored, and maintained

  USAGE:
  - Fill every field. Use "N/A — [justification]" for inapplicable items.
  - Use the ID prefixes (PRE-, BIZ-, CTX-, COM-, PRC-) when referencing items across documents.
  - Keep this document as the single source of truth for high-level design decisions.
  - Update it when requirements, scope, or architecture change — never let it drift.
-->

# Template

```markdown
# Project Overview

## Executive Summary
<!-- A concise snapshot an AI agent or new team member can read in 60 seconds. -->

- **Title**:
- **Description**:
- **Business Context**:
- **Version**:
- **Date**:
- **Objective**:
- **Problem Statement**: 1–2 sentences on the pain point.
- **Proposed Solution**: 1–2 sentences on the fix.
- **Success Criteria**: 3–5 measurable KPIs.
- **Goals**: The goals of this project.
- **Personas**: Brief persona list with what they can do.
- **Tech Stack**: Technologies, languages, frameworks, and third-party services used.
- **Architecture**: Brief description of system architecture (microservices / monolith / frontend-backend / data pipeline / RAG / etc.).

## Scope

### Definitions & Ubiquitous language

| Term | Definition |
|------|-----------|
| | |


### Project Context
- What is the project about?
- Who are the target users and what are their needs and pain points?

### System Boundaries
**In Scope:**
-

**Out of Scope:**
-

### Target Users
**Primary Users:**
-

**Secondary Users:**
-
- How many users typically use the system? How many are concurrent?
- Do we need roles? What roles and permissions?
- What are the user personas?
- What should the user be able to do?

### Roadmap 
<!--
  High-level overview of ALL phases. Read this to understand the full project roadmap. Keep it scannable — one row per phase.
-->

| Phase | Title | Description | feature |
|-------|-------|-------------|-----------------|
| 1 | | [One-sentence summary] | |
| 2 | | [One-sentence summary] | |
| 3 | | [One-sentence summary] | |

---

# 1 · PRESENTATION LAYER
<!-- How the system is triggered, exposed, and delivers output to its consumers. -->
  <add api contracts, ui, cli, and output delivery details here>

## 1.1 Entry Points
<!-- Every way the system can be invoked. -->
- **PRE-EP-001**: [Trigger type] — [Description] (e.g., HTTP request, cron schedule, CLI command, event subscription, webhook, user action)
<!-- Add rows as needed -->

## 1.2 Interfaces & Contracts
<!-- The public-facing contracts the system exposes. -->

### API Interfaces
```Yaml
method: [GET | POST | PUT | DELETE | PATCH | SUBSCRIBE | PUBLISH | INVOKE]
path: [URL path, channel name, or event topic]
request:
  type: [JSON | YAML | GraphQL | gRPC | Other]
  schema: [Reference to schema or example payload]
response:
  type: [JSON | YAML | GraphQL | gRPC | Other]
  schema: [Reference to schema or example payload]
auth:
  type: [None | API Key | OAuth2 | JWT | Session | Other]
```
<add notes on versioning, rate limiting, pagination, filtering, and error handling conventions here>

### Schemas
  - add input schemas like DTOs, contracts, APIs...etc
  - add output schemas like expected returns, responses, errors thrown...etc
  - if the function name or description are self explanatory, do not add the schema
    Example: add(x,y) --> no need. 
  - use objects/dictionaries to express the contract/schema

### UI Interfaces
- **CSS Framework**: (e.g., Tailwind, Bootstrap, Material UI, CSS Modules)
- **Component Library**: (e.g., shadcn/ui, Radix, Ant Design, Chakra UI)
- **Design System**: Does one exist? Reference DESIGN.md / tailwind.config if available.
- **Primary Colors**:
- **Secondary Colors**:
- **Visual Tone**: Minimalist / feature-rich / dashboard-heavy / etc.
- **Page Map**: How many pages/views? Describe the sitemap or navigation tree.
- **Primary Interaction Mode**: Forms, modals, page navigations, drag-and-drop, CLI, etc.
- **Responsive Strategy**: Mobile-first / desktop-first / specific breakpoints.
- **Accessibility**: WCAG level target.

### CLI / Script Interfaces
| ID | Command | Arguments | Description |
|----|---------|-----------|-------------|
| PRE-CLI-001 | | | |

## 1.3 Output & Delivery
<!-- How the system delivers results back to consumers. -->
- **PRE-OUT-001**: [Output type] — [Description] (e.g., JSON response, rendered HTML, async webhook callback, file download, push notification, streamed SSE)
<!-- Add rows as needed -->

#### **Communication** (protocols, events, human-in-loop):
- Protocol: <if-this-is-a-system-wide-phase>
  - Pattern: (request-response | pub-sub | event | RPC | hook | function call/coroutine | promise...etc)
  - Human-in-Loop: <describe-human-in-the-loop-mechanism-if-needed>

---

# 2 · BUSINESS LOGIC
<!-- What the system does: requirements, architecture, patterns, data models, and design choices. -->

## 2.1 Functional Requirements
<!-- High-level requirements only. Detailed specs go in plan files. -->
- **BHV-001**: [Behavior or user story]
- **REQ-001**: [Functional requirement]
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


## 2.2 Non-Functional Requirements
<!-- Performance, reliability, scalability, and operational qualities. -->
| Category | Requirement | Target / SLA |
|----------|-------------|--------------|
| Availability | | |
| Reliability | | |
| Fault Tolerance | | |
| Performance | Response time, throughput | |
| Scalability | Expected load, growth patterns | |
| Monitoring | What is observed and alerted on | |
| Localization | Languages, regions, i18n | |

## 2.3 System Architecture Overview
<!-- The structural view of the system: components, layers, and how they compose. -->

### Architecture Pattern
- **Pattern**: (MVC / Clean Architecture / Hexagonal / Microservices / Modular Monolith / Pipeline / Event-Driven / SOA / etc.)
- **Paradigm**: Functional / Object-Oriented / Mixed — justify the choice.
- **Justification**: Why this pattern fits the requirements and constraints.
- **structure**: folder structure

### Component Map
<!-- List every major component, its responsibility, and its folder structure. Use ASCII diagrams. -->
| Component | Responsibility | Tech | Folder |
|-----------|---------------|------|--------|
| Frontend | | | |
| Backend | | | |
| Database | | | |
| Infrastructure | | | |
| External Services | | | |
| Scripts / Jobs | | | |

### Architecture Diagram

<!-- Replace with an ASCII system diagram showing component relationships and data flow.-->
┌──────────┐     ┌──────────┐     ┌──────────┐
│          │────▶│          │────▶│          │
│          │     │          │     │          │
└──────────┘     └──────────┘     └──────────┘
     ▲                ▲                ▲
     │                │                │
     └────────────────┴────────────────┘

### Evaluation Strategy
- How to measure output quality and accuracy.

### Tradeoff Decisions
<!-- Explicit tradeoffs: CAP theorem position, consistency vs. availability, latency vs. throughput, etc. -->
- **Complexity Management**: How is complexity managed across layers?
- **Decoupling Strategy**: How are abstractions and interfaces used to keep the system easy to change?
- **CAP Position**: Which two of Consistency, Availability, Partition-tolerance are prioritized and why?

## 2.4 Design Choices
<!-- Focus on WHAT and WHY, not HOW. The "how" belongs in implementation plans. -->
- **DES-001**: [Brief design choice]
  - **Goal**: Why?
  - **REF**: Based on what? (requirement / document / constraint / ADR)

## 2.5 Data Model
<!-- Describe the full data models per submodule with rationale. Use tables, ERDs, or code blocks. -->

### Core Entities
| Entity | Attributes | Relationships | Module |
|--------|-----------|---------------|--------|
| | | | |

### Data Structures & Flows
<!-- It is imperative to descibe the data flow in the system. Prefer visual representations such as diagrams or flowcharts. -->
- Describe how data is structured within the system.
- Describe how data flows through the system end-to-end. use diagrams, flowcharts, or sequence diagrams to illustrate the flow.
- Describe decision points and branching logic.

### Business Rules & Validations
- Input validation strategy: (zod / pydantic / yup / JSON Schema / etc.)
- Domain-level validation rules.
- User management rules and permission enforcement.

---

# 3 · CONTEXT
<!-- Where data lives, what external information the system depends on, and how it is configured. -->

## 3.1 Data Storage
<!-- Every persistence mechanism the system uses. -->
| ID | Store Type | Technology | Purpose | Data Retained |
|----|-----------|------------|---------|---------------|
| CTX-DS-001 | Relational DB | (PostgreSQL / MySQL) | | |
| CTX-DS-002 | NoSQL DB | (MongoDB / Cassandra) | | |
| CTX-DS-003 | In-Memory Store | (Redis / Memcached) | | |
| CTX-DS-004 | File System / Object Store | (S3 / local FS) | | |
| CTX-DS-005 | Vector Store | (Pinecone / pgvector) | | |

### ORM / Data Access Layer
- **Technology**: (Sequelize / TypeORM / Prisma / Drizzle / SQLAlchemy / etc.)
- **Migration Strategy**: Schema versioning, rollback plan, seed data approach.

## 3.2 External Information Sources
<!-- APIs, data feeds, and external contracts the system depends on. -->
| ID | Source | Protocol | Contract / Schema | Auth Method | SLA |
|----|--------|----------|-------------------|-------------|-----|
| CTX-EXT-001 | | | | | |

### Third-Party Services
- **SVC-001**: [Service name] — [Required capabilities and SLA requirements]

### Integration Strategy
- Which LLM provider is used? How are embedding models consumed?
- Are third-party services installed locally or connected via API keys in env files?
- Payment gateways, email providers, analytics, etc.

## 3.3 Settings & Configuration
<!-- All configuration the system needs to run. -->

### Environment Variables
| Variable | Purpose | Secret? | Default |
|----------|---------|---------|---------|
| | | | |

### Configuration Files
| File | Purpose | Per-Environment? |
|------|---------|-----------------|
| | | |

### Secrets Management
- Where are secrets stored? (vault, env file, cloud secrets manager)
- Rotation policy.

---

# 4 · COMMUNICATION
<!-- How components, services, and humans exchange information. -->

## 4.1 Internal Communication (Component ↔ Component)
| ID | From | To | Protocol | Pattern | Sync/Async | Notes |
|----|------|----|----------|---------|------------|-------|
| COM-INT-001 | | | (HTTP / gRPC / in-process) | (request-response / pub-sub / event) | | |

## 4.2 External Communication (System ↔ Outside World)
| ID | Direction | Counterparty | Protocol | Contract | Auth | Notes |
|----|-----------|--------------|----------|----------|------|-------|
| COM-EXT-001 | inbound / outbound | | (REST / WebSocket / webhook / AMQP) | | | |

## 4.3 Event & Message Patterns
<!-- Describe any event-driven, message queue, or streaming patterns. -->
- **Pattern**: (pub/sub / CQRS / event sourcing / saga / choreography / orchestration)
- **Broker**: (Kafka / RabbitMQ / Redis Streams / SQS / none)
- **Dead Letter Strategy**: How are failed messages handled?

## 4.4 Human-in-the-Loop
<!-- Where human intervention or approval is required in the system flow. -->
| ID | Trigger | Human Action Required | Fallback if No Response |
|----|---------|----------------------|------------------------|
| COM-HIL-001 | | | |

## 4.5 Interfaces & Data Contracts Summary
<!-- Cross-component contracts, shared types, and schema registries. -->
- Shared type definitions / schema registry location.
- API versioning strategy: (URL path / header / query param).
- Error response format and status code conventions.
- Rate limiting, pagination, and filtering conventions.

---

# 5 · PROCESS
<!-- How the system is built, shipped, monitored, and maintained over its lifecycle. -->

## 5.1 Runtime & Language Fit
- **Languages**: Per component — justify each choice.
- **Runtime**: (Node.js / Python / JVM / .NET / Go / Rust / etc.)
- **Runtime vs. Compile-Time Concerns**: Type checking, AOT compilation, startup time, etc.

## 5.2 Deployment
- **Environments**: (local / dev / staging / production)
- **Infrastructure**: (cloud provider / containerization / serverless / bare metal)
- **Container Strategy**: Docker images, base images, multi-stage builds.
- **Orchestration**: (Kubernetes / ECS / Docker Compose / none)
- **Deployment Pattern**: (blue-green / canary / rolling / recreate)
- **Rollback Procedure**: Steps to revert a failed deployment.

## 5.3 CI/CD & Release Management
- **Pipeline**: (GitHub Actions / GitLab CI / Jenkins / etc.)
- **Branching Strategy**: (trunk-based / GitFlow / feature branches)
- **Automated Gates**: Lint → test → build → security scan → deploy.
- **Release Versioning**: (semver / calver / commit hash)
- **Contract Management**: How API or schema changes are versioned and communicated.

## 5.4 Quality Assurance & Testing
| Level | Framework | Scope | Coverage Target |
|-------|-----------|-------|-----------------|
| Unit | | | |
| Integration | | | |
| End-to-End | | | |
| Performance | | | |
| Security | | | |

- **Test Data Management**: Creation, fixtures, factories, cleanup approach.
- **CI Integration**: Where tests run in the pipeline.

## 5.5 Observability & Monitoring
- **Logging**:
  - Framework: (winston / pino / bunyan / structlog / serilog / etc.)
  - Unified or per-language?
  - Structured logging format.
  - Log levels per environment (dev: debug, prod: info+).
  - Centralized aggregation: (ELK / CloudWatch / Datadog / etc.)
- **Metrics**: What is measured? (latency, error rate, saturation, traffic)
- **Tracing**: (OpenTelemetry / Jaeger / X-Ray / none)
- **Alerting**: Thresholds and escalation policies.
- **Health Checks**: Endpoints and probes (liveness / readiness).

## 5.6 Error Handling & Resilience
- **Error Handling Strategy**: (try-catch / Result monad / error boundaries / asserts)
- **Resilience Patterns**: (retries with backoff / circuit breakers / bulkheads / fallbacks)
- **Graceful Degradation**: What happens when a dependency is down?

## 5.7 Security Practices
- **Authentication Strategy**: (OAuth2 / JWT / session / API key / SSO / MFA)
- **Authorization Model**: (RBAC / ABAC / ACL / policy-based)
- **Threat Model**: Key threats and mitigations aligned to OWASP Top 10.
- **Data Protection**: Encryption at rest, in transit, PII masking, retention policies.
- **Dependency Security**: CVE scanning, dependency audit frequency, lockfile policy.

## 5.8 Administration & Operations
- **Operational Runbooks**: Key procedures for common incidents.
- **Backup & Recovery**: Strategy and RPO/RTO targets.
- **Scaling Procedures**: Manual and automatic scaling triggers.
- **On-Call**: Rotation and escalation paths.

---

# Dependencies & External Integrations

## External Systems
- **EXT-001**: [External system name] — [Purpose and integration type]

## Third-Party Services
- **SVC-001**: [Service name] — [Required capabilities and SLA requirements]

## Infrastructure Dependencies
- **INF-001**: [Infrastructure component] — [Requirements and constraints]

## Data Dependencies
- **DAT-001**: [External data source] — [Format, frequency, and access requirements]

## Compliance Dependencies
- **COM-DEP-001**: [Regulatory or compliance requirement] — [Impact on implementation]

---

# Package Dependencies
<!-- List all dependencies to install, structured per subsystem. Verify versions for compatibility and CVEs. -->

### [subsystem-name]
| Package | Version | Purpose | CVE Check |
|---------|---------|---------|-----------|
| | | | ✅ / ⚠️ |

---

# Appendix A: Glossary
| Term | Definition |
|------|-----------|
| | |

# Appendix B: References
| Document | Location | Link |
|----------|----------|------|
| | | |

# Appendix C: Decision Log
<!-- Record key design decisions with context for future reference. -->
| ID | Decision | Context | Alternatives Considered | Date |
|----|----------|---------|------------------------|------|
| ADR-001 | | | | |
```
```

## Selecting the right template

| The user asks for | Template | Format |
| --- | --- | --- |
| html format of the scope or hld | [references/hld-report-template.html](references/hld-report-template.html) | HTML |
| hld, update agents.md, markdown report | template above here in this file | Markdown |
| Restructure existing code without changing behavior, pay down tech debt, phased cleanup, refactor plan | [references/refactor-template.md](references/refactor-template.md) | Markdown |
| One decision and its alternatives | `architecture-decision-records` skill | Markdown |
| A pitch or a commercial offer | `proposal` skill | Markdown |