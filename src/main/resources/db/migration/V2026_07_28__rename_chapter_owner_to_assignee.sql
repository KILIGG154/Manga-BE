-- Spec v2.1: Tantou assigns Chapter to Mangaka via assignee (not owner).
-- Rename OwnerId column to AssigneeId on Chapter table.

IF EXISTS (SELECT 1 FROM sys.columns
           WHERE object_id = OBJECT_ID('dbo.[Chapter]') AND name = 'OwnerId')
BEGIN
    EXEC sp_rename 'dbo.[Chapter].OwnerId', 'AssigneeId', 'COLUMN';
END