-- =============================================================================
-- Migration: add index for AI-08 scheduler query
-- Decision Log 2026-07-27 §AI-08:
-- Scheduler chạy `findByChapterStatusAndPublishDateLessThanEqual(SCHEDULED, today)`.
-- Index trên (Status, PublishDate) giúp query O(log n) thay vì full-scan.
-- =============================================================================

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Chapter_Status_PublishDate')
BEGIN
    CREATE INDEX IX_Chapter_Status_PublishDate
        ON Chapter (Status, PublishDate);
    PRINT 'Created index IX_Chapter_Status_PublishDate';
END
ELSE
BEGIN
    PRINT 'Index IX_Chapter_Status_PublishDate already exists — skipping';
END
GO