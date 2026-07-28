package group1.com.MangaSystemAndManagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Technical Spec v2.1 §4.2 — Body of {@code POST /api/v1/production-plans/{planId}/extend}.
 */
@Getter
@Setter
public class ExtendProductionPlanRequest {

    @NotNull(message = "New end date is required")
    private LocalDate newEndDate;

    @NotNull(message = "Reason code is required")
    private String reasonCode;

    private String reasonNote;
}