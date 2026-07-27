package group1.com.MangaSystemAndManagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@Entity
@Table(name = "DevelopmentPlan")
public class DevelopmentPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProjectId", nullable = false, unique = true)
    @JsonIgnore
    private Project project;

    @Nationalized
    @Column(name = "StoryDirection", columnDefinition = "nvarchar(max)")
    private String storyDirection;

    @Nationalized
    @Column(name = "WorldSetting", columnDefinition = "nvarchar(max)")
    private String worldSetting;

    @Nationalized
    @Column(name = "MainCharacters", columnDefinition = "nvarchar(max)")
    private String mainCharacters;

    @Nationalized
    @Column(name = "ArcPlanning", columnDefinition = "nvarchar(max)")
    private String arcPlanning;

    @Column(name = "EstimatedVolumes")
    private Integer estimatedVolumes;

    @Column(name = "EstimatedChapters")
    private Integer estimatedChapters;

    @Nationalized
    @Column(name = "TargetAudience", length = 255)
    private String targetAudience;

    @Nationalized
    @Column(name = "ReleaseStrategy", length = 255)
    private String releaseStrategy;

    @Nationalized
    @Column(name = "BusinessGoal", length = 255)
    private String businessGoal;

    @Nationalized
    @Column(name = "Notes", columnDefinition = "nvarchar(max)")
    private String notes;

    @Nationalized
    @Column(name = "ApprovalStatus", length = 50)
    private String approvalStatus;
}
