package group1.com.MangaSystemAndManagement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Decision Log 2026-07-27 §AI-07 (follow-up):
 * Leader override endpoint để recall Chapter lần 3+ khi đã đạt cap.
 * Endpoint: POST /api/workflow/chapters/{chapterId}/override-recall
 */
@Data
public class OverrideRecallRequest {
    @NotNull
    private Long leaderId;

    @NotNull
    @Size(min = 15, max = 2000, message = "recallReason phải tối thiểu 15 ký tự")
    private String recallReason;
}