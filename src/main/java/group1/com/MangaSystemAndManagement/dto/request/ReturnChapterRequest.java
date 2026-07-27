package group1.com.MangaSystemAndManagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReturnChapterRequest {
    @NotBlank
    @Size(max = 2000)
    private String rejectionReason;
}