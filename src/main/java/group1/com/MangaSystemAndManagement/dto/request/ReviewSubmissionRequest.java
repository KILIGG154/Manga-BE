package group1.com.MangaSystemAndManagement.dto.request;

import group1.com.MangaSystemAndManagement.model.ProductionSubmissionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Body for the Mangaka (or Tantō) approval/rejection endpoint. For a SubTask
 * submission, only the Mangaka may review; for a TASK_LEVEL submission the
 * reviewer must hold the {@code TANTOU_EDITOR} role.
 *
 * <p>The service rejects with HTTP 400 if {@link ProductionSubmissionStatus#REJECTED} is
 * passed without a note. – a rejection must always explain why.</p>
 */
@Getter
@Setter
public class ReviewSubmissionRequest {

    @NotNull(message = "Reviewer account ID is required")
    private Long reviewerId;

    @NotNull(message = "Decision is required (APPROVED|REJECTED)")
    private ProductionSubmissionStatus decision;

    /**
     * Optional for APPROVED. Mandatory for REJECTED – the note is forwarded
     * to the Assistant (or upstream reviewer) so they know what to fix.
     */
    private String note;
}
