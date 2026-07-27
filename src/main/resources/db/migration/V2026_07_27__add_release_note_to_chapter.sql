-- =============================================================================
-- Migration: add release_note column to Chapter table
-- Decision Log 2026-07-27 §AI-01: releaseNote is OPTIONAL (nullable).
-- Sprint 4: manual SQL migration for production (SQL Server).
-- =============================================================================
-- Pre-flight: confirm column does not exist before running.
-- Safe to run multiple times thanks to IF NOT EXISTS.

IF COL_LENGTH('Chapter', 'release_note') IS NULL
BEGIN
    ALTER TABLE Chapter
        ADD release_note NVARCHAR(MAX) NULL;
    PRINT 'Added column Chapter.release_note';
END
ELSE
BEGIN
    PRINT 'Column Chapter.release_note already exists — skipping';
END
GO