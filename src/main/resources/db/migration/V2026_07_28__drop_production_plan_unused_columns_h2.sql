-- V2026_07_28__drop_production_plan_unused_columns_h2.sql
-- Drop unused columns: schedule, resources, budget, assistantAllocation, risk
-- H2 uses different syntax for NVARCHAR(MAX) columns

ALTER TABLE ProductionPlan DROP COLUMN IF EXISTS Schedule;
ALTER TABLE ProductionPlan DROP COLUMN IF EXISTS Resources;
ALTER TABLE ProductionPlan DROP COLUMN IF EXISTS Budget;
ALTER TABLE ProductionPlan DROP COLUMN IF EXISTS AssistantAllocation;
ALTER TABLE ProductionPlan DROP COLUMN IF EXISTS Risk;
