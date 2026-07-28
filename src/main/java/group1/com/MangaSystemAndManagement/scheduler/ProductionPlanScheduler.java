package group1.com.MangaSystemAndManagement.scheduler;

import group1.com.MangaSystemAndManagement.service.interfaces.ProductionPlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Technical Spec v2.1 §5 — Production Plan cronjobs.
 *
 * <ul>
 *   <li>{@code markOverduePlans()} — runs daily at 00:01. Flips ACTIVE/EXTENDED Plans
 *       whose {@code endDate} is before today into OVERDUE.</li>
 *   <li>{@code promoteDraftPlansToActive()} — runs daily at 00:05. Flips DRAFT
 *       Plans whose {@code startDate} is on or before today into ACTIVE.</li>
 * </ul>
 */
@Component
public class ProductionPlanScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProductionPlanScheduler.class);

    private final ProductionPlanService service;

    public ProductionPlanScheduler(ProductionPlanService service) {
        this.service = service;
    }

    @Scheduled(cron = "${manga.plan.overdue-cron:0 1 0 * * *}")
    public void runOverdue() {
        try {
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            int count = service.markOverduePlans(today);
            if (count > 0) log.warn("[Plan v2.1] Marked {} plan(s) as OVERDUE", count);
        } catch (Exception e) {
            log.error("[Plan v2.1] Overdue cronjob failed: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "${manga.plan.activate-cron:0 5 0 * * *}")
    public void runActivate() {
        try {
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            int count = service.promoteDraftPlansToActive(today);
            if (count > 0) log.info("[Plan v2.1] Promoted {} DRAFT plan(s) to ACTIVE", count);
        } catch (Exception e) {
            log.error("[Plan v2.1] Activate cronjob failed: {}", e.getMessage(), e);
        }
    }
}