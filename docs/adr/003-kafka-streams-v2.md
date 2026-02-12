# ADR-003-v2: Kafka Streams with State Recovery

## Amendment History
- v1.0 (2026-02-10): Initial decision
- v2.0 (2026-02-11): Added state recovery mechanism

## Critical Feedback Addressed
"컨테이너 재시작 시 RocksDB 상태 손실 위험"

## Solution
- Docker Volume 마운트
- Changelog Topic 활성화
- StateListener 구현
- Health Indicator 추가

## Recovery Metrics
- 목표: 30초 내 복구
- 측정: Prometheus 메트릭
