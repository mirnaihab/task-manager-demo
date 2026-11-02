---
name: clean-code
description: Universal coding standards, code best practices for implementation and refactoring, and clean code principles to ensure readability, maintainability, security, and scalability of codebases. Use this skill to write or review any code. Analyze the rules here carefully and apply them to your code review.
---

<checklist id="clean-code-rules" version="2.0">

<section id="general-rules" title="General Rules" priority="critical">
  <preamble>
    Code is clean if it can be understood easily by everyone on the team.
    Clean code can be read and enhanced by a developer other than its original author.
    The fundamental measure: is this code easy to change?
  </preamble>

  <rule id="GEN-01"  summary="Keep it Super Simple">
    Simpler is always better. Reject unnecessary complexity. The simplest solution that works is the best solution. Avoid over-engineering but respect the best practices and patterns. Apply YAGNI and DRY principles to avoid over-engineering. 
  </rule>

  <rule id="GEN-02"  summary="Easy to change">
    The fundamental rule of design is code must be easy to change. This is achieved by
    decoupling and abstractions. Ask: "Does this design make it easier or harder to
    change the code in the future?"
  </rule>

  <rule id="GEN-03"  summary="Interface First">
    Generate and always favor the interface or abstract base class BEFORE generating concrete implementations.
  </rule>

  <rule id="GEN-04"  summary="Pattern Consistency">
    Apply design patterns consistently across the codebase. If a pattern is used in one
    part, use it in similar situations elsewhere to maintain a consistent design language.
  </rule>

  <rule id="GEN-05"  summary="No commented-out code">
    Don't comment out code. Just remove it. Version control preserves history.
  </rule>

  <rule id="GEN-06"  summary="Comments explain intent, not description">
    Use comments as explanation of intent or code clarification, never as description of
    what the code literally does.
  </rule>

  <rule id="GEN-07"  summary="Unified interfaces for similar classes">
    Alternative classes with different interfaces should be unified. Classes that perform
    similar responsibilities but expose different method signatures must share an interface.
  </rule>

  <rule id="GEN-08"  summary="Async failure handling">
    Failures on async code must have a fallback or graceful degradation strategy. Never let
    an async failure crash the system or leave it in an inconsistent state.
  </rule>

  <rule id="GEN-09"  summary="No optimistic updates without rollback">
    Optimistic fetching or DB updates must have rollback when not in a transactional context.
  </rule>
  
  <rule id="GEN-010"  summary="Use design patterns when appropriate">
    Apply design patterns judiciously to solve recurring problems. Avoid using patterns unnecessarily, as this can lead to over-engineering.

    Patterns should be used when they provide clear benefits in maintainability, flexibility, or readability. Always document the intent of the pattern and how it is applied.

    Example patterns: SOLID principles, behavioral (eg: strategy), creational (eg: factory), structural (eg: adapter), and architectural patterns.
  </rule>

  <rule id="GEN-011"  summary="Preferred architecture patterns">
    For each monolithic module/subsystem in the codebases, prefer a layered architecture that separates concerns into distinct layers (e.g., presentation, business logic, data access). This promotes maintainability and testability. Avoid mixing responsibilities across layers.

    Prefer using modular monolith approach for monolithic applications, where each module is self-contained and has clear boundaries. This allows for easier refactoring and potential future migration to microservices if needed.

    Prefer monorepo architecture for multi-module projects, where all modules are stored in a single repository. This simplifies dependency management and promotes code sharing across modules.

    System architecture like microservices, event-driven, or serverless should be defined by the user in the implementation plan. If not defined, default to a modular monolith architecture with layered design.
  </rule>

   <rule id="OOP-01"  summary="Favor Composition over Inheritance">
    Use composition to combine behaviors dynamically at runtime. Avoid deep inheritance
    trees. Use delegation where appropriate to reuse behavior without breaking encapsulation.
  </rule>

  <rule id="DES-01"  summary="Configurable data at high levels">
    Keep configurable data at high levels of the architecture. Do not import environment variables into low-level modules. Avoid hard-coded values in low-level code. Use dependency injection or configuration objects to pass data down the layers.
  </rule>
 
  <rule id="FUN-01"  summary="No side effects from functions">
    Avoid introducing side effects from functions where possible. Functions/methods should either return something OR do something, and be named accordingly. Never both. If a side effect is detected, a rollback mechanism is implemented, such as for database delete/insert flows.
  </rule>
 
  <rule id="SMELL-01"  summary="No flag arguments">
    Simple interfaces, APIs and function signatures are easier to understand and use. **Avoid** flag arguments, exposing many fine-grained configuration options, and complex method signatures. Favor high-level abstractions and clear, simple entry points.
  </rule>
  
  <rule id="SMELL-02"  summary="No anemic models">
    **Avoid** anemic models, useless middleman or wrapper classes that add a layer for the sake of layering. if the system is small, over layering does not add value. Avoid unnecessary abstractions that do not provide clear benefits in maintainability, flexibility, or readability depending on project complexity.
  </rule>

  <rule id="SMELL-03"  summary="Message Chains (Train Wrecks)">
    If a caller navigates through multiple objects to get a result, it is a sign of tight coupling and violation of the Law of Demeter. Avoid message chains like `a.b().c().d()`. Instead, provide higher-level methods that encapsulate the chain. Deep Nesting in general (>5 levels) is a code smell and should be avoided.
  </rule>

  <rule id="SMELL-16"  summary="Shotgun Surgery">
    A single logical change requires small edits scattered across many unrelated classes.
    Look for: a feature toggle or config change touching five or more files.
    Fix: consolidate the scattered logic into one location.
  </rule>

  <rule id="LOG-01"  summary="No silent failures">
    Avoid silent failures. Ensure errors are logged with sufficient context to facilitate
    debugging and maintenance.
  </rule>
 
  <rule id="LOG-02"  summary="Logging mechanism">
    Use the following log message template if possible. prioritise using logging frameworks.
      {
        "timestamp": "2026-05-24T12:48:44.123Z",
        "type": "info" | "debug" | "warn" | "error"| "event_event_name",
        "message": "File uploaded",
        "correlationId": "asb3c4d5-e6f7-8901-abcd-ef1234567890",
        "context": {
          "module": "files.upload",
          "function": "upload_to_blob",
          "userId": "usr_12345",
          "projectId": "proj_123",
          "resourceId": "file_67890",
          "action": "upload",
          "metadata":{
              "key":"value"
          }
        },
        "error": null | {
          type:
          level: "low" | "mid" "high"
          message:
          stack:
          code:
        }
      }


      # Logging Quick Reference

      ## Every log line must have
      - timestamp (automatic)
      - level (debug | info | warn | error | fatal)
      - service (automatic)
      - correlationId (from middleware)
      - message (human-readable, past tense, greppable)

      ## Add when available
      - context.module, context.function
      - context.userId, context.resourceId, context.action
      - duration_ms (for timed operations)
      - error.type, error.message, error.stack (on failures)

      ## Message format
      [Entity] [past-tense verb] [outcome]
        "Order created successfully"
        "Payment processing failed: gateway timeout"

      ## Do not
      - Log sensitive data (passwords, tokens, PII)
      - Duplicate structured fields in the message
      - Use warn for expected behavior
      - Use error if the system recovered
      - Split one event across multiple log lines

  </rule>

  <rule id="LOG-03"  summary="Correct log levels used consistently">
    Use info, debug, warning, error, and critical log levels appropriately in every
    class and function. Consider implementing a centralized error handling mechanism
    (e.g., global exception handler) for consistent error responses.
  </rule>

  <rule id="SEC-01"  summary="CWE-20: Improper Input Validation" owasp="A05">
      All external input must be validated against strict allowlists. Use validation
      libraries (zod, pydantic, etc.).
      <example lang="python">assert username.isalnum()</example>
    </rule>

  <rule id="SEC-02"  summary="Security">
      Security is a first-class concern. All code must be reviewed for security vulnerabilities. Follow OWASP Top 10:2025 and other relevant security guidelines. Avoid hard-coded secrets,passing user input to db/system directly,  use secure storage, and implement proper centralized authentication and authorization mechanisms with zero trust all the time.
  </rule>
</section>

</checklist>


