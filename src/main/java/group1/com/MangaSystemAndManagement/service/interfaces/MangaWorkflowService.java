package group1.com.MangaSystemAndManagement.service.interfaces;

import group1.com.MangaSystemAndManagement.dto.request.NameSubmissionRequest;
import group1.com.MangaSystemAndManagement.dto.request.ReviewRequest;
import group1.com.MangaSystemAndManagement.dto.request.ResubmitRequest;
import group1.com.MangaSystemAndManagement.dto.response.SubmissionReviewResponse;
import group1.com.MangaSystemAndManagement.model.Submission;
import group1.com.MangaSystemAndManagement.model.SubmissionReview;

import java.util.List;

public interface MangaWorkflowService {
    Submission submitName(NameSubmissionRequest req);
    SubmissionReview reviewByTantou(ReviewRequest req);
    SubmissionReview reviewByBoard(ReviewRequest req);
    Submission submitToBoard(Long submissionId, Long tantouId);
    Submission resubmitName(ResubmitRequest req);
    List<Submission> listSubmissions(String status);
    List<SubmissionReviewResponse> listReviewsForSubmission(Long submissionId);
    void assignTantouToProject(Long projectId, Long tantouId);
    void assignMangakaToProject(Long projectId, Long mangakaId);
    Submission requestRevision(Long submissionId, Long leaderId, String comment);
}
