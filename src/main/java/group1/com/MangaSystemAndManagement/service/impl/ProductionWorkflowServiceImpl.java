package group1.com.MangaSystemAndManagement.service.impl;

import group1.com.MangaSystemAndManagement.dto.request.*;
import group1.com.MangaSystemAndManagement.dto.response.*;
import group1.com.MangaSystemAndManagement.exception.WorkflowRuleViolationException;
import group1.com.MangaSystemAndManagement.model.*;
import group1.com.MangaSystemAndManagement.repository.*;
import group1.com.MangaSystemAndManagement.service.interfaces.ProductionWorkflowService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionWorkflowServiceImpl implements ProductionWorkflowService {

    @PersistenceContext
    private EntityManager em;

    private final ProjectRepository projectRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final ChapterRepository chapterRepository;
    private final TaskRepository taskRepository;
    private final SubTaskRepository subTaskRepository;
    private final FeedbackRepository feedbackRepository;
    private final AssetRepository assetRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest req, Long editorId) {
        Account creator = getAccount(editorId);
        if (!creator.hasRole(SystemRoleName.EDITORIAL_BOARD_MEMBER) && !creator.hasRole(SystemRoleName.LEADER_BOARD)) {
            throw new AccessDeniedException("Only EDITORIAL_BOARD_MEMBER or LEADER_BOARD can create projects");
        }

        Account tantou = getAccount(req.getTantouId());
        if (!tantou.hasRole(SystemRoleName.TANTOU_EDITOR)) {
            throw new IllegalArgumentException("Assigned account must have the TANTOU_EDITOR role");
        }

        Project project = new Project();
        project.setTitle(req.getTitle());
        project.setGenre(req.getGenre());
        project.setTargetAudience(req.getTargetAudience());
        project.setFormat(req.getFormat());
        project.setProjectWorkflowStatus(ProjectWorkflowStatus.ACTIVE); // Auto active
        project.setTantou(tantou); // Tantou is assigned as the tantou of the project

        project = projectRepository.save(project);

        return mapToProjectResponse(project);
    }

    @Override
    @Transactional
    public ProjectResponse updateProjectByBoard(Long projectId, UpdateProjectBoardRequest req, Long editorId) {
        Account editor = getAccount(editorId);
        if (!editor.hasRole(SystemRoleName.EDITORIAL_BOARD_MEMBER) && !editor.hasRole(SystemRoleName.LEADER_BOARD)) {
            throw new AccessDeniedException("Only EDITORIAL_BOARD_MEMBER or LEADER_BOARD can update project status and Tantou");
        }

        Project project = getProject(projectId);
        if (req.getProjectWorkflowStatus() != null) {
            project.setProjectWorkflowStatus(req.getProjectWorkflowStatus());
        }
        if (req.getTantouId() != null) {
            Account tantou = getAccount(req.getTantouId());
            if (!tantou.hasRole(SystemRoleName.TANTOU_EDITOR)) {
                throw new IllegalArgumentException("Assigned account must have the TANTOU_EDITOR role");
            }
            project.setTantou(tantou);
        }

        project = projectRepository.save(project);
        return mapToProjectResponse(project);
    }

    @Override
    @Transactional
    public ProjectResponse updateProjectByTantou(Long projectId, UpdateProjectTantouRequest req, Long tantouId) {
        Account tantou = getAccount(tantouId);
        if (!tantou.hasRole(SystemRoleName.TANTOU_EDITOR)) {
            throw new AccessDeniedException("Only TANTOU can update project details");
        }

        Project project = getProject(projectId);
        
        // Optionally verify if this Tantou is the owner of this project
//        if (project.getOwner() == null || project.getOwner().getId() != tantouId) {
//            throw new AccessDeniedException("You are not the assigned Tantou for this project");
//        }

        if (req.getGenre() != null) {
            project.setGenre(req.getGenre());
        }
        if (req.getTargetAudience() != null) {
            project.setTargetAudience(req.getTargetAudience());
        }
        if (req.getFormat() != null) {
            project.setFormat(req.getFormat());
        }

        project = projectRepository.save(project);
        return mapToProjectResponse(project);
    }

    @Override
    @Transactional
    public ProjectResponse activateProject(Long projectId, Long requesterId) {
        Account requester = getAccount(requesterId);
        if (!requester.hasRole(SystemRoleName.TANTOU_EDITOR)) {
            throw new AccessDeniedException("Only TANTOU can activate projects");
        }

        Project project = getProject(projectId);

        // Auto-Plan Initialization rule
        if (project.getProjectWorkflowStatus() != ProjectWorkflowStatus.ACTIVE) {
            project.setProjectWorkflowStatus(ProjectWorkflowStatus.ACTIVE);
            project = projectRepository.save(project);

            // Create initial ProductionPlan if none exists
            List<ProductionPlan> existingPlans = productionPlanRepository.findByProjectIdOrderByStartDateDesc(project.getId());
            if (existingPlans.isEmpty()) {
                ProductionPlan plan = new ProductionPlan();
                plan.setProject(project);
                plan.setTitle(project.getTitle() + " - Initial Plan");
                plan.setPlanStatus(PlanStatus.ACTIVE);
                productionPlanRepository.save(plan);
            }
        }

        return mapToProjectResponse(project);
    }

    @Override
    public PlanDashboardResponse getPlanDashboard(Long planId, Long requesterId) {
        Account requester = getAccount(requesterId);
        if (!requester.hasRole(SystemRoleName.TANTOU_EDITOR) && !requester.hasRole(SystemRoleName.MANGAKA)) {
            throw new AccessDeniedException("Only TANTOU or MANGAKA can view plan dashboard");
        }

        ProductionPlan plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        PlanDashboardResponse response = new PlanDashboardResponse();
        response.setId(plan.getId());
        response.setProjectId(plan.getProject().getId());
        response.setStartDate(plan.getStartDate());
        response.setEndDate(plan.getEndDate());
        response.setTotalVolumeTarget(plan.getTotalVolumeTarget());
        response.setPlanStatus(plan.getPlanStatus());

        List<Chapter> chapters = chapterRepository.findByProductionPlanId(plan.getId());

        long totalTasks = 0;
        long completedTasks = 0;

        List<ChapterWithTasksResponse> chapterResponses = chapters.stream().map(c -> {
            ChapterWithTasksResponse cr = new ChapterWithTasksResponse();
            cr.setId(c.getId());
            cr.setChapterNumber(c.getChapterNumber());
            cr.setTitle(c.getTitle());
            cr.setTargetPageCount(c.getTargetPageCount());
            cr.setPublishDate(c.getPublishDate());
            cr.setChapterStatus(c.getChapterStatus());

            List<Task> tasks = taskRepository.findByChapterId(c.getId());
            List<TaskWithSubTasksResponse> taskResponses = tasks.stream()
                    .map(this::mapToTaskWithSubTasksResponse)
                    .collect(Collectors.toList());
            cr.setTasks(taskResponses);

            return cr;
        }).collect(Collectors.toList());

        response.setChapters(chapterResponses);

        // Calculate progress based on chapters completion
        long completedChapters = chapters.stream().filter(
                c -> c.getChapterStatus() == ChapterStatus.COMPLETED || c.getChapterStatus() == ChapterStatus.PUBLISHED)
                .count();
        double progress = chapters.isEmpty() ? 0.0 : ((double) completedChapters / chapters.size()) * 100;
        response.setCompletionPercentage(progress);

        return response;
    }

    @Override
    @Transactional
    public ChapterResponse createChapter(CreateChapterRequest req, Long requesterId) {
        Account requester = getAccount(requesterId);
        if (!requester.hasRole(SystemRoleName.TANTOU_EDITOR)) {
            throw new AccessDeniedException("Only TANTOU can create chapters");
        }

        ProductionPlan plan = productionPlanRepository.findById(req.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plan not found"));
        em.refresh(plan);

        assertPlanNotPaused(plan);

        if (req.getStartDate() != null && req.getEndDate() != null
                && req.getStartDate().isAfter(req.getEndDate())) {
            throw new IllegalArgumentException("Chapter start date cannot be after end date");
        }
        if (plan.getStartDate() != null && req.getStartDate().isBefore(plan.getStartDate())) {
            throw new IllegalArgumentException("Chapter start date cannot be before Production Plan start date (" + plan.getStartDate() + ")");
        }

        if (plan.getEndDate() != null && req.getEndDate().isAfter(plan.getEndDate())) {
            throw new IllegalArgumentException("Chapter end date cannot be after Production Plan end date (" + plan.getEndDate() + ")");
        }

        Chapter chapter = new Chapter();
        chapter.setProductionPlan(plan);
        chapter.setProject(plan.getProject());
        chapter.setChapterNumber(req.getChapterNumber());
        chapter.setTitle(req.getTitle());
        chapter.setTargetPageCount(req.getTargetPageCount());
        chapter.setPublishDate(req.getPublishDate());
        chapter.setStartDate(req.getStartDate());
        chapter.setEndDate(req.getEndDate());
        chapter.setChapterStatus(req.getChapterStatus());
        chapter.setDeadline(req.getDeadline());
        chapter.setPriority(req.getPriority());
        chapter.setOwner(requester);

// Guard: DRAFT/COMPLETED plans are already covered by assertPlanNotPaused above.
// Technical Spec v2.1 §3.1: ACTIVE/EXTENDED/OVERDUE plans host chapter production.

        chapter = chapterRepository.save(chapter);

        ChapterResponse response = new ChapterResponse();
        response.setId(chapter.getId());
        response.setChapterNumber(chapter.getChapterNumber());
        response.setTitle(chapter.getTitle());
        response.setTargetPageCount(chapter.getTargetPageCount());
        response.setPublishDate(chapter.getPublishDate());
        response.setStartDate(chapter.getStartDate());
        response.setEndDate(chapter.getEndDate());
        response.setChapterStatus(chapter.getChapterStatus());
        if (chapter.getOwner() != null) {
            response.setOwnerId(chapter.getOwner().getId());
            response.setOwnerName(chapter.getOwner().getFirstName() + " " + chapter.getOwner().getLastName());
        }
        if (chapter.getAssignee() != null) {
            response.setAssigneeId(chapter.getAssignee().getId());
            response.setAssigneeName(chapter.getAssignee().getFirstName() + " " + chapter.getAssignee().getLastName());
        }
        response.setProjectId(chapter.getProject().getId());
        if (chapter.getProductionPlan() != null) {
            response.setPlanId(chapter.getProductionPlan().getId());
        }

        return response;
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, UpdateTaskStatusRequest req) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        Account requester = getAccount(req.getRequesterId());

        assertPlanNotPaused(task.getChapter() != null ? task.getChapter().getProductionPlan() : null);

        boolean isTantou = requester.hasRole(SystemRoleName.TANTOU_EDITOR);
        boolean isMangaka = requester.hasRole(SystemRoleName.MANGAKA);
        boolean isAssistant = requester.hasRole(SystemRoleName.ASSISTANT);

        // Permissions check
        if (!isTantou && !isMangaka && !isAssistant) {
            throw new AccessDeniedException("You don't have permission to update task status");
        }

        if (isAssistant && (task.getAssignee() == null || task.getAssignee().getId() != requester.getId())) {
            throw new AccessDeniedException("Assistants can only update tasks assigned to them");
        }

        // Strict Quality Control (Feedback Loop) Rule
        if (task.getTaskWorkflowStatus() == TaskWorkflowStatus.REVIEW && !isTantou) {
            // Check if there is pending feedback or if it's already in review and locked
            throw new IllegalStateException("Task is locked in REVIEW. Waiting for feedback.");
        }

        task.setTaskWorkflowStatus(req.getStatus());
        task = taskRepository.save(task);

        // Chapter Roll-up Validation Check - if a task is updated, chapter status might
        // need check
        // If a task is no longer DONE, we can't have chapter COMPLETED.
        if (req.getStatus() != TaskWorkflowStatus.DONE && task.getChapter() != null
                && task.getChapter().getChapterStatus() == ChapterStatus.COMPLETED) {
            Chapter chapter = task.getChapter();
            chapter.setChapterStatus(ChapterStatus.IN_PRODUCTION);
            chapterRepository.save(chapter);
        }

        // Also if task is updated TO DONE, maybe chapter should be completed? That's
        // typically manual, but let's enforce guard on Chapter completion manually.

        return mapToTaskResponse(task);
    }

    @Override
    @Transactional
    public FeedbackResponse createFeedback(Long taskId, CreateFeedbackRequest req) {
        Account creator = getAccount(req.getCreatedById());
        if (!creator.hasRole(SystemRoleName.TANTOU_EDITOR)) {
            throw new AccessDeniedException("Only TANTOU can create feedback");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        assertPlanNotPaused(task.getChapter() != null ? task.getChapter().getProductionPlan() : null);

        if (task.getTaskWorkflowStatus() != TaskWorkflowStatus.REVIEW) {
            throw new IllegalStateException("Feedback can only be provided for tasks in REVIEW status");
        }

        Feedback feedback = new Feedback();
        feedback.setTask(task);
        feedback.setCreatedBy(creator);
        feedback.setContent(req.getContent());
        feedback.setAttachmentUrl(req.getAttachmentUrl());
        feedback.setDecision(req.getDecision());
        feedback = feedbackRepository.save(feedback);

        // Feedback Loop rule:
        if (req.getDecision() == FeedbackDecision.REJECTED) {
            task.setTaskWorkflowStatus(TaskWorkflowStatus.IN_PROGRESS);
        } else if (req.getDecision() == FeedbackDecision.APPROVED) {
            task.setTaskWorkflowStatus(TaskWorkflowStatus.DONE);
        }
        taskRepository.save(task);

        FeedbackResponse response = new FeedbackResponse();
        BeanUtils.copyProperties(feedback, response);
        response.setTaskId(task.getId());
        response.setCreatedById(creator.getId());
        response.setCreatedByName(creator.getFirstName() + " " + creator.getLastName());
        return response;
    }

    @Override
    @Transactional
    public ChapterResponse assignChapter(Long chapterId, AssignChapterRequest req) {
        Account requester = getAccount(req.getRequesterId());
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));
        Account mangaka = getAccount(req.getMangakaId());

        if (!requester.hasRole(SystemRoleName.TANTOU_EDITOR)) {
            throw new AccessDeniedException("Only Tantou Editor can assign chapters");
        }

        assertPlanNotPaused(chapter.getProductionPlan());

        if (!mangaka.hasRole(SystemRoleName.MANGAKA)) {
            throw new IllegalArgumentException("The assignee must be a Mangaka");
        }

        // Spec v2.1: Tantou creates chapter (owner); Tantou then assigns to a Mangaka (assignee).
        // owner stays untouched on assign; only assignee is updated.
        chapter.setAssignee(mangaka);
        chapterRepository.save(chapter);

        ChapterResponse response = new ChapterResponse();
        org.springframework.beans.BeanUtils.copyProperties(chapter, response);
        response.setProjectId(chapter.getProject().getId());
        response.setAssigneeId(mangaka.getId());
        response.setAssigneeName(mangaka.getFirstName() + " " + mangaka.getLastName());
        if (chapter.getOwner() != null) {
            response.setOwnerId(chapter.getOwner().getId());
            response.setOwnerName(chapter.getOwner().getFirstName() + " " + chapter.getOwner().getLastName());
        }
        return response;
    }

    @Override
    @Transactional
    public TaskResponse createManualTask(Long chapterId, CreateManualTaskRequest req) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));
        Account requester = getAccount(req.getRequesterId());

        if (!requester.hasRole(SystemRoleName.MANGAKA)) {
            throw new AccessDeniedException("Only Mangaka can manually create Tasks");
        }
        if (chapter.getAssignee() == null) {
            throw new AccessDeniedException("Chapter has no Mangaka assigned yet");
        }
        Long assigneeId = chapter.getAssignee().getId();
        if (assigneeId == null || !assigneeId.equals(requester.getId())) {
            throw new AccessDeniedException(
                    "Only the Mangaka assigned to this chapter can create Tasks under it");
        }

        assertPlanNotPaused(chapter.getProductionPlan());

        if (chapter.getChapterStatus() != ChapterStatus.IN_PRODUCTION) {
            throw new WorkflowRuleViolationException(
                    "Chapter must be IN_PRODUCTION to accept new Tasks (current: "
                            + chapter.getChapterStatus() + ")");
        }

        if (req.getDeadlineDate() != null && chapter.getEndDate() != null
                && req.getDeadlineDate().isAfter(chapter.getEndDate())) {
            throw new IllegalArgumentException(
                    "Task deadline (" + req.getDeadlineDate()
                            + ") cannot be after Chapter end date (" + chapter.getEndDate() + ")");
        }

        Task task = new Task();
        task.setChapter(chapter);
        task.setTitle(req.getTitle());
        task.setDescription(req.getDescription());
        task.setAcceptanceCriteria(req.getAcceptanceCriteria());
        task.setProductionTaskType(req.getProductionTaskType());
        task.setTaskWorkflowStatus(TaskWorkflowStatus.TODO);
        task.setAssignee(requester);
        task.setDeadlineDate(req.getDeadlineDate());
        task.setDeadlineTime(req.getDeadlineTime() != null ? req.getDeadlineTime() : java.time.LocalTime.of(23, 59));
        task.setProgressPercentage(0);

        task = taskRepository.save(task);

        TaskResponse response = new TaskResponse();
        org.springframework.beans.BeanUtils.copyProperties(task, response);
        response.setChapterId(chapter.getId());
        if (task.getAssignee() != null) {
            response.setAssigneeId(task.getAssignee().getId());
            response.setAssigneeName(task.getAssignee().getFirstName() + " " + task.getAssignee().getLastName());
        }
        return response;
    }

    @Override
    @Transactional
    public TaskResponse assignTask(Long taskId, AssignTaskRequest req) {
        Account requester = getAccount(req.getRequesterId());
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        Account assignee = getAccount(req.getAssigneeId());

        assertPlanNotPaused(task.getChapter() != null ? task.getChapter().getProductionPlan() : null);

        boolean isTantou = requester.hasRole(SystemRoleName.TANTOU_EDITOR);
        boolean isMangaka = requester.hasRole(SystemRoleName.MANGAKA);

        if (!isTantou && !isMangaka) {
            throw new AccessDeniedException("You don't have permission to assign tasks");
        }

        if (isMangaka) {
            if (task.getProductionTaskType() != TaskType.INKING
                    && task.getProductionTaskType() != TaskType.BACKGROUND) {
                throw new AccessDeniedException("Mangaka can only assign INKING or BACKGROUND tasks");
            }
            if (!assignee.hasRole(SystemRoleName.ASSISTANT)) {
                throw new AccessDeniedException("Mangaka can only assign tasks to Assistants");
            }
        }

        if (isTantou && !assignee.hasRole(SystemRoleName.MANGAKA)) {
            // Note: Spec says Tantou can assign to Mangaka. Could be flexible, but sticking
            // to spec.
            // Actually, Tantou can assign to MANGAKA, Mangaka to ASSISTANT.
            if (!assignee.hasRole(SystemRoleName.MANGAKA) && !assignee.hasRole(SystemRoleName.ASSISTANT)) {
                throw new AccessDeniedException("Tantou can only assign to Mangaka or Assistant");
            }
        }

        if (req.getDeadline() != null) {
            java.time.Instant deadline = req.getDeadline();
            Chapter chapter = task.getChapter();
            
            if (chapter.getStartDate() != null && deadline.isBefore(chapter.getStartDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())) {
                throw new IllegalArgumentException("Task deadline cannot be before Chapter start date (" + chapter.getStartDate() + ")");
            }

            if (chapter.getEndDate() != null && deadline.isAfter(chapter.getEndDate().atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())) {
                throw new IllegalArgumentException("Task deadline cannot be after Chapter end date (" + chapter.getEndDate() + ")");
            }

            task.setDeadlineDate(java.time.LocalDate.ofInstant(deadline, java.time.ZoneId.systemDefault()));
            task.setDeadlineTime(java.time.LocalTime.ofInstant(deadline, java.time.ZoneId.systemDefault()));
        }

        task.setAssignee(assignee);
        task = taskRepository.save(task);
        return mapToTaskResponse(task);
    }

    @Override
    public List<AssetResponse> getProjectAssets(Long projectId, Long requesterId) {
        // RBAC: Any member of the project. For simplicity, just checking if valid
        // account,
        // real implementation would check Project.getTantou() or Project.getMangaka()
        Account requester = getAccount(requesterId);

        List<Asset> assets = assetRepository.findByProjectId(projectId);
        return assets.stream().map(a -> {
            AssetResponse r = new AssetResponse();
            BeanUtils.copyProperties(a, r);
            r.setProjectId(projectId);
            return r;
        }).collect(Collectors.toList());
    }

    // Helper methods

    private Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    private Project getProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    /**
     * Technical Spec v2.1 §3.1: ACTIVE/EXTENDED/OVERDUE plans allow chapter/task
     * mutations. DRAFT (future plans) and COMPLETED (closed plans) block writes.
     * OVERDUE is allowed so Tantou can still Extend/Complete it before re-closing.
     */
    private void assertPlanNotPaused(ProductionPlan plan) {
        if (plan != null) {
            if (plan.getId() != null) {
                plan = productionPlanRepository.findById(plan.getId()).orElse(plan);
            }
            PlanStatus status = plan.getPlanStatus();
            if (status == PlanStatus.DRAFT) {
                throw new IllegalStateException(
                        "Production Plan " + plan.getId() + " is DRAFT. Plan chưa đến ngày bắt đầu.");
            }
            if (status == PlanStatus.COMPLETED) {
                throw new IllegalStateException(
                        "Production Plan " + plan.getId() + " is COMPLETED. Plan đã đóng.");
            }
            if (plan.getProject() != null
                    && plan.getProject().getProjectWorkflowStatus() == ProjectWorkflowStatus.CANCELLED) {
                throw new IllegalStateException(
                        "Project " + plan.getProject().getId() + " is CANCELLED. Cannot mutate chapters/tasks.");
            }
        }
    }

    private ProjectResponse mapToProjectResponse(Project p) {
        ProjectResponse r = new ProjectResponse();
        BeanUtils.copyProperties(p, r);
        return r;
    }

    private TaskResponse mapToTaskResponse(Task t) {
        TaskResponse r = new TaskResponse();
        BeanUtils.copyProperties(t, r);
        if (t.getAssignee() != null) {
            r.setAssigneeId(t.getAssignee().getId());
            r.setAssigneeName(t.getAssignee().getFirstName() + " " + t.getAssignee().getLastName());
        }
        return r;
    }

    /**
     * Map a Task into the richer response that includes its SubTasks and the
     * files already submitted against each SubTask. Used by the dashboard and
     * chapter endpoints that need the full task tree.
     */
    private TaskWithSubTasksResponse mapToTaskWithSubTasksResponse(Task t) {
        TaskWithSubTasksResponse r = new TaskWithSubTasksResponse();
        BeanUtils.copyProperties(t, r);
        if (t.getAssignee() != null) {
            r.setAssigneeId(t.getAssignee().getId());
            r.setAssigneeName(t.getAssignee().getFirstName() + " " + t.getAssignee().getLastName());
        }

        List<SubTask> subTasks = subTaskRepository.findByTaskId(t.getId());
        List<SubTaskResponse> subTaskResponses = subTasks.stream()
                .map(this::mapToSubTaskResponse)
                .collect(Collectors.toList());
        r.setSubTasks(subTaskResponses);
        return r;
    }

    private SubTaskResponse mapToSubTaskResponse(SubTask st) {
        SubTaskResponse r = new SubTaskResponse();
        r.setId(st.getId());
        r.setTaskId(st.getTask() != null ? st.getTask().getId() : null);
        if (st.getAssignee() != null) {
            r.setAssigneeId(st.getAssignee().getId());
            r.setAssigneeName(st.getAssignee().getFirstName() + " " + st.getAssignee().getLastName());
        }
        r.setTitle(st.getTitle());
        r.setDescription(st.getDescription());
        r.setSubtaskStatus(st.getSubtaskStatus());
        r.setDeadlineDate(st.getDeadlineDate());
        r.setDeadlineTime(st.getDeadlineTime());
        r.setCreatedAt(st.getCreatedAt());
        r.setUpdatedAt(st.getUpdatedAt());

        // Files already submitted against this SubTask – flatten every submission
        // round into a single list of files (newest submissions first).
        List<SubmissionFileResponse> files = subTaskRepository.findById(st.getId())
                .map(SubTask::getSubmissions)
                .orElse(List.of())
                .stream()
                .sorted((a, b) -> {
                    Instant ia = a.getSubmittedAt() == null ? Instant.EPOCH : a.getSubmittedAt();
                    Instant ib = b.getSubmittedAt() == null ? Instant.EPOCH : b.getSubmittedAt();
                    return ib.compareTo(ia);
                })
                .flatMap(sub -> sub.getFiles() == null ? java.util.stream.Stream.empty()
                        : sub.getFiles().stream()
                                .sorted((f1, f2) -> {
                                    Integer o1 = f1.getFileOrder() == null ? Integer.MAX_VALUE : f1.getFileOrder();
                                    Integer o2 = f2.getFileOrder() == null ? Integer.MAX_VALUE : f2.getFileOrder();
                                    return Integer.compare(o1, o2);
                                }))
                .map(this::mapToSubmissionFileResponse)
                .collect(Collectors.toList());
        r.setSubmittedFiles(files);
        return r;
    }

    private SubmissionFileResponse mapToSubmissionFileResponse(SubmissionFile sf) {
        SubmissionFileResponse r = new SubmissionFileResponse();
        r.setId(sf.getId());
        r.setOriginalName(sf.getOriginalName());
        r.setFilePath(sf.getFilePath());
        r.setFileSize(sf.getFileSize());
        r.setContentType(sf.getContentType());
        r.setFileType(sf.getFileType());
        r.setFileOrder(sf.getFileOrder());
        return r;
    }

    // Explicitly add chapter completion endpoint logic here? The spec says "Một
    // Chapter KHÔNG ĐƯỢC phép chuyển trạng thái thành COMPLETED nếu vẫn còn ít nhất
    // một Task con của nó có trạng thái khác DONE."
    // Let's add a method to update chapter status

    @Transactional
    public ChapterWithTasksResponse updateChapterStatus(Long chapterId, ChapterStatus status, Long requesterId) {
        Account requester = getAccount(requesterId);
        if (!requester.hasRole(SystemRoleName.TANTOU_EDITOR)) {
            throw new AccessDeniedException("Only TANTOU can update chapter status");
        }

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));

        assertPlanNotPaused(chapter.getProductionPlan());

        if (status == ChapterStatus.COMPLETED) {
            boolean hasIncompleteTasks = taskRepository.existsByChapterIdAndTaskWorkflowStatusNot(chapterId,
                    TaskWorkflowStatus.DONE);
            if (hasIncompleteTasks) {
                throw new IllegalStateException("Cannot complete chapter: not all tasks are DONE.");
            }

            // Decision Log 2026-07-27 §AI-09: reset rejectionCount to 0 when chapter
            // is re-completed (IN_PRODUCTION → COMPLETED) so the chapter gets a fresh
            // "budget" of 2 return attempts. Done only on re-completion, not the first
            // time (which would also be 0).
            if (chapter.getRejectionCount() != null && chapter.getRejectionCount() > 0) {
                chapter.setRejectionCount(0);
            }
        }

        chapter.setChapterStatus(status);
        chapter = chapterRepository.save(chapter);

        // Auto-complete the ProductionPlan when all its chapters are PUBLISHED
        if (status == ChapterStatus.PUBLISHED && chapter.getProductionPlan() != null) {
            ProductionPlan plan = chapter.getProductionPlan();
            boolean allPublished = !chapterRepository
                    .existsByProductionPlanIdAndChapterStatusNot(plan.getId(), ChapterStatus.PUBLISHED);
            if (allPublished && plan.getPlanStatus() != PlanStatus.COMPLETED) {
                plan.setPlanStatus(PlanStatus.COMPLETED);
                productionPlanRepository.save(plan);
            }
        }

        ChapterWithTasksResponse cr = new ChapterWithTasksResponse();
        BeanUtils.copyProperties(chapter, cr);
        List<Task> tasks = taskRepository.findByChapterId(chapter.getId());
        cr.setTasks(tasks.stream()
                .map(this::mapToTaskWithSubTasksResponse)
                .collect(Collectors.toList()));
        return cr;
    }

    // =========================================================================
    // Publishing flow
    // =========================================================================

    @Override
    public List<ChapterResponse> getPublishableChapters(Long projectId, Long requesterId) {
        Account requester = getAccount(requesterId);
        if (!requester.hasRole(SystemRoleName.LEADER_BOARD)
                && !requester.hasRole(SystemRoleName.EDITORIAL_BOARD_MEMBER)) {
            throw new AccessDeniedException(
                    "Only LEADER_BOARD or EDITORIAL_BOARD_MEMBER can view publishable chapters");
        }
        // Verify project exists
        getProject(projectId);
        return chapterRepository
                .findByProjectIdAndChapterStatus(projectId, ChapterStatus.COMPLETED)
                .stream()
                .map(ChapterResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChapterResponse publishChapter(Long chapterId, Long requesterId, java.time.LocalDate publishDate, String releaseNote) {
        Account requester = getAccount(requesterId);
        // BA V3 §3.1: both LEADER_BOARD and EDITORIAL_BOARD_MEMBER can publish (single-signoff).
        if (!requester.hasRole(SystemRoleName.LEADER_BOARD)
                && !requester.hasRole(SystemRoleName.EDITORIAL_BOARD_MEMBER)) {
            throw new AccessDeniedException(
                    "Only LEADER_BOARD or EDITORIAL_BOARD_MEMBER can publish a chapter");
        }

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found: " + chapterId));

        if (chapter.getChapterStatus() != ChapterStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Only COMPLETED chapters can be published (current status: "
                    + chapter.getChapterStatus() + ")");
        }
        if (chapter.getChapterStatus() == ChapterStatus.PUBLISHED) {
            throw new IllegalStateException("Chapter is already PUBLISHED");
        }

        chapter.setPublishDate(publishDate != null ? publishDate : java.time.LocalDate.now());
        chapter.setChapterStatus(ChapterStatus.PUBLISHED);
        // BA V3 §3.1 — record who/when published (single-signoff audit trail).
        chapter.setPublishedBy(requesterId);
        chapter.setPublishedAt(java.time.Instant.now());
        // Decision Log 2026-07-27 §AI-01: releaseNote is OPTIONAL (nullable).
        chapter.setReleaseNote(releaseNote != null && !releaseNote.isBlank() ? releaseNote : null);
        chapter = chapterRepository.save(chapter);

        // Auto-complete the ProductionPlan if all chapters are now PUBLISHED.
        // Decision Log §AI-03: dynamic — based on existing chapters only, NOT on targetChapterCount.
        if (chapter.getProductionPlan() != null) {
            ProductionPlan plan = chapter.getProductionPlan();
            boolean allPublished = !chapterRepository
                    .existsByProductionPlanIdAndChapterStatusNot(plan.getId(), ChapterStatus.PUBLISHED);
            if (allPublished && plan.getPlanStatus() != PlanStatus.COMPLETED) {
                plan.setPlanStatus(PlanStatus.COMPLETED);
                productionPlanRepository.save(plan);
            }
        }

        return ChapterResponse.from(chapter);
    }

    // Backward-compat overload — used by controller endpoint that doesn't yet accept releaseNote.
    @Override
    @Transactional
    public ChapterResponse publishChapter(Long chapterId, Long leaderId, java.time.LocalDate publishDate) {
        return publishChapter(chapterId, leaderId, publishDate, null);
    }

    @Override
    @Transactional
    public ChapterResponse recallChapter(Long chapterId, Long requesterId, RecallChapterRequest request) {
        Account requester = getAccount(requesterId);
        // BA V3 §3.4: only LEADER_BOARD or EDITORIAL_BOARD_MEMBER can recall.
        if (!requester.hasRole(SystemRoleName.LEADER_BOARD)
                && !requester.hasRole(SystemRoleName.EDITORIAL_BOARD_MEMBER)) {
            throw new AccessDeniedException(
                    "Only LEADER_BOARD or EDITORIAL_BOARD_MEMBER can recall a chapter");
        }

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found: " + chapterId));

        // Decision Log 2026-07-27 §AI-07: recallCount cap = 2. Lần 3 requires Leader override.
        int currentRecallCount = chapter.getRecallCount() == null ? 0 : chapter.getRecallCount();
        if (currentRecallCount >= 2) {
            throw new IllegalStateException(
                    "Chapter đã bị thu hồi " + currentRecallCount
                            + " lần (đã đạt giới hạn tối đa). Bắt buộc Leader can thiệp xử lý đặc biệt.");
        }

        return doRecall(chapter, request.getRecallReason());
    }

    @Override
    @Transactional
    public ChapterResponse overrideRecallChapter(Long chapterId, Long requesterId, OverrideRecallRequest request) {
        Account requester = getAccount(requesterId);
        // Decision Log 2026-07-27 §AI-07 follow-up: override chỉ Leader.
        if (!requester.hasRole(SystemRoleName.LEADER_BOARD)) {
            throw new AccessDeniedException(
                    "Only LEADER_BOARD can override the recall limit");
        }

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found: " + chapterId));

        if (chapter.getChapterStatus() != ChapterStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "Only PUBLISHED chapters can be recalled (current status: "
                            + chapter.getChapterStatus() + ")");
        }

        // Override path: skip the recallCount cap, force-recall.
        return doRecall(chapter, request.getRecallReason());
    }

    /**
     * Shared logic for recall (auto + override). BA V3 §3.4 + Decision Log §AI-04.
     * Does NOT auto-reopen Tasks — Tantou must explicitly call markTaskRevision.
     */
    private ChapterResponse doRecall(Chapter chapter, String recallReason) {
        if (chapter.getChapterStatus() != ChapterStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "Only PUBLISHED chapters can be recalled (current status: "
                            + chapter.getChapterStatus() + ")");
        }

        int currentRecallCount = chapter.getRecallCount() == null ? 0 : chapter.getRecallCount();
        chapter.setChapterStatus(ChapterStatus.IN_PRODUCTION);
        chapter.setRecallCount(currentRecallCount + 1);
        chapter.setRecallReason(recallReason);
        chapter = chapterRepository.save(chapter);

        // If Plan was COMPLETED, roll back to ACTIVE.
        if (chapter.getProductionPlan() != null) {
            ProductionPlan plan = chapter.getProductionPlan();
            if (plan.getPlanStatus() == PlanStatus.COMPLETED) {
                plan.setPlanStatus(PlanStatus.ACTIVE);
                productionPlanRepository.save(plan);
            }
        }

        return ChapterResponse.from(chapter);
    }

    /**
     * Decision Log 2026-07-27 §AI-04: endpoint mới để Tantou chọn 1 Task cụ thể và set sang
     * REVISION_REQUIRED. Task chỉ chuyển nếu Chapter thuộc Plan = ACTIVE/EXTENDED/OVERDUE
     * (không phải DRAFT/COMPLETED).
     */
    @Override
    @Transactional
    public group1.com.MangaSystemAndManagement.dto.response.TaskResponse markTaskRevision(
            Long taskId, group1.com.MangaSystemAndManagement.dto.request.MarkTaskRevisionRequest req) {
        Account tantou = getAccount(req.getTantouId());
        boolean isTantou = tantou.hasRole(SystemRoleName.TANTOU_EDITOR);
        boolean isLeader = tantou.hasRole(SystemRoleName.LEADER_BOARD);
        if (!isTantou && !isLeader) {
            throw new AccessDeniedException(
                    "Only TANTOU_EDITOR or LEADER_BOARD can mark a task for revision");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        Chapter chapter = task.getChapter();
        if (chapter == null || chapter.getProductionPlan() == null) {
            throw new IllegalStateException(
                    "Task must belong to a Chapter attached to an active ProductionPlan");
        }

        // AI-04: only meaningful if Chapter is back in production (post-Recall/Return).
        if (chapter.getChapterStatus() != ChapterStatus.IN_PRODUCTION) {
            throw new IllegalStateException(
                    "Mark-as-revision only allowed while Chapter is IN_PRODUCTION (current: "
                            + chapter.getChapterStatus() + ")");
        }

        ProductionPlan plan = chapter.getProductionPlan();
        assertPlanNotPaused(plan);  // pause vẫn chặn

        if (task.getTaskWorkflowStatus() != TaskWorkflowStatus.DONE
                && task.getTaskWorkflowStatus() != TaskWorkflowStatus.REVIEW) {
            throw new IllegalStateException(
                    "Only DONE or REVIEW tasks can be marked for revision (current: "
                            + task.getTaskWorkflowStatus() + ")");
        }

        task.setTaskWorkflowStatus(TaskWorkflowStatus.REVISION_REQUIRED);
        task = taskRepository.save(task);
        return group1.com.MangaSystemAndManagement.dto.response.TaskResponse.from(task);
    }

    @Override
    @Transactional
    public ChapterResponse returnChapterToProduction(Long chapterId, Long requesterId, ReturnChapterRequest request) {
        return doReturn(chapterId, requesterId, request, false);
    }

    @Override
    @Transactional
    public ChapterResponse overrideReturnLimit(Long chapterId, Long requesterId, ReturnChapterRequest request) {
        return doReturn(chapterId, requesterId, request, true);
    }

    /**
     * Shared logic for return + override. BA V3 §3.3:
     *  - Auto-return: rejectionCount < 2 → transition COMPLETED → IN_PRODUCTION, ++rejectionCount.
     *  - Auto-return: rejectionCount >= 2 → refuse; lock chapter in COMPLETED_NEEDS_REVIEW.
     *  - Override (Leader only): skip the cap, force-return.
     */
    private ChapterResponse doReturn(Long chapterId, Long requesterId, ReturnChapterRequest request, boolean override) {
        Account requester = getAccount(requesterId);
        boolean isLeader = requester.hasRole(SystemRoleName.LEADER_BOARD);
        boolean isBoard = requester.hasRole(SystemRoleName.EDITORIAL_BOARD_MEMBER);

        if (!isLeader && !isBoard) {
            throw new AccessDeniedException(
                    "Only LEADER_BOARD or EDITORIAL_BOARD_MEMBER can return a chapter");
        }
        if (override && !isLeader) {
            throw new AccessDeniedException(
                    "Only LEADER_BOARD can override the rejection limit");
        }

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found: " + chapterId));

        if (chapter.getChapterStatus() != ChapterStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Only COMPLETED chapters can be returned (current status: "
                            + chapter.getChapterStatus() + ")");
        }

        int currentRejectionCount = chapter.getRejectionCount() == null ? 0 : chapter.getRejectionCount();
        if (currentRejectionCount >= 2 && !override) {
            chapter.setChapterStatus(ChapterStatus.COMPLETED_NEEDS_REVIEW);
            chapter = chapterRepository.save(chapter);
            throw new IllegalStateException(
                    "Chapter đã bị trả về " + currentRejectionCount
                            + " lần. Bắt buộc tổ chức họp Hội đồng để chốt phương án. "
                            + "Chapter đã được khóa ở COMPLETED_NEEDS_REVIEW.");
        }

        chapter.setChapterStatus(ChapterStatus.IN_PRODUCTION);
        chapter.setRejectionCount(currentRejectionCount + 1);
        chapter.setRejectionReason(request.getRejectionReason());
        chapter = chapterRepository.save(chapter);

        // Decision Log 2026-07-27 §AI-04: do NOT auto-reopen Tasks on Return.
        // Tantou chủ động gọi markTaskRevision(taskId) cho từng Task cụ thể.
        // Tasks giữ nguyên trạng thái.

        // Roll back Plan if it was COMPLETED (BA V3 §2.1 soft-terminal behavior).
        if (chapter.getProductionPlan() != null) {
            ProductionPlan plan = chapter.getProductionPlan();
            if (plan.getPlanStatus() == PlanStatus.COMPLETED) {
                plan.setPlanStatus(PlanStatus.ACTIVE);
                productionPlanRepository.save(plan);
            }
        }

        return ChapterResponse.from(chapter);
    }

    // =========================================================================
    // AI-08: schedule + auto-publish scheduler
    // =========================================================================

    @Override
    @Transactional
    public ChapterResponse scheduleChapter(Long chapterId, Long requesterId, ScheduleChapterRequest request) {
        Account requester = getAccount(requesterId);
        if (!requester.hasRole(SystemRoleName.TANTOU_EDITOR)
                && !requester.hasRole(SystemRoleName.LEADER_BOARD)
                && !requester.hasRole(SystemRoleName.EDITORIAL_BOARD_MEMBER)) {
            throw new AccessDeniedException(
                    "Only TANTOU/LEADER/BOARD can schedule a chapter");
        }

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found: " + chapterId));

        if (chapter.getChapterStatus() != ChapterStatus.COMPLETED
                && chapter.getChapterStatus() != ChapterStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Only COMPLETED or SCHEDULED chapters can be scheduled (current status: "
                            + chapter.getChapterStatus() + ")");
        }

        if (request.getPublishDate() == null) {
            throw new IllegalArgumentException("publishDate is required");
        }
        if (request.getPublishDate().isBefore(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("publishDate must not be in the past");
        }

        chapter.setChapterStatus(ChapterStatus.SCHEDULED);
        chapter.setPublishDate(request.getPublishDate());
        chapter = chapterRepository.save(chapter);
        return ChapterResponse.from(chapter);
    }

    @Override
    @Transactional
    public int publishDueScheduledChapters() {
        java.time.LocalDate today = java.time.LocalDate.now();
        List<Chapter> due = chapterRepository
                .findByProductionPlanIdAndChapterStatus(null, ChapterStatus.SCHEDULED); // placeholder
        // Use a custom query instead:
        due = chapterRepository.findByChapterStatusAndPublishDateLessThanEqual(
                ChapterStatus.SCHEDULED, today);
        int count = 0;
        for (Chapter chapter : due) {
            try {
                chapter.setChapterStatus(ChapterStatus.PUBLISHED);
                // publishedBy = system (0). Caller null-allowed since field is Long.
                chapter.setPublishedBy(0L);
                chapter.setPublishedAt(java.time.Instant.now());
                chapterRepository.save(chapter);
                count++;

                // Auto-complete Plan if all chapters are now PUBLISHED (§AI-03).
                if (chapter.getProductionPlan() != null) {
                    ProductionPlan plan = chapter.getProductionPlan();
                    boolean allPublished = !chapterRepository
                            .existsByProductionPlanIdAndChapterStatusNot(plan.getId(), ChapterStatus.PUBLISHED);
                    if (allPublished && plan.getPlanStatus() != PlanStatus.COMPLETED) {
                        plan.setPlanStatus(PlanStatus.COMPLETED);
                        productionPlanRepository.save(plan);
                    }
                }
            } catch (Exception e) {
                // best-effort: log and continue with next chapter
                System.err.println("[AI-08 scheduler] Failed to publish chapter "
                        + chapter.getId() + ": " + e.getMessage());
            }
        }
        return count;
    }
}
