package group1.com.MangaSystemAndManagement.dto.response;

import group1.com.MangaSystemAndManagement.model.ChapterComment;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ChapterCommentResponse {
    private Long id;
    private Long chapterId;
    private Long authorId;
    private String authorName;
    private String body;
    private Instant createdAt;

    public static ChapterCommentResponse from(ChapterComment c) {
        ChapterCommentResponse r = new ChapterCommentResponse();
        r.id = c.getId();
        r.chapterId = c.getChapter() != null ? c.getChapter().getId() : null;
        r.authorId = c.getAuthorId();
        r.authorName = c.getAuthorName();
        r.body = c.getBody();
        r.createdAt = c.getCreatedAt();
        return r;
    }
}