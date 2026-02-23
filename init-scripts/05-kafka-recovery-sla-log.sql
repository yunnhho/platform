CREATE TABLE IF NOT EXISTS kafka_recovery_sla_log (
    id BIGSERIAL PRIMARY KEY,
    checked_at TIMESTAMP NOT NULL,
    recovery_duration_ms BIGINT NOT NULL,
    threshold_ms BIGINT NOT NULL,
    compliant BOOLEAN NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_kafka_recovery_sla_log_checked_at
    ON kafka_recovery_sla_log (checked_at DESC);

