package group1.com.MangaSystemAndManagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "Project")
@JsonIgnoreProperties({"productionPlan"})
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Nationalized
    @Column(name = "Title", nullable = false)
    private String title;

    @Nationalized
    @Column(name = "Description", columnDefinition = "nvarchar(max)")
    private String description;

    @Size(max = 50)
    @Nationalized
    @Column(name = "Status", length = 50)
    private String status;

    @ColumnDefault("getdate()")
    @Column(name = "CreatedAt")
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OwnerId")
    @JsonIgnore
    private Account owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TantouId")
    @JsonIgnore
    private Account tantou;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MangakaId")
    @JsonIgnore
    private Account mangaka;

    @Column(name = "StartDate")
    private Instant startDate;

    @Column(name = "ExpectedEndDate")
    private Instant expectedEndDate;

    @Size(max = 50)
    @Nationalized
    @Column(name = "CurrentPhase", length = 50)
    private String currentPhase;

    // --- Production Workflow Fields ---

    @Size(max = 100)
    @Nationalized
    @Column(name = "genre", length = 100)
    private String genre;

    @Size(max = 100)
    @Nationalized
    @Column(name = "target_audience", length = 100)
    private String targetAudience;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", length = 50)
    private ProjectFormat format;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_status", length = 50)
    private ProjectWorkflowStatus projectWorkflowStatus = ProjectWorkflowStatus.DRAFT;

    @OneToOne(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DevelopmentPlan developmentPlan;

    @JsonIgnore
    @OneToOne(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ProductionPlan productionPlan;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<Asset> assets;
}