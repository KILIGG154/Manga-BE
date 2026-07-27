package group1.com.MangaSystemAndManagement.controller;

import group1.com.MangaSystemAndManagement.dto.request.ForceClosePlanRequest;
import group1.com.MangaSystemAndManagement.dto.request.PausePlanRequest;
import group1.com.MangaSystemAndManagement.dto.request.ProductionPlanRequest;
import group1.com.MangaSystemAndManagement.dto.response.ProductionPlanResponse;
import group1.com.MangaSystemAndManagement.dto.response.ResponseBase;
import group1.com.MangaSystemAndManagement.model.ProductionPlan;
import group1.com.MangaSystemAndManagement.service.interfaces.ProductionPlanService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Production Plan", description = "Production Plan management APIs")
@RequiredArgsConstructor
public class ProductionPlanController {

    private final ProductionPlanService service;

    @PostMapping("/projects/{projectId}/production-plans")
    public ResponseEntity<ResponseBase> create(@PathVariable Long projectId, @RequestBody ProductionPlanRequest request) {
        try {
            ProductionPlan result = service.createProductionPlan(projectId, request);
            return ResponseEntity.status(201).body(new ResponseBase(201, "Production plan created successfully", result));
        } catch (Exception e) {
            return ResponseEntity.status(409).body(new ResponseBase(409, e.getMessage(), null));
        }
    }

    /**
     * Deprecated: BA V3 removed pre-approval. Plan starts in IN_PROGRESS.
     * Kept for backward-compatibility — still flips status if called.
     */
    @Deprecated
    @PostMapping("/production-plans/{id}/approve")
    public ResponseEntity<ResponseBase> approve(
            @PathVariable Long id,
            @RequestParam Long requesterId) {
        try {
            ProductionPlan result = service.approveProductionPlan(id, requesterId);
            return ResponseEntity.status(200).body(new ResponseBase(200, "Production plan approved successfully", result));
        } catch (org.springframework.security.access.AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(409).body(new ResponseBase(409, e.getMessage(), null));
        }
    }

    /** Pause a Plan (BA V3 §2.2). Allowed: TANTOU_EDITOR, LEADER_BOARD, EDITORIAL_BOARD_MEMBER. */
    @PostMapping("/production-plans/{id}/pause")
    public ResponseEntity<ResponseBase> pause(
            @PathVariable Long id,
            @RequestParam Long requesterId,
            @Valid @RequestBody PausePlanRequest request) {
        try {
            ProductionPlan result = service.pausePlan(id, requesterId, request);
            return ResponseEntity.ok(new ResponseBase(200, "Production plan paused", result));
        } catch (org.springframework.security.access.AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(409).body(new ResponseBase(409, ise.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    /** Resume a paused Plan (BA V3 §2.2). Same roles as pause. */
    @PostMapping("/production-plans/{id}/resume")
    public ResponseEntity<ResponseBase> resume(
            @PathVariable Long id,
            @RequestParam Long requesterId) {
        try {
            ProductionPlan result = service.resumePlan(id, requesterId);
            return ResponseEntity.ok(new ResponseBase(200, "Production plan resumed", result));
        } catch (org.springframework.security.access.AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(409).body(new ResponseBase(409, ise.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    /** Force-close a Plan (BA V3 §2.1). Allowed: LEADER_BOARD, EDITORIAL_BOARD_MEMBER. */
    @PostMapping("/production-plans/{id}/force-close")
    public ResponseEntity<ResponseBase> forceClose(
            @PathVariable Long id,
            @RequestParam Long requesterId,
            @Valid @RequestBody ForceClosePlanRequest request) {
        try {
            ProductionPlan result = service.forceClosePlan(id, requesterId, request);
            return ResponseEntity.ok(new ResponseBase(200, "Production plan force-closed", result));
        } catch (org.springframework.security.access.AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(409).body(new ResponseBase(409, ise.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @GetMapping("/production-plans/{id}")
    public ResponseEntity<ResponseBase> getById(@PathVariable Long id) {
        try {
            ProductionPlan result = service.getProductionPlan(id);
            return ResponseEntity.status(200).body(new ResponseBase(200, "Success", result));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(new ResponseBase(404, e.getMessage(), null));
        }
    }

    @GetMapping("/production-plans")
    public ResponseEntity<ResponseBase> getAll() {
        try {
            List<ProductionPlan> result = service.getAllProductionPlans();
            List<ProductionPlanResponse> response = result.stream()
                    .map(ProductionPlanResponse::from)
                    .toList();
            return ResponseEntity.status(200).body(new ResponseBase(200, "Success", response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }
}
