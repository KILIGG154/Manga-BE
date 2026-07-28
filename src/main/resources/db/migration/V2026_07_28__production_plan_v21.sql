-- =============================================================================
-- Migration: Production Plan v2.1 refactor
-- Decision Log 2026-07-28: align schema with Technical Spec v2.1
--
-- Changes:
--   1. Add column `title` (VARCHAR(255) NOT NULL) with unique per project
--   2. Add column `deadline_date` (DATE) and `publish_date` (DATE)
--   3. Add column `actual_end_date` (DATE NULLABLE)
--   4. Add column `created_by` (BIGINT NOT NULL)
--   5. Migrate legacy plan_status values:
--        IN_PROGRESS -> ACTIVE
--        PAUSED      -> EXTENDED
--        CANCELLED   -> COMPLETED
--   6. Create table `plan_extension_log`
-- =============================================================================

IF NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('ProductionPlan') AND name = 'title')
BEGIN
    ALTER TABLE ProductionPlan ADD title NVARCHAR(255) NULL;
END
GO

IF EXISTS (SELECT 1 FROM sys.columns
           WHERE object_id = OBJECT_ID('ProductionPlan') AND name = 'title')
   AND EXISTS (SELECT 1 FROM ProductionPlan WHERE title IS NULL)
BEGIN
    UPDATE ProductionPlan
    SET title = N'Production Plan ' + FORMAT(GETDATE(), 'MM/yyyy')
    WHERE title IS NULL;
END
GO

IF EXISTS (SELECT 1 FROM sys.columns
           WHERE object_id = OBJECT_ID('ProductionPlan') AND name = 'title')
   AND (SELECT IS_NULLABLE FROM sys.columns
        WHERE object_id = OBJECT_ID('ProductionPlan') AND name = 'title') = 'YES'
BEGIN
    ALTER TABLE ProductionPlan ALTER COLUMN title NVARCHAR(255) NOT NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('ProductionPlan') AND name = 'deadline_date')
BEGIN
    ALTER TABLE ProductionPlan ADD deadline_date DATE NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('ProductionPlan') AND name = 'publish_date')
BEGIN
    ALTER TABLE ProductionPlan ADD publish_date DATE NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('ProductionPlan') AND name = 'actual_end_date')
BEGIN
    ALTER TABLE ProductionPlan ADD actual_end_date DATE NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('ProductionPlan') AND name = 'created_by')
BEGIN
    ALTER TABLE ProductionPlan ADD created_by BIGINT NULL;
END
GO

UPDATE ProductionPlan SET plan_status = 'ACTIVE'    WHERE plan_status = 'IN_PROGRESS';
UPDATE ProductionPlan SET plan_status = 'EXTENDED'  WHERE plan_status = 'PAUSED';
UPDATE ProductionPlan SET plan_status = 'COMPLETED' WHERE plan_status = 'CANCELLED';
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID('ProductionPlan') AND name = 'idx_project_title')
BEGIN
    CREATE UNIQUE INDEX idx_project_title ON ProductionPlan (ProjectId, title);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID('ProductionPlan') AND name = 'idx_plan_status_dates')
BEGIN
    CREATE INDEX idx_plan_status_dates ON ProductionPlan (plan_status, end_date);
END
GO

IF OBJECT_ID('plan_extension_log', 'U') IS NULL
BEGIN
    CREATE TABLE plan_extension_log (
        id              BIGINT IDENTITY(1,1) PRIMARY KEY,
        plan_id         BIGINT NOT NULL,
        old_end_date    DATE NOT NULL,
        new_end_date    DATE NOT NULL,
        reason_code     VARCHAR(50) NOT NULL,
        reason_note     NVARCHAR(MAX) NULL,
        extended_by     BIGINT NOT NULL,
        extended_at     DATETIME2 NOT NULL DEFAULT GETDATE(),
        CONSTRAINT fk_extension_plan FOREIGN KEY (plan_id)
            REFERENCES ProductionPlan(Id) ON DELETE CASCADE
    );
    CREATE INDEX idx_extension_plan ON plan_extension_log (plan_id);
END
GO