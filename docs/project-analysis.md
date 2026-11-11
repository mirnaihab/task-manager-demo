# Project Analysis: Task Manager Build

- **Created**: 2026-11-03
- **Scope**: Every step of the build, from the first commit to the documentation refresh.
- **Sources**: git history, `operations/runs/*` (run events), `operations/archives/`, `docs/`, and the local Forgie session database (token and cost data).
- **Time zone**: Run times are UTC. Git times are in the commit zone (+02:00).

## 1. Summary

| Item | Value |
|---|---|
| Product | Task manager: Node.js API, native Android app, native iOS app. |
| Phases done | 1 backend, 2 Android, 3 iOS. One debt fix (TD-001). |
| Phase open | 4 hosting, release, README. It waits for ADR-001. |
| Commits | 7 |
| Code size | Backend source 534 lines, backend tests 491 lines, Android 2,463 lines (Kotlin), iOS 1,824 lines (Swift). |
| Tests (last evidence) | Backend 58. Android 41 unit and 15 UI. iOS 37 unit and 11 UI. |
| Docs | 11 ADRs, HLD, PRD, technical debt list, user stories, this analysis. |
| Agent calls | 385 model calls in 19 sessions. |
| Tokens | 30.21 million input. 0.48 million output. |
| Cost | 65.34 USD (tool estimate, not an invoice). See section 5. |

Key finding: input tokens are 98.5 percent of all tokens. Agents re-read a large context on each call. No prompt cache was used on the main model. Section 7 explains this.

## 2. How the work was organized

The project uses an agentic workflow engine (`.forgie/`). One orchestrator agent starts a workflow. The engine then runs specialist agents in order.

| Agent | Model | Job |
|---|---|---|
| orchestrator | `openai.gpt-5.6-terra` | Talks to the user. Starts workflows. Sends answers back. |
| scoping | `bedrock.anthropic.claude-sonnet-5-5` | Writes the HLD and the phase roadmap. |
| planner | `bedrock.anthropic.claude-sonnet-5-5` | Writes one plan per phase. No code. |
| builder | `bedrock.anthropic.claude-sonnet-5-5` | Writes code from an approved plan. |
| analyser | `bedrock.anthropic.claude-sonnet-5-5` | Reviews the change. Reports findings. |
| documentation | `bedrock.anthropic.claude-sonnet-5-5` | Archives plans. Updates docs, ADRs, debt. |

The `feature` workflow runs: scope, plan, then build and review in a loop per phase, then document. The `implement` workflow runs: plan, build and review, document. The `docs` workflow runs one documentation step.

## 3. Step by step (what happened)

### Step 0. Setup before any workflow (2026-11-02, afternoon)

| Commit | Time | What it did |
|---|---|---|
| `d222770` | 15:54 | Added `docs/user-stories.md`: requirements, stack choices, deployment requirement. |
| `14ae580` | 16:12 | Added empty native Android and iOS projects. |
| `8b3218e` | 16:26 | Added the agent toolkit: agents, skills, `forgie.jsonc`, workflow engine. |

No token data exists for these commits. They were made before the tracked sessions in this project. Section 8 lists this gap.

### Step 1. Feature run: scope (14:52 UTC, 1.9 min)

The orchestrator started the `feature` workflow with the request to build the task manager. The scoping agent read the user stories and wrote the HLD and the four phase roadmap. It made 7 calls.

### Step 2. Feature run: plan (14:54 UTC, 18.1 min)

The planner agent turned the roadmap into four phase plans in `operations/specs/feature_task-manager/`. It made 34 calls. It is the most costly planning step in the project (8.05 USD).

### Step 3. Feature run: phase 1 backend build, three attempts

| Attempt | Start (UTC) | Result | Length | Cost (USD) |
|---|---|---|---|---|
| 1 | 15:12 | Failed: "The operation was aborted." | 8.0 min | 1.02 |
| 2 | 15:24 | Failed: same error. | 10.0 min | 0.95 |
| 3 | 15:44 | Succeeded. | 3.5 min | 2.38 |

Cause: the model gateway sends nothing while it writes a large file. The engine stall limit (240 s) stopped healthy calls. The fix was commit `09c7de9`. It raised `modelStallMs` to 900000 ms (15 minutes) in `.forgie/workflow-engine.js`. Each failure ended the run, and the operator rewound it from `root.2.0` (events 12 and 17). The two failed attempts cost about 1.97 USD (time-window estimate) with no kept result. The partial backend files from the early attempts were committed in `662bc62` and `09c7de9`.

### Step 4. Feature run: phase 1 review (15:47 UTC, 3.8 min)

The analyser reviewed the backend. Result: APPROVE with notes. Evidence: build passes, lint has 0 errors and 0 warnings, 55 of 55 tests pass, migration runs up, down and up.

### Step 5. Feature run: phase 2 Android (15:51 UTC)

| Part | Length | Calls | Cost (USD) |
|---|---|---|---|
| Build | 29.2 min | 70 | 17.85 |
| Review | 2.5 min | 9 | 1.06 |

The builder wrote the Compose app, one `TaskListViewModel`, the API client and the tests. It changed three library versions because the planned versions need Kotlin 2.4 (ADR-008). Evidence: `assembleDebug lint test assembleRelease connectedDebugAndroidTest` gave BUILD SUCCESSFUL, with 41 unit tests and 15 UI tests. The review gave APPROVE with notes. This is the most costly step of the project.

### Step 6. Feature run: phase 3 iOS (16:23 UTC)

| Part | Length | Calls | Cost (USD) |
|---|---|---|---|
| Build | 21.4 min (plus a long wait, see below) | 55 | 12.33 |
| Review | 2.9 min | 12 | 1.53 |

The builder wrote the SwiftUI app. At 17:27 UTC the engine blocked the step: the builder gave no sign of life for 1,126 s after 2 automatic extensions. The question was "continue or cancel". The operator answered "continue" at 22:04 UTC, about 4.6 hours later. The wait used no tokens. The build then finished in 21.4 min of active time. Evidence: Debug and Release builds succeed with no Swift warnings, 37 unit tests and 11 UI tests pass. The review gave APPROVE with notes.

### Step 7. Feature run: document (22:29 UTC, 28.4 min)

The documentation agent made 27 calls. It archived plans 1 to 3 in `operations/archives/2026-11-03-task-manager-phases-1-3/`, wrote `summary.md`, wrote ADR-001 to ADR-011, the PRD, the technical debt list, the README and updated the HLD and `AGENTS.md`. The run ended as `succeeded` at 22:57 UTC. Total length of the run: about 8 hours of wall time, of which about 4.6 hours was the wait for the operator.

### Step 8. Commit of phases 1 to 3 (`2caef5b`, 01:00 +02:00)

106 files changed, 5,968 lines added, 284 removed. This commit covers TD-003 (uncommitted work) for phases 1 to 3. The `AGENTS.md` text still says phases 1 to 3 are uncommitted. See section 9.

### Step 9. Debt fix TD-001 (`implement` runs, 23:03 to 23:09 UTC)

| Run | Result | Note |
|---|---|---|
| `implement-…636193` | Canceled after 1.2 min | Plan step only. 0.22 USD. |
| `implement-…745425` | Succeeded in 3.3 min | Plan, build, review, document. 2.29 USD. |

The change: `createPool` in `backend/src/db.ts` now calls `pool.on('error')` and logs with `logger.error`. A crash from an idle client error is gone. Three tests failed first (RED) and then passed (GREEN). Evidence: build, lint and 58 of 58 tests pass. The review gave APPROVE with 3 low findings. Commit: `a2c7904`.

### Step 10. Phase 4 planning runs (2026-11-03)

| Run | Start (UTC) | Result | Length | Cost (USD) |
|---|---|---|---|---|
| `implement-…704116653` | 11:08 | Failed: "The operation was aborted." | 28.0 min | 3.81 |
| `implement-…710477611` | 12:54 | Still marked `running`. Its planner session still adds calls. | n/a | 2.96 |

Phase 4 needs the hosting decision (ADR-001). The run events do not state why run 1 stopped. Neither run produced code. The cost so far, 6.77 USD, bought no kept result. The existing `plan_phase_4.md` dates from the first feature run.

### Step 11. This documentation step (12:57 UTC)

The `docs` workflow started the documentation agent with the request for this analysis. The agent read the run events and the local session database, and wrote this file. The cost of this step is partial. See section 8.

## 4. What the project produced

| Area | Result |
|---|---|
| Backend | Express 5, zod, `pg`, no ORM. Layers: routes, service, repository. SQL migrations. Dockerfile (not built). |
| Android | Kotlin and Jetpack Compose. Package `com.demo.taskmanager`. |
| iOS | Swift and SwiftUI. Scheme `TaskManager`. |
| Decisions | ADR-001 to ADR-011. ADR-001 (hosting) is open. |
| Archives | `2026-11-03-task-manager-phases-1-3`, `2026-11-03-fix-td-001`. |

## 5. Token consumption and billing per step

Snapshot: 2026-11-03 13:05 UTC. Source: the `tokens` and `cost` fields of each assistant message in the local Forgie database. Each row sums all calls of one session. "Input" is the new prompt tokens for each call, added together. "Output" is the generated tokens.

| ID | Step | Calls | Input tokens | Output tokens | Cost (USD) |
|---|---|---|---|---|---|
| A1 | Orchestrator (feature run, first part) | 10 | 62,911 | 699 | 0.16 |
| A2 | Scope | 7 | 323,936 | 11,084 | 0.76 |
| A3 | Plan (phases 1 to 4) | 34 | 3,543,536 | 96,008 | 8.05 |
| A4 | Build phase 1 backend (3 attempts) | 30 | 2,020,559 | 30,527 | 4.35 |
| A5 | Review phase 1 | 9 | 434,053 | 14,645 | 1.01 |
| A6 | Build phase 2 Android | 70 | 8,523,383 | 80,168 | 17.85 |
| A7 | Review phase 2 | 9 | 480,394 | 9,484 | 1.06 |
| A8 | Build phase 3 iOS | 55 | 5,915,131 | 49,797 | 12.33 |
| A9 | Review phase 3 | 12 | 703,379 | 11,853 | 1.53 |
| A10 | Document (archive, ADRs, HLD) | 27 | 2,210,792 | 37,059 | 4.79 |
| B1 | TD-001 plan, canceled run | 3 | 102,742 | 1,284 | 0.22 |
| B2 | TD-001 plan | 5 | 205,727 | 5,182 | 0.46 |
| B3 | TD-001 build | 8 | 327,832 | 4,010 | 0.70 |
| B4 | TD-001 review | 5 | 190,722 | 2,196 | 0.40 |
| B5 | TD-001 documentation | 9 | 344,644 | 4,116 | 0.73 |
| C1 | Phase 4 plan, run 1 (aborted) | 22 | 1,637,853 | 53,283 | 3.81 |
| C2 | Phase 4 plan, run 2 (not finished) | 16 | 1,258,971 | 44,444 | 2.96 |
| C3 | Documentation refresh (this step, partial) | 25 | 1,185,245 | 23,969 | 2.61 |
| D1 | Orchestrator session for rewinds and later runs | 29 | 742,002 | 3,913 | 1.57 |
| | **Total** | **385** | **30,213,812** | **483,721** | **65.34** |

Notes on the table:
1. `Input` for `openai.gpt-5.6-terra` (rows A1 and D1) is the uncached part. It also read 367,854 cached tokens and used 159 reasoning tokens. Its cost is 0.39 USD.
2. The Sonnet rows have zero cached tokens. See section 7.
3. Row A4 includes the two failed attempts. Row C3 and the whole project grow while this step runs.
4. Another session in the project directory, "Context, usage, and remaining tasks" (53,384 input, 770 output, 0.11 USD), is not in the table. It is a side question, not a build step.

### 5.1 Cost by group

| Group | Calls | Input | Output | Cost (USD) | Share |
|---|---|---|---|---|---|
| Feature run (A1 to A10) | 263 | 24,218,074 | 341,324 | 51.88 | 79.4 percent |
| TD-001 fix (B1 to B5) | 30 | 1,171,667 | 16,788 | 2.51 | 3.8 percent |
| Phase 4 planning (C1, C2) | 38 | 2,896,824 | 97,727 | 6.77 | 10.4 percent |
| This documentation step (C3) | 25 | 1,185,245 | 23,969 | 2.61 | 4.0 percent |
| Orchestrator, later sessions (D1) | 29 | 742,002 | 3,913 | 1.57 | 2.4 percent |

Group shares use the total of section 5 as the base.

### 5.2 Cost by agent role

| Role | Calls | Input | Output | Cost (USD) | Share |
|---|---|---|---|---|---|
| builder | 163 | 16,786,905 | 164,502 | 35.22 | 53.9 percent |
| planner | 80 | 6,748,829 | 200,201 | 15.50 | 23.7 percent |
| documentation | 61 | 3,740,681 | 65,144 | 8.13 | 12.4 percent |
| analyser | 35 | 1,808,548 | 38,178 | 4.00 | 6.1 percent |
| orchestrator | 39 | 804,913 | 4,612 | 1.73 | 2.7 percent |
| scoping | 7 | 323,936 | 11,084 | 0.76 | 1.2 percent |

### 5.3 Cost by model

| Model | Calls | Input | Output | Cached read | Cost (USD) |
|---|---|---|---|---|---|
| `bedrock.anthropic.claude-sonnet-5-5` | 364 | 30,063,301 | 482,236 | 0 | 64.95 |
| `openai.gpt-5.6-terra` | 21 | 150,511 | 1,485 | 367,854 | 0.39 |

## 6. Where the money went

1. **Builders cost 54 percent.** The Android build (17.85 USD) and the iOS build (12.33 USD) are 46 percent of the total. They made 125 calls with large contexts.
2. **Planning cost 24 percent.** The phase plan (8.05 USD) and two phase 4 planning runs (6.77 USD so far) are the largest items.
3. **Waste is about 9.0 USD.** Failed builder attempts 1.97 USD. Phase 4 planning runs 6.77 USD with no kept result. The canceled TD-001 plan run is 0.22 USD.
4. **Review is cheap.** The four reviews cost 4.00 USD, 6 percent of the total, and found 0 critical and 0 high issues.
5. **Wait time is free.** The 4.6 hour wait for the operator used no tokens.

## 7. Why input tokens dominate

A builder call carries the plan, the code it read and all earlier tool output. The agent sends the full context again on every call. Android build: 70 calls, 8.52 million input tokens, so about 122,000 input tokens per call. Output was only 80,168 tokens.

Cost matches the list price rate of about 2 USD per million input tokens and 10 USD per million output tokens. This holds for all 348 Sonnet calls checked (at the time of the check) to within 0.0005 USD each. The input share of the cost is about 92 percent (61.09 USD input, 4.94 USD output).

The Sonnet calls report no cached tokens. The orchestrator model reports 367,854 cached tokens. If the gateway for Sonnet supported prompt caching, the input cost could fall a lot. This is a cost lever to test. I did not test it.

## 8. Limits of the data

1. **Cost is an estimate.** The `cost` field comes from the tool and its price table. It is not a cloud invoice. Check the real bill with the provider.
2. **The rates are inferred.** I derived 2 USD and 10 USD per million tokens from the data. The provider price page was not checked.
3. **Steps before 2026-11-02 14:52 UTC have no data here.** The three setup commits (`d222770`, `14ae580`, `8b3218e`) have no tracked sessions in this project directory. Older sessions in other directories are not counted.
4. **The numbers are a snapshot.** This documentation step and the orchestrator session were still adding calls when the snapshot was taken.
5. **Failed attempt costs use a time window.** Attempt 1 and 2 of the phase 1 build share one session. I split them by timestamp.
6. **Run `implement-…710477611` shows `running`.** Its state may be stale. I did not cancel it.
7. **No GitHub, CI or hosted run exists.** No Docker build, static scan or hosted run has passed. No claim in this file says otherwise.

## 9. Findings and next steps

| ID | Finding | Severity | Next step |
|---|---|---|---|
| F-1 | `AGENTS.md` and `docs/technical-debt.md` said phases 1 to 3 were uncommitted. Git shows they are committed. | Low | Fixed in this step (TD-003 closed). |
| F-2 | Phase 4 planning failed or stalled twice and cost 6.77 USD. | Medium | Decide ADR-001 first. Then run one planning step with the choice in the request. |
| F-3 | Sonnet calls show no prompt cache use. | Medium | Test prompt caching with the gateway. Compare cost on one builder step. |
| F-4 | Builder stalls caused two aborted runs. | Low | The 15 minute limit in `.forgie/workflow-engine.js` fixed it. Keep it. |
| F-5 | Run `implement-…710477611` still shows `running`. | Low | Cancel it if no planner is active. |
| F-6 | The README has no link to this analysis. | Low | Fixed in this step. |

## 10. How to repeat the numbers

1. Open the database read-only: `sqlite3 -readonly ~/.local/share/forgie/forgie.db`.
2. Join `message` to `session` on the session id.
3. Filter `session.directory` to the project directory and the message role to `assistant`.
4. Sum `tokens.input`, `tokens.output` and `cost` from the message `data` JSON.
5. Read step times and failures from `operations/runs/*/events.jsonl`.
