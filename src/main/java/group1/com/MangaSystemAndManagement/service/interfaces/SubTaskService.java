package group1.com.MangaSystemAndManagement.service.interfaces;

import group1.com.MangaSystemAndManagement.dto.request.CreateSubTaskRequest;
import group1.com.MangaSystemAndManagement.dto.response.SubTaskResponse;

import java.util.List;

public interface SubTaskService {

    /**
     * A Mangaka (or Tantō) splits a chapter-level Task into smaller SubTasks
     * for an Assistant. Deadline is validated against the parent task.
     */
    SubTaskResponse createSubTask(Long taskId, CreateSubTaskRequest req);

    /** All sub-tasks attached to a given chapter-level task. */
    List<SubTaskResponse> listByTask(Long taskId, Long requesterId);

    /**
     * Sub-tasks assigned to a specific Assistant. Security check:
     * non-Mangaka/non-Tantō callers may only query their own id.
     */
    List<SubTaskResponse> listByAssignee(Long assigneeId, Long requesterId);

    /**
     * Convenience overload for the Assistant dashboard: the caller passes
     * only their own {@code userId}. The service verifies the account exists
     * and holds the {@code ASSISTANT} role before returning the list, so
     * no separate {@code requesterId} is required.
     */
    List<SubTaskResponse> listByAssistant(Long assistantId);

    SubTaskResponse getById(Long id, Long requesterId);

    /**
     * Move a COMPLETED SubTask back to NEEDS_REVISION so the Assistant can
     * be asked for further changes.
     */
    SubTaskResponse reopen(Long id, Long requesterId);
}
