-- =============================================================================
-- Migration: create PlanComment + ChapterComment tables
-- Decision Log 2026-07-27 §AI-05 + §AI-12.
-- Sprint 5: Comment system for ProductionPlan + Chapter (used during PAUSED +
-- for Return/Recall discussion).
-- =============================================================================
-- Pre-flight: confirm tables do not exist before running.
-- Safe to run multiple times thanks to IF NOT EXISTS.

IF NOT EXISTS (SELECT 1 FROM sysobjects WHERE name='PlanComment' AND xtype='U')
BEGIN
    CREATE TABLE PlanComment (
        Id                BIGINT IDENTITY(1,1) PRIMARY KEY,
        ProductionPlanId  BIGINT NOT NULL,
        AuthorId          BIGINT NOT NULL,
        AuthorName        NVARCHAR(255) NULL,
        Body              NVARCHAR(MAX) NOT NULL,
        CreatedAt         DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),

        CONSTRAINT FK_PlanComment_ProductionPlan
            FOREIGN KEY (ProductionPlanId) REFERENCES ProductionPlan(Id)
    );

    CREATE INDEX IX_PlanComment_PlanId_CreatedAt
        ON PlanComment (ProductionPlanId, CreatedAt);

    PRINT 'Created table PlanComment';
END
ELSE
BEGIN
    PRINT 'Table PlanComment already exists — skipping';
END
GO

IF NOT EXISTS (SELECT 1 FROM sysobjects WHERE name='ChapterComment' AND xtype='U')
BEGIN
    CREATE TABLE ChapterComment (
        Id           BIGINT IDENTITY(1,1) PRIMARY KEY,
        ChapterId    BIGINT NOT NULL,
        AuthorId     BIGINT NOT NULL,
        AuthorName   NVARCHAR(255) NULL,
        Body         NVARCHAR(MAX) NOT NULL,
        CreatedAt    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),

        CONSTRAINT FK_ChapterComment_Chapter
            FOREIGN KEY (ChapterId) REFERENCES Chapter(Id)
    );

    CREATE INDEX IX_ChapterComment_ChapterId_CreatedAt
        ON ChapterComment (ChapterId, CreatedAt);

    PRINT 'Created table ChapterComment';
END
ELSE
BEGIN
    PRINT 'Table ChapterComment already exists — skipping';
END
GO