package group1.com.MangaSystemAndManagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;
import java.util.List;

/**
 * A submission of work (PSD files etc.) in the production pipeline.
 *
 * <p>Polymorphic over a chapter-level {@link Task} (for {@code TASK_LEVEL} submissions
 * from Mangaka up to Tantō) or a chapter-internal {@link SubTask} (for
 * {@code ROUGH_SKETCH}/{@code REVISION}/{@code FINAL} rounds between Assistant and Mangaka).</p>
 *
 * <p>Either {@code task} or {@code subTask} is populated – never both – enforced at DB
 * level by the {@code CK_Submission_Polymorphic} check constraint (V4 migration). The
 * legacy {@code project} / {@code planning} associations are kept (nullable) for audit
 * purposes and to remain compatible with the older {@code Name Submission} flow.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "Submission")
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    /** Legacy audit column – null for new submissions; not the polymorphic target. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProjectId")
    @JsonIgnore
    private Project project;

    /** Legacy audit column – null for new submissions. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PlanningId")
    @JsonIgnore
    private Planning planning;

    // ------------------------------------------------------------------
    // Polymorphic target – exactly one of the two is set for new rows.
    // ------------------------------------------------------------------

    /** Submission attached to a chapter-level {@link Task}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submittable_task_id")
    @JsonIgnore
    private Task task;

    /** Submission attached to a {@link SubTask}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submittable_subtask_id")
    @JsonIgnore
    private SubTask subTask;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SubmittedBy", nullable = false)
    @JsonIgnore
    private Account submittedBy;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Title")
    private String title;

    @Nationalized
    @Column(name = "Story", columnDefinition = "nvarchar(max)")
    private String story;

    @Nationalized
    @Column(name = "CharacterDescription", columnDefinition = "nvarchar(max)")
    private String characterDescription;

    @Nationalized
    @Column(name = "WorldSetting", columnDefinition = "nvarchar(max)")
    private String worldSetting;

    /** Free-form text content/notes (was the only audit field in the legacy schema). */
    @Size(max = 1000)
    @Nationalized
    @Column(name = "ContentUrl", length = 1000)
    private String contentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "name_status", length = 50)
    private NameSubmissionStatus nameStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "production_status", length = 50)
    private ProductionSubmissionStatus productionStatus;

    /** Workflow classifier – see {@link SubmissionType}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", length = 50, nullable = false)
    private SubmissionType submissionType;

    /** For {@link SubmissionType#REVISION} – points to the most recent REJECTED parent. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_submission_id")
    @JsonIgnore
    private Submission parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    @JsonIgnore
    private Account reviewer;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ColumnDefault("getdate()")
    @Column(name = "SubmittedAt")
    private Instant submittedAt;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubmissionFile> files;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Feedback> feedbacks;
}
