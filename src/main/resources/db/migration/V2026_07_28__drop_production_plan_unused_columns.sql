-- V2026_07_28__drop_production_plan_unused_columns.sql
-- Drop unused columns: schedule, resources, budget, assistantAllocation, risk

ALTER TABLE ProductionPlan DROP COLUMN IF EXISTS Schedule;
ALTER TABLE ProductionPlan DROP COLUMN IF EXISTS Resources;
ALTER TABLE ProductionPlan DROP COLUMN IF EXISTS Budget;
ALTER TABLE ProductionPlan DROP COLUMN IF EXISTS AssistantAllocation;
ALTER TABLE ProductionPlan DROP COLUMN IF EXISTS Risk;
