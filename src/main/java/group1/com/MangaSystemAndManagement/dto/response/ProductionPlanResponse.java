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
    private String milestones;
    private String schedule;
    private String chapterTimeline;
    private Instant deadline;
    private String resources;
    private Double budget;
    private String assistantAllocation;
    private String priority;
    private String risk;
    // Decision Log 2026-07-27 §AI-10: approvalStatus field removed from ProductionPlan entity.
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalVolumeTarget;
    private PlanStatus planStatus;
    private Integer completionPercentage;
    private List<ChapterResponse> chapters;

    public static ProductionPlanResponse from(ProductionPlan pp) {
        if (pp == null) return null;
        ProductionPlanResponse r = new ProductionPlanResponse();
        r.id = pp.getId();
        if (pp.getProject() != null) {
            r.projectId = pp.getProject().getId();
            r.projectTitle = pp.getProject().getTitle();
        }
        r.milestones = pp.getMilestones();
        r.schedule = pp.getSchedule();
        r.chapterTimeline = pp.getChapterTimeline();
        r.deadline = pp.getDeadline();
        r.resources = pp.getResources();
        r.budget = pp.getBudget();
        r.assistantAllocation = pp.getAssistantAllocation();
        r.priority = pp.getPriority();
        r.risk = pp.getRisk();
        r.startDate = pp.getStartDate();
        r.endDate = pp.getEndDate();
        r.totalVolumeTarget = pp.getTotalVolumeTarget();
        r.planStatus = pp.getPlanStatus();
        r.completionPercentage = pp.getCompletionPercentage();
        if (pp.getChapters() != null) {
            r.chapters = pp.getChapters().stream()
                    .map(ChapterResponse::from)
                    .collect(Collectors.toList());
        }
        return r;
    }
}