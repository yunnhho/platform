# Industrial Safety Monitoring Platform

실시간 산업 안전 모니터링, 이벤트 소싱 기반 불변 로그, Kafka 스트리밍 분석, Outbox 알림 전송, 관측성 대시보드를 포함한 통합 프로젝트입니다.

### 1. 프로젝트 목적
- 센서 데이터를 1초 주기로 수집하고 이벤트 로그로 저장
- 이벤트 로그 수정/삭제를 애플리케이션 + DB 트리거 + 해시 체인으로 방어
- Kafka/Kafka Streams 기반 실시간 처리와 상태 복구
- Outbox 패턴으로 알림 전송 신뢰성 강화
- Prometheus/Grafana 기반 운영 지표 관측
- Hot-Warm-Cold 계층화 저장(MinIO 아카이브)
- Kafka Lag/SLA 운영 확인 자동화

### 2. 핵심 구성
- Backend: Spring Boot 3.4, Java 21, PostgreSQL, Redis, Kafka, Kafka Streams
- Frontend: React 18, STOMP over WebSocket
- Monitoring: Prometheus, Grafana
- Infra: Docker Compose

### 3. 저장소 구조
- `backend/`: API, 스케줄러, 도메인, Kafka/Outbox/Streams, 테스트
- `frontend/`: 대시보드 UI
- `init-scripts/`: DB 스키마, Outbox, 불변성 트리거, 파티셔닝, SLA 로그
- `monitoring/`: Prometheus 설정, Grafana 데이터소스/대시보드
- `docs/adr/`: 아키텍처 의사결정 문서
- `scripts/`: 운영/명세 검증 스크립트

### 4. 실행 전 준비
- Docker, Docker Compose
- Java 21 (로컬에서 백엔드 단독 실행 시)
- Node.js 18+ (로컬에서 프론트 단독 실행 시)

### 5. 실행 방법
1. 인프라/애플리케이션 실행
```bash
docker-compose up -d
```
2. 상태 확인
```bash
docker-compose ps
```
3. 주요 접속 주소
- Backend: `http://localhost:8080`
- Frontend: `http://localhost:3001`
- Kafka UI: `http://localhost:8085`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (admin / admin)
- MinIO Console: `http://localhost:9001` (minioadmin / minioadmin)

### 6. 프론트 확인 포인트
1. 메인 대시보드 접속
- `http://localhost:3001`
2. 실시간 반영 확인
- 센서 카드 값/상태 변화
- Outbox 대기 건수
- 감사 로그 요약(UPDATE/DELETE 차단 건수)
3. WebSocket 경로
- `/ws/sensors`

### 7. 핵심 API
- `GET /api/sensors/latest`
- `GET /api/events/recent`
- `GET /api/audit/stats`
- `GET /api/audit/modification-attempts`
- `GET /api/outbox/stats`
- `GET /api/storage/tiers/summary`
- `POST /api/storage/tiers/archive/run`
- `GET /api/kafka/lag?groupId=safety-group`
- `GET /api/streams/sla`

### 8. 검증 포인트
1. 이벤트 불변성(DB 트리거)
```sql
UPDATE safety_event_log
SET payload = '{}'
WHERE event_id = (SELECT event_id FROM safety_event_log LIMIT 1);
```
예상 결과: `IMMUTABILITY_VIOLATION` 예외 발생

2. 감사 로그 기록 확인
```sql
SELECT *
FROM event_audit_log
ORDER BY attempted_at DESC
LIMIT 20;
```

3. Kafka Streams 상태 확인
```bash
curl http://localhost:8080/actuator/health
```
응답에서 `components.kafkaStreams`의 상태와 `lastRecoveryTimeMs` 확인

4. 감사 통계 API
```bash
curl http://localhost:8080/api/audit/stats
```

5. Storage Tier 요약 API
```bash
curl http://localhost:8080/api/storage/tiers/summary
```

6. Kafka Lag API
```bash
curl "http://localhost:8080/api/kafka/lag?groupId=safety-group"
```

7. Streams SLA API
```bash
curl http://localhost:8080/api/streams/sla
```

### 9. 테스트
```bash
cd backend
./gradlew.bat test --no-daemon --console=plain
```

### 10. 운영 지표
- `safety_immutability_violation_total`
- `safety_outbox_sent_total`
- `safety_outbox_failed_total`
- `kafka_streams_state_recovery_time_ms`
- `kafka_streams_state_code`

### 11. 월별 파티셔닝
- 스크립트: `init-scripts/06-monthly-partitioning.sql`
- `safety_event_log`를 월별 RANGE 파티셔닝으로 유지
- 기존 비파티션 테이블이 있으면 마이그레이션 처리

### 12. 자동 검증 스크립트
1. P0 검증
```bash
.\scripts\verify-p0.ps1
```
2. 명세 통합 검증(v2.1)
```bash
.\scripts\verify-spec-v2.1.ps1
```

### 13. Kafka 이슈 대응 메모
증상:
- `NodeExistsException` (broker id 충돌)
- `InconsistentClusterIdException`

기본 조치:
```bash
docker-compose restart zookeeper
Start-Sleep -Seconds 20
docker-compose up -d kafka
```

필요 시(데이터 재생성 허용):
```bash
docker-compose rm -sf kafka
docker volume rm platform_kafka_data
docker-compose up -d kafka
```

### 14. 참고 문서
- `PROJECT_SPECIFICATION_v2.1.md`
- `docs/adr/001-event-sourcing-v2.md`
- `docs/adr/003-kafka-streams-v2.md`

### 15. 환경변수(.env) 설정
1. 로컬 환경파일 생성
```powershell
Copy-Item .env.example .env
Copy-Item frontend/.env.example frontend/.env
```
2. 민감정보 값 변경
- `.env`: `POSTGRES_PASSWORD`, `MINIO_ROOT_PASSWORD`, `MINIO_SECRET_KEY`, `GRAFANA_ADMIN_PASSWORD`
- `frontend/.env`: 프론트 API/WS 접속 주소
3. `.env` 파일은 git에 커밋되지 않음
- `.gitignore`에 `.env`, `.env.*`, `frontend/.env`, `frontend/.env.*` 반영 완료
4. 환경변수 적용 실행
```bash
docker-compose up -d --build
```

### 16. Git 업로드 전 체크리스트
1. 추적 제외 파일 확인
```bash
git check-ignore -v .env frontend/.env
```
2. 과거에 캐시가 스테이징된 경우 인덱스에서만 제거
```bash
git rm -r --cached frontend/.npm-cache
```
3. 최종 상태 확인
```bash
git status --short
```
