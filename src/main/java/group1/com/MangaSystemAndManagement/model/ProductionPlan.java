package group1.com.MangaSystemAndManagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "ProductionPlan")
public class ProductionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProjectId", nullable = false, unique = true)
    @JsonIgnore
    private Project project;

    @Nationalized
    @Column(name = "Milestones", columnDefinition = "nvarchar(max)")
    private String milestones;

    @Nationalized
    @Column(name = "Schedule", columnDefinition = "nvarchar(max)")
    private String schedule;

    @Nationalized
    @Column(name = "ChapterTimeline", columnDefinition = "nvarchar(max)")
    private String chapterTimeline;

    @Column(name = "Deadline")
    private Instant deadline;

    @Nationalized
    @Column(name = "Resources", columnDefinition = "nvarchar(max)")
    private String resources;

    @Column(name = "Budget")
    private Double budget;

    @Nationalized
    @Column(name = "AssistantAllocation", columnDefinition = "nvarchar(max)")
    private String assistantAllocation;

    @Nationalized
    @Column(name = "Priority", length = 50)
    private String priority;

    @Nationalized
    @Column(name = "Risk", columnDefinition = "nvarchar(max)")
    private String risk;

    // Decision Log 2026-07-27 §AI-10: removed `approval_status` column.
    // Field used to be PlanApprovalStatus enum; replaced by planStatus flow.
    // Migration: V2026_07_27__drop_production_plan_approval_status_column.

    // --- Production Workflow Fields ---

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "total_volume_target")
    /**
     * Decision Log 2026-07-27 §AI-03: targetChapterCount (called {@code total_volume_target}
     * in DB) is treated as a DASHBOARD measurement only — it does NOT hard-block
     * auto-complete of the ProductionPlan. Auto-complete runs as soon as every existing
     * chapter of the Plan is PUBLISHED (dynamic).
     */
    private Integer totalVolumeTarget;

    // Note: @Converter(autoApply = true) in PlanStatusConverter handles DB mapping.
    // No @Enumerated needed — avoids double conversion.
    @Column(name = "plan_status", length = 50)
    private PlanStatus planStatus = PlanStatus.IN_PROGRESS;

    /**
     * Rolled-up completion across all chapters of this Plan (0–100).
     * Recomputed every time a chapter transitions to COMPLETED/PUBLISHED.
     */
    @Column(name = "completion_percentage")
    private Integer completionPercentage = 0;

    // --- Pause / Resume fields (BA V3 §2.2) ---

    /** User ID who paused the plan; null when not paused. */
    @Column(name = "paused_by")
    private Long pausedBy;

    /** Timestamp when the plan was last paused; null when not paused. */
    @Column(name = "paused_at")
    private Instant pausedAt;

    /** Reason for the most recent pause; reset to NULL on Resume. */
    @Nationalized
    @Column(name = "pause_reason", columnDefinition = "nvarchar(max)")
    private String pauseReason;

    @OneToMany(mappedBy = "productionPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Chapter> chapters;

    // --- Decision Log 2026-07-27 §AI-05: Comments (dùng khi plan PAUSED) ---

    @OneToMany(mappedBy = "productionPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanComment> comments;

    // --- Decision Log 2026-07-27 §AI-11: helper ---

    /**
     * Returns {@code true} when the Plan is in a state where chapters/tasks can be
     * actively produced — i.e. {@link PlanStatus#IN_PROGRESS} or {@link PlanStatus#PAUSED}.
     * PAUSED is still "active" in the sense that the Plan exists and is open; mutations
     * are blocked by {@code assertPlanNotPaused} but the dashboard keeps showing it.
     */
    public boolean isActive() {
        return planStatus == PlanStatus.IN_PROGRESS || planStatus == PlanStatus.PAUSED;
    }
}
