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
    private LocalDate publishDate;
    private ChapterStatus chapterStatus;
    private Long projectId;
    private Long ownerId;
    private String ownerName;
    private String releaseNote;
    private List<TaskResponse> tasks;

    public static ChapterResponse from(Chapter c) {
        ChapterResponse r = new ChapterResponse();
        r.id = c.getId();
        r.chapterNumber = c.getChapterNumber();
        r.title = c.getTitle();
        r.targetPageCount = c.getTargetPageCount();
        r.publishDate = c.getPublishDate();
        r.chapterStatus = c.getChapterStatus();
        r.releaseNote = c.getReleaseNote();
        if (c.getProject() != null) r.projectId = c.getProject().getId();
        if (c.getOwner() != null) {
            r.ownerId = c.getOwner().getId();
            r.ownerName = c.getOwner().getFirstName() + " " + c.getOwner().getLastName();
        }
        if (c.getTasks() != null) {
            r.tasks = c.getTasks().stream()
                    .map(TaskResponse::from)
                    .collect(Collectors.toList());
        }
        return r;
    }
}
