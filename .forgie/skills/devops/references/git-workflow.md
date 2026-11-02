# Git Workflow & Conventional Commits

Establish consistent branching, commit messages, and conflict resolution across any team. Ensure clear delivery performance following DORA and Lean principles.

## Branching Strategies
1. **GitHub Flow**: Recommended for Continuous Deployment.
   - `main` is protected and always deployable.
   - Feature branches from `main` → PR → review → merge → deploy immediately.
2. **Trunk-Based Development**: High-Velocity Teams (5+).
   - Everyone commits directly to `main` or short-lived (1-2 day) branches.
   - Feature flags mask incomplete work. CI must pass.
3. **GitFlow**: Enterprise/Scheduled releases.
   - `main` (production), `develop` (integration), `release/*`, `hotfix/*`, `feature/*`.

## Commit Messages & Conventional Commits
Use clear, actionable messages following the conventional format:
`<type>(<scope>): <subject>`

**Types**:
- `feat`: New feature (e.g. `feat(auth): add OAuth2 login`)
- `fix`: Bug fix (e.g. `fix(api): correct null response mapping`)
- `docs`: Documentation (`docs(readme): add config spec`)
- `style`: Formatting/lint.
- `refactor`: Structural codebase overhaul without behavioral shifts.
- `test`: Test cases.
- `chore`: Tooling, dependencies update.
- `perf`: Performance.

*To trigger automated commit formatting with Copilot, utilize this structure:*
```xml
<commit-message>
  <type>feat|fix|docs...!</type>
  <scope>(optional component)</scope>
  <description>short, imperative summary (lowercase start)</description>
</commit-message>
```

## Review Checklist (Pull Requests)
- [ ] Self-reviewed locally.
- [ ] PR size < 500 lines ideally (single feature per PR).
- [ ] Includes `What`, `Why`, and `How` in the description.
- [ ] Resolves linked tickets (e.g. `Closes #123`).
- [ ] Code style matches standard, tests pass locally.

## Merge vs. Rebase
- **Merge**: Use when bringing feature branches into `main` (preserves history and traces parallel work).
- **Rebase**: Use exclusively to update local, unshared feature branches with the latest `main` (keeps history linear). **NEVER** rebase public/shared branches.

## Conflict Resolution
1. Verify conflicts: `git merge feature-branch --no-commit --no-ff`.
2. Inspect the file, either resolve markers manually or use commands:
   - `git checkout --ours file.ts` (Keep current branch's file)
   - `git checkout --theirs file.ts` (Keep incoming branch's file)
3. Prevent conflicts: Keep branches small, rebase daily, use feature flags.

## Reverting & Branch Management
```bash
# Delete merged remote-tracking references
git fetch -p
git branch --merged main | grep -v "^\*\|main" | xargs -n 1 git branch -d

# Undo a public mistake safely
git revert HEAD

# Save work in progress safely
git stash push -m "WIP message"
git stash pop
```

## Useful Git Configuration
```bash
# Better diff algorithm
git config --global diff.algorithm histogram
# Default pull behavior
git config --global pull.rebase true
# Auto-correct typos
git config --global help.autocorrect 1
# Aliases
git config --global alias.co checkout
git config --global alias.visual "log --oneline --graph --all"
```

## Pre-Commit / Pre-Push Hooks
Automate validation before sharing:
```bash
#!/bin/bash
# .git/hooks/pre-commit
npm run lint || exit 1
npm test || exit 1
if git diff --cached | grep -E '(password|api_key|secret)'; then
    echo "Possible secret detected. Commit aborted."
    exit 1
fi
```