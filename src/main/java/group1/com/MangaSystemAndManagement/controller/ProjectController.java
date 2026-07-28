package group1.com.MangaSystemAndManagement.controller;
import group1.com.MangaSystemAndManagement.dto.request.AssignTantouRequest;
import group1.com.MangaSystemAndManagement.dto.request.CancelProjectRequest;
import group1.com.MangaSystemAndManagement.dto.request.ProjectRequest;
import group1.com.MangaSystemAndManagement.dto.response.ProjectResponse;
import group1.com.MangaSystemAndManagement.dto.response.ResponseBase;
import group1.com.MangaSystemAndManagement.model.Project;
import group1.com.MangaSystemAndManagement.service.interfaces.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/projects")
@Tag(name = "Project", description = "Project management APIs")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService service;
    /**
     * Create a project manually.
     * - ADMIN: seeding / emergency use
     * - EDITORIAL_BOARD_MEMBER: creates the project after approving a Name Submission,
     *   then assigns a Tantou via POST /{projectId}/tantou
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('EDITORIAL_BOARD_MEMBER')")
    public ResponseEntity<ResponseBase> create(@RequestBody ProjectRequest request) {
        try {
            Project result = service.create(request);
            return ResponseEntity.status(201).body(new ResponseBase(201, "Created successfully", result));
        } catch (Exception e) {
            return ResponseEntity.status(409).body(new ResponseBase(409, e.getMessage(), null));
        }
    }
    @GetMapping
    public ResponseEntity<ResponseBase> findAll() {
        try {
            List<Project> result = service.findAll();
            List<ProjectResponse> response = result.stream()
                    .map(ProjectResponse::from)
                    .toList();
            return ResponseEntity.status(200).body(new ResponseBase(200, "Success", response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<ResponseBase> findById(@PathVariable Long id) {
        try {
            return service.findById(id)
                    .map(p -> ResponseEntity.status(200).body(new ResponseBase(200, "Success", ProjectResponse.from(p))))
                    .orElseGet(() -> ResponseEntity.status(404).body(new ResponseBase(404, "Not found", null)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }
    @PutMapping("/{id}")
    public ResponseEntity<ResponseBase> update(@PathVariable Long id, @RequestBody ProjectRequest request) {
        try {
            Project result = service.update(id, request);
            return ResponseEntity.status(200).body(new ResponseBase(200, "Updated successfully", result));
        } catch (Exception e) {
            return ResponseEntity.status(409).body(new ResponseBase(409, e.getMessage(), null));
        }
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseBase> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.status(200).body(new ResponseBase(200, "Deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(409).body(new ResponseBase(409, e.getMessage(), null));
        }
    }

    /**
     * Assign a Tantō (editor-in-charge) to a project.
     * Body: { "tantouId": <accountId> }
     */
    @PostMapping("/{projectId}/tantou")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MANAGER') or hasAuthority('TANTOU_EDITOR')")
    @Operation(summary = "Assign Tantō to a project")
    public ResponseEntity<ResponseBase> assignTantou(
            @PathVariable Long projectId,
            @Valid @RequestBody AssignTantouRequest request) {
        try {
            Project result = service.assignTantou(projectId, request.getTantouId());
            return ResponseEntity.status(200)
                    .body(new ResponseBase(200, "Tantō assigned successfully", ProjectResponse.from(result)));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(403).body(new ResponseBase(403, e.getMessage(), null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(new ResponseBase(409, e.getMessage(), null));
        }
    }

    /**
     * Cancel a Project (BA V3 §2.1). Cascades to Plan; PUBLISHED chapters stay public;
     * further writes to non-published chapters are blocked.
     *
     * Allowed: LEADER_BOARD or EDITORIAL_BOARD_MEMBER.
     */
    @PostMapping("/{projectId}/cancel")
    @PreAuthorize("hasAuthority('LEADER_BOARD') or hasAuthority('EDITORIAL_BOARD_MEMBER')")
    @Operation(summary = "Cancel a project (Leader or Board)")
    public ResponseEntity<ResponseBase> cancel(
            @PathVariable Long projectId,
            @RequestParam Long requesterId,
            @Valid @RequestBody CancelProjectRequest request) {
        try {
            Project result = service.cancelProject(projectId, requesterId, request.getReason());
            return ResponseEntity.status(200)
                    .body(new ResponseBase(200, "Project cancelled", ProjectResponse.from(result)));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(403).body(new ResponseBase(403, e.getMessage(), null));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(409).body(new ResponseBase(409, e.getMessage(), null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    /**
     * Get all projects assigned to a specific Tantō (editor-in-charge).
     * Returns full project details including related owner, tantou, mangaka, and plans.
     *
     * @param tantouId the Account id of the Tantō
     */
    @GetMapping("/tantou/{tantouId}")
    @PreAuthorize("hasAuthority('TANTOU_EDITOR') or hasAuthority('ADMIN')")
    @Operation(summary = "Get all projects by Tantō editor ID")
    public ResponseEntity<ResponseBase> findByTantouId(@PathVariable Long tantouId) {
        try {
            List<Project> result = service.findByTantouId(tantouId);
            List<ProjectResponse> response = result.stream()
                    .map(ProjectResponse::from)
                    .toList();
            return ResponseEntity.status(200).body(new ResponseBase(200, "Success", response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }
}
