package group1.com.MangaSystemAndManagement.service.interfaces;

import group1.com.MangaSystemAndManagement.dto.request.*;
import group1.com.MangaSystemAndManagement.dto.response.*;

import java.time.LocalDate;
import java.util.List;

public interface ProductionWorkflowService {

    ProjectResponse createProject(CreateProjectRequest req, Long creatorId);

    ProjectResponse activateProject(Long projectId, Long requesterId);

    ProjectResponse updateProjectByBoard(Long projectId, UpdateProjectBoardRequest req, Long editorId);

    ProjectResponse updateProjectByTantou(Long projectId, UpdateProjectTantouRequest req, Long tantouId);

    PlanDashboardResponse getPlanDashboard(Long planId, Long requesterId);

    ChapterResponse createChapter(CreateChapterRequest req, Long requesterId);

    ChapterWithTasksResponse updateChapterStatus(Long chapterId, group1.com.MangaSystemAndManagement.model.ChapterStatus status, Long requesterId);

    ChapterResponse assignChapter(Long chapterId, AssignChapterRequest req);

    /**
     * Spec v2.1 §AI-MT-01: Mangaka (chapter assignee) manually creates a Task under a Chapter.
     * Allowed: MANGAKA who is the {@code assignee} of the chapter.
     * The new Task defaults to status TODO, assignee = requester, and inherits the chapter's project/plan.
     * If a deadline is supplied it must not exceed the parent Chapter's {@code endDate}.
     */
    TaskResponse createManualTask(Long chapterId, CreateManualTaskRequest req);

    TaskResponse updateTaskStatus(Long taskId, UpdateTaskStatusRequest req);

    FeedbackResponse createFeedback(Long taskId, CreateFeedbackRequest req);

    TaskResponse assignTask(Long taskId, AssignTaskRequest req);

    List<AssetResponse> getProjectAssets(Long projectId, Long requesterId);

    /**
     * Returns all COMPLETED chapters for a given project that are ready to be published.
     * Accessible by LEADER_BOARD and EDITORIAL_BOARD_MEMBER.
     */
    List<ChapterResponse> getPublishableChapters(Long projectId, Long requesterId);

    /**
     * BA V3 §3.1 — list chapters assigned to a specific Mangaka (for "My Chapters" view).
     * Accessible by the Mangaka themselves (requesterId must match assigneeId) or by
     * TANTOU_EDITOR / LEADER_BOARD / EDITORIAL_BOARD_MEMBER of the parent project.
     */
    /**
     * List every chapter whose assignee is {@code assigneeId}.
     * Caller is resolved from Spring Security (JWT).
     */
    List<ChapterResponse> getChaptersAssignedToMangaka(Long assigneeId);

    /**
     * Leader publishes a chapter by setting its publishDate and transitioning status to PUBLISHED.
     * Allowed: LEADER_BOARD or EDITORIAL_BOARD_MEMBER (BA V3 §3.1).
     * Chapter must be COMPLETED. If publishDate is null, defaults to today.
     */
    ChapterResponse publishChapter(Long chapterId, Long leaderId, LocalDate publishDate);

    /**
     * Decision Log 2026-07-27 §AI-01: extended publish with optional releaseNote.
     * If releaseNote is null or blank, it is stored as NULL.
     */
    ChapterResponse publishChapter(Long chapterId, Long leaderId, LocalDate publishDate, String releaseNote);

    /**
     * Decision Log 2026-07-27 §AI-04: Tantou chủ động chọn Task cần sửa sau Recall/Return.
     * Endpoint: POST /tasks/{id}/mark-revision.
     * Allowed: TANTOU_EDITOR (who is owner of the project) or LEADER_BOARD.
     * Transitions a single Task from DONE to REVISION_REQUIRED.
     */
    group1.com.MangaSystemAndManagement.dto.response.TaskResponse markTaskRevision(
            Long taskId, MarkTaskRevisionRequest req);

    /**
     * Recall a PUBLISHED chapter (BA V3 §3.4). Allowed: LEADER_BOARD or EDITORIAL_BOARD_MEMBER.
     * Requires recallReason (>= 15 chars). Transitions chapter back to IN_PRODUCTION, increments
     * recallCount, and rolls back Plan to IN_PROGRESS if it was COMPLETED.
     */
    ChapterResponse recallChapter(Long chapterId, Long requesterId, RecallChapterRequest request);

    /**
     * Decision Log 2026-07-27 §AI-07 follow-up: Leader override endpoint for recall when
     * the chapter has already been recalled the maximum number of times (cap = 2).
     * Allowed: LEADER_BOARD only. Skips the recallCount cap; forces the recall.
     */
    ChapterResponse overrideRecallChapter(Long chapterId, Long requesterId, OverrideRecallRequest request);

    /**
     * Return a COMPLETED chapter to production (BA V3 §3.3). Allowed: LEADER_BOARD or
     * EDITORIAL_BOARD_MEMBER. Increments rejectionCount. After 2 rejections the chapter is
     * locked into COMPLETED_NEEDS_REVIEW until a Leader overrides (see {@link #overrideReturnLimit}).
     */
    ChapterResponse returnChapterToProduction(Long chapterId, Long requesterId, ReturnChapterRequest request);

    /**
     * Override the rejection limit and force-return a chapter even though rejectionCount >= 2
     * (BA V3 §3.3). Allowed: LEADER_BOARD only.
     */
    ChapterResponse overrideReturnLimit(Long chapterId, Long requesterId, ReturnChapterRequest request);

    /**
     * Decision Log 2026-07-27 §AI-08: lên lịch xuất bản.
     * Chapter COMPLETED → SCHEDULED, ghi nhận publishDate. Scheduler sẽ tự động
     * SCHEDULED → PUBLISHED khi publishDate đến.
     * Allowed: TANTOU_EDITOR, LEADER_BOARD, EDITORIAL_BOARD_MEMBER.
     */
    ChapterResponse scheduleChapter(Long chapterId, Long requesterId, ScheduleChapterRequest request);

    /**
     * Decision Log 2026-07-27 §AI-08: scheduler job tự động.
     * Tìm tất cả Chapter SCHEDULED có publishDate &lt;= today và chuyển sang PUBLISHED.
     * Trả về số chapter đã publish (cho log/audit).
     * Không throw — best-effort.
     */
    int publishDueScheduledChapters();
}

