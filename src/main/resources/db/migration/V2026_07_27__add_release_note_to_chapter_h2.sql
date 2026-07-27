-- =============================================================================
-- H2 / dev migration: add release_note column to Chapter table
-- Decision Log 2026-07-27 §AI-01: releaseNote is OPTIONAL (nullable).
-- Used by H2 in-memory database during development & integration tests.
-- (Flyway will pick this up if datasource is H2; otherwise SQL Server file is used.)
-- =============================================================================

ALTER TABLE Chapter ADD COLUMN IF NOT EXISTS release_note CLOB NULL;