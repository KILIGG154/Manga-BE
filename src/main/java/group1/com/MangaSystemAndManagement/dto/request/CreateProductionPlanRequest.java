package group1.com.MangaSystemAndManagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Technical Spec v2.1 §4.1 — Input for creating a Production Plan.
 *
 * <p>Server-side derivations:
 * <ul>
 *   <li>{@code title} is suggested by the backend as {@code "[Project] - Production Plan MM/YYYY"} if absent.</li>
 *   <li>{@code planStatus} is derived from {@code startDate} vs. today (BR-02).</li>
 *   <li>{@code createdBy} is taken from the authenticated principal.</li>
 * </ul>
 */
@Getter
@Setter
public class CreateProductionPlanRequest {

    @NotNull(message = "Title is required")
    private String title;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Deadline date is required")
    private LocalDate deadlineDate;

    @NotNull(message = "Publish date is required")
    private LocalDate publishDate;
}