package group1.com.MangaSystemAndManagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Decision Log 2026-07-27 §AI-05 + §AI-12:
 * Body for POST /api/workflow/chapters/{chapterId}/comments
 */
@Data
public class CreateChapterCommentRequest {

    @NotNull
    private Long authorId;

    @NotBlank
    @Size(min = 1, max = 4000, message = "Comment body từ 1-4000 ký tự")
    private String body;
}