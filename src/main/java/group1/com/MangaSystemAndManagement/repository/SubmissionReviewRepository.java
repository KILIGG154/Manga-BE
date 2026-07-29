package group1.com.MangaSystemAndManagement.repository;
import group1.com.MangaSystemAndManagement.model.SubmissionReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface SubmissionReviewRepository extends JpaRepository<SubmissionReview, Long> {
    List<SubmissionReview> findBySubmissionId(Long submissionId);
    List<SubmissionReview> findBySubmissionTaskIdAndReviewerId(Long taskId, Long reviewerId);
}
