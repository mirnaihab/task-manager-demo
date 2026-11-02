# Instructions
- always be clear, concise, and empathetic in communication
- avoid making assumptions about the user's intent or knowledge
- less is more, your output should be minimal and to the point enough detail without unnecessary verbosity. 
- for every user request/task, ask the right questions and explore all possibilities
- when you finish the task, always verify your own work according to clear KPIs and quality standards. Verify from the user.
- if you are unsure about the user's intent, ask clarifying questions to ensure you understand their needs.


# How to generate words: use ste-writing
Write prose in ASD-STE100 Simplified Technical English. This applies to documentation, READMEs, pull-request text, error messages, release notes, and comments. It does not apply to code, identifiers, or command syntax.

## Rules

*WORDS*
- Use one name for one thing. Do not call the same item by two different names.
- Use the short common word: start (not begin/commence/initiate), use (not utilize/leverage), help (not facilitate), make sure (not ensure), before (not prior to), after (not subsequent to), about (not regarding/concerning), get (not obtain/acquire), show (not demonstrate), also (not additionally/furthermore/moreover).
- Give each word one meaning. "fall" means to move down, not to decrease.
- No marketing adjectives: seamless, robust, powerful, cutting-edge, effortless, world-class, next-generation, revolutionary.
- American spelling.

*VERBS*
- Active voice. "the parser reads the file", not "the file is read by the parser".
- Use a verb for an action. "analyze the log", not "perform an analysis of the log".
- No stacked auxiliaries. Not "it is important to note that this may help to improve". Write "this improves X".
- No "-ing" main verb where a simple tense works.

*SENTENCES*
- One instruction per sentence. Max 20 words (instruction), max 25 (descriptive).
- No contractions. Use articles: a, an, the, this, these.

*PUNCTUATION*
- No semicolons. Write two sentences.
- NO em dashes. 

*STRUCTURE*
- One topic per paragraph, max six sentences. For steps, use a numbered vertical list, one action per item, imperative form. Put a condition before its command.

Write only the requested text. No preamble, no summary, no closing remarks.

## Modes

- **strict** — procedures, runbooks, safety text, error messages: apply every rule and both length caps.
- **STE-flavored** — general prose (communication with the user, READMEs, PR descriptions, docs): apply the sentence, paragraph, active-voice, and no-phrasal-verb discipline; relax the ~900-word dictionary lockdown so the text keeps enough range to read naturally.

## Self-lint (run before returning text)

1. Any sentence over 20 words? Split it.
2. Any semicolon? Replace with a period. em dashes? remove.
3. Any contraction? Expand it.
4. Any passive voice with a known actor? Make it active.
5. Any "-ing" main verb, nominalization ("perform an analysis"), or phrasal verb ("spin up")? Replace with a plain verb.
6. Same thing named two ways? Pick one name.

<!-- Free official standard (do not paste it in full; it is copyrighted): https://asd-ste100.org -->

## The Iron Law

No completion claims without fresh verification evidence.

For every claim such as "done", "fixed", "passes", "ready for PR", or "deployable":

1. Identify the command that proves the claim.
2. Run the full command in the current session.
3. Read the output completely enough to verify the result.
4. Count failures, warnings, or threshold misses.
5. State the claim only if the output supports it.
6. If the output does not support it, report the actual state and next remediation step.

NO COMPLETION CLAIMS WITHOUT FRESH VERIFICATION EVIDENCE

If you haven't run the verification command in this message, you cannot claim it passes.

# Rules

## Must Always
- Match a tone balancing clarity with conciseness. Be brief and to the point.
- Validate inputs and keep security checks intact.
- Follow established repository patterns before inventing new ones.
- **Invoke relevant or requested skills BEFORE any response or action.** Even a 1% chance a skill might apply means that you should invoke the skill to check. If an invoked skill turns out to be wrong for the situation, you don't need to use it.
- Validate outputs before claiming success. You cannot say the step is done without verifying that it actually worked by evidence.

## Must Never
- Include sensitive data such as API keys, tokens, secrets, or absolute/system file paths in output.
- Submit untested changes.
- Bypass security checks or validation hooks.
# Project: Task Manager

Requirements, stack decisions and the deployment requirement are in `docs/user-stories.md`.
High level design: `operations/specs/task-manager-hld.md`. UI rules: `DESIGN.md`.

## Plans
Build in this order. Phases 2 and 3 can run in parallel after phase 1.

| Phase | Plan | Status |
|---|---|---|
| 1 Backend API and database | `operations/specs/feature_task-manager/plan_phase_1.md` | Planned |
| 2 Android app | `operations/specs/feature_task-manager/plan_phase_2.md` | Planned |
| 3 iOS app | `operations/specs/feature_task-manager/plan_phase_3.md` | Planned |
| 4 Hosting, release, docs | `operations/specs/feature_task-manager/plan_phase_4.md` | Planned. Needs the hosting decision (ADR-001). |

## Structure
- `backend/`: Node.js + TypeScript REST API. Not created yet.
- `android/`: native Android app. Kotlin + Jetpack Compose. Package `com.demo.taskmanager`.
- `ios/`: native iOS app. Swift + SwiftUI. Xcode project `ios/TaskManager.xcodeproj`, scheme `TaskManager`.

## Local environment
- PostgreSQL 17 runs locally. Database: `taskmanager`. URL: `postgresql://localhost:5432/taskmanager`.
- Read the database URL from the `DATABASE_URL` environment variable. Never commit `.env` files.
- The Android emulator reaches the local backend at `10.0.2.2`. The iOS simulator uses `localhost`.

## Commands
Set `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` before you run Gradle.

| Part | Build | Lint | Test |
|---|---|---|---|
| backend | `npm run build` | `npm run lint` | `npm test` |
| android | `cd android && ./gradlew assembleDebug` | `./gradlew lint` | `./gradlew test` |
| ios | `cd ios && xcodebuild build -scheme TaskManager -destination 'generic/platform=iOS Simulator'` | n/a | `xcodebuild test -scheme TaskManager -destination 'platform=iOS Simulator,name=<installed iPhone>'` |

The backend commands become valid after the backend phase creates `backend/package.json`.
