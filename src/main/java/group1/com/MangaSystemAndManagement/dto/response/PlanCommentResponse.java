package group1.com.MangaSystemAndManagement.dto.response;

import group1.com.MangaSystemAndManagement.model.PlanComment;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class PlanCommentResponse {
    private Long id;
    private Long planId;
    private Long authorId;
    private String authorName;
    private String body;
    private Instant createdAt;

    public static PlanCommentResponse from(PlanComment c) {
        PlanCommentResponse r = new PlanCommentResponse();
        r.id = c.getId();
        r.planId = c.getProductionPlan() != null ? c.getProductionPlan().getId() : null;
        r.authorId = c.getAuthorId();
        r.authorName = c.getAuthorName();
        r.body = c.getBody();
        r.createdAt = c.getCreatedAt();
        return r;
    }
}