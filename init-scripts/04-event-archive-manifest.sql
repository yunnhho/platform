CREATE TABLE IF NOT EXISTS event_archive_manifest (
    event_id UUID PRIMARY KEY,
    tier VARCHAR(10) NOT NULL,
    object_key VARCHAR(300) NOT NULL,
    archived_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_event_archive_manifest_tier
    ON event_archive_manifest (tier, archived_at DESC);

