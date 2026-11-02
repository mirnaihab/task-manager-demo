---
name: requirements-clarification
description: think through ideas, investigate problems, clarify requirements, get more information before planning or implementation. Use proactively for any user request.
---

Enter clarify mode. Think deeply. Visualize freely. Follow the conversation wherever it goes.

**IMPORTANT: clarify mode is for thinking, not implementing.** You may read files, search code, and investigate the codebase, but you must NEVER write code or implement features.

---

## The Behavior

- **Curious, not prescriptive** - Ask questions that emerge naturally, don't follow a script
- **Open threads, not interrogations** - Surface multiple interesting directions and let the user follow what resonates. Don't funnel them through a single path of questions.
- **Visual** - Use ASCII diagrams liberally when they'd help clarify thinking
- **Adaptive** - Follow interesting threads, pivot when new information emerges
- **Patient** - Don't rush to conclusions, let the shape of the problem emerge
- **Grounded** - clarify the actual codebase when relevant, don't just theorize

---

## What You Might Do

Depending on what the user brings, you might:

- **clarify the problem space**
- **Investigate the codebase**
- **Compare options**
- **Surface risks and unknowns**
- **Visualize**
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

## Handling Different Entry Points

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

---

## Discovery Workflow

1. Explore the current project context: files, docs, tests, and nearby patterns.
2. Assess scope early. If the request is really multiple projects, decompose it before drilling into details.
3. Ask clarifying questions one at a time.
4. Prefer multiple-choice questions when possible.
5. Propose 2-3 approaches with trade-offs and a recommendation.
6. Get approval on the direction before writing the final artifact.
7. Ask the user to review the written artifact before transitioning to detailed planning.

---

## Guardrails

- **Don't implement** - Never write code or implement features. Creating OpenSpec artifacts is fine, writing application code is not.
- **Don't fake understanding** - If something is unclear, dig deeper
- **Don't rush** - Discovery is thinking time, not task time
- **Don't force structure** - Let patterns emerge naturally
- **Don't auto-capture** - Offer to save insights, don't just do it
- **Do visualize** - A good diagram is worth many paragraphs
- **Do explore the codebase** - Ground discussions in reality
- **Do question assumptions** - Including the user's and your own

----

# Template for clarification mode

## instructions for how to use the template
- **Priority 1**. Depending on the complexity of the user request based on size, if the request is simple, you can select only a subset of the most important questions here and keep clarification light. complex requests require a full set of questions to be answered.
     - complex means:
         - multiple components,
         - multiple user stories,
         - multiple features, 
         - multiple user roles, 
         - multiple workflows, 
         - multiple integrations, 
         - multiple edge cases, 
         - multiple non-functional requirements, 
         - multiple technologies,
         - or any combination of the above.
- **Priority 2**. Do not skip any relevant question. If a question is not applicable. 
- **Priority 3**. If a question is already answered in the user prompt, pre-fill it and confirm with the user.
- **Priority 4**. If a question can be answered by investigating the current directory, codebase, or web search, do so and record the source of the answer.
- **Priority 5**. Provide sensible defaults or suggestions for all questions. Your defaults must be proportional to the project context — do not recommend Kafka when a simple REST API suffices.

## Template structure:
<!--
  PURPOSE: Gather a thorough understanding of "What are we building?" and "How are we
  building it?" before any planning or implementation begins.

  USAGE:
  1. Walk through each <section> in order: Business Requirements → High Level Design → Low Level Design.
  2. For every <question>, fill answer="" with the gathered information.
  3. Use source="" to record HOW the answer was obtained: "user", "codebase", "inferred", "web-search".
  4. Set status="" to "answered", "pending", or "n/a" for each question.
  5. You CANNOT proceed to planning until all applicable questions have status="answered" or status="n/a".
  6. Provide sensible defaults or suggestions for all questions. Your defaults must be proportional
     to the project context — do not recommend Kafka when a simple REST API suffices.
  7. If the user prompt already covers a question, pre-fill it and confirm with the user.
  8. Always summarize your understanding back to the user and get confirmation before planning.

  SCOPE RULE: If building from scratch, ALL answers are mandatory either from user or search. For bug fixes, patches,
  or small features, use judgement — mark inapplicable questions as status="n/a" with justification.
-->

<requirements-template id="clarification" version="2.0">

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- PART 1: BUSINESS REQUIREMENTS — "What are we building and why?"           -->
<!-- Understanding the problem space, users, goals, and success criteria.      -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<section id="business-requirements" title="Business Requirements" goal="What are we building and why?">

  <!-- ── Project Context ── -->
  <group id="project-context" title="Project Context">
    <preamble>
      Understanding the project context ensures requirements are relevant and
      aligned with overall project goals.
    </preamble>
    <question id="BIZ-01" status="" source="" answer=""
      prompt="What is the project about? Describe the product, its domain, and the problem it solves." />
    <question id="BIZ-02" status="" source="" answer=""
      prompt="Who are the target users and what are their needs and pain points?" />
    <question id="BIZ-03" status="" source="" answer=""
      prompt="What is the business value or motivation behind this request? Why now?" />

  </group>

  <!-- ── Users & Personas ── -->
  <group id="users" title="Users and Personas">
    <preamble>
      Understanding the target users ensures requirements are user-centric and
      focused on solving real problems.
    </preamble>
    <question id="BIZ-05" status="" source="" answer=""
      prompt="How many users are typically using the system? What is the expected concurrent user count?" />
    <question id="BIZ-06" status="" source="" answer=""
      prompt="Do we need user roles? If so, what roles exist and what permissions does each have?" />
    <question id="BIZ-07" status="" source="" answer=""
      prompt="What are the user personas? Describe each persona's goals, behaviors, and technical proficiency." />
    <question id="BIZ-08" status="" source="" answer=""
      prompt="What should the user be able to do? List the core user capabilities and workflows." />
  </group>

  <!-- ── Scope & Classification ── -->
  <group id="scope" title="Scope and Classification">
    <preamble>
      Classifying the request type and scope prevents over- or under-engineering.
    </preamble>
    <question id="BIZ-09" status="" source="" answer=""
      prompt="What is the scope of the request? (bug fix | refactor | POC | MVP | production feature | small feature | other)" />
    <question id="BIZ-10" status="" source="" answer=""
      prompt="What are the areas of focus? (frontend | backend | API | database | infrastructure | full-stack | other)" />
    <question id="BIZ-11" status="" source="" answer=""
      prompt="Are there hard deadlines, budget constraints, or compliance requirements that bound the solution?" />
  </group>

  <!-- ── Functional Requirements ── -->
  <group id="functional-requirements" title="Functional Requirements">
    <question id="BIZ-12" status="" source="" answer=""
      prompt="What are the main behaviors, scenarios, or use cases?" />
    <question id="BIZ-13" status="" source="" answer=""
      prompt="What are the key scenarios? what can the user do? and what are the goals or success criteria?" />
    <question id="BIZ-14" status="" source="" answer=""
      prompt="How will the user(s) be able to achieve their scenarios? what would they do?" />
    <question id="BIZ-15" status="" source="" answer=""
      prompt="List all functional requirements — what the system MUST do." />
    <question id="BIZ-16" status="" source="" answer=""
      prompt="Are there any edge cases, boundary conditions, or negative scenarios that must be handled?" />
  </group>

  <!-- ── Non-Functional Requirements ── -->
  <group id="non-functional-requirements" title="Non-Functional Requirements">
    <question id="BIZ-17" status="" source="" answer=""
      prompt="What are the non-functional requirements? (performance, reliability, availability, maintainability, portability)" />
    <question id="BIZ-18" status="" source="" answer=""
      prompt="What are the scalability requirements? Must the system handle 10x, 100x, or 1000x current load?" />
    <question id="BIZ-19" status="" source="" answer=""
      prompt="Are there specific performance requirements? (response time SLAs, throughput targets, latency budgets)" />
    <question id="BIZ-20" status="" source="" answer=""
      prompt="Are there specific security, compliance, or data privacy requirements? (GDPR, HIPAA, SOC2, etc.)" />
    <question id="BIZ-21" status="" source="" answer=""
      prompt="Are there accessibility requirements? What WCAG level must be met?" />
  </group>

  <!-- ── Bug / Error Context (conditional) ── -->
  <group id="bug-context" title="Bug and Error Context" condition="Populate ONLY if the request is to fix a bug or solve an error.">
    <question id="BUG-01" status="" source="" answer=""
      prompt="What is the impact of the bug or error? (severity, affected users, business impact)" />
    <question id="BUG-02" status="" source="" answer=""
      prompt="When does the error occur? Under what conditions or triggers?" />
    <question id="BUG-03" status="" source="" answer=""
      prompt="What are the steps to reproduce the bug or error?" />
    <question id="BUG-04" status="" source="" answer=""
      prompt="What are the expected vs. actual behaviors?" />
    <question id="BUG-05" status="" source="" answer=""
      prompt="What are the error messages or logs associated with the bug?" />
    <question id="BUG-06" status="" source="" answer=""
      prompt="What are the potential root causes? (Use code search to investigate.)" />
    <question id="BUG-07" status="" source="" answer=""
      prompt="What are the potential solutions or workarounds? List risks and trade-offs for each." />
  </group>

</section>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- PART 2: HIGH LEVEL DESIGN — "How does the system fit together?"           -->
<!-- Architecture, component topology, data flow, and technology selection.    -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<section id="high-level-design" title="High Level Design" goal="How does the system fit together?">

  <!-- ── System Architecture ── -->
  <group id="system-architecture" title="System Architecture">
    <question id="HLD-01" status="" source="" answer=""
      prompt="What are the main components of the system? (frontend, backend, database, API gateway, message queue, etc.) Describe each component's responsibility." />
    <question id="HLD-02" status="" source="" answer=""
      prompt="How do the components interact? Describe the data flow between them. Draw an ASCII diagram if helpful." />
    <question id="HLD-03" status="" source="" answer=""
      prompt="What architectural pattern does the system follow or should follow? (MVC, Clean Architecture, Hexagonal, Microservices, Modular Monolith, Serverless, etc.) Justify the choice." />
    <question id="HLD-04" status="" source="" answer=""
      prompt="Should we follow a functional or object-oriented paradigm for the implementation? Or a mix? Justify." />
    <question id="HLD-05" status="" source="" answer=""
      prompt="Are there existing codebase conventions, patterns, or architectural decisions already in place that must be respected?" />
    <question id="HLD-06" status="" source="" answer=""
      prompt="should the architecture be layered? if so, how many layers and what are their boundaries?" />
  </group>

  <!-- ── Technology Stack ── -->
  <group id="technology-stack" title="Technology Stack">
    <question id="HLD-07" status="" source="" answer=""
      prompt="What programming languages are used or should be used for each component? should they be statically typed?" />
    <question id="HLD-08" status="" source="" answer=""
      prompt="What frameworks are used or should be used? (e.g., Express/Fastify/NestJS for Node; React/Angular/Vue for frontend; state management; form handling; testing frameworks) and what are their versions?" />
    <question id="HLD-09" status="" source="" answer=""
      prompt="What ORM or data access layer is used? (e.g., Sequelize, TypeORM, Prisma, Drizzle, SQLAlchemy, etc.) and what are their versions?" />
    <question id="HLD-10" status="" source="" answer=""
      prompt="What data storage technologies are used? (relational: PostgreSQL/MySQL; NoSQL: MongoDB/Cassandra; in-memory: Redis; file: S3; vector: Pinecone/pgvector)" />
    <question id="HLD-11" status="" source="" answer=""
      prompt="What logging framework is used? Is it unified across services or per-language? (e.g., winston, pino, bunyan, structlog, serilog)" />
  </group>

  <!-- ── Communication & Integration ── -->
  <group id="communication" title="Communication and Integration">
    <question id="HLD-12" status="" source="" answer=""
      prompt="What communication protocols are used between system components? (HTTP/REST, GraphQL, gRPC, WebSockets, AMQP, etc.)" />
    <question id="HLD-13" status="" source="" answer=""
      prompt="Is the communication synchronous or asynchronous? Where is each used and why?" />
    <question id="HLD-14" status="" source="" answer=""
      prompt="What is the integration strategy for third-party services? (e.g., LLM providers, embedding models, payment gateways — installed locally or connected via API keys in env files?)" />
    <question id="HLD-15" status="" source="" answer=""
      prompt="Are there any event-driven or message-queue patterns needed? (e.g., pub/sub, CQRS, event sourcing)" />
  </group>

  <!-- ── Infrastructure & Deployment ── -->
  <group id="infrastructure" title="Infrastructure and Deployment">
    <question id="HLD-16" status="" source="" answer=""
      prompt="What are the deployment environments? (cloud provider: AWS/Azure/GCP; containerization: Docker/Kubernetes; serverless; bare metal; local-only)" />
    <question id="HLD-17" status="" source="" answer=""
      prompt="What are the necessary configurations and environment setups needed? (env vars, secrets management, config files) and what are the dependancies & their versions?" />
    <question id="HLD-18" status="" source="" answer=""
      prompt="What CI/CD pipeline exists or should be created? (GitHub Actions, GitLab CI, Jenkins, etc.)" />
    <question id="HLD-19" status="" source="" answer=""
      prompt="What is the branching and release strategy? (trunk-based, GitFlow, feature branches, etc.)" />
  </group>

  <!-- ── Security Architecture ── -->
  <group id="security-architecture" title="Security Architecture">
    <question id="HLD-20" status="" source="" answer=""
      prompt="What is the authentication strategy? (OAuth2, JWT, session-based, API keys, SSO, MFA)" />
    <question id="HLD-21" status="" source="" answer=""
      prompt="What is the authorization model? (RBAC, ABAC, ACL, policy-based)" />
    <question id="HLD-22" status="" source="" answer=""
      prompt="Are there specific security requirements or threat models that must be addressed? (OWASP Top 10 alignment, penetration testing, secrets rotation)" />
    <question id="HLD-23" status="" source="" answer=""
      prompt="How is sensitive data handled? (encryption at rest, in transit, PII masking, data retention policies)" />
  </group>

  <!-- ── Frontend / UI Architecture (conditional) ── -->
  <group id="frontend-architecture" title="Frontend and UI Architecture" condition="Populate ONLY if the request involves frontend or UI/UX work.">
    <question id="UI-01" status="" source="" answer=""
      prompt="Is there a design system in place? Check for tailwind.config, theme files, DESIGN.md, or similar." />
    <question id="UI-02" status="" source="" answer=""
      prompt="What CSS framework is being used? (Tailwind, Bootstrap, Material UI, styled-components, CSS Modules, etc.)" />
    <question id="UI-03" status="" source="" answer=""
      prompt="Should we use a component library? (shadcn/ui, Radix, Ant Design, Chakra UI, etc.)" />
    <question id="UI-04" status="" source="" answer=""
      prompt="What are the primary and secondary colors? Suggest sensible defaults based on project context.">
      <rule>This question is essential for any UI/frontend related work, give sensible suggestions</rule>
      </question>
    <question id="UI-05" status="" source="" answer=""
      prompt="How many pages/views should exist? Describe the page hierarchy or sitemap." />
    <question id="UI-06" status="" source="" answer=""
      prompt="What is the primary mode of user interaction? (forms, modals, page navigations, drag-and-drop, CLI, etc.)" />
    <question id="UI-07" status="" source="" answer=""
      prompt="Is the design more minimalist or more complex/feature-rich? What is the visual tone?" />
    <question id="UI-08" status="" source="" answer=""
      prompt="Should we create a design system? Reference the frontend-patterns skill and its design-system.md." />
    <question id="UI-09" status="" source="" answer=""
      prompt="What is the responsive design strategy? (mobile-first, desktop-first, specific breakpoints)" />
    <question id="UI-10" status="" source="" answer=""
      prompt="What are the key external libraries to be used? example: for form handling react-hook-form/formik, for charts recharts/d3 ...etc" />
  </group>

</section>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- PART 3: LOW LEVEL DESIGN — "How do we build each piece?"                  -->
<!-- Dependencies, component internals, data models, APIs, and constraints.    -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<section id="low-level-design" title="Low Level Design" goal="How do we build each piece?">

  <!-- ── Dependencies & Versions ── -->
  <group id="dependencies" title="Dependencies and Versions">
    <preamble>
      For each system component, identify dependencies. Verify latest versions,
      cross-compatibility, and known security vulnerabilities.
    </preamble>
    <question id="LLD-01" status="" source="" answer=""
      prompt="For each system component, list all dependencies (libraries, packages, SDKs)." />
    <question id="LLD-02" status="" source="" answer=""
      prompt="What are the exact versions of each dependency? Are they the latest stable? Are they compatible with each other?">
      <rule>Check the latest versions of each dependency. Verify compatibility. Check for known CVEs. Address vulnerabilities in the requirements and plan.</rule>
      <rule>Validate the versions of installable dependencies with the user before finalizing.</rule>
    </question>
    <question id="LLD-03" status="" source="" answer=""
      prompt="Are there any dependency constraints? (version locks, license restrictions, organizational policies on approved packages)" />
  </group>

  <!-- ── Data Model & Storage ── -->
  <group id="data-model" title="Data Model and Storage Design">
    <question id="LLD-04" status="" source="" answer=""
      prompt="What are the core entities/models? Describe their attributes, types, and relationships." />
    <question id="LLD-05" status="" source="" answer=""
      prompt="What is the database schema? Include tables, columns, indexes, and constraints. Draw an ERD if helpful." />
    <question id="LLD-06" status="" source="" answer=""
      prompt="What is the data migration strategy? (schema versioning, rollback plan, seed data)" />
    <question id="LLD-07" status="" source="" answer=""
      prompt="What caching strategy is needed? (cache layers, invalidation policy, TTL, cache-aside vs. write-through)" />
    <question id="LLD-08" status="" source="" answer=""
      prompt="What is the data validation strategy at each layer? (input validation, schema validation, business rule validation)" />
  </group>

  <!-- ── API & Interface Design ── -->
  <group id="api-design" title="API and Interface Design">
    <question id="LLD-09" status="" source="" answer=""
      prompt="What API endpoints or interfaces need to be created or modified? List them with HTTP methods, paths, request/response shapes." />
    <question id="LLD-10" status="" source="" answer=""
      prompt="What is the API versioning strategy? (URL path, header-based, query param)" />
    <question id="LLD-11" status="" source="" answer=""
      prompt="What error response format and status code conventions should be followed?" />
    <question id="LLD-12" status="" source="" answer=""
      prompt="What rate limiting, pagination, and filtering strategies apply to the APIs?" />
    <question id="LLD-13" status="" source="" answer=""
      prompt="Are there internal interfaces or contracts between modules/services or layers that need to be defined?" />
  </group>

  <!-- ── Component Internals ── -->
  <group id="component-internals" title="Component Internals">
    <question id="LLD-14" status="" source="" answer=""
      prompt="For each component, what are the internal modules, classes, or functions? Describe their responsibilities." />
    <question id="LLD-15" status="" source="" answer=""
      prompt="What design patterns should be applied within each component? (Repository, Strategy, Factory, Observer, etc.) Justify each." />
    <question id="LLD-16" status="" source="" answer=""
      prompt="What is the error handling and resilience strategy within each component? (retries, circuit breakers, fallbacks, dead letter queues)" />
    <question id="LLD-17" status="" source="" answer=""
      prompt="What is the concurrency model? Are there shared resources that need synchronization? (mutexes, semaphores, queues, actor model)" />
  </group>

  <!-- ── Testing Strategy ── -->
  <group id="testing" title="Testing Strategy">
    <question id="LLD-18" status="" source="" answer=""
      prompt="What testing frameworks and tools are used or should be used? (Jest, Mocha, Pytest, Cypress, Playwright, etc.)" />
    <question id="LLD-19" status="" source="" answer=""
      prompt="What types of tests are required? (unit, integration, e2e, contract, performance, security)" />
    <question id="LLD-20" status="" source="" answer=""
      prompt="What is the minimum acceptable test coverage? Are there specific critical paths that MUST have tests?" />
    <question id="LLD-21" status="" source="" answer=""
      prompt="What is the test data strategy? (fixtures, factories, mocks, test databases, seeding)" />
  </group>

  <!-- ── Observability ── -->
  <group id="observability" title="Observability and Monitoring">
    <question id="LLD-22" status="" source="" answer=""
      prompt="What logging strategy should be followed? (structured logging, log levels per environment, centralized log aggregation)" />
    <question id="LLD-23" status="" source="" answer=""
      prompt="What monitoring and alerting is needed? (health checks, metrics, dashboards, on-call)" />
    <question id="LLD-24" status="" source="" answer=""
      prompt="What tracing or profiling is needed? (distributed tracing with OpenTelemetry, APM tools)" />
  </group>

  <!-- ── Constraints & Risks ── -->
  <group id="constraints-risks" title="Constraints and Risks">
    <question id="LLD-25" status="" source="" answer=""
      prompt="For each system component, what are the known constraints? (memory limits, CPU budget, network bandwidth, cold start times)" />
    <question id="LLD-26" status="" source="" answer=""
      prompt="What are the known technical risks? List each with likelihood, impact, and mitigation strategy." />
    <question id="LLD-27" status="" source="" answer=""
      prompt="Are there any known technical debt items that this work will interact with or should address?" />
    <question id="LLD-28" status="" source="" answer=""
      prompt="What are the rollback and disaster recovery procedures if the deployment fails?" />
  </group>

</section>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- COMPLETION GATE                                                           -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<completion-gate> 
  1. Every question in business-requirements must have status="answered" or status="n/a".
  2. Every question in high-level-design must have status="answered" or status="n/a".
  3. Every question in low-level-design must have status="answered" or status="n/a".
  4. All status="n/a" questions must include a justification in the answer field.
  5. Summarize the full understanding back to the user and receive explicit confirmation.

  TOTAL QUESTIONS: 70 (Business: 23 | HLD: 24 | LLD: 23)
  Conditional groups (bug-context, frontend-architecture) are included in counts but
  may be marked n/a in bulk when their condition is not met.
</completion-gate>

</requirements-template>
