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

-- dblink은 트리거 예외로 롤백되더라도 감사 로그를 별도 트랜잭션으로 남기기 위해 사용
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS dblink;
EXCEPTION
    WHEN OTHERS THEN
        RAISE WARNING 'dblink extension is unavailable. Immutable audit persistence may be degraded.';
END
$$;

-- 🔒 Level 2: Database TRIGGER
CREATE OR REPLACE FUNCTION prevent_event_modification()
RETURNS TRIGGER AS $$
DECLARE
    v_audit_payload JSONB;
    v_conn TEXT;
    v_sql TEXT;
BEGIN
    v_audit_payload := jsonb_build_object(
        'operation', TG_OP,
        'event_id', OLD.event_id,
        'attempted_by', current_user,
        'denied_reason', 'Event logs are immutable - blocked by database trigger',
        'original_data', to_jsonb(OLD),
        'attempted_data', CASE WHEN TG_OP = 'UPDATE' THEN to_jsonb(NEW) ELSE NULL END,
        'client_ip', inet_client_addr(),
        'client_application', current_setting('application_name', true)
    );

    -- Local socket + current DB role to avoid hard-coded credentials.
    v_conn := format(
        'dbname=%s user=%s',
        current_database(),
        current_user
    );
    v_sql := format(
        'INSERT INTO event_audit_log (operation, event_id, attempted_by, denied_reason, original_data, attempted_data, client_ip, client_application)
         VALUES (%L, %L::uuid, %L, %L, %L::jsonb, %L::jsonb, %L::inet, %L)',
        v_audit_payload ->> 'operation',
        v_audit_payload ->> 'event_id',
        v_audit_payload ->> 'attempted_by',
        v_audit_payload ->> 'denied_reason',
        (v_audit_payload -> 'original_data')::TEXT,
        (v_audit_payload -> 'attempted_data')::TEXT,
        v_audit_payload ->> 'client_ip',
        v_audit_payload ->> 'client_application'
    );

    -- 모든 수정 시도 기록(별도 트랜잭션)
    BEGIN
        PERFORM dblink_exec(v_conn, v_sql);
    EXCEPTION
        WHEN OTHERS THEN
            RAISE WARNING 'Failed to persist audit log via dblink: %', SQLERRM;
    END;

    -- 작업 차단
    RAISE EXCEPTION 'IMMUTABILITY_VIOLATION: Event logs cannot be modified. EventID: %',
        OLD.event_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- safety_event_log 테이블은 Phase 1에서 생성될 예정이므로,
-- 트리거 생성은 테이블 생성 직후에 수행되거나 별도 스크립트로 분리될 수 있습니다.
-- 여기서는 테이블이 존재하는 경우에만 트리거를 생성합니다.
DO $$
BEGIN
    IF to_regclass('public.safety_event_log') IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_trigger
            WHERE tgname = 'prevent_event_update'
              AND tgrelid = 'public.safety_event_log'::regclass
              AND NOT tgisinternal
        ) THEN
            CREATE TRIGGER prevent_event_update
                BEFORE UPDATE OR DELETE ON safety_event_log
                FOR EACH ROW
                EXECUTE FUNCTION prevent_event_modification();
        END IF;
    END IF;
END
$$;
