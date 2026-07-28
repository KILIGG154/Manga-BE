-- =============================================================================
-- Migration (H2 test profile): Production Plan v2.1 refactor
-- Mirrors V2026_07_28__production_plan_v21.sql for H2 in-memory testing.
-- =============================================================================

ALTER TABLE ProductionPlan ADD COLUMN IF NOT EXISTS title NVARCHAR(255);
ALTER TABLE ProductionPlan ADD COLUMN IF NOT EXISTS deadline_date DATE;
ALTER TABLE ProductionPlan ADD COLUMN IF NOT EXISTS publish_date DATE;
ALTER TABLE ProductionPlan ADD COLUMN IF NOT EXISTS actual_end_date DATE;
ALTER TABLE ProductionPlan ADD COLUMN IF NOT EXISTS created_by BIGINT;

UPDATE ProductionPlan SET title = CONCAT('Production Plan ', FORMATDATETIME(CURRENT_TIMESTAMP, 'MM/yyyy')) WHERE title IS NULL;
ALTER TABLE ProductionPlan ALTER COLUMN title SET NOT NULL;

UPDATE ProductionPlan SET plan_status = 'ACTIVE'    WHERE plan_status = 'IN_PROGRESS';
UPDATE ProductionPlan SET plan_status = 'EXTENDED'  WHERE plan_status = 'PAUSED';
UPDATE ProductionPlan SET plan_status = 'COMPLETED' WHERE plan_status = 'CANCELLED';

CREATE UNIQUE INDEX IF NOT EXISTS idx_project_title ON ProductionPlan (ProjectId, title);
CREATE INDEX IF NOT EXISTS idx_plan_status_dates ON ProductionPlan (plan_status, end_date);

CREATE TABLE IF NOT EXISTS plan_extension_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id         BIGINT NOT NULL,
    old_end_date    DATE NOT NULL,
    new_end_date    DATE NOT NULL,
    reason_code     VARCHAR(50) NOT NULL,
    reason_note     NVARCHAR(MAX),
    extended_by     BIGINT NOT NULL,
    extended_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_extension_plan FOREIGN KEY (plan_id)
        REFERENCES ProductionPlan(Id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_extension_plan ON plan_extension_log (plan_id);