# ADR-0005: Stateless JWT Authentication & RBAC Architecture

## Status
Accepted

## Date
2026-09-11

## Context
CareFlow requires a robust authentication and authorization mechanism that supports:
1. Multi-role hospital staff (physicians, nurses, pharmacists, lab technicians, billing officers, administrators, and patients).
2. Stateless REST API interactions for the Angular frontend and external systems.
3. High horizontal scalability and low latency without database lookups on every incoming request.
4. Defense against credential-stuffing and brute-force attacks.

We evaluated three primary authentication patterns:
1. Stateful HTTP sessions with cookies (standard JSESSIONID).
2. OAuth2 / OpenID Connect authorization server (e.g. Keycloak).
3. Self-contained stateless JSON Web Tokens (JWT) signed via HMAC-SHA256 with Spring Security.

## Decision
We chose **Stateless JWT Authentication using Spring Security and HMAC-SHA256**:
- **Statelessness**: `SessionCreationPolicy.STATELESS` is enforced across all filter chains.
- **Claims in Token**: The issued JWT encapsulates `userId`, `username`, `email`, and the user's `roles` and `permissions`. This allows downstream filters and authorization guards to evaluate RBAC without querying MySQL on every request.
- **Password Security**: Passwords are saved strictly as BCrypt hashes with a work factor (strength) of 12. Plaintext passwords never touch persistence.
- **Brute-Force Protection**: An automated 5-strike lockout policy is enforced at the service level; after 5 consecutive failed attempts, the account transitions to `LOCKED` status for 15 minutes.
- **Unified Error Handling**: Custom `AuthenticationEntryPoint` and `AccessDeniedHandler` implementations ensure that 401 and 403 errors are returned as JSON adhering strictly to CareFlow's `ApiErrorResponse` contract, preventing default HTML error pages or redirects.

## Alternatives Considered

### 1. Stateful HTTP Sessions (JSESSIONID)
- **Drawbacks**: Requires sticky sessions or a shared session store (Redis) across application instances. Cross-Site Request Forgery (CSRF) mitigation overhead complicates REST API consumption.
- **Verdict**: Rejected for modern decoupled Angular single-page application architecture.

### 2. External Identity Provider / OAuth2 Server (Keycloak)
- **Drawbacks**: Adds significant operational complexity, container resource consumption, and maintenance overhead prematurely for Phase 1.
- **Verdict**: Deferred. The internal modular monolith authentication boundary can easily be transitioned to delegate to Keycloak/OAuth2 in future phases if required.

## Consequences

### Positive:
- High performance: Token verification is computational (HMAC verification) without database round-trips.
- Clean contract: Fully decoupled client authentication via `Authorization: Bearer <token>` header.
- Angular ready: Standard HTTP interceptor on Angular can store the JWT in memory/local storage and attach it to requests.

### Negative:
- Invalidation challenge: Stateless JWTs cannot be revoked before expiration without maintaining a token revocation list/blocklist (e.g. in Redis). For Phase 1, access tokens have a short TTL (24h) and account status is validated at login.

## References
- Master Build Prompt §14, §44, §45, §78
- `AGENTS.md`
