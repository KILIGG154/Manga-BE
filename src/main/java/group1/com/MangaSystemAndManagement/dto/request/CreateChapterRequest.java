package group1.com.MangaSystemAndManagement.dto.request;

import group1.com.MangaSystemAndManagement.model.ChapterStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
public class CreateChapterRequest {

    @NotNull(message = "Production plan ID is required")
    private Long planId;

    @NotNull(message = "Chapter number is required")
    private Integer chapterNumber;

    private String title;

    @NotNull(message = "Chapter status is required")
    private ChapterStatus chapterStatus;

    private Integer targetPageCount;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate publishDate;

    private Instant deadline;

    private String priority;
}
