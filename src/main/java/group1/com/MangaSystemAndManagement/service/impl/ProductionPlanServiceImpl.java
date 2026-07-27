package group1.com.MangaSystemAndManagement.service.impl;

import group1.com.MangaSystemAndManagement.dto.request.ForceClosePlanRequest;
import group1.com.MangaSystemAndManagement.dto.request.PausePlanRequest;
import group1.com.MangaSystemAndManagement.dto.request.ProductionPlanRequest;
import group1.com.MangaSystemAndManagement.exception.ResourceNotFoundException;
import group1.com.MangaSystemAndManagement.model.Account;
import group1.com.MangaSystemAndManagement.model.PlanStatus;
import group1.com.MangaSystemAndManagement.model.ProductionPlan;
import group1.com.MangaSystemAndManagement.model.Project;
import group1.com.MangaSystemAndManagement.model.SystemRoleName;
import group1.com.MangaSystemAndManagement.repository.AccountRepository;
import group1.com.MangaSystemAndManagement.repository.ProductionPlanRepository;
import group1.com.MangaSystemAndManagement.repository.ProjectRepository;
import group1.com.MangaSystemAndManagement.service.interfaces.ProductionPlanService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductionPlanServiceImpl implements ProductionPlanService {

    @PersistenceContext
    private EntityManager em;

    private final ProductionPlanRepository productionPlanRepository;
    private final ProjectRepository projectRepository;
    private final AccountRepository accountRepository;

    @Override
    public ProductionPlan createProductionPlan(Long projectId, ProductionPlanRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // BA V3 §5.1: Active Plan starts directly in IN_PROGRESS; no more pre-approval.
        // Decision Log §AI-10 (2026-07-27): approvalStatus field removed entirely.
        ProductionPlan plan = new ProductionPlan();
        plan.setProject(project);
        plan.setMilestones(request.getMilestones());
        plan.setSchedule(request.getSchedule());
        plan.setChapterTimeline(request.getChapterTimeline());
        plan.setDeadline(request.getDeadline());
        plan.setResources(request.getResources());
        plan.setBudget(request.getBudget());
        plan.setAssistantAllocation(request.getAssistantAllocation());
        plan.setPriority(request.getPriority());
        plan.setRisk(request.getRisk());
        plan.setPlanStatus(PlanStatus.IN_PROGRESS);

        return productionPlanRepository.save(plan);
    }

    @Override
    @Deprecated
    public ProductionPlan approveProductionPlan(Long id, Long requesterId) {
        // Role guard: only LEADER_BOARD or EDITORIAL_BOARD_MEMBER may approve
        Account approver = accountRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + requesterId));
        if (!approver.hasRole(SystemRoleName.LEADER_BOARD)
                && !approver.hasRole(SystemRoleName.EDITORIAL_BOARD_MEMBER)) {
            throw new AccessDeniedException(
                    "Only LEADER_BOARD or EDITORIAL_BOARD_MEMBER can approve a Production Plan");
        }

        ProductionPlan plan = productionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Production Plan not found"));
        plan.setPlanStatus(PlanStatus.IN_PROGRESS);
        return productionPlanRepository.save(plan);
    }

    @Override
    @Transactional
    public ProductionPlan pausePlan(Long planId, Long requesterId, PausePlanRequest request) {
        Account requester = accountRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + requesterId));
        if (!requester.hasRole(SystemRoleName.TANTOU_EDITOR)
                && !requester.hasRole(SystemRoleName.LEADER_BOARD)
                && !requester.hasRole(SystemRoleName.EDITORIAL_BOARD_MEMBER)) {
            throw new AccessDeniedException(
                    "Only TANTOU_EDITOR, LEADER_BOARD or EDITORIAL_BOARD_MEMBER can pause a Plan");
        }

        ProductionPlan plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Production Plan not found"));

        if (plan.getPlanStatus() == PlanStatus.PAUSED) {
            throw new IllegalStateException("Plan is already paused");
        }
        if (plan.getPlanStatus() == PlanStatus.COMPLETED
                || plan.getPlanStatus() == PlanStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Cannot pause a Plan in terminal state: " + plan.getPlanStatus());
        }

        plan.setPlanStatus(PlanStatus.PAUSED);
        plan.setPausedBy(requesterId);
        plan.setPausedAt(Instant.now());
        plan.setPauseReason(request.getReason());
        productionPlanRepository.save(plan);
        em.flush();
        return plan;
    }

    @Override
    @Transactional
    public ProductionPlan resumePlan(Long planId, Long requesterId) {
        Account requester = accountRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + requesterId));
        if (!requester.hasRole(SystemRoleName.TANTOU_EDITOR)
                && !requester.hasRole(SystemRoleName.LEADER_BOARD)
                && !requester.hasRole(SystemRoleName.EDITORIAL_BOARD_MEMBER)) {
            throw new AccessDeniedException(
                    "Only TANTOU_EDITOR, LEADER_BOARD or EDITORIAL_BOARD_MEMBER can resume a Plan");
        }

        ProductionPlan plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Production Plan not found"));

        if (plan.getPlanStatus() != PlanStatus.PAUSED) {
            throw new IllegalStateException("Only PAUSED plans can be resumed (current: "
                    + plan.getPlanStatus() + ")");
        }

        plan.setPlanStatus(PlanStatus.IN_PROGRESS);
        plan.setPausedBy(null);
        plan.setPausedAt(null);
        plan.setPauseReason(null);
        productionPlanRepository.save(plan);
        em.flush();
        // Reload to get the confirmed DB state (avoids stale in-memory entity state)
        return productionPlanRepository.findById(planId).orElse(plan);
    }

    @Override
    @Transactional
    public ProductionPlan forceClosePlan(Long planId, Long requesterId, ForceClosePlanRequest request) {
        Account requester = accountRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + requesterId));
        if (!requester.hasRole(SystemRoleName.LEADER_BOARD)
                && !requester.hasRole(SystemRoleName.EDITORIAL_BOARD_MEMBER)) {
            throw new AccessDeniedException(
                    "Only LEADER_BOARD or EDITORIAL_BOARD_MEMBER can force-close a Plan");
        }

        ProductionPlan plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Production Plan not found"));

        if (plan.getPlanStatus() != PlanStatus.IN_PROGRESS
                && plan.getPlanStatus() != PlanStatus.PAUSED) {
            throw new IllegalStateException(
                    "Force-close is only allowed from IN_PROGRESS or PAUSED (current: "
                            + plan.getPlanStatus() + ")");
        }

        plan.setPlanStatus(PlanStatus.COMPLETED);
        // Reuse pauseReason as the close-reason field for Sprint 1 (Sprint 2 may add a dedicated field).
        plan.setPauseReason(request.getReason());
        return productionPlanRepository.save(plan);
    }

    @Override
    public ProductionPlan getProductionPlan(Long id) {
        return productionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Production Plan not found"));
    }

    @Override
    public List<ProductionPlan> getAllProductionPlans() {
        return productionPlanRepository.findAll();
    }
}
