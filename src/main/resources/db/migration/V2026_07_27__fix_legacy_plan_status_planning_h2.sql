-- H2 / dev migration: Fix legacy PLAN_STATUS = 'PLANNING'
UPDATE ProductionPlan
SET plan_status = 'IN_PROGRESS'
WHERE plan_status = 'PLANNING';