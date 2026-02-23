# Backend Test Scenarios

Last updated: 2026-02-23

## Scope

This document defines executable backend test scenarios and the matching automated tests.

## Preconditions

1. Start dependencies:
   `docker-compose up -d postgres redis zookeeper kafka`
2. Run tests in backend module:
   `cd backend && .\gradlew.bat test --no-daemon --console=plain`
3. For P0 operational verification bundle:
   `.\scripts\verify-p0.ps1`
4. For full specification verification bundle:
   `.\scripts\verify-spec-v2.1.ps1`

## Scenarios

| ID | Scenario | Expected Result | Automated Test |
|---|---|---|---|
| TS-01 | Spring application context starts with test overrides | Context loads without bean creation failure | `com.safety.platform.IndustrialSafetyPlatformApplicationTests.contextLoads` |
| TS-02 | Saving sensor reading creates immutable event log entry | Event count increases, latest event matches sensor ID, hash fields are set | `com.safety.platform.service.EventServiceIntegrationTest.saveSensorReading_persistsEventLog` |
| TS-03 | Event log update attempt is blocked | Repository update throws exception with immutable message | `com.safety.platform.service.EventServiceIntegrationTest.updateAttempt_throwsByPreUpdateGuard` |
| TS-04 | Audit statistics aggregation is correct | UPDATE/DELETE totals and last attempted time are returned | `com.safety.platform.service.AuditServiceIntegrationTest.getStatistics_returnsOperationCounts` |
| TS-05 | Recent events API contract | `GET /api/events/recent` returns 200 and event list payload | `com.safety.platform.controller.EventLogControllerTest` |
| TS-06 | Outbox stats API contract | `GET /api/outbox/stats` returns 200 and `pending` count | `com.safety.platform.controller.OutboxControllerTest` |
| TS-07 | Audit APIs contract | `GET /api/audit/stats` and `GET /api/audit/modification-attempts` return expected JSON schema/content | `com.safety.platform.controller.AuditLogControllerTest` |
| TS-08 | Sensor latest API contract | `GET /api/sensors/latest` returns cached reading list payload | `com.safety.platform.controller.SensorControllerTest` |
| TS-09 | Hash chain validator integrity check | Valid chain returns true, broken chain returns false | `com.safety.platform.service.EventIntegrityValidatorTest` |
| TS-10 | Kafka lag API contract | `GET /api/kafka/lag` returns consumer group lag payload | `com.safety.platform.controller.KafkaLagControllerTest` |
| TS-11 | Storage tier APIs contract | `GET /api/storage/tiers/summary`, `POST /api/storage/tiers/archive/run` return expected payload | `com.safety.platform.controller.StorageTierControllerTest` |
| TS-12 | Streams SLA API contract | `GET /api/streams/sla` returns threshold/check summary payload | `com.safety.platform.controller.StreamsSlaControllerTest` |
| TS-13 | Streams SLA capture logic | New recovery metric sample is persisted once, duplicate sample is ignored | `com.safety.platform.service.StreamsSlaServiceTest` |
| TS-14 | Full v2.1 acceptance verification | End-to-end checks for partitioning, immutability/audit, storage tiers, kafka lag, streams SLA, Kafka UI | `scripts/verify-spec-v2.1.ps1` |

## P0 Operational Bundle

- Script: `scripts/verify-p0.ps1`
- Purpose:
  - Validate immutable update attempts are blocked and audit count increases.
  - Validate backend container health state is `healthy`.
  - Validate `/api/audit/stats` and `/actuator/health` basic responses.

## Execution Record

- Status: Completed
- Executed at: 2026-02-20
- Command: `cd backend && .\gradlew.bat test --no-daemon --console=plain`
- Summary: 11 tests executed, 0 failures, 0 errors
- Per-suite:
  - `com.safety.platform.IndustrialSafetyPlatformApplicationTests`: 1 passed
  - `com.safety.platform.service.EventServiceIntegrationTest`: 2 passed
  - `com.safety.platform.service.AuditServiceIntegrationTest`: 1 passed
  - `com.safety.platform.service.EventIntegrityValidatorTest`: 2 passed
  - `com.safety.platform.controller.EventLogControllerTest`: 1 passed
  - `com.safety.platform.controller.OutboxControllerTest`: 1 passed
  - `com.safety.platform.controller.AuditLogControllerTest`: 2 passed
  - `com.safety.platform.controller.SensorControllerTest`: 1 passed

### Incremental Check (2026-02-23)

- Command:
  - `cd backend && .\gradlew.bat test --no-daemon --console=plain --tests "com.safety.platform.controller.AuditLogControllerTest" --tests "com.safety.platform.service.EventIntegrityValidatorTest"`
- Result:
  - 2 target suites passed (exit code 0)

### P0 Runtime Verification (2026-02-23)

- Command:
  - `docker-compose up -d --build backend frontend`
  - `docker exec safety-postgres psql -U safety_user -d safety_db -f /docker-entrypoint-initdb.d/03-immutability-constraints.sql`
  - `.\scripts\verify-p0.ps1`
- Result:
  - Immutable update attempt blocked with `IMMUTABILITY_VIOLATION`.
  - `event_audit_log` count increased from 4 to 5.
  - Backend container health reached `healthy`.
  - `/actuator/health` returned `UP`.

### Feature Runtime Verification (2026-02-23)

- Command:
  - `docker-compose up -d --build backend`
  - `docker exec safety-postgres psql -U safety_user -d safety_db -f /docker-entrypoint-initdb.d/04-event-archive-manifest.sql`
  - `docker exec safety-postgres psql -U safety_user -d safety_db -f /docker-entrypoint-initdb.d/05-kafka-recovery-sla-log.sql`
  - `GET /api/storage/tiers/summary`
  - `POST /api/storage/tiers/archive/run`
  - `GET /api/kafka/lag?groupId=safety-group`
  - `GET /api/streams/sla`
- Result:
  - Storage tier summary returned hot/warm/cold and archived counts.
  - Storage archive API executed successfully (`No unarchived cold events found.`).
  - Kafka lag API returned per-topic lag and total lag.
  - Streams SLA API returned threshold, checks, and recent compliance history.

### Full Regression (2026-02-23)

- Command:
  - `cd backend && .\gradlew.bat test --no-daemon --console=plain`
- Result:
  - 18 tests executed, 0 failures, 0 errors (BUILD SUCCESSFUL)

### Full Specification Verification (2026-02-24)

- Command:
  - `.\scripts\verify-spec-v2.1.ps1`
- Result:
  - Monthly partitioning verified (`safety_event_log` relkind=`p`, partitions=16).
  - Immutability block + audit increase verified (`event_audit_log` count increased).
  - Storage tier APIs (`/api/storage/tiers/*`) verified.
  - Kafka lag API (`/api/kafka/lag`) verified.
  - Streams SLA API (`/api/streams/sla`) verified.
  - Kafka UI reachable (`http://localhost:8085`, HTTP 200).
