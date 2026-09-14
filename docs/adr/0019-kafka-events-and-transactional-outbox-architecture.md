# ADR-0019: Asynchronous Domain Events, Kafka Streaming, and Transactional Outbox Architecture

## Status
Accepted

## Context
CareFlow operates across 20 distinct modular monolith domain packages (§38, §103 Phase 18). While intra-module operations execute within strict relational transaction boundaries (ACID), cross-module notifications, audit streams, and secondary integrations must not artificially lengthen user-facing HTTP transactions, introduce dual-write race conditions, or fail core workflows due to external infrastructure latency (§51, §52, §53).

Key requirements and architectural constraints:
1. **Asynchronous Decoupling via Apache Kafka (§51)**:
   - High-value domain events (`AppointmentBooked`, `AppointmentCancelled`, `PaymentCompleted`, `LabResultCompleted`) must be broadcast asynchronously.
   - Per-aggregate ordering must be preserved by assigning the aggregate root ID (e.g., `appointmentId`, `invoiceId`) as the Kafka partition key.
2. **Elimination of Dual-Write Vulnerabilities via Transactional Outbox (§53)**:
   - Publishing directly to an external message broker within a database transaction is strictly prohibited. If Kafka is slow or fails, the database transaction locks or rolls back; if the database transaction commits but the network fails before Kafka ack, the event is permanently lost.
   - Solution: The Transactional Outbox pattern writes the event payload to an `outbox_events` table inside the *exact same* local database transaction as the aggregate. An asynchronous relay (`OutboxRelayService`) polls and publishes pending records.
3. **Eventual Consistency & Core Workflow Isolation (§52, §76 Scenario 6)**:
   - Downstream integration failures (e.g. SMTP email server down, SMS gateway timeout) must **never** roll back the primary business transaction. If an appointment booking commits, the appointment remains permanently booked even if the notification fails.
4. **Consumer Idempotency against Duplicate Deliveries (§51, §76 Scenario 7)**:
   - Apache Kafka provides at-least-once delivery semantics; network blips and consumer rebalances inevitably cause duplicate deliveries.
   - Consumers must enforce database-backed deduplication by recording `(event_id, consumer_group)` in the `processed_events` table before executing side-effects.
5. **Consumer Failure & Dead-Letter Topic (DLT) Routing (§51, §76 Scenario 8)**:
   - Poison messages must not block partition processing. The consumer container uses `DefaultErrorHandler` with bounded retries (2 attempts with fixed backoff) and routes exhausted failures to `careflow.events.dlt` via `DeadLetterPublishingRecoverer`.
6. **Resilience under Kafka Outage (§6, §53, §76 Scenario 8)**:
   - If the Kafka broker is completely unreachable, outbox events safely accumulate with status `PENDING` in the database without crashing user requests or interrupting hospital operations.

## Decision
1. **Database Schema & Migrations (Flyway V22)**:
   - `outbox_events`: Stores `id`, `aggregate_type`, `aggregate_id`, `event_type`, `payload` (JSON), `topic`, `partition_key`, `status` (`PENDING`, `PUBLISHED`, `FAILED`), `attempt_count`, `occurred_at`, `published_at`, `error_message`.
   - `processed_events`: Stores composite key `(event_id, consumer_group)`, `event_type`, `status`, `processed_at`.
2. **Domain Event Model (`com.careflow.common.event`)**:
   - `DomainEvent` base contract providing `eventId`, `eventType`, `aggregateType`, `aggregateId`, `occurredAt`, `version`.
   - Concrete events: `AppointmentBookedEvent`, `AppointmentCancelledEvent`, `PaymentCompletedEvent`, `LabResultCompletedEvent`.
3. **Transactional Outbox Engine (`com.careflow.common.outbox`)**:
   - `OutboxService` / `OutboxServiceImpl`: Invoked within the caller's `@Transactional` boundary to persist events atomically.
   - `OutboxRelayService`: Periodic `@Scheduled` relay querying pending events, dispatching via `KafkaTemplate`, and marking records `PUBLISHED`. Catches broker connection failures and updates `attempt_count` and `error_message` gracefully.
4. **Kafka Configuration (`com.careflow.common.config.KafkaConfig`)**:
   - Topics: `careflow.appointments`, `careflow.billing`, `careflow.laboratory`, `careflow.events.dlt`.
   - Consumer group: `careflow-notification-group`.
   - Error handler: `DefaultErrorHandler` configured with `DeadLetterPublishingRecoverer`.
   - Feature flag: `careflow.kafka.enabled` allowing seamless offline and hermetic CI testing.
5. **Idempotent Consumer (`AppointmentEventConsumer`)**:
   - Consumes from `careflow.appointments`.
   - Queries `ProcessedEventRepository` to detect and drop duplicate deliveries (§76 Scenario 7).
   - Isolates notification exceptions to guarantee eventual consistency (§76 Scenario 6).

## Consequences
- **Positive**:
  - Zero dual-write discrepancies between the database and message broker.
  - Core clinical and administrative transactions have sub-second latency, free from broker network dependency.
  - True eventual consistency: core data remains committed regardless of downstream transport failures.
  - Guaranteed exactly-once business processing semantics via database-backed consumer deduplication.
  - Unhandled poison messages are safely redirected to the Dead Letter Topic without stalling message consumption.
- **Negative / Trade-offs**:
  - Small polling lag (configurable, default 2 seconds) between database transaction commit and Kafka publication.
  - `outbox_events` and `processed_events` tables require periodic archiving or TTL cleanup in high-volume production environments.
