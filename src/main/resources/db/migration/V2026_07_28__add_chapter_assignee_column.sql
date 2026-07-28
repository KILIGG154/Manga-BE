-- Spec v2.1: Chapter now has both Owner (Tantou creator) and Assignee (assigned Mangaka).
-- Keep OwnerId; add AssigneeId.

IF NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('dbo.[Chapter]') AND name = 'AssigneeId')
BEGIN
    ALTER TABLE dbo.[Chapter] ADD AssigneeId BIGINT NULL;
END