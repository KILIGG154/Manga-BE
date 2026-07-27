package group1.com.MangaSystemAndManagement.dto.response;

import group1.com.MangaSystemAndManagement.model.ReviewStage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionReviewResponse {
    private Long id;
    private Long submissionId;
    private Long reviewerId;
    private String reviewerEmail;
    private String reviewerName;
    private ReviewStage stage;
    private String decision;
    private String comment;
    private Instant reviewedAt;

    public static SubmissionReviewResponse from(group1.com.MangaSystemAndManagement.model.SubmissionReview sr) {
        SubmissionReviewResponse r = new SubmissionReviewResponse();
        r.setId(sr.getId());
        if (sr.getSubmission() != null) {
            r.setSubmissionId(sr.getSubmission().getId());
        }
        if (sr.getReviewer() != null) {
            r.setReviewerId(sr.getReviewer().getId());
            r.setReviewerEmail(sr.getReviewer().getEmail());
            r.setReviewerName(sr.getReviewer().getFirstName() + " " + sr.getReviewer().getLastName());
        }
        r.setStage(sr.getStage());
        r.setDecision(sr.getDecision());
        r.setComment(sr.getComment());
        r.setReviewedAt(sr.getReviewedAt());
        return r;
    }
}
