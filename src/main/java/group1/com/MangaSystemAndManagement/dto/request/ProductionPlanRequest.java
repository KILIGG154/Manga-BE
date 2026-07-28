package group1.com.MangaSystemAndManagement.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductionPlanRequest {
    private String milestones;
    private String chapterTimeline;
    private Instant deadline;
    private String priority;
}
