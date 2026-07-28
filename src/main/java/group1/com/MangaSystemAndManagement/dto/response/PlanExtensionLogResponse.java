package group1.com.MangaSystemAndManagement.dto.response;

import group1.com.MangaSystemAndManagement.model.PlanExtensionLog;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
public class PlanExtensionLogResponse {
    private Long id;
    private Long planId;
    private LocalDate oldEndDate;
    private LocalDate newEndDate;
    private String reasonCode;
    private String reasonNote;
    private Long extendedBy;
    private Instant extendedAt;

    public static PlanExtensionLogResponse from(PlanExtensionLog log) {
        if (log == null) return null;
        PlanExtensionLogResponse r = new PlanExtensionLogResponse();
        r.id = log.getId();
        r.planId = log.getProductionPlan() != null ? log.getProductionPlan().getId() : null;
        r.oldEndDate = log.getOldEndDate();
        r.newEndDate = log.getNewEndDate();
        r.reasonCode = log.getReasonCode();
        r.reasonNote = log.getReasonNote();
        r.extendedBy = log.getExtendedBy();
        r.extendedAt = log.getExtendedAt();
        return r;
    }
}