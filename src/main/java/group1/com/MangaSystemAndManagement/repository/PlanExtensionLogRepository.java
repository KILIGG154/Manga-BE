package group1.com.MangaSystemAndManagement.repository;

import group1.com.MangaSystemAndManagement.model.PlanExtensionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanExtensionLogRepository extends JpaRepository<PlanExtensionLog, Long> {
    List<PlanExtensionLog> findByProductionPlanIdOrderByExtendedAtDesc(Long planId);
}