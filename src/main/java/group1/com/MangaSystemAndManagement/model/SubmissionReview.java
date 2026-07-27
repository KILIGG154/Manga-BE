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

@Getter
@Setter
@Entity
@Table(name = "SubmissionReview")
public class SubmissionReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SubmissionId", nullable = false)
    @JsonIgnore
    private Submission submission;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ReviewerId", nullable = false)
    @JsonIgnore
    private Account reviewer;

    @Enumerated(EnumType.STRING)
    @Column(name = "Stage", length = 50)
    private ReviewStage stage;

    @Size(max = 50)
    @Nationalized
    @Column(name = "Decision", length = 50)
    private String decision;

    @Nationalized
    @Column(name = "Comment", columnDefinition = "nvarchar(max)")
    private String comment;

    @ColumnDefault("getdate()")
    @Column(name = "ReviewedAt")
    private Instant reviewedAt;

}
