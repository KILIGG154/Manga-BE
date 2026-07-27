package group1.com.MangaSystemAndManagement.dto.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * Decision Log 2026-07-27 §AI-01: Release Note is OPTIONAL.
 * All fields except leaderId are optional — UI lets Hội đồng leave them blank.
 */
@Data
public class PublishChapterRequest {
    private Long leaderId;
    private LocalDate publishDate;
    private String releaseNote;
}