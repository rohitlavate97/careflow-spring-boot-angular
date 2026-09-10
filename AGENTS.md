# CareFlow — Agent Operating Rules

Enterprise Hospital & Healthcare Operations Platform.
Authoritative specification: `CareFlow — Enterprise Hospital & Healthcare Operations Platform — Master Build Prompt.md`
(section references below are `§N` from that document).

These rules apply to **every** agent working in this repository.

---

## 1. What this project is

A **modular monolith**: Java 21 · Spring Boot 3.x · Spring Security · Spring Data JPA /
Hibernate · Flyway · MySQL 8+ · Maven, with an Angular + TypeScript frontend.
Redis, Kafka, Docker, GitHub Actions, Prometheus, Grafana and OpenTelemetry are added
progressively, only when a real problem justifies them.

It is a **software engineering portfolio and interview-preparation project**. It makes
no medical claims — no diagnosis, no clinical decision-making, no treatment
recommendations (§3).

Modules (`com.careflow.<module>`, package-by-feature):

```
identity  staff  patient  department  scheduling  appointment  queue  consultation
clinical  prescription  pharmacy  laboratory  admission  billing  insurance
document  notification  audit  reporting  administration
```

Each module: `controller · service · repository · domain · dto · mapper · exception`.

---

## 2. The development loop — follow for every functionality (§84)

```
Understand requirement → Inspect repository → Identify what exists → Identify what's missing
→ Define business rules → Design domain model → Design database changes → Flyway migration
→ Implement backend → Validation → Security → API → Angular UI
→ Tests → Integration tests → Simulate failure scenario → Debug
→ Review code → Update docs → Review git diff → ONE commit → Push → STOP
```

**STOP means stop** (§85). Never start the next functionality automatically. After each
commit, report:

```
Completed functionality · Files changed · Database changes · APIs added · Frontend changes
Tests added · Failure scenario tested · Documentation updated · Git commit · Push result
Recommended next functionality
```

---

## 3. Standing rules

- **Repository first** (§86) — inspect before changing. Never blindly recreate working code.
- **No fake implementation** (§87) — no TODOs, placeholders, dummy services, fake success
  responses or hard-coded data. Mark anything deliberately incomplete.
- **Business rules first** (§88) — actors, preconditions, rules, state transitions,
  validation, authorization, transaction boundary, concurrency risk, failure behaviour.
- **API first** (§89) — Requirement → Domain → Database → Service → REST API → Tests → Angular.
  Never build UI the backend does not support.
- **Contract integrity** (§90) — a DTO / field / status code / validation / error change
  updates Angular in the same change.
- **Security first** (§91) — for every endpoint: who can call this, what role, which
  resource, what data can they see, what can they modify, does it need audit logging.
- **Database-first concurrency** (§92) — always ask: *what happens if two requests arrive
  at exactly the same time?*
- **One functionality = one commit** (§81), Conventional Commits (§83).

## 4. Never (§105)

skip tests · expose JPA entities · use `double` for money · use `ddl-auto=update` ·
store plain-text passwords · hard-code secrets · ignore concurrency · ignore
authorization · create giant services · create microservices prematurely · swallow
exceptions · log sensitive data · fake repositories or responses · silently change
requirements · combine unrelated features in one commit · continue automatically after
a commit.

## 5. Definition of Done (§104)

Business rules · migration · backend · validation · authorization · API · frontend where
applicable · unit tests · integration tests · failure case tested · logging · audit where
required · docs updated · code reviewed · diff reviewed · build passes · tests pass ·
commit created · branch pushed.

## 6. Primary engineering principle (§113)

For every feature ask: *Is it correct? secure? transactional? concurrent-safe? testable?
observable? maintainable? performant? auditable? What happens when it fails?*

---

## 7. Agent roster

Definitions live in `.claude/agents/`. Each file is self-contained markdown with YAML
frontmatter and can be used by Claude Code, Antigravity, or any agent runner that reads
markdown agent definitions.

| Agent | Use it for |
|---|---|
| [`careflow-architect`](.claude/agents/careflow-architect.md) | Repo & architecture audit, feature design specs, module boundaries, ADRs, phase planning. **Start here for any new functionality.** |
| [`careflow-backend`](.claude/agents/careflow-backend.md) | Spring Boot service implementation, business rules, state machines, transaction boundaries. |
| [`careflow-jpa-db`](.claude/agents/careflow-jpa-db.md) | Schema design, Flyway migrations, entity mapping, fetch strategy, N+1, projections, indexing. |
| [`careflow-concurrency`](.claude/agents/careflow-concurrency.md) | Race conditions, locking strategy, idempotency, the six concurrency labs, multi-threaded tests. |
| [`careflow-security`](.claude/agents/careflow-security.md) | Spring Security, JWT, RBAC, resource-level authorization, security test matrix, PHI/PII handling. |
| [`careflow-api`](.claude/agents/careflow-api.md) | Endpoint design, DTOs, validation, error contract, status codes, pagination, OpenAPI. |
| [`careflow-angular`](.claude/agents/careflow-angular.md) | Angular feature modules, guards, interceptors, reactive forms, role workspaces, contract sync. |
| [`careflow-testing`](.claude/agents/careflow-testing.md) | Unit / repository / controller / security / integration / concurrency / E2E tests, Testcontainers, seed data. |
| [`careflow-events`](.claude/agents/careflow-events.md) | Redis caching, Kafka events, transactional outbox, eventual consistency, graceful degradation. |
| [`careflow-observability`](.claude/agents/careflow-observability.md) | Structured logging, correlation IDs, Actuator, Micrometer, Prometheus/Grafana, tracing. |
| [`careflow-devops`](.claude/agents/careflow-devops.md) | Docker & Compose, profiles/config, GitHub Actions, quality gates, background jobs, rate limiting. |
| [`careflow-debugger`](.claude/agents/careflow-debugger.md) | Anything that fails — root-cause process and the CareFlow debugging playbook. |
| [`careflow-reviewer`](.claude/agents/careflow-reviewer.md) | Pre-commit code review and the Definition of Done gate. |
| [`careflow-git`](.claude/agents/careflow-git.md) | Staging, the single commit, Conventional Commit message, push, STOP report. |
| [`careflow-mentor`](.claude/agents/careflow-mentor.md) | "Why?" explanations, interview guide, system-design answers, text diagrams. |

### Typical chain for one functionality

```
careflow-architect   (spec)
   ↓
careflow-jpa-db      (migration + entities)
   ↓
careflow-backend     (service + rules)   ← careflow-concurrency if shared state
   ↓
careflow-api         (endpoint + DTOs)   ← careflow-security (authz)
   ↓
careflow-angular     (UI)
   ↓
careflow-testing     (tests)             ← careflow-debugger if red
   ↓
careflow-observability (logs/metrics)
   ↓
careflow-reviewer    (gate)
   ↓
careflow-git         (commit → push → STOP)
```
