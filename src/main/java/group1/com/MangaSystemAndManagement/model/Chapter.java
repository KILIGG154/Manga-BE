package group1.com.MangaSystemAndManagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "Chapter")
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "ChapterNumber")
    private Integer chapterNumber;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Title")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 50)
    private ChapterStatus chapterStatus;

    @Column(name = "TargetPageCount")
    private Integer targetPageCount;

    @Column(name = "StartDate")
    private LocalDate startDate;

    @Column(name = "EndDate")
    private LocalDate endDate;

    @Column(name = "PublishDate")
    private LocalDate publishDate;

    @Column(name = "Pages")
    private Integer pages;

    @Column(name = "Deadline")
    private Instant deadline;

    @Size(max = 50)
    @Nationalized
    @Column(name = "Priority", length = 50)
    private String priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OwnerId")
    private Account owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProjectId")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductionPlanId")
    @JsonIgnore
    private ProductionPlan productionPlan;


    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks;

    // --- Decision Log 2026-07-27 §AI-05 + §AI-12: Chapter comments ---

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChapterComment> comments;

    // --- BA V3 §3.4: Recall tracking ---

    /** How many times this chapter has been recalled after publishing. */
    @Column(name = "recall_count")
    private Integer recallCount = 0;

    /** Reason for the most recent recall; required on every recall. */
    @Nationalized
    @Column(name = "recall_reason", columnDefinition = "nvarchar(max)")
    private String recallReason;

    // --- BA V3 §3.3: Rejection tracking ---

    /** Number of times Hội đồng has returned this chapter to production. Capped at 2 auto-return. */
    @Column(name = "rejection_count")
    private Integer rejectionCount = 0;

    /** Reason for the most recent rejection. */
    @Nationalized
    @Column(name = "rejection_reason", columnDefinition = "nvarchar(max)")
    private String rejectionReason;

    // --- BA V3 §3.1: Publish audit ---

    /** User who actually clicked "Publish" (can be LEADER or BOARD). */
    @Column(name = "published_by")
    private Long publishedBy;

    /** Timestamp of the publish action. */
    @Column(name = "published_at")
    private Instant publishedAt;

    // --- Decision Log 2026-07-27 AI-01: Release Note (optional) ---

    /**
     * Optional release note set by Hội đồng when clicking Publish.
     * Nullable — UI lets it blank. Stored as Nationalized Text for VN content.
     */
    @Nationalized
    @Column(name = "release_note", columnDefinition = "nvarchar(max)")
    private String releaseNote;

    // --- BA V3 §3.2: Optimistic locking ---

    @Version
    @Column(name = "version")
    private Long version;
}