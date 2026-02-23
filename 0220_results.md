# 0220 결과 정리 (PROJECT_SPECIFICATION_v2.1 대비)

작성일: 2026-02-20

## 1) 종합 완성도 추정

- 구현 완성도(체크리스트 기준): 약 82%
- 운영 완성도(실행/신뢰성 기준): 약 68~72%
- 판단: 기능 구현은 상당 부분 완료, 운영 신뢰성 이슈 2건으로 Production-Ready 평가는 보류

## 2) 주요 분석 결과

### A. 명세 대비 구현된 항목(핵심)

- 이벤트 불변성 Level 1(@PreUpdate) 구현
  - `backend/src/main/java/com/safety/platform/domain/SafetyEventLog.java`
- DB 트리거/감사로그 DDL 존재(Level 2)
  - `init-scripts/03-immutability-constraints.sql`
- Hash chaining 저장/검증 로직 및 스케줄러 구현(Level 3, 선택)
  - 저장: `backend/src/main/java/com/safety/platform/service/EventService.java`
  - 검증: `backend/src/main/java/com/safety/platform/service/EventIntegrityValidator.java`
- Kafka Streams 토폴로지/상태 리스너/헬스 인디케이터 구현
  - `backend/src/main/java/com/safety/platform/config/KafkaStreamsConfig.java`
  - `backend/src/main/java/com/safety/platform/config/KafkaStreamsLifecycleConfig.java`
  - `backend/src/main/java/com/safety/platform/config/KafkaStreamsHealthIndicator.java`
- Outbox 패턴 + 스케줄러 구현
  - `backend/src/main/java/com/safety/platform/service/OutboxService.java`
  - `backend/src/main/java/com/safety/platform/scheduler/OutboxScheduler.java`
- Circuit Breaker 적용(알림 전송)
  - `backend/src/main/java/com/safety/platform/service/AlertService.java`
- 모니터링 지표/대시보드 존재
  - 메트릭: `backend/src/main/java/com/safety/platform/service/MetricsService.java`
  - Grafana: `monitoring/grafana/dashboards/safety-overview.json`
- React + WebSocket 대시보드 동작
  - `frontend/src/App.js`
  - `backend/src/main/java/com/safety/platform/config/WebSocketConfig.java`

### B. 실제 동작 확인 결과

- `GET /actuator/health`: `UP` 확인
- `GET /api/sensors/latest`, `GET /api/events/recent`, `GET /api/audit/stats`, `GET /api/outbox/stats`: 응답 정상
- DB 직접 UPDATE 시도 시 `IMMUTABILITY_VIOLATION` 발생(차단 정상)

## 3) 핵심 이슈(중요)

### 이슈 1. DB 트리거 차단은 되지만 감사로그 누적이 안 됨

- 현상:
  - `UPDATE safety_event_log ...` 실행 시 예외는 정상 발생
  - 그러나 `event_audit_log` 건수는 증가하지 않음(시도 전/후 동일)
- 영향:
  - 명세의 "모든 수정 시도 추적" 요구와 충돌
  - 법적 증거성(추적 가능성) 저하
- 관련 파일:
  - `init-scripts/03-immutability-constraints.sql`

### 이슈 2. backend 컨테이너 healthcheck가 구조적으로 실패

- 현상:
  - `docker ps`에서 backend가 `unhealthy`
  - 원인: healthcheck는 `curl` 실행, runtime 이미지에 `curl` 부재
- 영향:
  - 운영 가용성 판단 왜곡
  - 오케스트레이션/자동복구 시 오동작 가능
- 관련 파일:
  - `docker-compose.yml` (backend healthcheck: curl 사용)
  - `backend/Dockerfile` (jre-alpine, curl 미설치)

## 4) 명세 대비 미흡/미구현 항목

- Hot-Warm-Cold 계층 저장 아키텍처: 실체 로직 미구현(설정/의존성 수준)
  - MinIO 설정/의존성은 있으나 저장/이관 워크플로우 부재
- Kafka UI 기반 Lag 운영 확인 체계 부재
- "상태 복구 30초 이내" SLA 자동 검증/리포팅 부재

## 5) 다음 턴에 바로 진행할 작업 계획

우선순위 P0:
1. 감사로그 누락 문제 수정
   - 목표: 수정/삭제 시도 1건마다 `event_audit_log`에 실제 1건 이상 누적
   - 방법: 트리거 로직을 롤백 영향에서 분리되는 방식으로 재설계
   - 검증: 시도 전후 count 증가 + `/api/audit/stats` 증가 확인

2. backend healthcheck 정상화
   - 목표: backend 컨테이너 `healthy`
   - 방법(택1):
     - A안: Dockerfile에 `curl` 설치
     - B안: healthcheck를 `wget` 또는 JVM 기반 체크로 변경
   - 검증: `docker inspect` health log에서 exit code 0 연속 확인

우선순위 P1:
3. 명세 체크리스트 재평가 문서 업데이트
   - 위 P0 완료 후 완성도 재산정
   - `docs/test-scenarios.md` 또는 별도 결과 문서에 반영

4. 운영 검증 커맨드 묶음 제공
   - 불변성/감사로그/헬스/복구 상태를 한 번에 확인하는 스크립트화

## 6) 비고

- 현재 테스트는 최근 실행 기준 `BUILD SUCCESSFUL` 상태였으나,
  운영 완성도는 위 2개 P0 이슈 해결 전까지 제한적임.

## 7) 후속 진행 (2026-02-23)

### P0-1 감사로그 누락 수정 반영

- 수정 파일: `init-scripts/03-immutability-constraints.sql`
- 변경 요약:
  - `prevent_event_modification()` 트리거에서 감사로그 기록 SQL을 구성해 `dblink_exec`로 별도 트랜잭션 실행
  - `dblink` extension 보장 로직 추가(`CREATE EXTENSION IF NOT EXISTS dblink`)
  - 이후 `IMMUTABILITY_VIOLATION` 예외를 유지해 수정/삭제는 계속 차단
- 기대 효과:
  - 수정/삭제 시도 시 예외로 차단되더라도 감사로그 누적 유지

### P0-2 backend healthcheck 정상화 반영

- 수정 파일: `backend/Dockerfile`
- 변경 요약:
  - runtime 이미지(`eclipse-temurin:21-jre-alpine`)에 `curl` 설치 추가
- 기대 효과:
  - `docker-compose.yml`의 기존 backend healthcheck(curl 기반)가 정상 수행
  - backend 컨테이너 `healthy` 판정 가능

### P1-4 운영 검증 커맨드 묶음 반영

- 추가 파일: `scripts/verify-p0.ps1`
- 문서 반영: `docs/test-scenarios.md`
- 스크립트 검증 항목:
  - 불변성 위반 시도 차단(`IMMUTABILITY_VIOLATION`)
  - 위반 시도 전/후 `event_audit_log` count 증가
  - backend 컨테이너 health=`healthy`
  - `/api/audit/stats`, `/actuator/health` 응답 확인

## 8) 실검증 결과 (2026-02-23)

### P0-1 감사로그 누락 이슈

- 상태: 해결
- 근거:
  - 수정된 트리거 SQL 재적용 후, 차단 예외가 발생해도 감사로그 누적 확인
  - 검증 스크립트 기준 `event_audit_log` count `4 -> 5` 증가

### P0-2 backend healthcheck 이슈

- 상태: 해결
- 근거:
  - `backend` 이미지 재빌드 후 container health=`healthy`
  - `GET /actuator/health` 결과 `UP`

### 참고 운영 이슈 처리

- Kafka 기동 중 `NodeExistsException` 발생 시 Zookeeper 재시작 후 Kafka 재기동으로 복구됨
  - 명령: `docker-compose restart zookeeper` -> 대기 -> `docker-compose up -d kafka`

## 9) 미구현 항목 처리 완료 (2026-02-23)

### 9-1. Hot-Warm-Cold 계층화 저장

- 상태: 해결
- 반영:
  - 아카이브 manifest 테이블/엔티티/리포지토리 추가
    - `init-scripts/04-event-archive-manifest.sql`
    - `backend/src/main/java/com/safety/platform/domain/EventArchiveManifest.java`
  - MinIO 기반 cold tier 아카이브 서비스/스케줄러/API 추가
    - `backend/src/main/java/com/safety/platform/service/StorageTierService.java`
    - `backend/src/main/java/com/safety/platform/scheduler/StorageTierScheduler.java`
    - `backend/src/main/java/com/safety/platform/controller/StorageTierController.java`
- 실검증:
  - `GET /api/storage/tiers/summary` 응답 정상
  - `POST /api/storage/tiers/archive/run` 응답 정상

### 9-2. Kafka Lag 운영 확인 체계

- 상태: 해결
- 반영:
  - AdminClient 기반 lag 계산 서비스/API 추가
    - `backend/src/main/java/com/safety/platform/service/KafkaLagService.java`
    - `backend/src/main/java/com/safety/platform/controller/KafkaLagController.java`
- 실검증:
  - `GET /api/kafka/lag?groupId=safety-group` 정상 응답(`totalLag`, topic별 lag 확인)

### 9-3. Streams 복구 SLA 자동 검증/리포팅

- 상태: 해결
- 반영:
  - SLA 로그 테이블/엔티티/리포지토리/서비스/스케줄러/API 추가
    - `init-scripts/05-kafka-recovery-sla-log.sql`
    - `backend/src/main/java/com/safety/platform/service/StreamsSlaService.java`
    - `backend/src/main/java/com/safety/platform/scheduler/StreamsSlaScheduler.java`
    - `backend/src/main/java/com/safety/platform/controller/StreamsSlaController.java`
- 실검증:
  - `GET /api/streams/sla` 정상 응답
  - threshold(30000ms), check 수, 최근 compliance 이력 확인

### 9-4. 월별 파티셔닝 + Kafka UI 운영 체계

- 상태: 해결
- 반영:
  - 월별 파티셔닝 마이그레이션/유지 스크립트 추가
    - `init-scripts/06-monthly-partitioning.sql`
  - Kafka UI 서비스 추가
    - `docker-compose.yml` (`kafka-ui`, port `8085`)
- 실검증:
  - 통합 검증 스크립트 `scripts/verify-spec-v2.1.ps1` 실행
  - `safety_event_log` relkind=`p`(partitioned), partitions=16 확인
  - Kafka UI `http://localhost:8085` HTTP 200 확인

## 10) 최종 검증 (2026-02-24)

- 통합 검증 스크립트:
  - `.\scripts\verify-spec-v2.1.ps1`
- 통과 항목:
  - 파티셔닝(월별)
  - 불변성 차단 + 감사로그 누적
  - Storage tier(요약/아카이브)
  - Kafka lag API
  - Streams SLA API
  - Prometheus 핵심 메트릭 노출
  - Kafka UI 접근
