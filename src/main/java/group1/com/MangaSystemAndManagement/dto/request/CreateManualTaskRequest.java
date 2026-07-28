package group1.com.MangaSystemAndManagement.dto.request;

import group1.com.MangaSystemAndManagement.model.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateManualTaskRequest {

    @NotNull(message = "requesterId is required")
    private Long requesterId;

    @NotBlank(message = "title is required")
    @Size(max = 255)
    private String title;

    @Size(max = 4000)
    private String description;

    @Size(max = 4000)
    private String acceptanceCriteria;

    @NotNull(message = "productionTaskType is required")
    private TaskType productionTaskType;

    /** Optional deadline; if set must not exceed the parent Chapter's endDate. */
    private LocalDate deadlineDate;

    /** Optional deadline time; defaults to 23:59 if absent. */
    private LocalTime deadlineTime;
}
