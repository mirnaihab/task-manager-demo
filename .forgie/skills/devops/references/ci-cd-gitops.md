# CI/CD Pipelines & GitHub Actions

Your mission is to construct boring, reliable CI/CD pipelines and deployment debugging methodologies focused strongly on automation, monitoring, rapid recovery, and GitOps workflows. 

## GitHub Actions Best Practices

### **1. Performance and Caching**
- **Caching**: Utilize `actions/cache` pinned to a full commit SHA to accelerate dependencies installation (Node, Pip, Maven). Use highly effective dynamic `key` strategies based on file hashes (e.g., `hashFiles('**/package-lock.json')`) and configure `restore-keys` to increase cache hits.
- **Matrix Strategies**: Accelerate testing executing isolated configurations in parallel utilizing `strategy.matrix`. Toggle `fail-fast: false` for comprehensive test reporting, or `true` to immediately block sequential pipelines.
- **Fast Checkout**: Use `actions/checkout@<SHA>` setting `fetch-depth: 1` as standard for building and testing. Disable submodules fetching (`submodules: false`) unless explicitly required. For extremely large monorepos, use `lfs: false`.
- **Artifact Passing**: Pass compiled binaries between jobs seamlessly with `actions/upload-artifact` and `actions/download-artifact`. Control explicit storage costs with `retention-days`.

### **2. Security & Dependencies**
- Do not blindly trust third-party actions; pin all `uses:` to a full commit `SHA` with a commented human-readable version block (`actions/checkout@34e... # v4.3.1`).
- Define explicit Global Permissions (`permissions: contents: read`) setting precise write privileges on individual jobs.
- Require environment secrets exclusively mapped from the `${{ secrets.X }}` context. Do not output them in raw scripts. Enforce OpenID Connect (OIDC) authentication for cloud deployments avoiding long-lived static credentials entirely.
- Implement robust automated security validations blocking pipelines on failure:
  - **SCA**: `dependency-review-action`, Trivy.
  - **SAST**: CodeQL, SonarQube, security linters (Bandit, ESLint).
  - **Secret Scanning**: TruffleHog. Prevent local commits utilizing pre-commit templates.
- **Image Signing**: Implement immutable builds signing artifacts via Cosign.

## Comprehensive Testing Strategies
Incorporate these tests seamlessly:
1. **Unit Tests**: Rapid execution, paralleled, publishing coverage results automatically (JUnit/Codecov). Fast Feedback.
2. **Integration Tests**: Leverage `services` within jobs to initialize lightweight relational DBs, Redis, or RabbitMQ via isolated Docker containers. Tests must cleanly disconnect.
3. **End-to-End (E2E)**: Utilize robust selectors against deployed staging environments (Cypress/Playwright). Integrate visual testing. Handle flakiness with implicit retries instead of sleep calls. Publish error videos/screenshots.
4. **Performance Tests**: Schedule Load testing sweeps matching baselines iteratively.

## Advanced Deployment Workflows & GitOps

### **Deployment Principles**
Deployments should strictly follow environment promotion steps from development to staging to production.
- **Staging Deployments**: Must mirror production entirely. Configure environment protection rules inside GitHub to govern this. Run comprehensive post-deployment validation.
- **Production Deployments**: Implement mandatory Manual Approvals linking environments to IT Change Mgmt if needed. Employ progressive rollout tactics (Canary/Blue-Green/Dark-launch functionality). Integrate Observability strictly.
- Establish automated Runbook fallbacks: Immediate rolling `kubectl rollout undo` upon probe failures.

### **Monitoring & Triage Procedures**
When dealing with a "3AM Deployment Failure":
1. **Triage Scope**: Determine the exact commit triggering the run, the extent of the failure, and assess if data migrations stop rollbacks.
2. **Failure Patterns**:
   - *Build Conflicts*: Fix by rigidly locking dependencies (`"express": "4.18.2"`).
   - *Env Mismatches*: Confirm exact runtime node versions (`.node-version`).
   - *Timeouts*: Lengthen kubernetes `readinessProbe` initial delays during heavy boots.
3. **Debugging Methodology**: Run `git diff HEAD~1 HEAD`, review deep logs, compare config maps across staging vs production. Do a local run emulating CI variables exactly if possible.
4. **Health Check Endpoints**: Implement explicit routing for synthetic monitors (e.g. `/health`) returning system up-time, JSON status blocks (`{ status: "healthy", database: "connected" }`), and returning HTTP 503 upon failure.

## Observability Expectations
Set strict escalation alerting rules according to performance indicators:
- **MTTR (Mean Time to Recovery)**: Monitor error spikes dynamically.
- **CFR (Change Failure Rate)**: Lower threshold limits automatically halting CI merges.
- Configure alert channels appropriately: Critical (Pager), High (Slack), Medium (Emails).