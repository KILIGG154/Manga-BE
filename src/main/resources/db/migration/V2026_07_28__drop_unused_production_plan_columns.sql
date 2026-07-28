-- =============================================================================
-- Migration: Drop unused ProductionPlan columns (Spec v2.1 refactor)
-- Decision Log 2026-07-28: trim entity down to the v2.1 contract.
--
-- Drops:
--   Milestones        - free-text field, not in v2.1 spec
--   ChapterTimeline   - free-text field, not in v2.1 spec
--   Deadline          - Instant, replaced by deadline_date (LocalDate)
--   Priority          - not in v2.1 spec
--   paused_by         - replaced by plan_extension_log
--   paused_at         - replaced by plan_extension_log
--   pause_reason      - replaced by plan_extension_log
-- =============================================================================

DECLARE @sql NVARCHAR(MAX) = N'';

SELECT @sql = @sql + 'ALTER TABLE ProductionPlan DROP COLUMN ' + QUOTENAME(name) + ';' + CHAR(10)
FROM sys.columns
WHERE object_id = OBJECT_ID('ProductionPlan')
  AND name IN ('Milestones', 'ChapterTimeline', 'Deadline', 'Priority',
               'paused_by', 'paused_at', 'pause_reason');

IF LEN(@sql) > 0
BEGIN
    EXEC sp_executesql @sql;
    PRINT 'Dropped unused ProductionPlan columns';
END
ELSE
BEGIN
    PRINT 'No unused ProductionPlan columns to drop';
END
GO