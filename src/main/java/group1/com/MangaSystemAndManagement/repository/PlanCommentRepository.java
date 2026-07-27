package group1.com.MangaSystemAndManagement.repository;

import group1.com.MangaSystemAndManagement.model.PlanComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanCommentRepository extends JpaRepository<PlanComment, Long> {
    List<PlanComment> findByProductionPlanIdOrderByCreatedAtAsc(Long planId);
}