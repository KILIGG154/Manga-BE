package group1.com.MangaSystemAndManagement.repository;

import group1.com.MangaSystemAndManagement.model.PlanStatus;
import group1.com.MangaSystemAndManagement.model.ProductionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionPlanRepository extends JpaRepository<ProductionPlan, Long> {

    Optional<ProductionPlan> findByProjectId(Long projectId);

    List<ProductionPlan> findByProjectIdOrderByStartDateDesc(Long projectId);

    boolean existsByProjectIdAndTitle(Long projectId, String title);

    List<ProductionPlan> findByPlanStatusAndStartDateLessThanEqual(PlanStatus status, LocalDate date);

    List<ProductionPlan> findByPlanStatusInAndEndDateBefore(Collection<PlanStatus> statuses, LocalDate date);

    @Query("select distinct p from ProductionPlan p left join fetch p.chapters")
    List<ProductionPlan> findAllWithChapters();
}