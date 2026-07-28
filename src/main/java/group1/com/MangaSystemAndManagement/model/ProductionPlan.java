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
    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "deadline_date")
    private LocalDate deadlineDate;

    @Column(name = "publish_date")
    private LocalDate publishDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "total_volume_target")
    private Integer totalVolumeTarget;

    @Column(name = "plan_status", length = 50)
    private PlanStatus planStatus = PlanStatus.DRAFT;

    @Column(name = "completion_percentage")
    private Integer completionPercentage = 0;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "productionPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Chapter> chapters;

    @OneToMany(mappedBy = "productionPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanComment> comments;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (planStatus == null) planStatus = PlanStatus.DRAFT;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isActive() {
        return planStatus == PlanStatus.ACTIVE
                || planStatus == PlanStatus.EXTENDED
                || planStatus == PlanStatus.OVERDUE;
    }
}