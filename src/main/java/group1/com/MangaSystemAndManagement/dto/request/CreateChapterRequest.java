package group1.com.MangaSystemAndManagement.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateChapterRequest {

    @NotNull(message = "Production plan ID is required")
    private Long planId;

    @NotNull(message = "Chapter number is required")
    private Integer chapterNumber;

    private String title;

    private String status;

    private Integer targetPageCount;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate publishDate;

    @JsonProperty("productionPlanId")
    public void setProductionPlanId(Long productionPlanId) {
        this.planId = productionPlanId;
    }
}
