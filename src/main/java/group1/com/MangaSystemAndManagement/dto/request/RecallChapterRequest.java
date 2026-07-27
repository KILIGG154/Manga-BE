package group1.com.MangaSystemAndManagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecallChapterRequest {
    @NotBlank
    @Size(min = 15, max = 2000)
    private String recallReason;
}