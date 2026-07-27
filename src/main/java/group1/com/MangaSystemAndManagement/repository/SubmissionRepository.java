package group1.com.MangaSystemAndManagement.repository;

import group1.com.MangaSystemAndManagement.model.Submission;
import group1.com.MangaSystemAndManagement.model.ProductionSubmissionStatus;
import group1.com.MangaSystemAndManagement.model.SubmissionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    /** Legacy helper used by the existing SubmissionController.findAll endpoint. */
    @Query("SELECT s FROM Submission s LEFT JOIN FETCH s.submittedBy")
    List<Submission> findAllWithSubmittedBy();

    /**
     * All submissions for a SubTask, newest first.
     * Sorted by {@code submittedAt} (no separate version column required).
     */
    List<Submission> findBySubTaskIdOrderBySubmittedAtDesc(Long subTaskId);

    /** First page of submissions for a SubTask (newest first). */
    List<Submission> findBySubTaskIdOrderBySubmittedAtDesc(Long subTaskId, Pageable pageable);

    /** All TASK_LEVEL submissions for a parent Task, newest first. */
    List<Submission> findByTaskIdOrderBySubmittedAtDesc(Long taskId);

    /**
     * Count how many rounds already exist for a given polymorphic target.
     * Returns 0 for a brand-new target. Used to enforce "first submission must
     * be ROUGH_SKETCH" without needing a separate {@code version} column.
     * Caller MUST pass exactly one of {@code taskId} / {@code subTaskId}.
     */
    @Query("""
           SELECT COUNT(s) FROM Submission s
           WHERE ( :taskId    IS NOT NULL AND s.task.id    = :taskId
                   AND :subTaskId IS NULL )
              OR ( :subTaskId IS NOT NULL AND s.subTask.id = :subTaskId
                   AND :taskId    IS NULL )
           """)
    long countByTarget(@Param("taskId") Long taskId,
                       @Param("subTaskId") Long subTaskId);

    /** Latest submission of a given type on a SubTask, if any. */
    @Query("""
           SELECT s FROM Submission s
           WHERE s.subTask.id = :subTaskId AND s.submissionType = :type
           ORDER BY s.submittedAt DESC
           """)
    List<Submission> findLatestBySubTaskAndType(@Param("subTaskId") Long subTaskId,
                                                @Param("type") SubmissionType type);

    /**
     * The most recent {@code REJECTED} submission on a SubTask. Used to attach
     * a new {@code REVISION} round to its rejected parent. Sort uses
     * {@code submittedAt} – no separate version column required.
     */
    Optional<Submission> findFirstBySubTaskIdAndProductionStatusOrderBySubmittedAtDesc(Long subTaskId,
                                                                             ProductionSubmissionStatus status);
}
