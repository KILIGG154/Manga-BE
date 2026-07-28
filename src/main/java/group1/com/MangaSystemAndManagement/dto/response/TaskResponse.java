package group1.com.MangaSystemAndManagement.dto.response;

import group1.com.MangaSystemAndManagement.model.SubTask;
import group1.com.MangaSystemAndManagement.model.Task;
import group1.com.MangaSystemAndManagement.model.TaskType;
import group1.com.MangaSystemAndManagement.model.TaskWorkflowStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class TaskResponse {
    private Long id;
    private String title;
    private TaskType productionTaskType;
    private TaskWorkflowStatus taskWorkflowStatus;
    private String acceptanceCriteria;
    private LocalDate deadlineDate;
    private LocalTime deadlineTime;
    private Long assigneeId;
    private String assigneeName;
    private List<SubTaskResponse> subTasks;
    private Long chapterId;

    public static TaskResponse from(Task t) {
        TaskResponse r = new TaskResponse();
        r.id = t.getId();
        if (t.getChapter() != null) {
            r.chapterId = t.getChapter().getId();
        }
        r.title = t.getTitle();
        r.productionTaskType = t.getProductionTaskType();
        r.taskWorkflowStatus = t.getTaskWorkflowStatus();
        r.acceptanceCriteria = t.getAcceptanceCriteria();
        r.deadlineDate = t.getDeadlineDate();
        r.deadlineTime = t.getDeadlineTime();
        if (t.getAssignee() != null) {
            r.assigneeId = t.getAssignee().getId();
            r.assigneeName = t.getAssignee().getFirstName() + " " + t.getAssignee().getLastName();
        }
        if (t.getSubTasks() != null) {
            r.subTasks = t.getSubTasks().stream()
                    .map(SubTaskResponse::from)
                    .collect(Collectors.toList());
        }
        return r;
    }
}
