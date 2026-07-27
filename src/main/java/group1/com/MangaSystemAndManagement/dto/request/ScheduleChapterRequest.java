package group1.com.MangaSystemAndManagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * Decision Log 2026-07-27 §AI-08:
 * Body for POST /api/workflow/chapters/{chapterId}/schedule
 *
 * <p>Đặt lịch xuất bản: chapter COMPLETED → SCHEDULED, scheduler tự động
 * SCHEDULED → PUBLISHED khi publishDate đến.</p>
 */
@Data
public class ScheduleChapterRequest {

    @NotNull
    private Long schedulerId;   // Tantou/Board/Leader gọi endpoint

    @NotNull
    private LocalDate publishDate;
}