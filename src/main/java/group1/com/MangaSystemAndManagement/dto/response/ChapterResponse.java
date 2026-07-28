package group1.com.MangaSystemAndManagement.dto.response;

import group1.com.MangaSystemAndManagement.model.Chapter;
import group1.com.MangaSystemAndManagement.model.ChapterStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ChapterResponse {
    private Long id;
    private Integer chapterNumber;
    private String title;
    private Integer targetPageCount;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate publishDate;
    private Instant deadline;
    private String priority;
    private ChapterStatus chapterStatus;
    private Long projectId;
    private Long planId;
    private Long ownerId;
    private String ownerName;
    private Long assigneeId;
    private String assigneeName;
    private String releaseNote;
    private Integer recallCount;
    private String recallReason;
    private Integer rejectionCount;
    private String rejectionReason;
    private Long publishedBy;
    private Instant publishedAt;
    private List<TaskResponse> tasks;

    public static ChapterResponse from(Chapter c) {
        ChapterResponse r = new ChapterResponse();
        r.id = c.getId();
        r.chapterNumber = c.getChapterNumber();
        r.title = c.getTitle();
        r.targetPageCount = c.getTargetPageCount();
        r.startDate = c.getStartDate();
        r.endDate = c.getEndDate();
        r.publishDate = c.getPublishDate();
        r.deadline = c.getDeadline();
        r.priority = c.getPriority();
        r.chapterStatus = c.getChapterStatus();
        r.releaseNote = c.getReleaseNote();
        r.recallCount = c.getRecallCount();
        r.recallReason = c.getRecallReason();
        r.rejectionCount = c.getRejectionCount();
        r.rejectionReason = c.getRejectionReason();
        r.publishedBy = c.getPublishedBy();
        r.publishedAt = c.getPublishedAt();
        if (c.getProject() != null) r.projectId = c.getProject().getId();
        if (c.getProductionPlan() != null) r.planId = c.getProductionPlan().getId();
        if (c.getOwner() != null) {
            r.ownerId = c.getOwner().getId();
            r.ownerName = c.getOwner().getFirstName() + " " + c.getOwner().getLastName();
        }
        if (c.getAssignee() != null) {
            r.assigneeId = c.getAssignee().getId();
            r.assigneeName = c.getAssignee().getFirstName() + " " + c.getAssignee().getLastName();
        }
        if (c.getTasks() != null) {
            r.tasks = c.getTasks().stream()
                    .map(TaskResponse::from)
                    .collect(Collectors.toList());
        }
        return r;
    }
}
