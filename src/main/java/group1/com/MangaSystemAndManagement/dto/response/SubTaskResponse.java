package group1.com.MangaSystemAndManagement.dto.response;

import group1.com.MangaSystemAndManagement.model.SubTask;
import group1.com.MangaSystemAndManagement.model.SubTaskWorkflowStatus;
import group1.com.MangaSystemAndManagement.model.TaskType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class SubTaskResponse {
    private Long id;
    private Long taskId;
    private Long assigneeId;
    private String assigneeName;
    private String title;
    private String description;
    private SubTaskWorkflowStatus subtaskStatus;
    private LocalDate deadlineDate;
    private LocalTime deadlineTime;
    private Instant createdAt;
    private Instant updatedAt;
    private List<SubmissionFileResponse> submittedFiles;

    /**
     * Map a {@link SubTask} into this DTO. Intentionally leaves
     * {@link #submittedFiles} untouched – callers that need the full file
     * list should populate it explicitly to avoid an N+1 query when the
     * SubTask list is large.
     */
    public static SubTaskResponse from(SubTask st) {
        SubTaskResponse r = new SubTaskResponse();
        r.id = st.getId();
        r.taskId = st.getTask() != null ? st.getTask().getId() : null;
        if (st.getAssignee() != null) {
            r.assigneeId = st.getAssignee().getId();
            r.assigneeName = st.getAssignee().getFirstName() + " " + st.getAssignee().getLastName();
        }
        r.title = st.getTitle();
        r.description = st.getDescription();
        r.subtaskStatus = st.getSubtaskStatus();
        r.deadlineDate = st.getDeadlineDate();
        r.deadlineTime = st.getDeadlineTime();
        r.createdAt = st.getCreatedAt();
        r.updatedAt = st.getUpdatedAt();
        return r;
    }
}