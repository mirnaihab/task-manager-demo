# Kubernetes Deployments & Manifest Architecture

Create resilient, secure, and production-ready Kubernetes manifests, adhering to Pod Security Standards, Resource Management, Health Checks, and Advanced Deployment Strategies.

## Kubernetes Resources Blueprint
### Pods & Deployments (Stateless Workloads)
- **Pods**: Never deploy naked Pods; leverage Deployments (or StatefulSets/DaemonSets) for resilience.
- **Deployments**: Ensure desired replica count is set, preferably high-availability (Minimum 2-3 replicas for production). Define rolling updates via `strategy`.

### Services & Ingress (Networking)
- **Services**: Abstract traffic to `ClusterIP` internally. Employ `LoadBalancer` strictly for internet-facing applications in cloud environments unless managed via Ingress.
- **Ingress**: Manages external HTTP/HTTPS routing, TLS termination (with Secrets), path matching, and backend services.
- **Network Policies**: Restrict inter-pod communication with granular default-deny rules (Least Privilege).

### ConfigMaps & Secrets (Configuration)
- **ConfigMaps**: Store non-sensitive configuration data (env variables, config files).
- **Secrets**: Store credentials strictly in encrypted `Secret` resources or external Key Vault operators. **Never place sensitive data in ConfigMaps**.

## SecurityContext Defaults & Hardening
Apply Pod Security Admission at the namespace level to enforce `Restricted` or `Baseline` standards.
```yaml
securityContext: # Pod-level
  runAsNonRoot: true
  runAsUser: 1000
  fsGroup: 2000
  seccompProfile:
    type: RuntimeDefault
# Container-level
securityContext:
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: true
  capabilities:
    drop: [ALL]
```
- Restrict file modifications utilizing `tmpfs` mounts.
- Sign and scan images. Avoid the `:latest` tag explicitly.

## Resource Quotas & Autoscaling (QoS)
- **Guaranteed Resources**: Defines absolute compute allocations ensuring stability. `requests == limits` prevents CPU throttling or OOMKilled evictions. Identify requests as baseline scheduling metrics, limits as upper bounds.
- **HPA / VPA**: Configure a Horizontal Pod Autoscaler (HPA) leveraging targets parameters like `targetCPUUtilizationPercentage`. Configure Vertical Pod Autoscaler (VPA) appropriately. Configure Pod Disruption Budgets (PDB) guaranteeing minimum availability during updates. Configure Anti-affinity rules across availability zones.

## Probe Management
Configure timeouts, delays, and thresholds specifically per service.
1. **Liveness**: Restart the container if it stalls heavily.
2. **Readiness**: Manage network routing; halt incoming traffic during initialization or overload.
3. **Startup**: Extend boot protection sequences for legacy, slow-starting services.

## Validation & Workflow Tooling
- Validation Commands:
  - `kubectl apply --dry-run=client/server -f manifest.yaml`
  - `kubeconform -strict manifest.yaml`
  - Conftest, Kyverno, or Datree for operational policy checking.
- Rollout Commands: `kubectl rollout status/undo/restart deployment/NAME`.

## Checklists & Conventions
- **Labels**: Standard labels applied: `app.kubernetes.io/name`, `instance`, `version`, `component`, `part-of`, `managed-by`. Additional tracking: `environment`, `team`, `cost-center`.
- **Annotations**: Incorporate scraping `prometheus.io/scrape/port`, and Git contextual hashes.

## Deployment Strategies
- **RollingUpdate (Default)**: Gradual replacement via `maxSurge` and `maxUnavailable`. Maximize availability.
- **Blue/Green**: Two identical environments; explicit traffic flips for risk-free zero-downtime cuts.
- **Canary / Dark Launching**: Gradual routing increment to subsets of users to test anomalies implicitly before switching major traffic. Support with Feature flags.

## Troubleshooting Cheatsheet
- **1. Pods Pending/CrashLoopBackOff**: Check `kubectl describe pod` for events. Scan `kubectl logs -c`. Check image pull credentials and resource capacity limitations. Verify ConfigMaps/Secrets exist.
- **2. Pods Not Ready**: Validate Readiness probe. Confirm application is binding to `0.0.0.0` or its appropriate service port, not localhost.
- **3. Service Not Accessible**: Validate Service selectors explicitly match Pod labels. Check `type`. Check Ingress Controller routing logs. Verify blocking `NetworkPolicies`.
- **4. Resource Exhaustion (OOMKilled)**: Check memory limits, adjust them. Review app leakage. Monitor `kubectl top pod`.