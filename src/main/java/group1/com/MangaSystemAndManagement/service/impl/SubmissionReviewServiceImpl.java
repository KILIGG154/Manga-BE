package group1.com.MangaSystemAndManagement.service.impl;
import group1.com.MangaSystemAndManagement.dto.request.SubmissionReviewRequest;
import group1.com.MangaSystemAndManagement.dto.response.SubmissionReviewResponse;
import group1.com.MangaSystemAndManagement.model.SubmissionReview;
import group1.com.MangaSystemAndManagement.repository.SubmissionReviewRepository;
import group1.com.MangaSystemAndManagement.service.interfaces.SubmissionReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionReviewServiceImpl implements SubmissionReviewService {
    private final SubmissionReviewRepository repository;

    private SubmissionReviewResponse toResponse(SubmissionReview entity) {
        SubmissionReviewResponse response = new SubmissionReviewResponse();
        response.setId(entity.getId());
        response.setSubmissionId(entity.getSubmission() != null ? entity.getSubmission().getId() : null);
        response.setReviewerId(entity.getReviewer() != null ? entity.getReviewer().getId() : null);
        response.setReviewerEmail(entity.getReviewer() != null ? entity.getReviewer().getEmail() : null);
        response.setStage(entity.getStage());
        response.setDecision(entity.getDecision());
        response.setComment(entity.getComment());
        response.setReviewedAt(entity.getReviewedAt());
        return response;
    }

    @Override
    @Transactional
    public SubmissionReview create(SubmissionReviewRequest request) {
        SubmissionReview entity = new SubmissionReview();
        org.springframework.beans.BeanUtils.copyProperties(request, entity);
        return repository.save(entity);
    }

    @Override
    public Optional<SubmissionReviewResponse> findById(Long id) {
        return repository.findById(id).map(this::toResponse);
    }

    @Override
    public List<SubmissionReviewResponse> findAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubmissionReviewResponse> findBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubmissionReviewResponse> findByTaskIdAndReviewerId(Long taskId, Long tantouId) {
        return repository.findBySubmissionTaskIdAndReviewerId(taskId, tantouId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SubmissionReview update(Long id, SubmissionReviewRequest request) {
        SubmissionReview entity = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("SubmissionReview not found with id " + id));
        org.springframework.beans.BeanUtils.copyProperties(request, entity);
        return repository.save(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("SubmissionReview not found with id " + id);
        }
        repository.deleteById(id);
    }
}
