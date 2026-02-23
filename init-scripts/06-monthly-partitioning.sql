-- Monthly partitioning for safety_event_log
-- Strategy:
-- 1) If table is already partitioned: ensure rolling monthly partitions exist.
-- 2) If table is a regular table: migrate to a range-partitioned table by occurred_at.

CREATE OR REPLACE FUNCTION ensure_safety_event_log_monthly_partitions(months_back INTEGER DEFAULT 1, months_ahead INTEGER DEFAULT 2)
RETURNS VOID AS $$
DECLARE
    i INTEGER;
    start_ts TIMESTAMP;
    end_ts TIMESTAMP;
    partition_name TEXT;
BEGIN
    FOR i IN -months_back..months_ahead LOOP
        start_ts := date_trunc('month', NOW()) + make_interval(months => i);
        end_ts := date_trunc('month', NOW()) + make_interval(months => i + 1);
        partition_name := format('safety_event_log_%s', to_char(start_ts, 'YYYYMM'));

        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I PARTITION OF safety_event_log FOR VALUES FROM (%L) TO (%L)',
            partition_name,
            start_ts,
            end_ts
        );
    END LOOP;

    EXECUTE 'CREATE TABLE IF NOT EXISTS safety_event_log_default PARTITION OF safety_event_log DEFAULT';
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    v_relkind CHAR;
BEGIN
    SELECT c.relkind
    INTO v_relkind
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public'
      AND c.relname = 'safety_event_log';

    IF v_relkind IS NULL THEN
        CREATE TABLE safety_event_log (
            event_id UUID NOT NULL,
            aggregate_id VARCHAR(100) NOT NULL,
            event_type VARCHAR(50) NOT NULL,
            occurred_at TIMESTAMP NOT NULL,
            payload JSONB NOT NULL,
            source_system VARCHAR(50) NOT NULL,
            version BIGINT,
            previous_hash VARCHAR(64),
            current_hash VARCHAR(64),
            PRIMARY KEY (event_id, occurred_at)
        ) PARTITION BY RANGE (occurred_at);
    ELSIF v_relkind = 'r' THEN
        IF to_regclass('public.safety_event_log_legacy') IS NULL THEN
            ALTER TABLE safety_event_log RENAME TO safety_event_log_legacy;
        END IF;

        CREATE TABLE IF NOT EXISTS safety_event_log (
            event_id UUID NOT NULL,
            aggregate_id VARCHAR(100) NOT NULL,
            event_type VARCHAR(50) NOT NULL,
            occurred_at TIMESTAMP NOT NULL,
            payload JSONB NOT NULL,
            source_system VARCHAR(50) NOT NULL,
            version BIGINT,
            previous_hash VARCHAR(64),
            current_hash VARCHAR(64),
            PRIMARY KEY (event_id, occurred_at)
        ) PARTITION BY RANGE (occurred_at);

        PERFORM ensure_safety_event_log_monthly_partitions(12, 2);

        INSERT INTO safety_event_log (
            event_id, aggregate_id, event_type, occurred_at, payload, source_system, version, previous_hash, current_hash
        )
        SELECT
            event_id, aggregate_id, event_type, occurred_at, payload, source_system, version, previous_hash, current_hash
        FROM safety_event_log_legacy
        ON CONFLICT (event_id, occurred_at) DO NOTHING;
    ELSIF v_relkind = 'p' THEN
        -- already partitioned, no migration needed
        NULL;
    ELSE
        RAISE EXCEPTION 'Unsupported relation kind for safety_event_log: %', v_relkind;
    END IF;
END
$$;

SELECT ensure_safety_event_log_monthly_partitions(1, 2);

CREATE INDEX IF NOT EXISTS idx_safety_event_log_occurred_at
    ON safety_event_log (occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_safety_event_log_aggregate_occurred_at
    ON safety_event_log (aggregate_id, occurred_at DESC);

