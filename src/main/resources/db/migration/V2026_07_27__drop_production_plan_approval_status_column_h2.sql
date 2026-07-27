-- =============================================================================
-- H2 / dev migration: DROP column ProductionPlan.approval_status
-- Decision Log 2026-07-27 §AI-10.
-- =============================================================================

ALTER TABLE ProductionPlan DROP COLUMN IF EXISTS approval_status;