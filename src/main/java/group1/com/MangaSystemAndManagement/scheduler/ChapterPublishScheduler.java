package group1.com.MangaSystemAndManagement.scheduler;

import group1.com.MangaSystemAndManagement.service.interfaces.ProductionWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Decision Log 2026-07-27 §AI-08:
 * Spring {@code @Scheduled} job that periodically asks the workflow service to
 * publish any {@code SCHEDULED} chapter whose {@code publishDate} is on or before today.
 *
 * <p>Schedule: every 5 minutes by default (configurable via
 * {@code manga.publish.cron} in {@code application.properties}).</p>
 *
 * <p>Best-effort: failures on individual chapters are logged and the loop continues.</p>
 */
@Component
public class ChapterPublishScheduler {

    private static final Logger log = LoggerFactory.getLogger(ChapterPublishScheduler.class);

    private final ProductionWorkflowService workflowService;

    public ChapterPublishScheduler(ProductionWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    /**
     * Cron-based entry point. Default: every 5 minutes.
     * Override in application.properties:
     * <pre>
     * manga.publish.cron=0 0/5 * * * *
     * </pre>
     */
    @Scheduled(cron = "${manga.publish.cron:0 0/5 * * * *}")
    public void run() {
        try {
            int count = workflowService.publishDueScheduledChapters();
            if (count > 0) {
                log.info("[AI-08] Auto-published {} scheduled chapter(s)", count);
            } else {
                log.debug("[AI-08] No due scheduled chapters found");
            }
        } catch (Exception e) {
            log.error("[AI-08] Scheduler failed: {}", e.getMessage(), e);
        }
    }
}