package group1.com.MangaSystemAndManagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Sub-task created by a Mangaka from a chapter-level {@link Task} and assigned
 * to an Assistant. Carries its own date+time deadline which MUST NOT exceed the
 * parent task deadline, and an internal version counter for optimistic locking
 * on reopened updates.
 *
 * <p>Table: {@code SubTask} (see V4 migration).</p>
 */
@Getter
@Setter
@Entity
@Table(name = "SubTask")
public class SubTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_task_id", nullable = false)
    @JsonIgnore
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    @JsonIgnore
    private Account assignee;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "nvarchar(max)")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "subtask_status", length = 50, nullable = false)
    private SubTaskWorkflowStatus subtaskStatus = SubTaskWorkflowStatus.TODO;

    @Column(name = "deadline_date")
    private LocalDate deadlineDate;

    @Column(name = "deadline_time")
    private LocalTime deadlineTime;

    /** Optimistic-lock version (separate from any submission version). */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @OneToMany(mappedBy = "subTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Submission> submissions;

    // ------------------------------------------------------------------
    // Transient helpers – never persisted.
    // ------------------------------------------------------------------

    /**
     * Composite deadline (date + time, defaulting to end-of-day if time missing).
     *
     * <p>The instant is anchored at UTC ({@link ZoneOffset#UTC}) rather than
     * {@link ZoneId#systemDefault()} so the same wall-clock deadline produced on
     * a developer laptop in UTC+7 and a server in UTC does not drift. Without
     * this, two clients viewing the same {@code SubTask} would see different
     * timestamps in the JSON {@code deadlineInstant} field.</p>
     */
    @Transient
    public Instant getDeadlineInstant() {
        if (deadlineDate == null) {
            return null;
        }
        LocalTime t = deadlineTime != null ? deadlineTime : LocalTime.of(23, 59, 59);
        return deadlineDate.atTime(t).atOffset(ZoneOffset.UTC).toInstant();
    }

    /** True when the SubTask has not been COMPLETED and the deadline has elapsed. */
    @Transient
    public boolean isOverdue() {
        Instant d = getDeadlineInstant();
        return d != null
                && subtaskStatus != SubTaskWorkflowStatus.COMPLETED
                && d.isBefore(Instant.now());
    }
}
