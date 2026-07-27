package group1.com.MangaSystemAndManagement.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Decision Log 2026-07-27 §AI-04:
 * Tantou chủ động chọn Task cần sửa sau Recall/Return.
 * Endpoint: POST /api/workflow/tasks/{taskId}/mark-revision
 *
 * <p>Required: the actor must be the project's Tantou or Board.</p>
 */
@Data
public class MarkTaskRevisionRequest {
    private Long tantouId;

    @Size(max = 1000)
    private String note;
}