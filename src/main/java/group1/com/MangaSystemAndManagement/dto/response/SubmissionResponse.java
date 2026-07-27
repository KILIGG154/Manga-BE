package group1.com.MangaSystemAndManagement.dto.response;

import group1.com.MangaSystemAndManagement.model.Submission;
import group1.com.MangaSystemAndManagement.model.NameSubmissionStatus;
import group1.com.MangaSystemAndManagement.model.ProductionSubmissionStatus;
import group1.com.MangaSystemAndManagement.model.SubmissionType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class SubmissionResponse {
    private Long id;
    /** Polymorphic target – exactly one of these is populated. */
    private Long taskId;
    private Long subTaskId;
    private SubmissionType submissionType;
    private Long parentSubmissionId;
    private NameSubmissionStatus nameStatus;
    private ProductionSubmissionStatus productionStatus;
    private String title;
    private String genre;
    private String targetAudience;
    private group1.com.MangaSystemAndManagement.model.ProjectFormat format;
    private String note;
    private Long submittedById;
    private String submittedByName;
    private Instant submittedAt;
    private Long reviewerId;
    private String reviewerName;
    private Instant reviewedAt;
    private List<SubmissionFileResponse> files;
    private Integer fileCount;

    private List<SubmissionReviewResponse> reviews;

    public static SubmissionResponse from(Submission s) {
        SubmissionResponse r = new SubmissionResponse();
        r.id = s.getId();
        r.taskId = s.getTask() != null ? s.getTask().getId() : null;
        r.subTaskId = s.getSubTask() != null ? s.getSubTask().getId() : null;
        r.submissionType = s.getSubmissionType();
        r.parentSubmissionId = s.getParent() != null ? s.getParent().getId() : null;
        r.nameStatus = s.getNameStatus();
        r.productionStatus = s.getProductionStatus();
        r.title = s.getTitle();
        r.genre = s.getGenre();
        r.targetAudience = s.getTargetAudience();
        r.format = s.getFormat();
        r.note = s.getContentUrl();
        if (s.getSubmittedBy() != null) {
            r.submittedById = s.getSubmittedBy().getId();
            r.submittedByName =
                    s.getSubmittedBy().getFirstName() + " " + s.getSubmittedBy().getLastName();
        }
        r.submittedAt = s.getSubmittedAt();
        if (s.getReviewer() != null) {
            r.reviewerId = s.getReviewer().getId();
            r.reviewerName =
                    s.getReviewer().getFirstName() + " " + s.getReviewer().getLastName();
        }
        r.reviewedAt = s.getReviewedAt();
        if (s.getFiles() != null) {
            r.files = s.getFiles().stream()
                    .map(SubmissionFileResponse::from)
                    .collect(Collectors.toList());
            r.fileCount = r.files.size();
        } else {
            r.fileCount = 0;
        }
        if (s.getReviews() != null) {
            r.reviews = s.getReviews().stream()
                    .map(SubmissionReviewResponse::from)
                    .collect(Collectors.toList());
        }
        return r;
    }
}
