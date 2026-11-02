# Docker & Containerization Patterns

Your goal is to build highly efficient, secure, portable container applications utilizing multi-stage builds. Ensure environments explicitly decouple development needs (hot-reloading, debuggers) from production needs.

## Dockerfile Design & Multi-Stage Builds
- **Builder Stage**: Group compilation, dependencies, and tools. Copy only finalized artifacts (`--from=builder`) to the runtime stage.
- **Base Images**: Depend on official, minimal base images (e.g., `alpine`, `slim`, distroless). ALWAYS identify specific SHA/semver tags (NEVER `:latest`).
- **Layer Optimization**: Order instructions from **least to most frequently changed** (e.g., copy `package.json`, install deps, then copy source code) to leverage build caching. Combine complex `RUN` commands with `&&` to clean up temporary artifacts in the same layer.
- **Security Check**:
  - **Run as non-root user**: `RUN addgroup -S appgroup && adduser -S appuser ... USER appuser`.
  - Exclude secrets from the image with `.dockerignore` (`.env`, `node_modules`, `.git`).
- **Health Checks**: Always implement a `HEALTHCHECK` instruction (e.g., `HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost/health || exit 1`).

## Docker Compose for Local Development
- **Split Configuration**: Utilize a base `docker-compose.yml` for shared settings and a `docker-compose.override.yml` for local enhancements (debug ports, hot reload volumes). Explicitly employ a `docker-compose.prod.yml` to define production deployments specifying restart policies and constraints.
- **Volume Strategies**:
  - `Named Volumes`: Persistent storage for DBs (`pgdata:/var/lib/postgresql/data`).
  - `Bind Mounts`: Development source code synchronization (`.:/app`).
  - `Anonymous Volumes`: Protecting container-managed dependencies from host overrides (`/app/node_modules`).
- **Networking**: Employ custom named networks. Control precisely what connects (e.g., database network exclusively accessible by the backend API, not the frontend). Expose host ports only when necessary `127.0.0.1:5432:5432`.

## Security & Hardening
- **Capabilities**: Drop unused Linux capabilities:
  ```yaml
  cap_drop:
    - ALL
  cap_add:
    - NET_BIND_SERVICE
  ```
- **Filesystem**: Set `read_only: true` on the container filesystem leveraging `tmpfs` mounts (`/tmp`) for writable necessities.
- **Secrets Management**: Map sensitive variables into containers using `env_file` or Docker Swarm secrets. UNDER NO CIRCUMSTANCES hardcode keys in Dockerfiles or Compose files directly. Ensure `.dockerignore` captures `.env*`.

## Common Troubleshooting Playbook
1. **Large Image Size**:
   Identify large files with `docker history <image>`. Migrate to Alpine/distroless. Clean package manager caches immediately during the install `RUN` block.
2. **Slow Builds**:
   Invalid cache layering. Guarantee `.dockerignore` filters fast-changing log/build files. Cache dependencies strictly before application logic arrays.
3. **Container Crashing on Startup**:
   Validate `CMD` and `ENTRYPOINT` syntax. Test with `docker logs <container_id>` or `docker compose exec <svc> sh`. Probe resource constraints (OOM errors) limiting RAM artificially.
4. **Permissions Issues Inside Container**:
   Confirm volume mounts do not overlap root-owned directories. Force `--chown=appuser:appgroup` during `COPY` operations. Configure the host directory permissions equally.
5. **Network Connectivity Issues**:
   Container-to-container traffic connects via Service names (`db:5432` not `localhost:5432`). Use `docker network inspect` to verify container placement. Execute `nslookup <service>` remotely inside the caller container.