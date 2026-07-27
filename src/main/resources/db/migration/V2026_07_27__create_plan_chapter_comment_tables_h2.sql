-- =============================================================================
-- H2 / dev migration: create PlanComment + ChapterComment tables
-- Decision Log 2026-07-27 §AI-05 + §AI-12.
-- Used by H2 in-memory database during development & integration tests.
-- =============================================================================

CREATE TABLE IF NOT EXISTS PlanComment (
    Id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    ProductionPlanId  BIGINT NOT NULL,
    AuthorId          BIGINT NOT NULL,
    AuthorName        VARCHAR(255),
    Body              CLOB NOT NULL,
    CreatedAt         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ProductionPlanId) REFERENCES ProductionPlan(Id)
);

CREATE INDEX IF NOT EXISTS IX_PlanComment_PlanId_CreatedAt
    ON PlanComment (ProductionPlanId, CreatedAt);

CREATE TABLE IF NOT EXISTS ChapterComment (
    Id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    ChapterId    BIGINT NOT NULL,
    AuthorId     BIGINT NOT NULL,
    AuthorName   VARCHAR(255),
    Body         CLOB NOT NULL,
    CreatedAt    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ChapterId) REFERENCES Chapter(Id)
);

CREATE INDEX IF NOT EXISTS IX_ChapterComment_ChapterId_CreatedAt
    ON ChapterComment (ChapterId, CreatedAt);