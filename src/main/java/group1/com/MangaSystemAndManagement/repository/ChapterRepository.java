package group1.com.MangaSystemAndManagement.repository;

import group1.com.MangaSystemAndManagement.model.Chapter;
import group1.com.MangaSystemAndManagement.model.ChapterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findByChapterStatus(ChapterStatus status);

    List<Chapter> findByProductionPlanId(Long productionPlanId);

    List<Chapter> findByProjectId(Long projectId);

    boolean existsByProductionPlanIdAndChapterStatusNot(Long productionPlanId, group1.com.MangaSystemAndManagement.model.ChapterStatus status);

    /** Chapters belonging to a project filtered by status — used for the publishable-chapters query. */
    List<Chapter> findByProjectIdAndChapterStatus(Long projectId, group1.com.MangaSystemAndManagement.model.ChapterStatus status);

    /** Chapters belonging to a production plan filtered by status — used for plan completion check. */
    List<Chapter> findByProductionPlanIdAndChapterStatus(Long productionPlanId, group1.com.MangaSystemAndManagement.model.ChapterStatus status);

    /**
     * Decision Log 2026-07-27 §AI-08: scheduler query.
     * Returns all chapters in {@code status} whose {@code publishDate} is on or before
     * {@code today}. Used by the auto-publish job to flip SCHEDULED → PUBLISHED.
     */
    List<Chapter> findByChapterStatusAndPublishDateLessThanEqual(ChapterStatus status, LocalDate today);

    /** Technical Spec v2.1 §4.3 (BR-05): chapters of a plan that are NOT in the given status. */
    long countByProductionPlanIdAndChapterStatusNot(Long productionPlanId, ChapterStatus status);

    /** BA V3 §3.1 — list chapters assigned to a specific Mangaka (for "My Chapters" view). */
    List<Chapter> findByAssigneeId(Long assigneeId);
}
