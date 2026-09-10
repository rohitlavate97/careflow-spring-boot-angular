# ADR-0001: Modular Monolith Architecture

## Status
Accepted

## Date
2026-09-11

## Context
CareFlow is an enterprise-grade Hospital & Healthcare Operations Platform managing 20 core business domains (identity, patient, doctor scheduling, appointments, queues, clinical consultations, pharmacy inventory, laboratory orders, admission, billing, and insurance claims).

Enterprise hospital operations are characterized by deep transactional coupling (e.g. appointment booking coupled with doctor availability; pharmacy dispensing coupled with inventory decrements; bed assignment coupled with patient admission). 

When designing the core architecture, we evaluated whether to construct:
1. A distributed microservices architecture from the outset.
2. A single unstructured monolithic application with shared entity and service layers.
3. A strictly modular monolith structured by domain package boundaries.

## Decision
We chose a **Modular Monolith** architecture built with Java 21, Spring Boot 3.x, and MySQL.

Key architectural boundaries:
- **Package-by-feature**: Each domain module resides under `com.careflow.<module>` and owns its internal architecture (`controller`, `service`, `repository`, `domain`, `dto`, `mapper`, `exception`).
- **Encapsulation**: Direct cross-module entity sharing or direct repository queries across domain boundaries are forbidden. Modules interact only through defined service interfaces or domain events.
- **Cross-cutting foundation**: Shared technical concerns (logging, correlation tracking, error models, base auditing) reside in `com.careflow.common`.
- **Database design**: A single unified database schema where tables are organized by domain prefixes, with foreign keys and unique constraints enforcing database-level integrity, and all schema evolutions governed strictly by Flyway.

## Alternatives Considered

### 1. Distributed Microservices
- **Drawbacks at this stage**: Premature distributed complexity, network latency, distributed transaction failure modes (2PC / Saga overhead), operational overhead (service meshes, distributed tracing complexity), and high deployment friction before domain boundaries have stabilized.
- **Verdict**: Rejected for initial phases. The modular monolith can be decomposed into microservices later along verified domain boundaries if scaling bottlenecks demand it.

### 2. Layered "Spaghetti" Monolith
- **Drawbacks**: Controllers, giant shared services, and mixed repositories sharing entities across the entire application create high coupling, making testing difficult and leading to brittle codebases prone to unintended regression side effects.
- **Verdict**: Rejected.

## Consequences

### Positive:
- **Transactional consistency**: ACID transactions can span closely related domain operations without distributed coordinators.
- **High development velocity**: Easy local orchestration, fast test feedback loop, and straightforward debugging.
- **Clear migration path**: Clean package boundaries make isolating any module into an autonomous microservice straightforward if individual scaling characteristics require it in the future.
- **Simpler observability**: End-to-end request tracing runs in-process with minimal network hops.

### Negative:
- **Discipline required**: Engineers must respect module boundaries and resist cross-package imports of JPA entities or repositories.
- **Single deployment artifact**: Deployments update the entire monolith, requiring comprehensive regression test coverage.

## References
- Master Build Prompt §1, §7, §8, §78
- `AGENTS.md`
