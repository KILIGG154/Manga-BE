-- ============================================================================
-- Migration: Fix NCLOB to NVARCHAR(MAX) conversion issues for SQL Server
-- Problem: @Lob + @Nationalized on String fields causes Hibernate to map
--          as NCLOB, which the SQL Server JDBC driver cannot convert back
--          to nvarchar for Hibernate's dirty-checking reads.
-- Fix:    ALTER all affected text columns to NVARCHAR(MAX).
--          This migration is idempotent (SAFE to re-run).
-- ============================================================================

-- === ProductionPlan columns ===
ALTER TABLE ProductionPlan ALTER COLUMN Milestones NVARCHAR(MAX);
ALTER TABLE ProductionPlan ALTER COLUMN Schedule NVARCHAR(MAX);
ALTER TABLE ProductionPlan ALTER COLUMN ChapterTimeline NVARCHAR(MAX);
ALTER TABLE ProductionPlan ALTER COLUMN Resources NVARCHAR(MAX);
ALTER TABLE ProductionPlan ALTER COLUMN AssistantAllocation NVARCHAR(MAX);
ALTER TABLE ProductionPlan ALTER COLUMN Risk NVARCHAR(MAX);
ALTER TABLE ProductionPlan ALTER COLUMN pause_reason NVARCHAR(MAX);

-- === Chapter columns ===
ALTER TABLE Chapter ALTER COLUMN recall_reason NVARCHAR(MAX);
ALTER TABLE Chapter ALTER COLUMN rejection_reason NVARCHAR(MAX);
ALTER TABLE Chapter ALTER COLUMN release_note NVARCHAR(MAX);

-- === Project columns ===
ALTER TABLE Project ALTER COLUMN Description NVARCHAR(MAX);

-- === SubTask columns ===
ALTER TABLE SubTask ALTER COLUMN description NVARCHAR(MAX);

-- === ChapterComment columns ===
ALTER TABLE ChapterComment ALTER COLUMN Body NVARCHAR(MAX);

-- === PlanComment columns ===
ALTER TABLE PlanComment ALTER COLUMN Body NVARCHAR(MAX);

-- === SubmissionReview columns ===
ALTER TABLE SubmissionReview ALTER COLUMN Comment NVARCHAR(MAX);

-- === Submission columns ===
ALTER TABLE Submission ALTER COLUMN Story NVARCHAR(MAX);
ALTER TABLE Submission ALTER COLUMN CharacterDescription NVARCHAR(MAX);
ALTER TABLE Submission ALTER COLUMN WorldSetting NVARCHAR(MAX);

-- === Feedback columns ===
ALTER TABLE Feedback ALTER COLUMN content NVARCHAR(MAX);

-- === DevelopmentPlan columns ===
ALTER TABLE DevelopmentPlan ALTER COLUMN StoryDirection NVARCHAR(MAX);
ALTER TABLE DevelopmentPlan ALTER COLUMN WorldSetting NVARCHAR(MAX);
ALTER TABLE DevelopmentPlan ALTER COLUMN MainCharacters NVARCHAR(MAX);
ALTER TABLE DevelopmentPlan ALTER COLUMN ArcPlanning NVARCHAR(MAX);
ALTER TABLE DevelopmentPlan ALTER COLUMN Notes NVARCHAR(MAX);
