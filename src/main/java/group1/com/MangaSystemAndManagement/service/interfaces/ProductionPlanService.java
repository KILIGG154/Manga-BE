package group1.com.MangaSystemAndManagement.service.interfaces;

import group1.com.MangaSystemAndManagement.dto.request.CreateProductionPlanRequest;
import group1.com.MangaSystemAndManagement.dto.request.ExtendProductionPlanRequest;
import group1.com.MangaSystemAndManagement.model.PlanExtensionLog;
import group1.com.MangaSystemAndManagement.model.ProductionPlan;

import java.time.LocalDate;
import java.util.List;

public interface ProductionPlanService {

    /**
     * Technical Spec v2.1 §4.1 — create a Production Plan under a Project.
     * Validates BR-01 (dates & min 20 days), BR-02 (initial status), BR-03 (title unique).
     */
    ProductionPlan createProductionPlan(Long projectId, Long requesterId, CreateProductionPlanRequest request);

    /** Spec v2.1 §4.2 — extend a Plan's end date with audit log. */
    ProductionPlan extendProductionPlan(Long planId, Long requesterId, ExtendProductionPlanRequest request);

    /** Spec v2.1 §4.3 — complete a Plan once 100% of its chapters are DONE. */
    ProductionPlan completeProductionPlan(Long planId, Long requesterId);

    /** Spec v2.1 §5 — scheduled jobs. */
    int promoteDraftPlansToActive(LocalDate today);

    int markOverduePlans(LocalDate today);

    ProductionPlan getProductionPlan(Long planId);

    List<ProductionPlan> getProductionPlansByProject(Long projectId);

    List<ProductionPlan> getAllProductionPlans();

    List<PlanExtensionLog> getExtensionLogs(Long planId);
}