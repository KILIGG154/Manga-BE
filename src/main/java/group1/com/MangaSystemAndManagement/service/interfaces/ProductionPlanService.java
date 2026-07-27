package group1.com.MangaSystemAndManagement.service.interfaces;

import group1.com.MangaSystemAndManagement.dto.request.ForceClosePlanRequest;
import group1.com.MangaSystemAndManagement.dto.request.PausePlanRequest;
import group1.com.MangaSystemAndManagement.dto.request.ProductionPlanRequest;
import group1.com.MangaSystemAndManagement.model.ProductionPlan;

import java.util.List;

public interface ProductionPlanService {
    ProductionPlan createProductionPlan(Long projectId, ProductionPlanRequest request);

    /** Approves a Production Plan. Deprecated in BA V3 — Plan is IN_PROGRESS on creation. */
    @Deprecated
    ProductionPlan approveProductionPlan(Long id, Long requesterId);

    /** Pause a Plan. Allowed: TANTOU_EDITOR, LEADER_BOARD, EDITORIAL_BOARD_MEMBER. */
    ProductionPlan pausePlan(Long planId, Long requesterId, PausePlanRequest request);

    /** Resume a paused Plan. Allowed: same roles as pause. */
    ProductionPlan resumePlan(Long planId, Long requesterId);

    /** Force-close a Plan (IN_PROGRESS or PAUSED → COMPLETED). Allowed: LEADER_BOARD, EDITORIAL_BOARD_MEMBER. */
    ProductionPlan forceClosePlan(Long planId, Long requesterId, ForceClosePlanRequest request);

    ProductionPlan getProductionPlan(Long id);
    List<ProductionPlan> getAllProductionPlans();
}
