-- init-scripts/03-immutability-constraints.sql

-- 감사 로그 테이블
CREATE TABLE IF NOT EXISTS event_audit_log (
    audit_id BIGSERIAL PRIMARY KEY,
    operation VARCHAR(20) NOT NULL,
    event_id UUID NOT NULL,
    attempted_by VARCHAR(100),
    attempted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    denied_reason TEXT,
    original_data JSONB,
    attempted_data JSONB,
    client_ip INET,
    client_application VARCHAR(200)
);

-- 🔒 Level 2: Database TRIGGER
CREATE OR REPLACE FUNCTION prevent_event_modification()
RETURNS TRIGGER AS $$
BEGIN
    -- 모든 수정 시도 기록
    INSERT INTO event_audit_log (
        operation, event_id, attempted_by, denied_reason, 
        original_data, attempted_data, client_ip, client_application
    ) VALUES (
        TG_OP, OLD.event_id, current_user,
        'Event logs are immutable - blocked by database trigger',
        to_jsonb(OLD),
        CASE WHEN TG_OP = 'UPDATE' THEN to_jsonb(NEW) ELSE NULL END,
        inet_client_addr(),
        current_setting('application_name', true)
    );
    
    -- 작업 차단
    RAISE EXCEPTION 'IMMUTABILITY_VIOLATION: Event logs cannot be modified. EventID: %', 
        OLD.event_id;
    
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- safety_event_log 테이블은 Phase 1에서 생성될 예정이므로, 
-- 트리거 생성은 테이블 생성 직후에 수행되거나 별도 스크립트로 분리될 수 있습니다.
-- 여기서는 함수만 먼저 정의합니다.
