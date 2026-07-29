package group1.com.MangaSystemAndManagement.service.impl;

import group1.com.MangaSystemAndManagement.config.properties.StorageProperties;
import group1.com.MangaSystemAndManagement.dto.request.CreateSubTaskSubmissionRequest;
import group1.com.MangaSystemAndManagement.dto.request.CreateSubmissionRequest;
import group1.com.MangaSystemAndManagement.dto.request.CreateTaskSubmissionRequest;
import group1.com.MangaSystemAndManagement.dto.request.ReviewSubmissionRequest;
import group1.com.MangaSystemAndManagement.dto.request.SubmissionRequest;
import group1.com.MangaSystemAndManagement.dto.response.SubmissionFileResponse;
import group1.com.MangaSystemAndManagement.dto.response.SubmissionResponse;
import group1.com.MangaSystemAndManagement.exception.EntityNotFoundException;
import group1.com.MangaSystemAndManagement.exception.ResourceNotFoundException;
import group1.com.MangaSystemAndManagement.exception.WorkflowRuleViolationException;
import group1.com.MangaSystemAndManagement.model.*;
import group1.com.MangaSystemAndManagement.repository.AccountRepository;
import group1.com.MangaSystemAndManagement.repository.PlanningRepository;
import group1.com.MangaSystemAndManagement.repository.SubTaskRepository;
import group1.com.MangaSystemAndManagement.repository.SubmissionFileRepository;
import group1.com.MangaSystemAndManagement.repository.SubmissionRepository;
import group1.com.MangaSystemAndManagement.repository.TaskRepository;
import group1.com.MangaSystemAndManagement.repository.ChapterRepository;
import group1.com.MangaSystemAndManagement.repository.ProductionPlanRepository;
import group1.com.MangaSystemAndManagement.repository.ProjectRepository;
import group1.com.MangaSystemAndManagement.repository.SubmissionReviewRepository;
import group1.com.MangaSystemAndManagement.service.interfaces.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionFileRepository submissionFileRepository;
    private final AccountRepository accountRepository;
    private final PlanningRepository planningRepository;
    private final TaskRepository taskRepository;
    private final SubTaskRepository subTaskRepository;
    private final ChapterRepository chapterRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final ProjectRepository projectRepository;
    private final SubmissionReviewRepository submissionReviewRepository;
    private final StorageProperties storageProperties;

    // =====================================================================
    // 1. Modern polymorphic create (Assistant → Mangaka round).
    // =====================================================================
    @Override
    @Transactional
    @Deprecated
    public SubmissionResponse create(CreateSubmissionRequest req) {
        // ----- (a) Validate polymorphic target exactly-one
        boolean hasTask = req.getTaskId() != null;
        boolean hasSubTask = req.getSubTaskId() != null;
        if (hasTask == hasSubTask) {
            throw new WorkflowRuleViolationException(
                    "Exactly one of taskId or subTaskId must be supplied");
        }

        // ----- (b) Load submitter
        Account submitter = accountRepository.findById(req.getRequesterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Submitter account not found: " + req.getRequesterId()));

        // ----- (c) Build the Submission shell – polymorphic branch
        Submission s = new Submission();
        s.setSubmittedBy(submitter);
        s.setSubmissionType(req.getSubmissionType());
        s.setProductionStatus(group1.com.MangaSystemAndManagement.model.ProductionSubmissionStatus.PENDING);
        s.setSubmittedAt(Instant.now());
        s.setContentUrl(req.getNote());

        if (hasSubTask) {
            SubTask subTask = subTaskRepository.findById(req.getSubTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "SubTask not found: " + req.getSubTaskId()));
            bindSubTask(submitter, subTask, req, s);
            s.setSubTask(subTask);
        } else { // hasTask
            Task task = taskRepository.findById(req.getTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Task not found: " + req.getTaskId()));
            bindTask(submitter, task, req, s);
            s.setTask(task);
        }

        // ----- (d) Files must be present
        List<MultipartFile> files = req.getFiles();
        if (files == null || files.isEmpty() || files.stream().allMatch(f -> f == null || f.isEmpty())) {
            throw new WorkflowRuleViolationException("At least one file must be uploaded");
        }

        // ----- (e) Parent linkage for REVISION only
        if (req.getSubmissionType() == SubmissionType.REVISION) {
            Submission parent = submissionRepository
                    .findFirstBySubTaskIdAndProductionStatusOrderBySubmittedAtDesc(
                            req.getSubTaskId(), group1.com.MangaSystemAndManagement.model.ProductionSubmissionStatus.REJECTED)
                    .orElseThrow(() -> new WorkflowRuleViolationException(
                            "REVISION submission requires a previously REJECTED parent"));
            s.setParent(parent);
        }

        s = submissionRepository.save(s);

        // ----- (f) Save files
        List<SubmissionFile> savedFiles = persistFiles(s, files, req.getSubmissionType());
        s.setFiles(savedFiles);

        // ----- (g) Roll-up effect on the SubTask status
        if (hasSubTask) {
            SubTask subTask = s.getSubTask();
            // Đơn giản hoá: mọi round mới → SubTask = IN_PROGRESS (chờ Mangaka review).
            subTask.setSubtaskStatus(SubTaskWorkflowStatus.IN_PROGRESS);
            subTaskRepository.save(subTask);
        }
        return SubmissionResponse.from(s);
    }

    // =====================================================================
    // 1b. URL-nested creation – the recommended path.
    //
    // The submitter identity is taken from the URL, so these methods can
    // enforce "SubTask round" vs "Task-level round" without relying on a
    // body-side "exactly one of subTaskId|taskId" discriminant.
    // =====================================================================

    @Override
    @Transactional
    public SubmissionResponse createForSubTask(Long subTaskId,
                                               CreateSubTaskSubmissionRequest req) {
        // Load submitter
        Account submitter = accountRepository.findById(req.getRequesterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Submitter account not found: " + req.getRequesterId()));

        // Load SubTask – we 404 here, not 400, because the URL is wrong.
        SubTask subTask = subTaskRepository.findById(subTaskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SubTask not found: " + subTaskId));

        SubmissionType type = req.getSubmissionType();
        // Reject TASK_LEVEL outright – that is a different URL.
        if (type == SubmissionType.TASK_LEVEL) {
            throw new WorkflowRuleViolationException(
                    "TASK_LEVEL is not valid for a SubTask submission; "
                    + "use POST /api/workflow/tasks/{taskId}/submissions instead");
        }

        // Build shell
        Submission s = new Submission();
        s.setSubmittedBy(submitter);
        s.setSubmissionType(type);
        s.setProductionStatus(group1.com.MangaSystemAndManagement.model.ProductionSubmissionStatus.PENDING);
        s.setSubmittedAt(Instant.now());
        s.setContentUrl(req.getNote());

        bindSubTaskRules(submitter, subTask, type);
        s.setSubTask(subTask);

        // Files must be present
        List<MultipartFile> files = req.getFiles();
        if (files == null || files.isEmpty()
                || files.stream().allMatch(f -> f == null || f.isEmpty())) {
            throw new WorkflowRuleViolationException("At least one file must be uploaded");
        }

        // REVISION must have a rejected parent
        if (type == SubmissionType.REVISION) {
            Submission parent = submissionRepository
                    .findFirstBySubTaskIdAndProductionStatusOrderBySubmittedAtDesc(
                            subTask.getId(), group1.com.MangaSystemAndManagement.model.ProductionSubmissionStatus.REJECTED)
                    .orElseThrow(() -> new WorkflowRuleViolationException(
                            "REVISION submission requires a previously REJECTED parent"));
            s.setParent(parent);
        }

        s = submissionRepository.save(s);
        s.setFiles(persistFiles(s, files, type));

        // Đơn giản hoá: mọi round mới → SubTask = IN_PROGRESS (chờ Mangaka review).
        subTask.setSubtaskStatus(SubTaskWorkflowStatus.IN_PROGRESS);
        subTaskRepository.save(subTask);
        return SubmissionResponse.from(s);
    }

    @Override
    @Transactional
    public SubmissionResponse createForTask(Long taskId,
                                            CreateTaskSubmissionRequest req) {
        // Load submitter
        Account submitter = accountRepository.findById(req.getRequesterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Submitter account not found: " + req.getRequesterId()));

        // Load Task – 404 if URL is wrong.
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task not found: " + taskId));

        SubmissionType type = req.getSubmissionType();
        // Reject anything that isn't TASK_LEVEL – different URL.
        if (type != SubmissionType.TASK_LEVEL) {
            throw new WorkflowRuleViolationException(
                    "Only TASK_LEVEL submissions are valid against a Task; "
                    + "use POST /api/workflow/subtasks/{subTaskId}/submissions instead");
        }

        Submission s = new Submission();
        s.setSubmittedBy(submitter);
        s.setSubmissionType(type);
        s.setProductionStatus(group1.com.MangaSystemAndManagement.model.ProductionSubmissionStatus.PENDING);
        s.setSubmittedAt(Instant.now());
        s.setContentUrl(req.getNote());

        bindTaskRules(submitter, task);
        s.setTask(task);

        // Files must be present
        List<MultipartFile> files = req.getFiles();
        if (files == null || files.isEmpty()
                || files.stream().allMatch(f -> f == null || f.isEmpty())) {
            throw new WorkflowRuleViolationException("At least one file must be uploaded");
        }

        s = submissionRepository.save(s);
        s.setFiles(persistFiles(s, files, type));
        return SubmissionResponse.from(s);
    }

    /**
     * Pure rule check for a SubTask submission – split out from
     * {@link #bindSubTask(Account, SubTask, CreateSubmissionRequest, Submission)}
     * so the URL-nested path doesn't have to construct a fake
     * {@link CreateSubmissionRequest}.
     */
    private void bindSubTaskRules(Account submitter, SubTask subTask, SubmissionType type) {
        if (!submitter.hasRole(SystemRoleName.ASSISTANT)) {
            throw new AccessDeniedException(
                    "Only an Assistant may submit a file for a SubTask");
        }
        if (subTask.getAssignee() == null
                || subTask.getAssignee().getId() != submitter.getId()) {
            throw new AccessDeniedException(
                    "Submitter is not the assigned Assistant of this SubTask");
        }
        if (subTask.getSubtaskStatus() == SubTaskWorkflowStatus.COMPLETED) {
            throw new WorkflowRuleViolationException(
                    "SubTask is already COMPLETED – re-open it before submitting again");
        }

        // State Machine validation (đơn giản hoá – 1 vòng duy nhất)
// Cho phép Assistant nộp ROUGH_SKETCH / REVISION / FINAL bất kỳ lúc nào,
// miễn là SubTask chưa COMPLETED. Submission type giờ chỉ còn mang tính phân loại.
        if (subTask.getSubtaskStatus() == SubTaskWorkflowStatus.COMPLETED) {
            throw new WorkflowRuleViolationException(
                    "SubTask is already COMPLETED – cannot submit again");
        }
    }

    /**
     * Pure rule check for a Task-level submission.
     */
    private void bindTaskRules(Account submitter, Task task) {
        if (!submitter.hasRole(SystemRoleName.MANGAKA)
                && !submitter.hasRole(SystemRoleName.TANTOU_EDITOR)) {
            throw new AccessDeniedException(
                    "Only Mangaka or Tantō may submit against a Task");
        }
        if (submitter.hasRole(SystemRoleName.MANGAKA)) {
            boolean anyOpen = subTaskRepository
                    .existsByTaskIdAndSubtaskStatusNot(task.getId(),
                            SubTaskWorkflowStatus.COMPLETED);
            if (anyOpen) {
                throw new WorkflowRuleViolationException(
                        "Cannot submit at Task level while any SubTask is not COMPLETED");
            }
        }
    }

    private void bindSubTask(Account submitter, SubTask subTask,
                             CreateSubmissionRequest req, Submission s) {
        // Only the assigned Assistant may submit on a SubTask.
        if (!submitter.hasRole(SystemRoleName.ASSISTANT)) {
            throw new AccessDeniedException(
                    "Only an Assistant may submit a file for a SubTask");
        }
        if (subTask.getAssignee() == null
                || subTask.getAssignee().getId() != submitter.getId()) {
            throw new AccessDeniedException(
                    "Submitter is not the assigned Assistant of this SubTask");
        }
        if (subTask.getSubtaskStatus() == SubTaskWorkflowStatus.COMPLETED) {
            throw new WorkflowRuleViolationException(
                    "SubTask is already COMPLETED – re-open it before submitting again");
        }

        SubmissionType type = req.getSubmissionType();
        if (type == SubmissionType.TASK_LEVEL) {
            throw new WorkflowRuleViolationException(
                    "TASK_LEVEL is not valid for a SubTask submission");
        }

        // State Machine validation (đơn giản hoá – 1 vòng duy nhất)
// Cho phép Assistant nộp ROUGH_SKETCH / REVISION / FINAL bất kỳ lúc nào,
// miễn là SubTask chưa COMPLETED. Submission type giờ chỉ còn mang tính phân loại.
        if (subTask.getSubtaskStatus() == SubTaskWorkflowStatus.COMPLETED) {
            throw new WorkflowRuleViolationException(
                    "SubTask is already COMPLETED – cannot submit again");
        }
    }

    private void bindTask(Account submitter, Task task,
                          CreateSubmissionRequest req, Submission s) {
        if (req.getSubmissionType() != SubmissionType.TASK_LEVEL) {
            throw new WorkflowRuleViolationException(
                    "Only TASK_LEVEL submissions are valid against a Task");
        }
        if (!submitter.hasRole(SystemRoleName.MANGAKA)
                && !submitter.hasRole(SystemRoleName.TANTOU_EDITOR)) {
            throw new AccessDeniedException(
                    "Only Mangaka or Tantō may submit against a Task");
        }
        // For Mangaka: every SubTask of this Task must already be COMPLETED.
        if (submitter.hasRole(SystemRoleName.MANGAKA)) {
            boolean anyOpen = subTaskRepository
                    .existsByTaskIdAndSubtaskStatusNot(task.getId(),
                            SubTaskWorkflowStatus.COMPLETED);
            if (anyOpen) {
                throw new WorkflowRuleViolationException(
                        "Cannot submit at Task level while any SubTask is not COMPLETED");
            }
        }
    }

    // =====================================================================
    // 2. History queries
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> historyBySubTask(Long subTaskId) {
        return submissionRepository
                .findBySubTaskIdOrderBySubmittedAtDesc(subTaskId).stream()
                .map(SubmissionResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> historyByTask(Long taskId) {
        return submissionRepository
                .findByTaskIdOrderBySubmittedAtDesc(taskId).stream()
                .map(SubmissionResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Submission> findById(Long id) {
        return submissionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Submission findByIdOrThrow(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Submission not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Submission> findAll() {
        return submissionRepository.findAllWithSubmittedBy();
    }

    // =====================================================================
    // 4. Review (approve / reject) – drives the workflow forward.
    // =====================================================================
    @Override
    @Transactional
    public SubmissionResponse review(Long submissionId, ReviewSubmissionRequest req) {
        Submission s = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Submission not found: " + submissionId));

        // ----- (a) Validate status & decision
        if (s.getProductionStatus() != group1.com.MangaSystemAndManagement.model.ProductionSubmissionStatus.PENDING) {
            throw new WorkflowRuleViolationException(
                    "Only PENDING submissions can be reviewed (current: "
                    + s.getProductionStatus() + ")");
        }
        if (req.getDecision() != group1.com.MangaSystemAndManagement.model.ProductionSubmissionStatus.APPROVED
                && req.getDecision() != group1.com.MangaSystemAndManagement.model.ProductionSubmissionStatus.REJECTED) {
            throw new WorkflowRuleViolationException(
                    "Decision must be APPROVED or REJECTED");
        }

        // ----- (b) Reject requires a note (BA rule)
        if (req.getDecision() == group1.com.MangaSystemAndManagement.model.ProductionSubmissionStatus.REJECTED
                && (req.getNote() == null || req.getNote().isBlank())) {
            throw new WorkflowRuleViolationException(
                    "A rejection note is required so the submitter knows what to fix");
        }

        // ----- (c) RBAC: who can review what
        Account reviewer = accountRepository.findById(req.getReviewerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reviewer not found: " + req.getReviewerId()));

        if (s.getSubmissionType() == SubmissionType.TASK_LEVEL) {
            // Tantō only – the Mangaka submits, the Tantō approves the whole task.
            if (!reviewer.hasRole(SystemRoleName.TANTOU_EDITOR)) {
                throw new AccessDeniedException(
                        "Only a Tantō can review a TASK_LEVEL submission");
            }
        } else {
            // SubTask round (ROUGH / REVISION / FINAL) – Mangaka only.
            if (!reviewer.hasRole(SystemRoleName.MANGAKA)) {
                throw new AccessDeniedException(
                        "Only a Mangaka can review a SubTask submission");
            }
            // Restrict: reviewer must be the assignee of the parent Task.
            Task parentTask = s.getSubTask() != null ? s.getSubTask().getTask() : null;
            if (parentTask != null
                    && (parentTask.getAssignee() == null
                        || parentTask.getAssignee().getId() != reviewer.getId())) {
                throw new AccessDeniedException(
                        "Only the Mangaka assigned to the parent Task may review this SubTask");
            }
        }

        // ----- (d) Persist decision on the Submission
        s.setProductionStatus(req.getDecision());
        s.setReviewer(reviewer);
        s.setReviewedAt(Instant.now());
        if (req.getNote() != null && !req.getNote().isBlank()) {
            s.setContentUrl(req.getNote()); // overwrite – "note" replaces the prior content
        }
        s = submissionRepository.save(s);

        // Create the historical review record
        group1.com.MangaSystemAndManagement.model.SubmissionReview reviewRecord = new group1.com.MangaSystemAndManagement.model.SubmissionReview();
        reviewRecord.setSubmission(s);
        reviewRecord.setReviewer(reviewer);
        reviewRecord.setDecision(req.getDecision().name());
        reviewRecord.setComment(req.getNote());
        reviewRecord.setReviewedAt(Instant.now());
        submissionReviewRepository.save(reviewRecord);

        // ----- (e) Cascade effects
        if (s.getSubTask() != null) {
            cascadeSubTaskReview(s);
        } else if (s.getTask() != null && req.getDecision() == group1.com.MangaSystemAndManagement.model.ProductionSubmissionStatus.APPROVED) {
            cascadeTaskApproval(s);
        }
        return SubmissionResponse.from(s);
    }

    private void cascadeSubTaskReview(Submission s) {
        // Spec v2.1 (đơn giản hoá): mỗi SubTask round do Mangaka review.
        //   APPROVED → SubTask = COMPLETED, Mangaka có thể nộp TASK_LEVEL ngay.
        //   REJECTED → SubTask quay về IN_PROGRESS, Assistant sửa rồi nộp lại.
        // (Bỏ stage rough/final 2 vòng – vì nhu cầu thực tế chỉ cần 1 vòng duy nhất.)
        SubTask sub = s.getSubTask();
        switch (s.getProductionStatus()) {
            case APPROVED -> {
                sub.setSubtaskStatus(SubTaskWorkflowStatus.COMPLETED);
                subTaskRepository.save(sub);
                // Roll-up Task + Plan
                recomputeTaskProgress(sub.getTask().getId());
            }
            case REJECTED -> {
                sub.setSubtaskStatus(SubTaskWorkflowStatus.IN_PROGRESS);
                subTaskRepository.save(sub);
            }
            default -> { /* no-op */ }
        }
    }

    private void cascadeTaskApproval(Submission s) {
        Task task = s.getTask();
        task.setTaskWorkflowStatus(TaskWorkflowStatus.DONE);
        task.setProgressPercentage(100);
        taskRepository.save(task);

        // Plan roll-up
        if (task.getChapter() != null) {
            recomputePlanProgress(task.getChapter().getProductionPlan().getId());
        }
    }

    /** Recompute the roll-up percentage for a Task. Always safe to call. */
    private void recomputeTaskProgress(Long taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) return;
        long total = subTaskRepository.countByTaskId(taskId);
        if (total == 0) {
            task.setProgressPercentage(0);
        } else {
            long done = subTaskRepository.countByTaskIdAndSubtaskStatus(
                    taskId, SubTaskWorkflowStatus.COMPLETED);
            int pct = (int) Math.round((done * 100.0) / total);
            task.setProgressPercentage(pct);
            if (pct == 100
                    && task.getTaskWorkflowStatus() != TaskWorkflowStatus.DONE) {
                // Marker only – Tantō still needs to do the Task-level review
                // before the chapter can transition to COMPLETED.
            }
        }
        taskRepository.save(task);

        if (task.getChapter() != null) {
            // Plan roll-up only when the whole Task is DONE (re-checked via
            // chapter COMPLETED/PUBLISHED count below).
            recomputePlanProgress(task.getChapter().getProductionPlan().getId());
        }
    }

    /** Recompute {@code ProductionPlan.completionPercentage} from its chapters. */
    private void recomputePlanProgress(Long planId) {
        ProductionPlan plan = productionPlanRepository.findById(planId).orElse(null);
        if (plan == null) return;

        List<Chapter> chapters = chapterRepository.findByProductionPlanId(planId);
        if (chapters.isEmpty()) {
            plan.setCompletionPercentage(0);
        } else {
            long done = chapters.stream()
                    .filter(c -> c.getChapterStatus() == ChapterStatus.COMPLETED
                              || c.getChapterStatus() == ChapterStatus.PUBLISHED)
                    .count();
            int pct = (int) Math.round((done * 100.0) / chapters.size());
            plan.setCompletionPercentage(pct);
        }
        productionPlanRepository.save(plan);
    }

    // =====================================================================
    // 3. Legacy endpoints (kept so the old controller endpoints still work)
    // =====================================================================
    @Override
    @Transactional
    @Deprecated
    public Submission submitFiles(Long accountId, SubmissionRequest request) {
        Account submitter = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with id " + accountId));

        Submission submission = new Submission();
        submission.setSubmittedBy(submitter);
        submission.setTitle(request.getTitle());
        submission.setContentUrl(request.getNote());
        submission.setProductionStatus(group1.com.MangaSystemAndManagement.model.ProductionSubmissionStatus.PENDING);
        submission.setSubmissionType(SubmissionType.TASK_LEVEL);
        submission.setSubmittedAt(Instant.now());

//        if (request.getPlanningId() != null) {
//            Planning planning = planningRepository.findById(request.getPlanningId())
//                    .orElseThrow(() -> new RuntimeException(
//                            "Planning not found with id " + request.getPlanningId()));
//            submission.setPlanning(planning);
//        }

        submission = submissionRepository.save(submission);

        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            submission.setFiles(persistFiles(submission, request.getFiles(), SubmissionType.TASK_LEVEL));
        }
        return submission;
    }

    @Override
    @Transactional
    @Deprecated
    public Submission update(Long id, SubmissionRequest request) {
        Submission entity = submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found with id " + id));
        if (request.getTitle() != null) entity.setTitle(request.getTitle());
        if (request.getNote() != null) entity.setContentUrl(request.getNote());
        return submissionRepository.save(entity);
    }

    @Override
    @Transactional
    @Deprecated
    public void delete(Long id) {
        if (!submissionRepository.existsById(id)) {
            throw new RuntimeException("Submission not found with id " + id);
        }
        submissionRepository.deleteById(id);
    }

    // =====================================================================
    // 5. Legacy editorial-board approve → auto-create Project
    // =====================================================================
    @Override
    @Transactional
    public Submission approveAndCreateProject(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found with id " + submissionId));

        if (submission.getNameStatus() != group1.com.MangaSystemAndManagement.model.NameSubmissionStatus.APPROVED) {
            throw new WorkflowRuleViolationException(
                    "Only APPROVED submissions can be converted to projects (current: " + submission.getNameStatus() + ")");
        }

        // Build project from submission fields
        Project project = new Project();
        project.setTitle(submission.getTitle() != null ? submission.getTitle() : "Untitled Project");
        project.setDescription(submission.getContentUrl());
        project.setOwner(submission.getSubmittedBy());
        project.setStatus("APPROVED");
        project.setProjectWorkflowStatus(ProjectWorkflowStatus.DRAFT);

        project = projectRepository.save(project);

        // Link submission to the new project
        submission.setProject(project);
        submission.setNameStatus(group1.com.MangaSystemAndManagement.model.NameSubmissionStatus.APPROVED);
        submission.setReviewedAt(Instant.now());

        return submissionRepository.save(submission);
    }

    // =====================================================================
    // Helpers
    // =====================================================================
    private List<SubmissionFile> persistFiles(Submission owner,
                                              List<MultipartFile> files,
                                              SubmissionType type) {
        Path uploadDir = storageProperties.uploadPath();
        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Cannot create upload directory: " + e.getMessage(), e);
        }

        FileType fileType = mapFileType(type);
        List<SubmissionFile> out = new ArrayList<>();
        int order = 0;
        for (MultipartFile multipartFile : files) {
            if (multipartFile == null || multipartFile.isEmpty()) continue;

            String originalName = multipartFile.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
            }

            String uniqueFilename = UUID.randomUUID().toString() + extension;
            Path targetPath = uploadDir.resolve(uniqueFilename);
            try {
                Files.copy(multipartFile.getInputStream(), targetPath,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to store file " + originalName + ": " + e.getMessage(), e);
            }

            String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(uniqueFilename)
                    .toUriString();

            SubmissionFile sf = new SubmissionFile();
            sf.setOriginalName(originalName);
            sf.setFilePath(fileUrl);
            sf.setFileSize(multipartFile.getSize());
            sf.setContentType(multipartFile.getContentType());
            sf.setFileType(fileType);
            sf.setFileOrder(order++);
            sf.setSubmission(owner);
            out.add(submissionFileRepository.save(sf));
        }
        return out;
    }

    private FileType mapFileType(SubmissionType type) {
        if (type == null) return FileType.COMPILATION;
        return switch (type) {
            case ROUGH_SKETCH -> FileType.ROUGH_SKETCH;
            case REVISION     -> FileType.REVISION;
            case FINAL        -> FileType.FINAL;
            case TASK_LEVEL   -> FileType.COMPILATION;
        };
    }
}
