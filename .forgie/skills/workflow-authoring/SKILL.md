---
name: workflow-authoring
description: Use when the user asks to create, change, list, run or debug a forgie workflow, or describes a job with several steps or several agents, for example "scope it, then build and review until it is clean, then document it". Turns a request in plain words into a typed workflow file, checks it, and runs it.
---

# Workflow authoring

The workflow engine runs agents. You write the workflow, the engine decides the order, the
retries, the loops and the recovery. You never schedule agents yourself.

The path is always the same.

```
request in plain words  ->  a typed workflow file  ->  the engine runs the agents
```

---

## 1 · Decide what to do first

| The user asks | Do this |
|---|---|
| "what can you run?" | Call `workflow_list`. Report the names. |
| A job that matches a workflow you already have | Go to section 2. |
| A job with steps or agents that no workflow covers | Go to section 3. |
| To change how an existing job runs | Edit that workflow file, then section 4 and section 5. |
| "how is my run going?" | Call `workflow_status` with the run id. |
| To stop work | Call `workflow_cancel` with the run id. |

Call `workflow_list` before you write a new file. A workflow may already do the job.

Do not write a workflow for a single question or a single lookup. Answer those yourself.

---

## 2 · Run a workflow that exists

1. Call `workflow_start` with the workflow name and the user request, unchanged.
2. Read the status in the result.
3. Follow section 6 for a `waiting` or `failed` run.
4. Report the outcome, the run id and the artifacts.

---

## 3 · Write a new workflow

Follow these steps in order.

1. Name the outcome in one line. That becomes the `description`.
2. Choose a workflow name in kebab-case, for example `api-migration`.
3. List the steps the user described. One step is one agent doing one job.
4. Pick an agent for each step from section 8.
5. Choose the shape with section 7.
6. Write the file to `.forgie/workflows/<name>.ts` in the user project.
7. Check it with section 5. Fix every problem before you run anything.
8. Run it with section 2.

There is no registration step. The engine imports every file in that folder and keeps every
exported workflow.

### The file

```ts
import { agent, chain, loop, workflow } from "forgie-workflow-engine"
import { hasOpenFindings, not } from "forgie-workflow-engine"

export const apiMigration = workflow({
  name: "api-migration",
  description: "Plan the api migration, apply it, and review it until it is clean.",
  root: chain(
    agent("planner", "Plan the migration to the new api. List every call site."),
    loop(
      agent("builder", "Apply the next part of the approved plan."),
      agent("analyser", "Review the change for broken call sites."),
      { maxIterations: 3, until: not(hasOpenFindings("medium")) },
    ),
    agent("documentation", "Record the migration and update the reference."),
  ),
})
```

Rules for the file:

1. Export the workflow with `export const`. The engine only keeps exported workflows.
2. Copy the import line from another file in the same folder. Inside the engine repository the
   import is a relative path such as `../../src/index.ts`. In a user project it is the package
   name `forgie-workflow-engine`.
3. Do not name the file `*.test.ts`, `*.spec.ts` or `test-helpers.ts`. The engine skips those.
4. Give the workflow a name no other workflow uses. A name that matches a built in workflow
   replaces it, which is how you tailor `feature` or `implement` for a team.

---

## 4 · The authoring contract

### Building blocks

| Call | Returns | Meaning |
|---|---|---|
| `agent(name, task, config?)` | a step | One agent runs one task and returns one report. |
| `delegate(workflowName, task, config?)` | a step | A whole workflow runs as one step, with its own run state. |
| `chain(...steps, config?)` | a group | Run in order. Each step reads the results of the steps before it. |
| `parallel(...steps, config?)` | a group | Run together, then join. |
| `loop(...steps, config)` | a group | Repeat the body until a condition stops it or the cap ends it. |
| `workflow({ ... })` | a workflow | Wraps one root step or group. |

Groups nest. A `chain` may hold a `loop`, and a `loop` may hold a `parallel`.

### Types

```ts
workflow({
  name: string          // required, kebab-case, unique
  description?: string  // one line, shown by workflow_list
  version?: number      // default 1
  root: Step            // required, the only entry
  limits?: {
    maxSteps?: number       // default 200, counts every agent call in the run
    maxConcurrency?: number // default 4
  }
})

type CallConfig = {        // agent() and delegate()
  id?: string              // kebab-case, unique in the workflow
  execution?: "hidden" | "interactive"  // default "hidden"
  context?: "curated" | "fork"       // default "curated"
  timeoutMs?: number       // default 600000, time without any sign of life
  pulseMs?: number         // default 120000, how often the engine checks a running step. 0 turns it off
  pulseNudges?: number     // default 2, quiet pulses that get a nudge to the agent
  maxDurationMs?: number   // default 14400000, hard ceiling for one attempt
  retries?: number         // default 0, maximum 10
  retryBackoffMs?: number  // default 1000
  model?: string           // "provider/model", defaults to the agent model
}

type GroupConfig = {       // chain()
  id?: string
  onFailure?: "fail" | "continue"   // default "fail"
}

type ParallelConfig = GroupConfig & {
  maxConcurrency?: number  // default 4
}

type LoopConfig = GroupConfig & {
  maxIterations: number    // required, 1 or more
  until?: Condition        // checked after each pass
  while?: Condition        // checked before each pass
}
```

### What the options mean

| Option | Use it when |
|---|---|
| `execution: "interactive"` | The user wants to chat with the agent during the step. The step opens its own window and ends only when the agent reports. A quiet session is not a stuck one, so `timeoutMs` does not apply. Use `maxDurationMs` for the ceiling. |
| `context: "fork"` | The step needs the parent conversation. This costs tokens, so keep it rare. |
| `retries` | The step can fail for a reason that passes on a second try, such as a flaky command. |
| `onFailure: "continue"` | Later steps still make sense after this step fails. |
| `id` | You want a stable name in the status output. Otherwise the engine uses the position. |

---

## 5 · Check the workflow before you run it

Run this from the engine repository:

```bash
npm run check -- /path/to/the/user/project
```

Read the result.

- Exit code 0 and `all workflows compile`. The workflow is ready.
- Exit code 1. The output names the file and the reason. Fix it and run the check again.

Never start a run before this passes. The engine also rejects a broken workflow, but the check
gives you the reason in one step.

---

## 6 · Composition rules the compiler enforces

The compiler rejects a workflow that breaks any of these. Follow them while you write.

1. Every group holds at least one step. An empty `chain`, `parallel` or `loop` is an error.
2. `loop` needs `maxIterations` of 1 or more. Every loop must end.
3. An explicit `id` is kebab-case and appears once in the workflow.
4. `delegate` names a workflow that exists.
5. Delegates do not form a cycle. Workflow A cannot delegate to B while B delegates to A.
6. `execution: "interactive"` never appears inside `parallel`. Two windows would fight for
   the user input.
7. The config is the last argument of a group. `chain(config, step)` is an error.

---

## 7 · Turn the words into a shape

| The user says | Use |
|---|---|
| "first ... then ..." , "after that" | `chain` |
| "at the same time", "both", "in parallel" | `parallel` |
| "until it is clean", "keep going until", "repeat" | `loop` with `until` |
| "while it still ...", "as long as" | `loop` with `while` |
| "up to three times", "no more than" | `loop` with `maxIterations` |
| "review it", "check the result" | add an `analyser` step |
| "let me watch", "I want to chat with it" | `execution: "interactive"` |
| "run the whole X flow as one step" | `delegate` |
| "if that fails, carry on" | `onFailure: "continue"` |
| "try again if it fails" | `retries` |

Two rules of judgment:

1. Put a review after any step that writes code. Use `analyser`.
2. Give every repair loop a cap. Three iterations suit most work.

---

## 8 · The agents you may call

| Agent | Use for | Writes files |
|---|---|---|
| `scoping` | A new project, a large feature, architecture options. Produces the design and the roadmap. | yes |
| `planner` | Turning one phase, one bug or one small feature into a plan. Never writes code. | yes |
| `builder` | Carrying out an approved plan. | yes |
| `analyser` | Reviewing changes. Returns findings with a severity. | no |
| `documentation` | Archiving the work and refreshing the documentation. | yes |
| `tester` | end to end and integration tests | yes |

Use the exact names. A workflow that names an unknown agent fails at run time.

---

## 9 · Conditions

Conditions are data, not functions. That keeps a run repeatable after a restart.

| Condition | True when |
|---|---|
| `hasOpenFindings(severity)` | The last report holds findings at that severity or higher that are not deferred. |
| `lastStatus(status)` | The last report has that status: `completed`, `blocked` or `failed`. |
| `always()` and `never()` | Fixed values. |
| `not(c)` | The opposite. |
| `and(a, b)` and `or(a, b)` | The usual combinations. |

Severity values are `critical`, `high`, `medium` and `low`.

The common repair loop reads:

```ts
{ maxIterations: 3, until: not(hasOpenFindings("medium")) }
```

That means: stop as soon as the review is clean, and never run more than three passes.

---

## 10 · The runtime tools

| Tool | Arguments | Returns |
|---|---|---|
| `workflow_list` | none | Every workflow, its source, and any file that failed to load. |
| `workflow_start` | `workflow: string`, `request: string` | The run id at once. The run continues in the background. |
| `workflow_resume` | `runId: string`, `instanceId: string`, `answers: Record<string, string>` | The run id at once. The run continues in the background. |
| `workflow_status` | `runId: string` | The state of the run and of every step, with step ids, the last update of each running step, and permission requests that wait. |
| `workflow_cancel` | `runId: string` | The final state. It stops a step that is running. |
| `workflow_rewind` | `runId: string`, `step: string`, `fresh?: boolean` | The run id at once. The step, and every step after it, runs again. A step in a loop needs its iteration, for example `build#2`. |
| `workflow_message` | `runId: string`, `text: string`, `step?: string`, `mode?: "steer" \| "note"` | Sends text to a running step. `steer` stops the current turn. `note` does not. |
| `workflow_permission` | `permissionId: string`, `response: "once" \| "always" \| "reject"` | Answers a permission request of a step. |
| `workflow_answer` | `requestId: string`, `answers?: string[][]`, `reject?: boolean` | Answers a question that a step asks with the question tool. One list of answers for each question. |
| `workflow_update` | `message: string` | For step agents. One line of progress during long work. |

Pass the user request to `workflow_start` unchanged. The engine builds the context each agent
needs. Do not summarize it and do not add requirements.

The engine writes to the orchestrator chat when a step finishes, when a step asks a question, and
when the run ends. Its messages start with `[workflow <run id>]`. Do not poll `workflow_status`.

### A run that is waiting

The engine message names the blocked instance and lists the questions.

1. Print the questions to the user, numbered, with the options and the recommendation.
2. Do not answer them yourself and do not start a second run.
3. Wait for the user.
4. Call `workflow_resume` with the run id, the blocked instance id, and one answer per question
   id. The keys of `answers` are the question ids.

The engine resumes the same agent session, so the agent keeps what it gathered.

### A run that failed

The engine already applied the retry policy of that step. Report the failing step and the reason,
then ask the user how to proceed. Do not start the same run again.

---

## 11 · The report contract

The engine gives every workflow agent this contract. Read it so you can explain a result.

```ts
// completed
{ status: "completed", summary: string, artifacts?: string[], outputs?: Record<string, string>,
  findings?: Array<{ id: string, severity: "critical" | "high" | "medium" | "low",
                     defer?: boolean, reason: string }> }

// blocked
{ status: "blocked", summary: string,
  questions: Array<{ id: string, question: string, options?: string[], recommendation?: string }> }

// failed
{ status: "failed", summary: string, retryable?: boolean }
```

An agent calls `workflow_report` once, at the end of its turn. The engine reads the tool call, not
the message text. `outputs` and `artifacts` become the inputs of the next step.

---

## 12 · Worked examples

### "Build the export feature, review it twice at most, then write the docs"

```ts
export const exportFeature = workflow({
  name: "export-feature",
  description: "Plan and build the export feature, review it, then document it.",
  root: chain(
    agent("planner", "Plan the export feature."),
    loop(
      agent("builder", "Build the approved plan."),
      agent("analyser", "Review the export code."),
      { maxIterations: 2, until: not(hasOpenFindings("medium")) },
    ),
    agent("documentation", "Document the export feature."),
  ),
})
```

### "Run a security review and a performance review at the same time, then plan the fixes"

```ts
export const dualReview = workflow({
  name: "dual-review",
  description: "Review security and performance together, then plan the repairs.",
  root: chain(
    parallel(
      agent("analyser", "Review the current changes for security problems."),
      agent("analyser", "Review the current changes for performance problems."),
      { maxConcurrency: 2, onFailure: "continue" },
    ),
    agent("planner", "Turn the findings into one repair plan."),
  ),
})
```

### "Scope the new portal, then let me drive the build myself"

```ts
export const guidedPortal = workflow({
  name: "guided-portal",
  description: "Scope the portal, then build it in a window the user drives.",
  root: chain(
    agent("scoping", "Scope the new portal and produce the roadmap."),
    agent("builder", "Build the first phase with the user watching.", {
      execution: "interactive",
      maxDurationMs: 3_600_000,
    }),
  ),
})
```

---

## 13 · Before you say the work is done

1. The file sits in `.forgie/workflows/` and exports the workflow.
2. `npm run check` passed.
3. Every agent name is one of the five in section 8.
4. Every loop has a cap.
5. Every step that writes code has a review after it.
6. You ran the workflow, or you told the user the exact name to run.
7. You reported the run id, the status and the artifacts.

Never claim a run finished unless the tool output says `succeeded`.
