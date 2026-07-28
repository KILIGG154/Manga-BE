package group1.com.MangaSystemAndManagement.controller;

import group1.com.MangaSystemAndManagement.dto.request.CreateSubTaskRequest;
import group1.com.MangaSystemAndManagement.dto.response.ResponseBase;
import group1.com.MangaSystemAndManagement.dto.response.SubTaskResponse;
import group1.com.MangaSystemAndManagement.exception.ResourceNotFoundException;
import group1.com.MangaSystemAndManagement.exception.WorkflowRuleViolationException;
import group1.com.MangaSystemAndManagement.service.interfaces.SubTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SubTask resources.
 *
 * <p>SubTask is always owned by a {@code Task}, which is in turn owned by a
 * {@code Chapter}. The URL structure reflects this hierarchy so the relationship
 * is self-describing.</p>
 *
 * <ul>
 *   <li>{@code POST /api/tasks/{taskId}/subtasks} – Mangaka/Tantō splits a Task.</li>
 *   <li>{@code GET  /api/tasks/{taskId}/subtasks} – list SubTasks of a Task.</li>
 *   <li>{@code GET  /api/users/{userId}/subtasks} – SubTasks assigned to a user
 *       (Assistant dashboard).</li>
 *   <li>{@code GET  /api/users/{userId}/subtasks/assigned} – SubTasks assigned
 *       to the given Assistant, no {@code requesterId} required (the path
 *       user id is assumed to be the caller).</li>
 *   <li>{@code GET  /api/subtasks/{subTaskId}} – fetch a single SubTask.</li>
 *   <li>{@code POST /api/subtasks/{subTaskId}/reopen} – reopen a COMPLETED SubTask.</li>
 * </ul>
 */
@RestController
@Tag(name = "SubTask", description = "Mangaka creates sub-tasks, Assistant receives")
@RequiredArgsConstructor
public class SubTaskController {

    private final SubTaskService subTaskService;

    // =====================================================================
    // 1. Nested collection under Task
    // =====================================================================
    @PostMapping("/api/tasks/{taskId}/subtasks")
    @Operation(summary = "Create a SubTask under a Task")
    public ResponseEntity<ResponseBase> create(@PathVariable Long taskId,
                                               @Valid @RequestBody CreateSubTaskRequest req) {
        try {
            SubTaskResponse created = subTaskService.createSubTask(taskId, req);
            return ResponseEntity.status(201)
                    .body(new ResponseBase(201, "SubTask created", created));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (WorkflowRuleViolationException w) {
            return ResponseEntity.status(400).body(new ResponseBase(400, w.getMessage(), null));
        } catch (ResourceNotFoundException r) {
            return ResponseEntity.status(404).body(new ResponseBase(404, r.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @GetMapping("/api/tasks/{taskId}/subtasks")
    @Operation(summary = "List SubTasks of a given Task")
    public ResponseEntity<ResponseBase> listByTask(@PathVariable Long taskId,
                                                   @RequestParam Long requesterId) {
        try {
            List<SubTaskResponse> result = subTaskService.listByTask(taskId, requesterId);
            return ResponseEntity.ok(new ResponseBase(200, "SubTasks retrieved", result));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (ResourceNotFoundException r) {
            return ResponseEntity.status(404).body(new ResponseBase(404, r.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    // =====================================================================
    // 2. Sub-resource under User (Assistant dashboard)
    // =====================================================================
    @GetMapping("/api/users/{userId}/subtasks")
    @Operation(summary = "List SubTasks assigned to a specific user (Assistant dashboard)")
    public ResponseEntity<ResponseBase> listByAssignee(@PathVariable Long userId,
                                                       @RequestParam Long requesterId) {
        try {
            List<SubTaskResponse> result = subTaskService.listByAssignee(userId, requesterId);
            return ResponseEntity.ok(new ResponseBase(200, "SubTasks retrieved", result));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (ResourceNotFoundException r) {
            return ResponseEntity.status(404).body(new ResponseBase(404, r.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @GetMapping("/api/users/{userId}/subtasks/assigned")
    @Operation(summary = "List SubTasks assigned to the given Assistant (no requesterId required)")
    public ResponseEntity<ResponseBase> listAssignedToAssistant(@PathVariable Long userId) {
        try {
            List<SubTaskResponse> result = subTaskService.listByAssistant(userId);
            return ResponseEntity.ok(new ResponseBase(200, "SubTasks retrieved", result));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (ResourceNotFoundException r) {
            return ResponseEntity.status(404).body(new ResponseBase(404, r.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    // =====================================================================
    // 3. Single SubTask resource
    // =====================================================================
    @GetMapping("/api/subtasks/{subTaskId}")
    @Operation(summary = "Get a single SubTask by its id")
    public ResponseEntity<ResponseBase> get(@PathVariable Long subTaskId,
                                            @RequestParam Long requesterId) {
        try {
            return ResponseEntity.ok(new ResponseBase(200, "SubTask retrieved",
                    subTaskService.getById(subTaskId, requesterId)));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (ResourceNotFoundException r) {
            return ResponseEntity.status(404).body(new ResponseBase(404, r.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    // =====================================================================
    // 4. Action on a single SubTask
    // =====================================================================
    @PostMapping("/api/subtasks/{subTaskId}/reopen")
    @Operation(summary = "Reopen a COMPLETED SubTask so the Assistant can fix it again")
    public ResponseEntity<ResponseBase> reopen(@PathVariable Long subTaskId,
                                               @RequestParam Long requesterId) {
        try {
            return ResponseEntity.ok(new ResponseBase(200, "SubTask reopened",
                    subTaskService.reopen(subTaskId, requesterId)));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (WorkflowRuleViolationException w) {
            return ResponseEntity.status(400).body(new ResponseBase(400, w.getMessage(), null));
        } catch (ResourceNotFoundException r) {
            return ResponseEntity.status(404).body(new ResponseBase(404, r.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }
}
