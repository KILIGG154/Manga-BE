package group1.com.MangaSystemAndManagement.controller;

import group1.com.MangaSystemAndManagement.dto.request.CreateChapterCommentRequest;
import group1.com.MangaSystemAndManagement.dto.request.CreatePlanCommentRequest;
import group1.com.MangaSystemAndManagement.dto.response.ChapterCommentResponse;
import group1.com.MangaSystemAndManagement.dto.response.PlanCommentResponse;
import group1.com.MangaSystemAndManagement.dto.response.ResponseBase;
import group1.com.MangaSystemAndManagement.service.interfaces.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Decision Log 2026-07-27 §AI-05 + §AI-12:
 * Comment endpoints for ProductionPlan + Chapter.
 *
 * <p>Useful when Plan is PAUSED (BA Spec V3 §2.2) — team can still discuss without
 * Slack/Zalo. Also used for Chapter-level discussion after Return/Recall.</p>
 */
@RestController
@RequestMapping("/api/workflow")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // -------- Plan comments (§AI-05) --------

    @PostMapping("/plans/{planId}/comments")
    @Operation(
        summary = "Add a comment to a ProductionPlan",
        description = "Allow any project member (Tantou/Mangaka/Assistant/Board/Leader/Admin) to post a comment. " +
                      "Append-only — no edit/delete.")
    public ResponseEntity<ResponseBase> addPlanComment(
            @PathVariable Long planId,
            @Valid @RequestBody CreatePlanCommentRequest req) {
        try {
            var res = commentService.addPlanComment(planId, req);
            return ResponseEntity.ok(new ResponseBase(200, "Comment added", res));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (RuntimeException re) {
            return ResponseEntity.status(404).body(new ResponseBase(404, re.getMessage(), null));
        }
    }

    @GetMapping("/plans/{planId}/comments")
    @Operation(summary = "List all comments for a ProductionPlan (chronological)")
    public ResponseEntity<ResponseBase> listPlanComments(@PathVariable Long planId) {
        try {
            List<PlanCommentResponse> res = commentService.listPlanComments(planId);
            return ResponseEntity.ok(new ResponseBase(200, "OK", res));
        } catch (RuntimeException re) {
            return ResponseEntity.status(404).body(new ResponseBase(404, re.getMessage(), null));
        }
    }

    // -------- Chapter comments (§AI-12) --------

    @PostMapping("/chapters/{chapterId}/comments")
    @Operation(
        summary = "Add a comment to a Chapter",
        description = "Project member posts a comment on a specific chapter (e.g. after Return/Recall).")
    public ResponseEntity<ResponseBase> addChapterComment(
            @PathVariable Long chapterId,
            @Valid @RequestBody CreateChapterCommentRequest req) {
        try {
            var res = commentService.addChapterComment(chapterId, req);
            return ResponseEntity.ok(new ResponseBase(200, "Comment added", res));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (RuntimeException re) {
            return ResponseEntity.status(404).body(new ResponseBase(404, re.getMessage(), null));
        }
    }

    @GetMapping("/chapters/{chapterId}/comments")
    @Operation(summary = "List all comments for a Chapter (chronological)")
    public ResponseEntity<ResponseBase> listChapterComments(@PathVariable Long chapterId) {
        try {
            List<ChapterCommentResponse> res = commentService.listChapterComments(chapterId);
            return ResponseEntity.ok(new ResponseBase(200, "OK", res));
        } catch (RuntimeException re) {
            return ResponseEntity.status(404).body(new ResponseBase(404, re.getMessage(), null));
        }
    }
}