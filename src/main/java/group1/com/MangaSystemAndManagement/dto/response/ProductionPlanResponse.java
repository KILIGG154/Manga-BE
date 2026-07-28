package group1.com.MangaSystemAndManagement.dto.response;

import group1.com.MangaSystemAndManagement.model.PlanStatus;
import group1.com.MangaSystemAndManagement.model.ProductionPlan;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ProductionPlanResponse {
    private Long id;
    private Long projectId;
    private String projectTitle;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate deadlineDate;
    private LocalDate publishDate;
    private LocalDate actualEndDate;
    private Integer totalVolumeTarget;
    private PlanStatus planStatus;
    private Integer completionPercentage;
    private Long createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ChapterResponse> chapters;
    private List<PlanExtensionLogResponse> extensionLogs;

    public static ProductionPlanResponse from(ProductionPlan pp) {
        if (pp == null) return null;
        ProductionPlanResponse r = new ProductionPlanResponse();
        r.id = pp.getId();
        if (pp.getProject() != null) {
            r.projectId = pp.getProject().getId();
            r.projectTitle = pp.getProject().getTitle();
        }
        r.title = pp.getTitle();
        r.startDate = pp.getStartDate();
        r.endDate = pp.getEndDate();
        r.deadlineDate = pp.getDeadlineDate();
        r.publishDate = pp.getPublishDate();
        r.actualEndDate = pp.getActualEndDate();
        r.totalVolumeTarget = pp.getTotalVolumeTarget();
        r.planStatus = pp.getPlanStatus();
        r.completionPercentage = pp.getCompletionPercentage();
        r.createdBy = pp.getCreatedBy();
        r.createdAt = pp.getCreatedAt();
        r.updatedAt = pp.getUpdatedAt();
        if (pp.getChapters() != null) {
            r.chapters = pp.getChapters().stream()
                    .map(ChapterResponse::from)
                    .collect(Collectors.toList());
        }
        return r;
    }
}