---
name: security
description: Use for security reviews, threat modeling, and security design. Use before any code changes to plan implementation. Use for when you need to create documentation, design decisions, or implementation plans or refactor plans.
---

## Overview

This reference details the OWASP requirements and as a reference guide for agents to review or design implementations.

<!-- Reference: https://owasp.org/www-project-developer-guide/assets/exports/OWASP_Developer_Guide.pdf -->

## OWASP Top 10

Use these as the default review lens for web and API changes:

- A01 Broken Access Control: verify ownership checks, role checks, tenant isolation, and server-side enforcement.
- A02 Cryptographic Failures: verify approved algorithms, key handling, secure randomness, and TLS.
- A03 Injection: verify parameterized queries, safe command execution, template safety, and validation.
- A04 Insecure Design: verify threat-aware defaults, abuse-case handling, and least privilege.
- A05 Security Misconfiguration: verify headers, environment hardening, disabled debug paths, and safe defaults.
- A06 Vulnerable and Outdated Components: verify dependency hygiene, supported versions, and supply-chain controls.
- A07 Identification and Authentication Failures: verify password handling, MFA where needed, session rotation, and anti-enumeration behavior.
- A08 Software and Data Integrity Failures: verify trusted build inputs, signed artifacts where applicable, and protected CI/CD.
- A09 Security Logging and Monitoring Failures: verify auditability without leaking secrets.
- A10 Server-Side Request Forgery: verify URL allow-lists, metadata protection, and egress controls.


## 1. Authentication

### User Authentication
- Require authentication for all pages/resources except those intended to be public.
- Perform all authentication server-side over an encrypted channel (HTTPS/TLS).
- Use centralized, security-vetted implementations for authentication and federation (e.g., Okta, PING).
- Segregate auth logic from the requested resource; use redirection to/from central auth controls.
- Validate authentication data only upon completion of all input.
- Prevent account enumeration: Use generic error messages (e.g., "Invalid username and/or password") and consistent responses across registration, reset, and API pathways.
- Do not store passwords in code or configuration files—use a Secrets Manager.
- Use only HTTP POST requests for authentication credentials.
- Re-authenticate users prior to critical operations or accessing admin sections.
- Implement Multi-Factor Authentication (MFA) for highly sensitive/high-value accounts.
- Restrict access and implement lockouts/rate-limiting to prevent brute-force and credential stuffing attacks.

### Server & System Authentication
- Establish server identity via SSL/TLS following a trust chain to a known root certificate.
- Validate server certificate host information.
- Disable HTTP TRACE (to mitigate WAF bypass) and WebDAV (unless required with a risk assessment).
- Grant web server accounts least privilege (only necessary folders and permissions).
- Do not rely solely on IP addresses or DNS names for identity.
- Ensure development/debug backdoors are immediately removed before production.

### Password Policy
- Enforce complexity (alphanumeric + special characters) and length requirements (min 8 chars, preferably 16+ or passphrases).
- Support self-reset mechanisms with temporary, short-lived, encrypted links/passwords sent via registered email.
- Require password changes on next use for temporary credentials.
- Limit bad guesses (e.g., 5 attempts in 5 minutes) before safe account lockout.
- Obscure password entry on screens (e.g., `type="password"`).
- Prevent password reuse and enforce minimum age (e.g., 1 day) to block rapid reuse looping.
- Disable "remember me" functionality for password fields.
- Hash and salt persisted passwords in the database using strong, vetted algorithms. Validate using hash comparisons.

## 2. Authorization & Access Control
- Build authorization around Role-Based Access Control (RBAC); persist rules securely (e.g., access matrix).
- Authenticate *before* checking authorization.
- Enforce least privilege: apply privileges to groups, and add users to those groups.
- Authorize consistently on *every* request (including APIs, POST/PUT/DELETE, server-side scripts, and rich clients).
- Do not rely on the `Referer` header alone as it can be spoofed.
- Prevent forced browsing and Insecure Direct Object References (IDOR): validate that users are authorized for the exact data/lookup IDs they request.
- Keep authorization state out of query strings.

## 3. Session Management

### Core Session Controls
- Use the server's/framework's built-in session management controls.
- Generate and manage session identifiers on a trusted server (never client-side).
- Close pre-login sessions and enforce a new session identifier on successful re-authentication.
- Rely on CSPRNG (Cryptographically Secure Pseudo-Random Number Generators) for session IDs.
- Set appropriate restricted domain and path values for cookies. Use `HttpOnly` and `Secure` flags.
- Establish short session inactivity timeouts based on risk/business needs.
- Fully terminate sessions on logout.
- Keep session IDs out of URLs, error messages, and logs (use HTTP cookies instead).

### JWT (JSON Web Tokens)
- Reject tokens with the `none` algorithm (`alg: "none"`).
- Enforce the appropriate key length and explicitly restrict accepted algorithms to prevent manipulation (e.g., from `RS256` to `HS256`).
- Validate `aud` (audience), `sub` (subject/client ID), and `iss` (issuer fields against expected values).
- Keep token lifetimes short (minutes to an hour). For revocability, use reference tokens or enforce blocklists during logout.
- Transmit JWTs strictly over HTTPS/SSL as payloads are plainly encoded.
- Rotate authorization server keys frequently and validate X509 certificates (PKIX).
- Always verify token signatures before processing payload data.

## 4. Input & Output Processing

### Input Validation
- Treat all input as untrusted. Validate type, format, length, and range centrally using security-vetted libraries.
- Parameterize all SQL/Database queries (use prepared statements and bind variables). Never concatenate raw input.
- Validate HTTP headers, redirects, hidden fields, dropdowns, and JS postbacks.
- Address hazardous characters discretely (e.g., null bytes `%00`, path traversal `../`, newlines `%0a`).
- Implement an allow-list approach for redirects, file handling, and inputs.

### Output Encoding
- Contextually encode/sanitize all output of untrusted data before reflecting it to the client, database, or OS (e.g., HTML entity encoding, SQL sanitization).
- Output encode on a trusted system (server-side).
- Quote variables in JS strings to prevent context shifting, avoiding unsafe functions like `eval()`, `setInterval()`, or innerHTML assignments.
- Canonicalize input (e.g., to UTF-8) to prevent character spoofing and double-encoding bypassing.

### JSON Integrity
- Serve JSON with `Content-Type: application/json` to prevent XSS.
- Parse JSON with standard native libraries (`JSON.parse()`); **never** use `eval()`.
- Generate fatal errors for duplicate keys rather than truncating them.
- Address JSON hijacking by wrapping/prefixing JSON or returning non-array objects.

## 5. Cryptography & Data Protection
- Only use standard, community-vetted cryptographic algorithms and protocols (do not roll your own).
- Do not use hard-coded keys, passwords as keys, or weak initial vectors (never route an IV twice for a fixed key).
- Store encryption/secret keys away from the data in secure vaults (HSM, KMS, Secrets Manager).
- Enforce Key Lifecycle management (Creation, Storage, Rotation, Revocation, Destruction).
- Establish message integrity via MAC/combined modes and ensure block sizes >= 128-bit / hash outputs >= 160-bit.
- Implement strict secure communication (TLS over strong cipher suites); never fallback to an insecure connection.
- Eliminate secrets from memory as soon as they are successfully processed.

## 6. Exception, Error Handling & Logging

### Error Handling
- Fail securely: default to safe states and release/unlock resources (connection pools, memory, handles) gracefully in `finally` blocks.
- Never expose stack traces, sensitive system information, or exact points of failure to the end user.
- Handle exceptions specifically and do not "swallow" exceptions blindly into empty `catch()` blocks without an audit trail.

### Secure Logging
- Log user ID, event type, date/time, success/fail state, origination, and target system state.
- **Never** log sensitive info (credentials, cookies, full payloads, session tokens, or unmasked crypto-keys).
- Enforce length limitations on log payloads to deter log exhaustion.
- Log critical actions: Admin actions, privilege escalations, validation failures, auth attempts (especially failures), access control failures, and exceptions.
- Restrict access to log files and consider cryptographic hash functions to validate log entry integrity.

## 7. Application Spoofing & Framing
- Prevent Clickjacking/Application Spoofing by applying HTTP headers like `X-Frame-Options: SAMEORIGIN` or `DENY`.
- Use `Content-Security-Policy: frame-ancestors 'none'` or `'self'` to restrict framing.
- Implement `SameSite` and `HttpOnly` attributes for cookies.
- Defend against Domain/Typo Squatting by monitoring threat intelligence and rigorously verifying packages/registries.

## 8. Content Security Policy (CSP)
- Deploy restrictive CSPs to limit script, style, and object executions (prevent XSS/Clickjacking).
- Prevent the wildcard `*` attribute. Preferred sources: `'self'`.
- Ban `'unsafe-inline'` and `'unsafe-eval'` for `script-src` and `style-src` wherever technically feasible.
- Specify `report-uri` to capture and monitor policy violations.

## 9. File & Memory Management

### File Uploads & Management
- Restrict upload paths to isolated environments, non-executable directories, or separated content servers.
- Authenticate users before allowing file uploads and strictly allow-list permitted file extensions/header bytes.
- Rename uploaded files using CSPRNG with at least 64 bits of randomness to prevent path prediction and direct execution.
- Prevent path traversal by explicitly disallowing directory paths relative to user-supplied input. Do not send absolute paths to clients.
- Implement virus/malware scanning on file intake.

### Memory & System Interaction
- Observe buffer boundaries meticulously (especially when using functions like `strncpy()` in systems programming).
- Ensure explicit cleanup of resources; do not rely strictly on garbage collection for immediate secure release (locks, file descriptors).
- Sanitize inputs leading closely to OS execution (avoid OS injection).

## 10. System Hardening & Secure Environment
- Maintain absolute parity between secure code deployment structure and production. Protect `WEB-INF`, `META-INF`, and internal mappings via `Deny All` rules.
- Prevent accidental direct execution of server-side raw code (e.g., JSP, ASP blocks) when bypassing standard frameworks.
- Strip development metadata, debug code, unused components, and source control histories (e.g., `.git/`) prior to production releases.
- Disable superfluous HTTP methods strictly via allow-listing (Drop `HEAD`, `TRACE`, etc.).
- Isolate non-public directories effectively through robust Server configurations and intelligent use of `robots.txt`.
- Apply strict supply-chain security: obtain dependencies securely, monitor explicitly for outdated/unmaintained components, and patch actively. 
- Ensure all default account passwords are eliminated and updated globally.


## 11. AI and LLM Patterns

- Sanitize or structure prompts instead of concatenating raw user input into privileged instructions.
- Remove sensitive data from model context when possible.
- Filter or validate model outputs before returning them to users or tools.
- Scope tools and retrieval access narrowly, with clear policy checks and rate limits.

## 12. Additional Items

- Use CSRF tokens for state-changing requests.
- Disable external entity resolution in XML parsers to prevent XXE.
- Reject untrusted serialized objects to prevent Unsafe Deserialization.
- Protect shared resources with synchronization. Check and use resources atomically.
- Apply limits and throttling. Always cap resources per user.
- Verify signatures and origins of data. Verify signatures on downloaded code. Always validate checksums.

