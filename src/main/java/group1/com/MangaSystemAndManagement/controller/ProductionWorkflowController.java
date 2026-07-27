package group1.com.MangaSystemAndManagement.controller;

import group1.com.MangaSystemAndManagement.dto.request.*;
import group1.com.MangaSystemAndManagement.dto.response.ResponseBase;
import group1.com.MangaSystemAndManagement.model.ChapterStatus;
import group1.com.MangaSystemAndManagement.model.ProjectWorkflowStatus;
import group1.com.MangaSystemAndManagement.service.interfaces.ProductionWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/workflow")
@Tag(name = "Production Workflow", description = "Manga production pipeline and task management")
@RequiredArgsConstructor
public class ProductionWorkflowController {

    private final ProductionWorkflowService workflowService;

    // --- Project & Plan Management ---

    @PostMapping("/projects")
    @Operation(summary = "Create a new project and assign Tantou (Editorial Board only)")
    public ResponseEntity<ResponseBase> createProject(@Valid @RequestBody CreateProjectRequest req,
            @RequestParam Long editorId) {
        try {
            var res = workflowService.createProject(req, editorId);
            return ResponseEntity.status(201).body(new ResponseBase(201, "Project created", res));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @PutMapping("/projects/{projectId}/status")
    @Operation(summary = "Update project status -> Triggers auto-plan creation if ACTIVE (Tantou only)")
    public ResponseEntity<ResponseBase> activateProject(@PathVariable Long projectId, @RequestParam Long tantouId) {
        try {
            var res = workflowService.activateProject(projectId, tantouId);
            return ResponseEntity.ok(new ResponseBase(200, "Project status updated", res));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @GetMapping("/production-plans/{planId}/dashboard")
    @Operation(summary = "Get full timeline, chapters, and progress of a Plan")
    public ResponseEntity<ResponseBase> getPlanDashboard(@PathVariable Long planId, @RequestParam Long requesterId) {
        try {
            var res = workflowService.getPlanDashboard(planId, requesterId);
            return ResponseEntity.ok(new ResponseBase(200, "Dashboard retrieved", res));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    // --- Production Pipeline ---

    @PostMapping("/chapters")
    @Operation(summary = "Create a new chapter -> Triggers auto-generation of 4 default tasks (Tantou only)")
    public ResponseEntity<ResponseBase> createChapter(@Valid @RequestBody CreateChapterRequest req,
            @RequestParam Long requesterId) {
        try {
            var res = workflowService.createChapter(req, requesterId);
            return ResponseEntity.status(201).body(new ResponseBase(201, "Chapter and default tasks created", res));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @PostMapping("/chapters/{chapterId}/assign")
    @Operation(summary = "Assign a Chapter to a Mangaka -> Auto-assigns all tasks in the chapter to the Mangaka")
    public ResponseEntity<ResponseBase> assignChapter(@PathVariable Long chapterId, @Valid @RequestBody AssignChapterRequest req) {
        try {
            var res = workflowService.assignChapter(chapterId, req);
            return ResponseEntity.ok(new ResponseBase(200, "Chapter assigned to Mangaka", res));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @PutMapping("/chapters/{chapterId}/status")
    @Operation(summary = "Update chapter status -> Roll-up validation prevents COMPLETED if tasks aren't DONE (Tantou only)")
    public ResponseEntity<ResponseBase> updateChapterStatus(@PathVariable Long chapterId, @RequestParam ChapterStatus status,
            @RequestParam Long requesterId) {
        try {
            var res = workflowService.updateChapterStatus(chapterId, status, requesterId);
            return ResponseEntity.ok(new ResponseBase(200, "Chapter status updated", res));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(400).body(new ResponseBase(400, ise.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @PutMapping("/tasks/{taskId}/status")
    @Operation(summary = "Update task status -> Setting to REVIEW locks the task for Feedback")
    public ResponseEntity<ResponseBase> updateTaskStatus(@PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskStatusRequest req) {
        try {
            var res = workflowService.updateTaskStatus(taskId, req);
            return ResponseEntity.ok(new ResponseBase(200, "Task status updated", res));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(400).body(new ResponseBase(400, ise.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @PostMapping("/tasks/{taskId}/feedback")
    @Operation(summary = "Submit Tantou feedback on a task -> Triggers task status update (APPROVED=DONE, REJECTED=IN_PROGRESS)")
    public ResponseEntity<ResponseBase> submitFeedback(@PathVariable Long taskId,
            @Valid @RequestBody CreateFeedbackRequest req) {
        try {
            var res = workflowService.createFeedback(taskId, req);
            return ResponseEntity.status(201).body(new ResponseBase(201, "Feedback submitted and task updated", res));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(400).body(new ResponseBase(400, ise.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @PostMapping("/tasks/{taskId}/assign")
    @Operation(summary = "Assign a task to a user")
    public ResponseEntity<ResponseBase> assignTask(@PathVariable Long taskId, @Valid @RequestBody AssignTaskRequest req) {
        try {
            var res = workflowService.assignTask(taskId, req);
            return ResponseEntity.ok(new ResponseBase(200, "Task assigned", res));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    /**
     * POST /api/workflow/tasks/{taskId}/mark-revision
     * Decision Log 2026-07-27 §AI-04: Tantou chủ động chọn 1 Task và set REVISION_REQUIRED.
     * Allowed: TANTOU_EDITOR hoặc LEADER_BOARD.
     * Body: { tantouId, note? }.
     * Chỉ có ý nghĩa khi Chapter đang ở IN_PRODUCTION (sau Return/Recall).
     */
    @PostMapping("/tasks/{taskId}/mark-revision")
    @Operation(
        summary = "Mark a single Task for revision (Tantou chooses)",
        description = "Transitions a Task (DONE/REVIEW) → REVISION_REQUIRED. §AI-04 — Tantou picks which Task needs rework.")
    public ResponseEntity<ResponseBase> markTaskRevision(
            @PathVariable Long taskId,
            @Valid @RequestBody MarkTaskRevisionRequest req) {
        try {
            var res = workflowService.markTaskRevision(taskId, req);
            return ResponseEntity.ok(new ResponseBase(200, "Task marked for revision", res));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(409).body(new ResponseBase(409, ise.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @GetMapping("/projects/{projectId}/assets")
    @Operation(summary = "List project assets")
    public ResponseEntity<ResponseBase> getProjectAssets(@PathVariable Long projectId, @RequestParam Long requesterId) {
        try {
            var res = workflowService.getProjectAssets(projectId, requesterId);
            return ResponseEntity.ok(new ResponseBase(200, "Assets retrieved", res));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    // --- Publishing flow (Board view + Leader publish) ---

    /**
     * GET /api/workflow/projects/{projectId}/chapters/publishable?requesterId={id}
     * LEADER_BOARD and EDITORIAL_BOARD_MEMBER can view the list of COMPLETED chapters
     * that are ready to be published.
     */
    @GetMapping("/projects/{projectId}/chapters/publishable")
    @Operation(
        summary = "List COMPLETED chapters ready to publish",
        description = "Accessible by LEADER_BOARD and EDITORIAL_BOARD_MEMBER. " +
                      "Returns chapters with status=COMPLETED for the given project.")
    public ResponseEntity<ResponseBase> getPublishableChapters(
            @PathVariable Long projectId,
            @RequestParam Long requesterId) {
        try {
            var chapters = workflowService.getPublishableChapters(projectId, requesterId);
            return ResponseEntity.ok(new ResponseBase(200, "Publishable chapters retrieved", chapters));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    /**
     * POST /api/workflow/chapters/{chapterId}/publish
     * Allowed: LEADER_BOARD or EDITORIAL_BOARD_MEMBER (BA V3 §3.1).
     * Body: PublishChapterRequest { publishDate?, releaseNote? } — both OPTIONAL (§AI-01).
     */
    @PostMapping("/chapters/{chapterId}/publish")
    @Operation(
        summary = "Publish a chapter (Leader or Board)",
        description = "Sets publishDate (defaults to today) and releaseNote (optional) and transitions chapter status to PUBLISHED. " +
                      "Chapter must currently be COMPLETED.")
    public ResponseEntity<ResponseBase> publishChapter(
            @PathVariable Long chapterId,
            @RequestParam Long requesterId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publishDate,
            @RequestBody(required = false) PublishChapterRequest body) {
        try {
            String releaseNote = body != null ? body.getReleaseNote() : null;
            var chapter = workflowService.publishChapter(chapterId, requesterId, publishDate, releaseNote);
            return ResponseEntity.ok(new ResponseBase(200, "Chapter published successfully", chapter));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(409).body(new ResponseBase(409, ise.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    /**
     * POST /api/workflow/chapters/{chapterId}/return?requesterId={id}
     * BA V3 §3.3. Allowed: LEADER_BOARD or EDITORIAL_BOARD_MEMBER.
     * Body: { rejectionReason: String }.
     * Increments rejectionCount; on the 3rd attempt the chapter is locked into
     * COMPLETED_NEEDS_REVIEW.
     */
    @PostMapping("/chapters/{chapterId}/return")
    @Operation(
        summary = "Return a chapter to production",
        description = "Transitions COMPLETED -> IN_PRODUCTION, increments rejectionCount. " +
                      "Locked at COMPLETED_NEEDS_REVIEW after 2 returns.")
    public ResponseEntity<ResponseBase> returnChapter(
            @PathVariable Long chapterId,
            @RequestParam Long requesterId,
            @Valid @RequestBody ReturnChapterRequest req) {
        try {
            var chapter = workflowService.returnChapterToProduction(chapterId, requesterId, req);
            return ResponseEntity.ok(new ResponseBase(200, "Chapter returned to production", chapter));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(409).body(new ResponseBase(409, ise.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    /**
     * POST /api/workflow/chapters/{chapterId}/override-return?requesterId={id}
     * BA V3 §3.3. Allowed: LEADER_BOARD only.
     * Forces the return even though rejectionCount >= 2.
     */
    @PostMapping("/chapters/{chapterId}/override-return")
    @Operation(
        summary = "Override the rejection limit (Leader only)",
        description = "Force-returns a chapter already rejected 2 times. LEADER_BOARD only.")
    public ResponseEntity<ResponseBase> overrideReturn(
            @PathVariable Long chapterId,
            @RequestParam Long requesterId,
            @Valid @RequestBody ReturnChapterRequest req) {
        try {
            var chapter = workflowService.overrideReturnLimit(chapterId, requesterId, req);
            return ResponseEntity.ok(new ResponseBase(200, "Return limit overridden", chapter));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(409).body(new ResponseBase(409, ise.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    /**
     * POST /api/workflow/chapters/{chapterId}/recall?requesterId={id}
     * Allowed: LEADER_BOARD or EDITORIAL_BOARD_MEMBER (BA V3 §3.4).
     * Body: { recallReason: String >= 15 chars }.
     * Cap: recallCount = 2 (Decision Log §AI-07). Beyond cap → use override-recall.
     */
    @PostMapping("/chapters/{chapterId}/recall")
    @Operation(
        summary = "Recall a published chapter (Leader or Board)",
        description = "Returns the chapter to IN_PRODUCTION with the supplied recallReason " +
                      "(minimum 15 chars). Triggers Plan rollback if Plan was COMPLETED.")
    public ResponseEntity<ResponseBase> recallChapter(
            @PathVariable Long chapterId,
            @RequestParam Long requesterId,
            @Valid @RequestBody RecallChapterRequest req) {
        try {
            var chapter = workflowService.recallChapter(chapterId, requesterId, req);
            return ResponseEntity.ok(new ResponseBase(200, "Chapter recalled successfully", chapter));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(409).body(new ResponseBase(409, ise.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    /**
     * POST /api/workflow/chapters/{chapterId}/override-recall?requesterId={id}
     * Decision Log 2026-07-27 §AI-07 follow-up:
     * Allowed: LEADER_BOARD only. Forces recall even when recallCount >= 2.
     * Body: { leaderId, recallReason (>= 15 chars) }.
     */
    @PostMapping("/chapters/{chapterId}/override-recall")
    @Operation(
        summary = "Override the recall limit (Leader only)",
        description = "Force-recall a chapter that has already been recalled 2 times. LEADER_BOARD only. " +
                      "Bypasses the recallCount cap from Decision Log §AI-07.")
    public ResponseEntity<ResponseBase> overrideRecall(
            @PathVariable Long chapterId,
            @RequestParam Long requesterId,
            @Valid @RequestBody OverrideRecallRequest req) {
        try {
            var chapter = workflowService.overrideRecallChapter(chapterId, requesterId, req);
            return ResponseEntity.ok(new ResponseBase(200, "Recall limit overridden", chapter));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(409).body(new ResponseBase(409, ise.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    /**
     * POST /api/workflow/chapters/{chapterId}/schedule?requesterId={id}
     * Decision Log 2026-07-27 §AI-08:
     * Lên lịch xuất bản. Chapter COMPLETED → SCHEDULED, save publishDate.
     * Scheduler tự động SCHEDULED → PUBLISHED khi publishDate đến.
     * Allowed: TANTOU_EDITOR, LEADER_BOARD, EDITORIAL_BOARD_MEMBER.
     * Body: { schedulerId, publishDate: "yyyy-MM-dd" }.
     */
    @PostMapping("/chapters/{chapterId}/schedule")
    @Operation(
        summary = "Schedule a chapter to be auto-published on a future date",
        description = "Chapter COMPLETED → SCHEDULED. The Spring cron job (every 5 minutes) " +
                      "flips SCHEDULED → PUBLISHED when publishDate is on or before today.")
    public ResponseEntity<ResponseBase> scheduleChapter(
            @PathVariable Long chapterId,
            @RequestParam Long requesterId,
            @Valid @RequestBody ScheduleChapterRequest req) {
        try {
            var chapter = workflowService.scheduleChapter(chapterId, requesterId, req);
            return ResponseEntity.ok(new ResponseBase(200, "Chapter scheduled", chapter));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(409).body(new ResponseBase(409, ise.getMessage(), null));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(400).body(new ResponseBase(400, iae.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    /**
     * POST /api/workflow/chapters/publish-scheduled  (admin/cron manual trigger)
     * Decision Log 2026-07-27 §AI-08:
     * Manual trigger for the scheduler job. Chỉ Leader. Trả về số chapter đã auto-publish.
     */
    @PostMapping("/chapters/publish-scheduled")
    @Operation(
        summary = "Manually trigger the auto-publish scheduler (Leader only)",
        description = "Runs publishDueScheduledChapters() once. Returns the count of chapters " +
                      "that were SCHEDULED → PUBLISHED.")
    public ResponseEntity<ResponseBase> triggerPublishScheduled() {
        int count = workflowService.publishDueScheduledChapters();
        return ResponseEntity.ok(new ResponseBase(200, "Published " + count + " chapter(s)", count));
    }
}
