# 프로젝트 진행 현황 및 트러블슈팅 보고서 (2026-02-12)

## 1. 완료된 작업 (Accomplishments)

### 📐 시스템 구조 및 환경 설정
- **멀티 모듈 구조 확정**: 루트 프로젝트 하위에 `backend`, `frontend` 폴더를 구성하고 Gradle 멀티 모듈 설정 완료.
- **인프라 구성 (Docker)**: PostgreSQL, Kafka, Redis, MinIO, Prometheus, Grafana를 포함한 `docker-compose.yml` 작성 및 가동 확인.
- **불변성 방어선 구축**:
    - **Level 1 (App)**: `SafetyEventLog` 엔티티 내 `@PreUpdate`를 통한 수정 차단 로직 구현.
    - **Level 2 (DB)**: PostgreSQL `BEFORE TRIGGER` 및 `event_audit_log` 테이블을 통한 데이터베이스 레벨의 강제 불변성 및 감사 로그 기록 시스템 구축. (수동 검증 완료: `UPDATE` 시도 시 `RAISE EXCEPTION` 발생 확인)

### ☕ 백엔드 개발 (Phase 1)
- **도메인 모델**: `SafetyEventLog`, `EventAuditLog`, `SensorStatus` 등 핵심 엔티티 및 DTO 구현.
- **데이터 수집 파이프라인**: 
    - 4개의 가상 센서 데이터를 생성하는 `MockScadaService` 구현.
    - 1초 주기로 데이터를 수집하여 저장하는 `SensorPollingScheduler` 및 `EventService` 구현.
- **API 구현**: 감사 로그 조회를 위한 `AuditLogController` 엔드포인트 구현.
- **설정**: Jackson `JavaTimeModule` 설정을 통한 `LocalDateTime` 직렬화 이슈 해결.

---

## 2. 현재 트러블슈팅 사항 (Pending Troubleshooting)

### 🚨 주요 이슈: 백엔드 기동 지연 및 로그 기록 실패
- **현상**: `java -jar` 또는 `./gradlew bootRun` 실행 시, Spring Boot 초기화 로그("No active profile set") 이후 추가적인 진행 로그가 남지 않음.
- **원인 분석 및 시도된 해결책**:
    1. **Spring Boot 버전 이슈**: 초기 4.0.2 버전 사용 시 자동 설정 오류 가능성 발견 -> **3.4.2(안정 버전)로 다운그레이드** 완료.
    2. **의존성 해결**: Gradle 9.3에서 `spring-boot-starter-kafka` 인식 불가 이슈 -> **`spring-kafka`로 명시적 교체**하여 빌드 성공.
    3. **인프라 연결성**: Kafka/Redis가 뜨기 전 백엔드가 기동될 경우 연결 대기(Timeout) 상태에 빠짐 -> **인프라 전체 기동 후 백엔드 실행** 시도 중.
    4. **환경 호환성**: Kafka Streams의 `state-dir` 경로가 유닉스 스타일(`/tmp/...`)로 되어 있어 윈도우에서 접근 오류 가능성 -> **상대 경로(`./kafka-streams-state`)로 수정**.
    5. **OS/쉘 특성**: 윈도우 PowerShell에서 `Start-Process` 및 리다이렉션(`>`) 사용 시 로그 파일이 생성되지 않거나 버퍼링되는 현상 발생.

### 🛠 조치 계획
- **로그 디버깅**: 백엔드 기동 시 Kafka 관련 설정을 제외(`exclude`)하여 순수 DB 로직부터 정상 기동 여부 재검토.
- **로그 출력 방식 변경**: 파일 리다이렉션 대신 쉘에서 직접 출력물을 일정 시간 동안 캡처하여 정확한 에러 메시지(StackTrace) 확보 필요.

---

## 3. 다음 단계 (Next Steps)
1. 백엔드 기동 시 발생하는 최종 에러 메시지 확보 및 해결.
2. Phase 1 데이터 수집 기능 정상 동작 확인 (DB 레코드 적재 여부).
3. Phase 2: Kafka 통합 및 메시지 발행/구독 아키텍처로 전환.
