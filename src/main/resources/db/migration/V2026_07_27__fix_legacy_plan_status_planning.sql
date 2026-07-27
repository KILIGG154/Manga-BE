-- =============================================================================
-- Fix legacy PLAN_STATUS = 'PLANNING' rows (not a valid enum value).
-- Decision Log 2026-07-27: safe data fix for production DB that pre-dates
-- the PlanStatus enum cleanup.
-- Safe to run idempotently.
-- =============================================================================

-- SQL Server
IF EXISTS (SELECT 1 FROM sys.columns
          WHERE object_id = OBJECT_ID('ProductionPlan') AND name = 'plan_status')
BEGIN
    UPDATE ProductionPlan
    SET plan_status = 'IN_PROGRESS'
    WHERE plan_status = 'PLANNING';

    DECLARE @updated INT = @@ROWCOUNT;
    PRINT 'Updated ' + CAST(@updated AS NVARCHAR) + ' ProductionPlan rows from PLANNING to IN_PROGRESS';
END