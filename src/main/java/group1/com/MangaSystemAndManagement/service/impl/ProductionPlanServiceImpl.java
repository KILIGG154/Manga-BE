package group1.com.MangaSystemAndManagement.service.impl;

import group1.com.MangaSystemAndManagement.dto.request.CreateProductionPlanRequest;
import group1.com.MangaSystemAndManagement.dto.request.ExtendProductionPlanRequest;
import group1.com.MangaSystemAndManagement.exception.ResourceNotFoundException;
import group1.com.MangaSystemAndManagement.model.Account;
import group1.com.MangaSystemAndManagement.model.ChapterStatus;
import group1.com.MangaSystemAndManagement.model.ChapterStatus;
import group1.com.MangaSystemAndManagement.model.PlanExtensionLog;
import group1.com.MangaSystemAndManagement.model.PlanStatus;
import group1.com.MangaSystemAndManagement.model.ProductionPlan;
import group1.com.MangaSystemAndManagement.model.Project;
import group1.com.MangaSystemAndManagement.model.SystemRoleName;
import group1.com.MangaSystemAndManagement.repository.AccountRepository;
import group1.com.MangaSystemAndManagement.repository.ChapterRepository;
import group1.com.MangaSystemAndManagement.repository.PlanExtensionLogRepository;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductionPlanServiceImpl implements ProductionPlanService {

    private static final long MIN_DURATION_DAYS = 20L;
    private static final DateTimeFormatter MM_YYYY = DateTimeFormatter.ofPattern("MM/yyyy", Locale.ROOT);
    private static final Set<PlanExtensionLog.ReasonCode> ALLOWED_REASONS =
            EnumSet.allOf(PlanExtensionLog.ReasonCode.class);

    @PersistenceContext
    private EntityManager em;

    private final ProductionPlanRepository productionPlanRepository;
    private final ProjectRepository projectRepository;
    private final AccountRepository accountRepository;
    private final ChapterRepository chapterRepository;
    private final PlanExtensionLogRepository planExtensionLogRepository;

    @Override
    @Transactional
    public ProductionPlan createProductionPlan(Long projectId, Long requesterId, CreateProductionPlanRequest request) {
        Account requester = requireTantou(requesterId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        validateDateOrder(request.getStartDate(), request.getEndDate(), request.getDeadlineDate(), request.getPublishDate());
        validateMinDuration(request.getStartDate(), request.getEndDate());

        String title = request.getTitle().trim();
        if (title.isEmpty()) {
            title = suggestDefaultTitle(project.getTitle(), request.getStartDate());
        }

        if (productionPlanRepository.existsByProjectIdAndTitle(projectId, title)) {
            throw new IllegalArgumentException("Tên Plan đã tồn tại trong Project này");
        }

        ProductionPlan plan = new ProductionPlan();
        plan.setProject(project);
        plan.setTitle(title);
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setDeadlineDate(request.getDeadlineDate());
        plan.setPublishDate(request.getPublishDate());
        plan.setCreatedBy(requester.getId());
        plan.setPlanStatus(deriveInitialStatus(request.getStartDate(), LocalDate.now()));

        return productionPlanRepository.save(plan);
    }

    @Override
    @Transactional
    public ProductionPlan extendProductionPlan(Long planId, Long requesterId, ExtendProductionPlanRequest request) {
        Account requester = requireTantou(requesterId);

        ProductionPlan plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Production Plan not found: " + planId));

        if (plan.getPlanStatus() == PlanStatus.DRAFT) {
            throw new IllegalStateException("Không thể gia hạn Plan ở trạng thái DRAFT");
        }
        if (plan.getPlanStatus() == PlanStatus.COMPLETED) {
            throw new IllegalStateException("Không thể gia hạn Plan đã hoàn thành");
        }

        if (!request.getNewEndDate().isAfter(plan.getEndDate())) {
            throw new IllegalArgumentException("Ngày kết thúc mới phải lớn hơn ngày kết thúc hiện tại");
        }

        PlanExtensionLog.ReasonCode reason;
        try {
            reason = PlanExtensionLog.ReasonCode.valueOf(request.getReasonCode());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Reason code không hợp lệ: " + request.getReasonCode()
                    + ". Chấp nhận: " + ALLOWED_REASONS);
        }

        LocalDate oldEndDate = plan.getEndDate();
        plan.setEndDate(request.getNewEndDate());
        plan.setPlanStatus(PlanStatus.EXTENDED);
        productionPlanRepository.save(plan);

        PlanExtensionLog log = new PlanExtensionLog();
        log.setProductionPlan(plan);
        log.setOldEndDate(oldEndDate);
        log.setNewEndDate(request.getNewEndDate());
        log.setReasonCode(reason.name());
        log.setReasonNote(request.getReasonNote());
        log.setExtendedBy(requester.getId());
        log.setExtendedAt(Instant.now());
        planExtensionLogRepository.save(log);

        em.flush();
        return plan;
    }

    @Override
    @Transactional
    public ProductionPlan completeProductionPlan(Long planId, Long requesterId) {
        requireTantou(requesterId);

        ProductionPlan plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Production Plan not found: " + planId));

        if (plan.getPlanStatus() == PlanStatus.COMPLETED) {
            throw new IllegalStateException("Plan đã ở trạng thái COMPLETED");
        }
        if (plan.getPlanStatus() == PlanStatus.DRAFT) {
            throw new IllegalStateException("Không thể hoàn thành Plan ở trạng thái DRAFT");
        }

        long notDone = chapterRepository.countByProductionPlanIdAndChapterStatusNot(
                planId, ChapterStatus.PUBLISHED);
        if (notDone > 0) {
            throw new IllegalStateException(
                    "Không thể hoàn thành Plan. Vẫn còn " + notDone + " Chapter chưa hoàn thành");
        }

        plan.setPlanStatus(PlanStatus.COMPLETED);
        plan.setActualEndDate(LocalDate.now());
        return productionPlanRepository.save(plan);
    }

    @Override
    @Transactional
    public int promoteDraftPlansToActive(LocalDate today) {
        List<ProductionPlan> drafts = productionPlanRepository.findByPlanStatusAndStartDateLessThanEqual(
                PlanStatus.DRAFT, today);
        drafts.forEach(p -> p.setPlanStatus(PlanStatus.ACTIVE));
        return drafts.size();
    }

    @Override
    @Transactional
    public int markOverduePlans(LocalDate today) {
        List<ProductionPlan> overdue = productionPlanRepository.findByPlanStatusInAndEndDateBefore(
                EnumSet.of(PlanStatus.ACTIVE, PlanStatus.EXTENDED), today);
        overdue.forEach(p -> p.setPlanStatus(PlanStatus.OVERDUE));
        return overdue.size();
    }

    @Override
    public ProductionPlan getProductionPlan(Long id) {
        return productionPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Production Plan not found: " + id));
    }

    @Override
    public List<ProductionPlan> getProductionPlansByProject(Long projectId) {
        return productionPlanRepository.findByProjectIdOrderByStartDateDesc(projectId);
    }

    @Override
    public List<ProductionPlan> getAllProductionPlans() {
        return productionPlanRepository.findAll();
    }

    @Override
    public List<PlanExtensionLog> getExtensionLogs(Long planId) {
        return planExtensionLogRepository.findByProductionPlanIdOrderByExtendedAtDesc(planId);
    }

    private Account requireTantou(Long requesterId) {
        Account a = accountRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + requesterId));
        if (!a.hasRole(SystemRoleName.TANTOU_EDITOR)
                && !a.hasRole(SystemRoleName.LEADER_BOARD)
                && !a.hasRole(SystemRoleName.EDITORIAL_BOARD_MEMBER)) {
            throw new AccessDeniedException("Chỉ TANTOU_EDITOR hoặc LEADER_BOARD mới có quyền này");
        }
        return a;
    }

    private static void validateDateOrder(LocalDate start, LocalDate end, LocalDate deadline, LocalDate publish) {
        if (start == null || end == null || deadline == null || publish == null) {
            throw new IllegalArgumentException("Start, End, Deadline và Publish date đều là bắt buộc");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Thứ tự mốc thời gian không hợp lệ: startDate phải <= endDate");
        }
        if (end.isAfter(deadline)) {
            throw new IllegalArgumentException("Thứ tự mốc thời gian không hợp lệ: endDate phải <= deadlineDate");
        }
        if (deadline.isAfter(publish)) {
            throw new IllegalArgumentException("Thứ tự mốc thời gian không hợp lệ: deadlineDate phải <= publishDate");
        }
    }

    private static void validateMinDuration(LocalDate start, LocalDate end) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end);
        if (days < MIN_DURATION_DAYS) {
            throw new IllegalArgumentException(
                    "Thời lượng sản xuất của một Production Plan tối thiểu phải từ 20 ngày (3 tuần) trở lên");
        }
    }

    private static String suggestDefaultTitle(String projectTitle, LocalDate startDate) {
        String safeProject = projectTitle == null ? "Project" : projectTitle;
        return safeProject + " - Production Plan " + MM_YYYY.format(startDate);
    }

    private static PlanStatus deriveInitialStatus(LocalDate startDate, LocalDate today) {
        return !startDate.isAfter(today) ? PlanStatus.ACTIVE : PlanStatus.DRAFT;
    }
}