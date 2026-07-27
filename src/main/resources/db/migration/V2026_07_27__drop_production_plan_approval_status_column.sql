-- =============================================================================
-- Migration: DROP column ProductionPlan.approval_status
-- Decision Log 2026-07-27 §AI-10 (Sprint 8 follow-up).
-- Per BA Spec V3 §5.2: "Giữ cột trong DB khoảng 2 sprint để fallback, sau đó DROP column."
-- The legacy field has been kept but unused for 2 sprints — safe to drop now.
--
-- Pre-flight: by the time this migration runs:
--   1. DataInitialized.migratePlanApprovalStatus() has already migrated all rows
--      to use planStatus (IN_PROGRESS / PAUSED / COMPLETED / CANCELLED).
--   2. ProductionPlan entity no longer has the approvalStatus field
--      (compile error if migration runs before code deploy).
-- =============================================================================

IF EXISTS (SELECT 1 FROM sys.columns
          WHERE object_id = OBJECT_ID('ProductionPlan') AND name = 'approval_status')
BEGIN
    ALTER TABLE ProductionPlan DROP COLUMN approval_status;
    PRINT 'Dropped column ProductionPlan.approval_status';
END
ELSE
BEGIN
    PRINT 'Column ProductionPlan.approval_status does not exist — skipping';
END
GO