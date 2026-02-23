| 기준 시점 | 구현 완성도(추정) | 운영 완성도(추정) |
|---|---:|---:|
| 2026-02-24 | 99~100% | 97~99% |

| Phase | 명세 핵심 | 상태 | 구현/실행 근거 | 남은 항목 |
|---|---|---|---|---|
| Phase 1 (MVP) | 이벤트 저장/조회, 불변성 L1/L2, Audit API, 1초 폴링 | 완료(운영 검증 포함) | `backend/src/main/java/com/safety/platform/domain/SafetyEventLog.java:56`, `init-scripts/03-immutability-constraints.sql:28`, `backend/src/main/java/com/safety/platform/scheduler/SensorPollingScheduler.java:27`, `backend/src/main/java/com/safety/platform/controller/AuditLogController.java:16`, `init-scripts/06-monthly-partitioning.sql`, `scripts/verify-spec-v2.1.ps1` | 없음 |
| Phase 2 (Streaming) | Kafka Producer/Consumer, Redis 캐시, Streams 상태저장소/Health | 완료 | `backend/src/main/java/com/safety/platform/service/KafkaProducerService.java`, `backend/src/main/java/com/safety/platform/service/KafkaConsumerService.java:25`, `backend/src/main/java/com/safety/platform/service/SensorCacheService.java`, `backend/src/main/resources/application.yaml:44`, `docker-compose.yml:145`, `backend/src/main/java/com/safety/platform/config/KafkaStreamsHealthIndicator.java:21`, `backend/src/main/java/com/safety/platform/controller/KafkaLagController.java:12`, `docker-compose.yml`(`kafka-ui`) | 없음 |
| Phase 3 (Advanced) | Kafka Streams 집계, 상태머신, Outbox, Circuit Breaker, 복구 | 완료 | `backend/src/main/java/com/safety/platform/config/KafkaStreamsConfig.java:30`, `backend/src/main/java/com/safety/platform/service/OutboxService.java:19`, `backend/src/main/java/com/safety/platform/scheduler/OutboxScheduler.java:14`, `backend/src/main/java/com/safety/platform/service/AlertService.java:19`, `backend/src/main/java/com/safety/platform/config/KafkaStreamsLifecycleConfig.java`, `backend/src/main/java/com/safety/platform/service/StreamsSlaService.java` | 없음 |
| Phase 4 (Monitoring/Frontend) | Prometheus/Grafana, React+WebSocket, Hash chaining(선택) | 완료(목표치 모니터링 단계) | `monitoring/prometheus.yml`, `monitoring/grafana/dashboards/safety-overview.json`, `frontend/src/App.js`, `backend/src/main/java/com/safety/platform/config/WebSocketConfig.java:10`, `backend/src/main/java/com/safety/platform/service/EventService.java:56`, `backend/src/main/java/com/safety/platform/service/EventIntegrityValidator.java:21`, `backend/src/main/java/com/safety/platform/controller/StorageTierController.java:13` | KPI(무단 수정 시도 0건/년)는 운영 추적 지표 |

| v2.1 핵심 항목 | 상태 | 근거 |
|---|---|---|
| 3단계 불변성 방어 | 완료(실행검증) | L1 `@PreUpdate`, L2 트리거/감사로그, 해시체인 저장/검증, 실검증에서 `IMMUTABILITY_VIOLATION` 및 감사로그 증가(4→5) |
| 감사 로그 시스템 | 완료 | `event_audit_log` 누적 및 `/api/audit/stats` 정상 |
| Kafka Streams 상태 복구 메커니즘 | 완료 | 상태볼륨/리스너/헬스 + SLA 자동 검증/리포트 API(`/api/streams/sla`) 구현 |
| Outbox Pattern | 완료(기능) | enqueue/process/stats 동작 |
| Hot-Warm-Cold 계층화 저장 | 완료 | tier summary + cold archive(manifest + MinIO object) + 수동/스케줄 실행 경로 구현 |
| 월별 파티셔닝 스크립트 | 완료 | `init-scripts/06-monthly-partitioning.sql` + 통합 검증에서 relkind=`p`, partitions=16 확인 |
| Kafka UI 기반 운영 확인 | 완료 | `docker-compose.yml`에 `kafka-ui` 추가 + 통합 검증에서 `http://localhost:8085` 200 확인 |

| 런타임 점검 항목(2026-02-23) | 결과 |
|---|---|
| 컨테이너 상태 | backend/kafka/postgres/redis/minio/grafana/prometheus 실행, backend healthy |
| 헬스 | `/actuator/health` = `UP` |
| 주요 API | `/api/sensors/latest`, `/api/events/recent`, `/api/audit/stats`, `/api/outbox/stats`, `/api/storage/tiers/summary`, `/api/kafka/lag`, `/api/streams/sla` 정상 응답 |
| 프론트 | `http://localhost:3001` 응답 200 |
| 감사로그 누적 건수 | `event_audit_log` = 5 |
| Kafka UI | `http://localhost:8085` 응답 200 |
| 파티셔닝 | `safety_event_log` relkind=`p`, 파티션 16개 |
