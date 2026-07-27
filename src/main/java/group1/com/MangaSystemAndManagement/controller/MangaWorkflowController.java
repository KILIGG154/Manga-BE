package group1.com.MangaSystemAndManagement.controller;

import group1.com.MangaSystemAndManagement.dto.request.NameSubmissionRequest;
import group1.com.MangaSystemAndManagement.dto.request.ReviewRequest;
import group1.com.MangaSystemAndManagement.dto.response.ResponseBase;
import group1.com.MangaSystemAndManagement.dto.request.ResubmitRequest;
import group1.com.MangaSystemAndManagement.model.Submission;
import group1.com.MangaSystemAndManagement.model.SubmissionReview;
import group1.com.MangaSystemAndManagement.service.interfaces.MangaWorkflowService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflow")
@Tag(name = "MangaWorkflow", description = "Manga Name submission and Tantor review flow")
@RequiredArgsConstructor
public class MangaWorkflowController {

    private final MangaWorkflowService workflowService;

    @PostMapping("/name/submit")
    public ResponseEntity<ResponseBase> submitName(@RequestBody NameSubmissionRequest req) {
        try {
            Submission saved = workflowService.submitName(req);
            return ResponseEntity.status(201).body(new ResponseBase(201, "Submitted", saved));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (RuntimeException re) {
            String msg = re.getMessage() == null ? "" : re.getMessage();
            if (msg.toLowerCase().contains("not found")) {
                return ResponseEntity.status(404).body(new ResponseBase(404, msg, null));
            }
            return ResponseEntity.status(500).body(new ResponseBase(500, msg, null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @PostMapping("/name/review/tantou")
    @io.swagger.v3.oas.annotations.Operation(summary = "Tantou Editor reviews the submission")
    public ResponseEntity<ResponseBase> reviewByTantou(@RequestBody ReviewRequest req) {
        try {
            SubmissionReview savedReview = workflowService.reviewByTantou(req);
            return ResponseEntity.status(200).body(new ResponseBase(200, "Tantou Review recorded", savedReview));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (RuntimeException re) {
            String msg = re.getMessage() == null ? "" : re.getMessage();
            if (msg.toLowerCase().contains("not found")) {
                return ResponseEntity.status(404).body(new ResponseBase(404, msg, null));
            }
            return ResponseEntity.status(400).body(new ResponseBase(400, msg, null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @PostMapping("/name/review/board")
    @io.swagger.v3.oas.annotations.Operation(summary = "Editorial Board Member votes on the submission")
    public ResponseEntity<ResponseBase> reviewByBoard(@RequestBody ReviewRequest req) {
        try {
            SubmissionReview savedReview = workflowService.reviewByBoard(req);
            return ResponseEntity.status(200).body(new ResponseBase(200, "Board Review recorded", savedReview));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (RuntimeException re) {
            String msg = re.getMessage() == null ? "" : re.getMessage();
            if (msg.toLowerCase().contains("not found")) {
                return ResponseEntity.status(404).body(new ResponseBase(404, msg, null));
            }
            return ResponseEntity.status(400).body(new ResponseBase(400, msg, null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @PostMapping("/name/{id}/submit-to-board")
    public ResponseEntity<ResponseBase> submitToBoard(@PathVariable Long id, @RequestParam Long tantouId) {
        try {
            Submission updated = workflowService.submitToBoard(id, tantouId);
            return ResponseEntity.status(200).body(new ResponseBase(200, "Submitted to Editorial Board", updated));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (RuntimeException re) {
            String msg = re.getMessage() == null ? "" : re.getMessage();
            if (msg.toLowerCase().contains("not found")) {
                return ResponseEntity.status(404).body(new ResponseBase(404, msg, null));
            }
            return ResponseEntity.status(400).body(new ResponseBase(400, msg, null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @PostMapping("/name/{id}/request-revision")
    @io.swagger.v3.oas.annotations.Operation(summary = "Leader Board requests revision for a submission")
    public ResponseEntity<ResponseBase> requestRevision(
            @PathVariable Long id,
            @RequestParam Long leaderId,
            @RequestParam String comment) {
        try {
            Submission updated = workflowService.requestRevision(id, leaderId, comment);
            return ResponseEntity.status(200).body(new ResponseBase(200, "Revision requested successfully", updated));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (RuntimeException re) {
            String msg = re.getMessage() == null ? "" : re.getMessage();
            if (msg.toLowerCase().contains("not found")) {
                return ResponseEntity.status(404).body(new ResponseBase(404, msg, null));
            }
            return ResponseEntity.status(400).body(new ResponseBase(400, msg, null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @GetMapping("/name/submissions")
    public ResponseEntity<ResponseBase> listSubmissions(@RequestParam(required = false) String status) {
        try {
            List<Submission> list = workflowService.listSubmissions(status);
            return ResponseEntity.ok(new ResponseBase(200, "Success", list));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @GetMapping("/name/{id}/reviews")
    public ResponseEntity<ResponseBase> listReviewsForSubmission(@PathVariable Long id) {
        try {
            var list = workflowService.listReviewsForSubmission(id);
            return ResponseEntity.ok(new ResponseBase(200, "Success", list));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @PostMapping("/name/resubmit")
    public ResponseEntity<ResponseBase> resubmitName(@RequestBody group1.com.MangaSystemAndManagement.dto.request.ResubmitRequest req) {
        try {
            Submission updated = workflowService.resubmitName(req);
            return ResponseEntity.ok(new ResponseBase(200, "Resubmitted", updated));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (RuntimeException re) {
            String msg = re.getMessage() == null ? "" : re.getMessage();
            if (msg.toLowerCase().contains("not found")) {
                return ResponseEntity.status(404).body(new ResponseBase(404, msg, null));
            }
            return ResponseEntity.status(500).body(new ResponseBase(500, msg, null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @PostMapping("/project/{projectId}/assign-tantou/{tantouId}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Assign Tantor into Project")
    public ResponseEntity<ResponseBase> assignTantouToProject(@PathVariable Long projectId, @PathVariable Long tantouId) {
        try {
            workflowService.assignTantouToProject(projectId, tantouId);
            return ResponseEntity.ok(new ResponseBase(200, "Tantou Editor successfully assigned to the project", null));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (RuntimeException re) {
            String msg = re.getMessage() == null ? "" : re.getMessage();
            if (msg.toLowerCase().contains("not found")) {
                return ResponseEntity.status(404).body(new ResponseBase(404, msg, null));
            }
            return ResponseEntity.status(400).body(new ResponseBase(400, msg, null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @PostMapping("/project/{projectId}/assign-mangaka/{mangakaId}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Assign a Mangaka to a Project")
    public ResponseEntity<ResponseBase> assignMangakaToProject(@PathVariable Long projectId, @PathVariable Long mangakaId) {
        try {
            workflowService.assignMangakaToProject(projectId, mangakaId);
            return ResponseEntity.ok(new ResponseBase(200, "Mangaka successfully assigned to the project", null));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (RuntimeException re) {
            String msg = re.getMessage() == null ? "" : re.getMessage();
            if (msg.toLowerCase().contains("not found")) {
                return ResponseEntity.status(404).body(new ResponseBase(404, msg, null));
            }
            return ResponseEntity.status(400).body(new ResponseBase(400, msg, null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }
}
