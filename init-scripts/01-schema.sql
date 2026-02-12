-- init-scripts/01-schema.sql

CREATE TABLE IF NOT EXISTS safety_event_log (
    event_id UUID PRIMARY KEY,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    payload JSONB NOT NULL,
    source_system VARCHAR(50) NOT NULL,
    version BIGINT,
    previous_hash VARCHAR(64),
    current_hash VARCHAR(64)
);
