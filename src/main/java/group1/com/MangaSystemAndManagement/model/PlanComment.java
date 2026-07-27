package group1.com.MangaSystemAndManagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;

/**
 * BA Spec V3 §2.2 + Decision Log 2026-07-27 §AI-05:
 * Comment trao đổi giữa các thành viên khi ProductionPlan ở trạng thái PAUSED
 * (cũng dùng được khi Plan IN_PROGRESS).
 *
 * <p>Không giới hạn viết bởi role nào; chỉ cần là thành viên Project.
 * Comments là append-only — không cho edit/delete để phục vụ audit trail.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "PlanComment")
public class PlanComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    /** FK to ProductionPlan — owning side ignored to avoid recursion. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ProductionPlanId", nullable = false)
    @JsonIgnore
    private ProductionPlan productionPlan;

    /** FK to Account (author). Stored as Long to avoid recursion. */
    @Column(name = "AuthorId", nullable = false)
    private Long authorId;

    /** Author display name snapshot at time of writing (denormalized for FE). */
    @Nationalized
    @Column(name = "AuthorName", length = 255)
    private String authorName;

    /** Comment body. Min 1 char, max 4000. */
    @Nationalized
    @Column(name = "Body", nullable = false, columnDefinition = "nvarchar(max)")
    private String body;

    @Column(name = "CreatedAt", nullable = false)
    private Instant createdAt = Instant.now();
}