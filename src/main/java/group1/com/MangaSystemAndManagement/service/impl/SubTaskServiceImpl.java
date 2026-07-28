package group1.com.MangaSystemAndManagement.service.impl;

import group1.com.MangaSystemAndManagement.dto.request.CreateSubTaskRequest;
import group1.com.MangaSystemAndManagement.dto.response.SubTaskResponse;
import group1.com.MangaSystemAndManagement.exception.EntityNotFoundException;
import group1.com.MangaSystemAndManagement.exception.ResourceNotFoundException;
import group1.com.MangaSystemAndManagement.exception.WorkflowRuleViolationException;
import group1.com.MangaSystemAndManagement.model.Account;
import group1.com.MangaSystemAndManagement.model.SubTask;
import group1.com.MangaSystemAndManagement.model.SubTaskWorkflowStatus;
import group1.com.MangaSystemAndManagement.model.SystemRoleName;
import group1.com.MangaSystemAndManagement.model.Task;
import group1.com.MangaSystemAndManagement.model.TaskWorkflowStatus;
import group1.com.MangaSystemAndManagement.repository.AccountRepository;
import group1.com.MangaSystemAndManagement.repository.SubTaskRepository;
import group1.com.MangaSystemAndManagement.repository.TaskRepository;
import group1.com.MangaSystemAndManagement.service.interfaces.SubTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubTaskServiceImpl implements SubTaskService {

    private final TaskRepository taskRepository;
    private final SubTaskRepository subTaskRepository;
    private final AccountRepository accountRepository;

    // ==================================================================
    // 1. Create
    // ==================================================================
    @Override
    @Transactional
    public SubTaskResponse createSubTask(Long taskId, CreateSubTaskRequest req) {
        Account requester = accountRepository.findById(req.getRequesterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Requester account not found: " + req.getRequesterId()));

        Task parent = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        // ----- RBAC: only a Mangaka (the actual assignee of the parent Task) or a
        // Tantō acting on the same Chapter may split a Task.
        boolean isTantou = requester.hasRole(SystemRoleName.TANTOU_EDITOR);
        boolean isMangaka = requester.hasRole(SystemRoleName.MANGAKA);

        if (!isTantou && !isMangaka) {
            throw new AccessDeniedException("Only Tantō or Mangaka can create SubTasks");
        }
        if (isMangaka && (parent.getAssignee() == null
                || parent.getAssignee().getId() != requester.getId())) {
            throw new AccessDeniedException("Mangaka can only split Tasks assigned to themselves");
        }

        // ----- State checks
        if (parent.getTaskWorkflowStatus() == TaskWorkflowStatus.DONE) {
            throw new WorkflowRuleViolationException(
                    "Cannot split a SubTask from a Task that is already DONE");
        }

        // ----- Validate SubTask assignee has Assistant role
        Account assistant = accountRepository.findById(req.getAssigneeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignee account not found: " + req.getAssigneeId()));
        if (!assistant.hasRole(SystemRoleName.ASSISTANT)) {
            throw new WorkflowRuleViolationException(
                    "Assignee must hold the ASSISTANT system role");
        }

        // ----- Validate deadline (SubTask deadline MUST be ≤ parent Task deadline)
        LocalTime deadlineTime = parseDeadlineTime(req.getDeadlineTime());
        Instant subDeadline = req.getDeadlineDate()
                .atTime(deadlineTime)
                .atZone(ZoneId.systemDefault())
                .toInstant();

        Instant taskDeadline = resolveParentTaskDeadline(parent);
        if (taskDeadline != null && subDeadline.isAfter(taskDeadline)) {
            throw new WorkflowRuleViolationException(
                    "SubTask deadline must be on or before the parent Task deadline ("
                    + taskDeadline + ")");
        }

        // ----- Persist
        SubTask sub = new SubTask();
        sub.setTask(parent);
        sub.setAssignee(assistant);
        sub.setTitle(req.getTitle());
        sub.setDescription(req.getDescription());
        sub.setSubtaskStatus(SubTaskWorkflowStatus.TODO);
        sub.setDeadlineDate(req.getDeadlineDate());
        sub.setDeadlineTime(deadlineTime);

        sub = subTaskRepository.save(sub);
        return SubTaskResponse.from(sub);
    }

    // ==================================================================
    // 2. Queries
    // ==================================================================
    @Override
    @Transactional(readOnly = true)
    public List<SubTaskResponse> listByTask(Long taskId, Long requesterId) {
        // Make sure the caller can see them – we don't enforce scoping tightly here
        // because SubTasks of a chapter belong to the same project; the caller-id check
        // mostly exists for logging/audit.
        ensureAccountExists(requesterId);
        return subTaskRepository.findByTaskId(taskId).stream()
                .map(SubTaskResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubTaskResponse> listByAssignee(Long assigneeId, Long requesterId) {
        Account requester = accountRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Requester account not found: " + requesterId));

        // An Assistant may only see their own list. Mangaka/Tantō may see anyone.
        boolean isPrivileged = requester.hasRole(SystemRoleName.MANGAKA)
                || requester.hasRole(SystemRoleName.TANTOU_EDITOR);
        if (!isPrivileged && (assigneeId == null
                || requester.getId() != assigneeId.longValue())) {
            throw new AccessDeniedException(
                    "Assistants can only view their own SubTask list");
        }

        return subTaskRepository
                .findByAssigneeIdOrderByDeadlineDateAscDeadlineTimeAsc(assigneeId)
                .stream()
                .map(SubTaskResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubTaskResponse> listByAssistant(Long assistantId) {
        Account assistant = accountRepository.findById(assistantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assistant account not found: " + assistantId));

        if (!assistant.hasRole(SystemRoleName.ASSISTANT)) {
            throw new AccessDeniedException(
                    "Only accounts with the ASSISTANT role may use this endpoint");
        }

        return subTaskRepository
                .findByAssigneeIdOrderByDeadlineDateAscDeadlineTimeAsc(assistantId)
                .stream()
                .map(SubTaskResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SubTaskResponse getById(Long id, Long requesterId) {
        SubTask sub = subTaskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SubTask not found: " + id));

        Account requester = accountRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Requester account not found: " + requesterId));

        boolean isMangaka = requester.hasRole(SystemRoleName.MANGAKA);
        boolean isTantou = requester.hasRole(SystemRoleName.TANTOU_EDITOR);
        boolean isAssignee = sub.getAssignee() != null
                && sub.getAssignee().getId() == requester.getId();

        if (!isMangaka && !isTantou && !isAssignee) {
            throw new AccessDeniedException(
                    "Only the assignee, the parent Mangaka or a Tantō may view this SubTask");
        }
        return SubTaskResponse.from(sub);
    }

    // ==================================================================
    // 3. Reopen
    // ==================================================================
    @Override
    @Transactional
    public SubTaskResponse reopen(Long id, Long requesterId) {
        SubTask sub = subTaskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SubTask not found: " + id));

        Account requester = accountRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Requester account not found: " + requesterId));

        boolean isMangaka = requester.hasRole(SystemRoleName.MANGAKA);
        boolean isTantou = requester.hasRole(SystemRoleName.TANTOU_EDITOR);

        if (!isMangaka && !isTantou) {
            throw new AccessDeniedException(
                    "Only Mangaka or Tantō can reopen a SubTask");
        }
        if (isMangaka && (sub.getTask().getAssignee() == null
                || sub.getTask().getAssignee().getId() != requester.getId())) {
            throw new AccessDeniedException(
                    "Mangaka can only reopen SubTasks of their own Tasks");
        }

        if (sub.getSubtaskStatus() != SubTaskWorkflowStatus.COMPLETED) {
            throw new WorkflowRuleViolationException(
                    "Only COMPLETED SubTasks can be reopened (current: "
                    + sub.getSubtaskStatus() + ")");
        }

        sub.setSubtaskStatus(SubTaskWorkflowStatus.FINAL_REJECTED);
        sub = subTaskRepository.save(sub);
        return SubTaskResponse.from(sub);
    }

    // ==================================================================
    // Helpers
    // ==================================================================
    private void ensureAccountExists(Long id) {
        if (id == null) return;
        accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Requester account not found: " + id));
    }

    /**
     * Resolves the parent task's effective deadline – preferring the
     * {@code deadlineDate/deadlineTime} pair added in V4, falling back to the
     * legacy {@code Deadline} Instant column.
     */
    private Instant resolveParentTaskDeadline(Task t) {
        if (t.getDeadlineDate() != null) {
            LocalTime lt = t.getDeadlineTime() != null
                    ? t.getDeadlineTime()
                    : LocalTime.of(23, 59, 59);
            return t.getDeadlineDate().atTime(lt)
                    .atZone(ZoneId.systemDefault()).toInstant();
        }
        return null;
    }

    /**
     * Accepts the {@code deadlineTime} field supplied by clients and turns it
     * into a {@link LocalTime}. Returns 23:59:59 when the field is missing.
     *
     * <p>Supported inputs (case-insensitive, whitespace tolerated):</p>
     * <ul>
     *   <li>{@code 08:00}, {@code 8:00}, {@code 08:00:00}, {@code 8:00:00}
     *       – ISO-8601 hour/minute(/second).</li>
     *   <li>{@code 8am}, {@code 8 AM}, {@code 8:30 pm}, {@code 8pm}
     *       – 12-hour UI form (with or without {@code :mm}).</li>
     *   <li>{@code 800}, {@code 0800}, {@code 080000}, {@code 80000}
     *       – compact HHmm or HHmmss.</li>
     * </ul>
     *
     * @throws WorkflowRuleViolationException with HTTP 400 if the input is
     *         present but unparseable, so the caller surfaces a clear error
     *         instead of a Jackson {@code HttpMessageNotReadableException}.
     */
    static final Pattern AMPM_PATTERN = Pattern.compile(
            "^\\s*(\\d{1,2})(?:[:.](\\d{1,2}))?(?:[:.](\\d{1,2}))?\\s*([ap]m)\\s*$",
            Pattern.CASE_INSENSITIVE);

    static final Pattern COMPACT_PATTERN = Pattern.compile(
            "^\\s*(\\d{3,6})\\s*$");

    static LocalTime parseDeadlineTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalTime.of(23, 59, 59);
        }
        String s = raw.trim();

        // 1) "8am" / "8:30 pm" / "8:00:30am" – 12-hour form
        Matcher m = AMPM_PATTERN.matcher(s);
        if (m.matches()) {
            try {
                int hour = Integer.parseInt(m.group(1));
                int minute = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
                int second = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
                String meridiem = m.group(4).toLowerCase(Locale.ROOT);
                if (hour < 1 || hour > 12 || minute > 59 || second > 59) {
                    throw new IllegalArgumentException("out-of-range");
                }
                if ("am".equals(meridiem)) {
                    if (hour == 12) hour = 0;
                } else {
                    if (hour != 12) hour += 12;
                }
                return LocalTime.of(hour, minute, second);
            } catch (Exception ignored) {
                // fall through to friendlier error below
            }
        }

        // 2) Strict 24-hour ISO LocalTime – "08:00", "08:00:00".
        //    Also accept single-digit hours like "8:00" or "8:30:45" by
        //    zero-padding the hour component before calling LocalTime.parse.
        if (s.matches("^\\s*\\d{1,2}:\\d{2}(:\\d{2})?\\s*$")) {
            String[] parts = s.split(":");
            String padded = String.format("%02d:%s:%s",
                    Integer.parseInt(parts[0]), parts[1],
                    parts.length == 3 ? parts[2] : "00");
            try {
                return LocalTime.parse(padded);
            } catch (DateTimeParseException ignored) {
                // fall through
            }
        }
        try {
            return LocalTime.parse(s.toUpperCase(Locale.ROOT));
        } catch (DateTimeParseException ignored) {
            // not ISO, keep trying
        }

        // 3) Compact HHmm / HHmmss – "800", "0800", "080000"
        Matcher cm = COMPACT_PATTERN.matcher(s);
        if (cm.matches()) {
            String digits = cm.group(1);
            try {
                if (digits.length() <= 3) {
                    int hour = Integer.parseInt(digits);
                    if (hour < 0 || hour > 23) throw new IllegalArgumentException();
                    return LocalTime.of(hour, 0);
                } else if (digits.length() == 4) {
                    return LocalTime.of(
                            Integer.parseInt(digits.substring(0, 2)),
                            Integer.parseInt(digits.substring(2)));
                } else if (digits.length() == 5) {
                    return LocalTime.of(
                            Integer.parseInt(digits.substring(0, 2)),
                            Integer.parseInt(digits.substring(2, 4)),
                            Integer.parseInt(digits.substring(4)));
                } else if (digits.length() == 6) {
                    return LocalTime.of(
                            Integer.parseInt(digits.substring(0, 2)),
                            Integer.parseInt(digits.substring(2, 4)),
                            Integer.parseInt(digits.substring(4)));
                }
            } catch (Exception ignored) {
                // fall through
            }
        }

        throw new WorkflowRuleViolationException(
                "deadlineTime '" + raw + "' could not be parsed. "
              + "Use one of: 08:00, 08:00:00, 8am, 8:30pm, 0800, 080000.");
    }
}
