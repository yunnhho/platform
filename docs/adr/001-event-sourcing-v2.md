# ADR-001-v2: Event Sourcing with 3-Level Immutability

## Amendment History
- v1.0 (2026-02-10): Initial decision
- v2.0 (2026-02-11): Added 3-level defense mechanism

## Critical Feedback Addressed
"@PreUpdate만으로는 DB 직접 접속 시 수정 가능"

## Solution
3단계 방어선:
1. Application: @PreUpdate
2. Database: TRIGGER
3. Cryptographic: Hash Chain (Phase 4)

## Legal Compliance
중대재해처벌법 제4조 완벽 준수
- 모든 수정 시도 추적
- 사후 검증 가능
- 법정 증거 능력 최대화
