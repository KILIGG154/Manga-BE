-- =============================================================================
-- H2 / dev migration: add index for AI-08 scheduler query
-- Decision Log 2026-07-27 §AI-08.
-- =============================================================================

CREATE INDEX IF NOT EXISTS IX_Chapter_Status_PublishDate
    ON Chapter (Status, PublishDate);