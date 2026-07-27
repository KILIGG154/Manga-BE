package group1.com.MangaSystemAndManagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForceClosePlanRequest {
    @NotBlank
    @Size(max = 1000)
    private String reason;
}