package group1.com.MangaSystemAndManagement.controller;

import group1.com.MangaSystemAndManagement.dto.request.CreateProductionPlanRequest;
import group1.com.MangaSystemAndManagement.dto.request.ExtendProductionPlanRequest;
import group1.com.MangaSystemAndManagement.dto.response.PlanExtensionLogResponse;
import group1.com.MangaSystemAndManagement.dto.response.ProductionPlanResponse;
import group1.com.MangaSystemAndManagement.dto.response.ResponseBase;
import group1.com.MangaSystemAndManagement.model.ProductionPlan;
import group1.com.MangaSystemAndManagement.service.interfaces.ProductionPlanService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Production Plan Management — REST endpoints aligned with Technical Spec v2.1.
 *
 * <p>Routes follow REST resource hierarchy:
 * <pre>
 *   POST   /api/v1/projects/{projectId}/production-plans
 *   GET    /api/v1/projects/{projectId}/production-plans
 *   GET    /api/v1/production-plans/{planId}
 *   POST   /api/v1/production-plans/{planId}/extend
 *   POST   /api/v1/production-plans/{planId}/complete
 *   GET    /api/v1/production-plans/{planId}/extension-logs
 * </pre>
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Production Plan", description = "Production Plan management APIs")
@RequiredArgsConstructor
public class ProductionPlanController {

    private final ProductionPlanService service;

    /** Spec v2.1 §4.1 — create a Plan under a Project. */
    @PostMapping("/projects/{projectId}/production-plans")
    public ResponseEntity<ResponseBase> create(
            @PathVariable Long projectId,
            @RequestParam Long requesterId,
            @Valid @RequestBody CreateProductionPlanRequest request) {
        try {
            ProductionPlan result = service.createProductionPlan(projectId, requesterId, request);
            ProductionPlanResponse body = ProductionPlanResponse.from(result);
            return ResponseEntity.status(201)
                    .body(new ResponseBase(201, "Production plan created successfully", body));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (IllegalArgumentException ae) {
            return ResponseEntity.status(400).body(new ResponseBase(400, ae.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @GetMapping("/projects/{projectId}/production-plans")
    public ResponseEntity<ResponseBase> listByProject(@PathVariable Long projectId) {
        try {
            List<ProductionPlanResponse> body = service.getProductionPlansByProject(projectId).stream()
                    .map(ProductionPlanResponse::from)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(new ResponseBase(200, "Success", body));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @GetMapping("/production-plans/{planId}")
    public ResponseEntity<ResponseBase> getById(@PathVariable Long planId) {
        try {
            ProductionPlanResponse body = ProductionPlanResponse.from(service.getProductionPlan(planId));
            return ResponseEntity.ok(new ResponseBase(200, "Success", body));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(new ResponseBase(404, e.getMessage(), null));
        }
    }

    /** Spec v2.1 §4.2 — extend a Plan's end date with audit log. */
    @PostMapping("/production-plans/{planId}/extend")
    public ResponseEntity<ResponseBase> extend(
            @PathVariable Long planId,
            @RequestParam Long requesterId,
            @Valid @RequestBody ExtendProductionPlanRequest request) {
        try {
            ProductionPlan result = service.extendProductionPlan(planId, requesterId, request);
            return ResponseEntity.ok(new ResponseBase(200, "Production plan extended",
                    ProductionPlanResponse.from(result)));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (IllegalArgumentException ae) {
            return ResponseEntity.status(400).body(new ResponseBase(400, ae.getMessage(), null));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(409).body(new ResponseBase(409, ise.getMessage(), null));
        }
    }

    /** Spec v2.1 §4.3 — complete a Plan once 100% of its chapters are PUBLISHED. */
    @PutMapping("/production-plans/{planId}/complete")
    public ResponseEntity<ResponseBase> complete(
            @PathVariable Long planId,
            @RequestParam Long requesterId) {
        try {
            ProductionPlan result = service.completeProductionPlan(planId, requesterId);
            return ResponseEntity.ok(new ResponseBase(200, "Production plan completed",
                    ProductionPlanResponse.from(result)));
        } catch (AccessDeniedException ad) {
            return ResponseEntity.status(403).body(new ResponseBase(403, ad.getMessage(), null));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(409).body(new ResponseBase(409, ise.getMessage(), null));
        }
    }

    /** Spec v2.1 §6.1 — audit trail for a Plan's extensions. */
    @GetMapping("/production-plans/{planId}/extension-logs")
    public ResponseEntity<ResponseBase> getExtensionLogs(@PathVariable Long planId) {
        try {
            List<PlanExtensionLogResponse> body = service.getExtensionLogs(planId).stream()
                    .map(PlanExtensionLogResponse::from)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(new ResponseBase(200, "Success", body));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @GetMapping("/production-plans")
    public ResponseEntity<ResponseBase> getAll() {
        try {
            List<ProductionPlanResponse> body = service.getAllProductionPlansWithChapters().stream()
                    .map(ProductionPlanResponse::from)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(new ResponseBase(200, "Success", body));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }
}