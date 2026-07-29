package group1.com.MangaSystemAndManagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A production Task generated automatically for each Chapter.
 * Four Tasks are created per Chapter (NAME_WIP, LINEART, INKING, BACKGROUND).
 *
 * <p>Legacy fields ({@code status}, {@code assignedTo}, {@code deadline},
 * {@code taskType}, {@code page}) have been removed. Use the production
 * workflow fields below.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "Task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ChapterId")
    @JsonIgnore
    private Chapter chapter;

    @Nationalized
    @jakarta.persistence.Lob
    @Column(name = "Title")
    private String title;

    @Nationalized
    @jakarta.persistence.Lob
    @Column(name = "Description")
    private String description;

    // --- Production Workflow Fields ---

    /** The type of production work this task represents. */
    @Enumerated(EnumType.STRING)
    @Column(name = "production_task_type", length = 50)
    private TaskType productionTaskType;

    @jakarta.persistence.Lob
    @Column(name = "acceptance_criteria")
    private String acceptanceCriteria;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_status", length = 50)
    private TaskWorkflowStatus taskWorkflowStatus = TaskWorkflowStatus.TODO;

    /** The Mangaka assigned to this task. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    @JsonIgnore
    private Account assignee;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Feedback> feedbacks;

    /**
     * Rolled-up completion percentage [0, 100] — recomputed whenever a
     * SubTask status changes. Number of COMPLETED SubTasks / total SubTask count.
     */
    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<SubTask> subTasks;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Submission> submissions;

    /** Deadline date for this task (must not exceed the parent Chapter's endDate). */
    @Column(name = "deadline_date")
    private LocalDate deadlineDate;

    /** Optional time component of the deadline; defaults to end-of-day if absent. */
    @Column(name = "deadline_time")
    private LocalTime deadlineTime;
}
