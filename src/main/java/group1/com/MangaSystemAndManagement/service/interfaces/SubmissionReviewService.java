package group1.com.MangaSystemAndManagement.service.interfaces;
import group1.com.MangaSystemAndManagement.dto.request.SubmissionReviewRequest;
import group1.com.MangaSystemAndManagement.dto.response.SubmissionReviewResponse;
import group1.com.MangaSystemAndManagement.model.SubmissionReview;
import java.util.List;
import java.util.Optional;
public interface SubmissionReviewService {
    SubmissionReview create(SubmissionReviewRequest request);
    Optional<SubmissionReviewResponse> findById(Long id);
    List<SubmissionReviewResponse> findAll();
    List<SubmissionReviewResponse> findBySubmissionId(Long submissionId);
    List<SubmissionReviewResponse> findByTaskIdAndReviewerId(Long taskId, Long tantouId);
    SubmissionReview update(Long id, SubmissionReviewRequest request);
    void delete(Long id);
}
