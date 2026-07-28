-- =============================================================================
-- Migration (H2 test profile): Drop unused ProductionPlan columns
-- =============================================================================

ALTER TABLE ProductionPlan DROP COLUMN IF EXISTS Milestones;
ALTER TABLE ProductionPlan DROP COLUMN IF EXISTS ChapterTimeline;
ALTER TABLE ProductionPlan DROP COLUMN IF EXISTS Deadline;
ALTER TABLE ProductionPlan DROP COLUMN IF EXISTS Priority;
ALTER TABLE ProductionPlan DROP COLUMN IF EXISTS paused_by;
ALTER TABLE ProductionPlan DROP COLUMN IF EXISTS paused_at;
ALTER TABLE ProductionPlan DROP COLUMN IF EXISTS pause_reason;