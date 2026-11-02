---
name: code-graph
description: Build and query a Graphify knowledge graph of a codebase instead of grepping files. Use when the user asks how a codebase fits together, which parts connect, what the architecture is, where a change has impact, or asks to map, index, or graph a repository with graphify. Use before broad code search on an unfamiliar repository. Use before planning feature addition to a codebase. Use before refactoring or redesigning a module. Use before reviewing code for depth, locality, and testability.
---

# Code Graph (Graphify)

## Overview

Graphify maps a project into a knowledge graph you query. Tree-sitter parses code
locally into an AST. No LLM reads the code, and nothing leaves the machine. Docs,
PDFs, images, and video use a model backend, and only if you configure one.

Each edge carries a tag:

- `EXTRACTED` — explicit in the source.
- `INFERRED` — resolved by Graphify.
- `AMBIGUOUS` — a guess. Verify before you trust it.

## The Iron Law

```
QUERY THE GRAPH BEFORE YOU GREP THE FILES
```

Read raw source only after the graph points you at the file. Report the answer only
after a command in this session prints it.

## When to use

Use this skill when:

- The repository is new to you and you need the shape of it.
- A question spans files ("what connects auth to the database?").
- You need the blast radius of a change.
- You need the path between two symbols.

Do not use this skill for a single known file, a one-line fix, or a literal string
search. Use `grep` for those.


The PyPI package is `graphifyy`. The command is `graphify`.

```bash
uv tool install graphifyy      # or: pipx install graphifyy
graphify --version             # verify the command resolves
```

If the shell reports `graphify: command not found`, run `uv tool update-shell` (or
`pipx ensurepath`) and open a new terminal.

## Workflow

### 1. Check for an existing graph

```bash
ls graphify-out/graph.json
```

If the file exists, skip to step 3. Teams commit `graphify-out/`, so the graph is
often already there.

### 2. Build the graph

```bash
graphify extract . --code-only     # local AST only, no API key, no network
```

Use `--code-only` on a mixed repository. Without it, Graphify sends docs, PDFs, and
images to a model backend. Drop the flag only when the user wants docs in the graph
and a backend is configured.

Output lands in `graphify-out/`:

| File | Contents |
| --- | --- |
| `graph.json` | The full graph. Query this. |
| `GRAPH_REPORT.md` | God nodes, surprising connections, suggested questions. |
| `graph.html` | Interactive view for the user, not for you. |

Add `--no-viz` to skip the HTML on large repositories.

### 3. Query, do not grep

```bash
graphify query "what connects the auth layer to the database?"
graphify path "UserService" "DatabasePool"
graphify explain "RateLimiter"
```

- `query` returns a scoped subgraph for a plain-language question.
- `path` traces the shortest hop chain between two nodes.
- `explain` lists one node, its source location, its community, and its edges.

Read `GRAPH_REPORT.md` only for a broad architecture review. For a specific
question, a scoped query costs less context.

### 4. Verify before you answer

`explain` prints a file and a line for each node. Open that location and confirm the
code says what the graph claims. Always verify an `INFERRED` or `AMBIGUOUS` edge.

### 5. Keep the graph fresh

```bash
graphify update .              # re-extract only changed files
graphify hook install          # rebuild after each commit, AST only, no API cost
```

A stale graph gives confident wrong answers. Update the graph after a refactor.

## Useful flags

| Command | Effect |
| --- | --- |
| `graphify extract . --code-only` | Index code only. No API key needed. |
| `graphify extract . --force` | Overwrite when the new graph has fewer nodes. Use after a refactor. |
| `graphify extract . --no-gitignore` | Include git-ignored generated code. |
| `graphify cluster-only . --resolution 1.5` | Re-cluster into smaller communities without re-extraction. |
| `graphify query "..." --graph path/to/graph.json` | Query a graph outside the default location. |
| `graphify export callflow-html` | Mermaid architecture and call-flow HTML. |

## Ignoring files

Graphify respects `.gitignore` in every directory. Add a `.graphifyignore` in the
project root for extra exclusions. The syntax matches `.gitignore`, and its patterns
run last, so they win on conflict.

```
# .graphifyignore
node_modules/
dist/
*.generated.py
```

## Always-on setup (optional)

Run once per project after the graph exists:

```bash
graphify vscode install        # VS Code Copilot Chat
graphify copilot install       # GitHub Copilot CLI
graphify agents install        # cross-framework: .agents/skills + AGENTS.md
```

This writes an instruction file that tells the assistant to query the graph first.
Add `--project` to write inside the repository instead of the user profile.

Remove it with `graphify uninstall`. Add `--purge` to also delete `graphify-out/`.

## MCP server (optional)

For repeated tool-call access instead of shell calls:

```bash
python -m graphify.serve graphify-out/graph.json
```

The server exposes `query_graph`, `get_node`, `get_neighbors`, and `shortest_path`.
The default transport is stdio and stays local. Use `--transport http` only for a
shared team server, and pair `--host 0.0.0.0` with `--api-key`.

## Privacy

- Code stays local. Tree-sitter parses it on the machine.
- Docs, PDFs, and images go to the configured model backend. `--code-only` skips them.
- Queries log to `~/.cache/graphify-queries.log` only when you opt in with
  `GRAPHIFY_QUERY_LOG_ENABLE=1`.
- Never pass an API key on the command line. Set it in the environment.

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| `command not found` | Run `uv tool update-shell` or `pipx ensurepath`, then open a new terminal. |
| Empty nodes for docs or PDFs | Those need a backend. Set the key, or use `--code-only`. |
| Fewer nodes after a rebuild | Deleted files leave stale nodes. Re-run with `--force`. |
| "extraction was incomplete, refusing to overwrite" | A pass crashed. Fix the cause and re-run. |
| `graph.html` too large to open | Re-run with `--no-viz` and query `graph.json` instead. |
| Conflict markers in `graph.json` | Run `graphify hook install`. It adds a union merge driver. |
| Skill version mismatch warning | Run `uv tool upgrade graphifyy`, then `graphify install`. |
| Duplicate nodes for one symbol | Run `graphify extract . --force`. |

## Red flags

Stop if you catch yourself doing any of these:

- Grepping a repository that has a `graphify-out/graph.json`.
- Reading `GRAPH_REPORT.md` in full to answer one narrow question.
- Reporting an `INFERRED` edge as fact without opening the source.
- Running `graphify extract` on a mixed repository without `--code-only` when no
  backend is configured.
- Answering from a graph built before the last refactor.
