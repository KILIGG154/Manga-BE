package group1.com.MangaSystemAndManagement.service.impl;

import group1.com.MangaSystemAndManagement.dto.request.NameSubmissionRequest;
import group1.com.MangaSystemAndManagement.dto.request.ReviewRequest;
import group1.com.MangaSystemAndManagement.dto.request.ResubmitRequest;
import group1.com.MangaSystemAndManagement.dto.response.SubmissionReviewResponse;
import group1.com.MangaSystemAndManagement.model.*;
import group1.com.MangaSystemAndManagement.repository.AccountRepository;
import group1.com.MangaSystemAndManagement.repository.ProjectRepository;
import group1.com.MangaSystemAndManagement.repository.PlanningRepository;
import group1.com.MangaSystemAndManagement.service.interfaces.MangaWorkflowService;
import group1.com.MangaSystemAndManagement.service.interfaces.SubmissionReviewService;
import group1.com.MangaSystemAndManagement.service.interfaces.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MangaWorkflowServiceImpl implements MangaWorkflowService {

    private final SubmissionService submissionService;
    private final SubmissionReviewService submissionReviewService;
    private final group1.com.MangaSystemAndManagement.repository.SubmissionRepository submissionRepository;
    private final AccountRepository accountRepository;
    private final ProjectRepository projectRepository;
    private final PlanningRepository planningRepository;

    @Override
    @Transactional
    public Submission submitName(NameSubmissionRequest req) {
        Optional<Account> accountOpt = accountRepository.findById(req.getSubmittedById());
        if (accountOpt.isEmpty()) {
            throw new RuntimeException("Submitting account not found");
        }
        Account submitter = accountOpt.get();
        boolean isMangaka = submitter.hasRole(SystemRoleName.MANGAKA);
        if (!isMangaka) {
            throw new AccessDeniedException("Only Mangaka can submit a Name");
        }

        Submission s = new Submission();
        
        if (req.getProjectId() != null && req.getProjectId() > 0) {
            Optional<Project> projectOpt = projectRepository.findById(req.getProjectId());
            if (projectOpt.isEmpty()) {
                throw new RuntimeException("Project not found");
            }
            s.setProject(projectOpt.get());
        }

        if (req.getPlanningId() != null && req.getPlanningId() > 0) {
            Optional<Planning> planningOpt = planningRepository.findById(req.getPlanningId());
            planningOpt.ifPresent(s::setPlanning);
        }
        s.setSubmittedBy(submitter);
        s.setTitle(req.getTitle());
        s.setContentUrl(req.getContentUrl());
        // Mangaka submits directly to Board — no Tantou review step
        s.setNameStatus(group1.com.MangaSystemAndManagement.model.NameSubmissionStatus.PENDING_BOARD_REVIEW);
        s.setSubmittedAt(Instant.now());

        return submissionRepository.save(s);
    }

    @Override
    @Transactional
    public SubmissionReview reviewByTantou(ReviewRequest req) {
        Optional<Submission> subOpt = submissionService.findById(req.getSubmissionId());
        if (subOpt.isEmpty()) {
            throw new RuntimeException("Submission not found");
        }
        Submission submission = subOpt.get();

        Optional<Account> reviewerOpt = accountRepository.findById(req.getReviewerId());
        if (reviewerOpt.isEmpty()) {
            throw new RuntimeException("Reviewer not found");
        }
        Account reviewer = reviewerOpt.get();
        if (!reviewer.hasRole(SystemRoleName.TANTOU_EDITOR)) {
            throw new AccessDeniedException("Only Tantou Editors can perform Editorial Review");
        }

        // Tantou review is skipped in the new flow, but if called, it expects PENDING_BOARD_REVIEW or PROCESSING
        if (submission.getNameStatus() != group1.com.MangaSystemAndManagement.model.NameSubmissionStatus.PENDING_BOARD_REVIEW && 
            submission.getNameStatus() != group1.com.MangaSystemAndManagement.model.NameSubmissionStatus.PROCESSING) {
            throw new RuntimeException("Submission must be in PENDING_BOARD_REVIEW or PROCESSING status for Editorial Review");
        }

        SubmissionReview review = new SubmissionReview();
        review.setSubmission(submission);
        review.setReviewer(reviewer);
        review.setStage(group1.com.MangaSystemAndManagement.model.ReviewStage.EDITORIAL);
        
        String decision = req.getDecision() != null ? req.getDecision().trim().toUpperCase() : "";
        if (decision.equals("APPROVE")) decision = "APPROVED";
        review.setDecision(decision);

        StringBuilder commentBuilder = new StringBuilder();
        if (req.getPacingPass() != null) commentBuilder.append("Pacing: ").append(req.getPacingPass() ? "PASS" : "FAIL").append(". ");
        if (req.getStructurePass() != null) commentBuilder.append("Structure: ").append(req.getStructurePass() ? "PASS" : "FAIL").append(". ");
        if (req.getImageFlowPass() != null) commentBuilder.append("ImageFlow: ").append(req.getImageFlowPass() ? "PASS" : "FAIL").append(". ");
        if (req.getComment() != null && !req.getComment().isBlank()) commentBuilder.append("Notes: ").append(req.getComment());
        review.setComment(commentBuilder.toString());
        review.setReviewedAt(Instant.now());

        group1.com.MangaSystemAndManagement.dto.request.SubmissionReviewRequest reviewReq = new group1.com.MangaSystemAndManagement.dto.request.SubmissionReviewRequest();
        org.springframework.beans.BeanUtils.copyProperties(review, reviewReq);
        SubmissionReview savedReview = submissionReviewService.create(reviewReq);

        if (decision.equals("APPROVED")) {
            submission.setNameStatus(group1.com.MangaSystemAndManagement.model.NameSubmissionStatus.PROCESSING);
        } else {
            submission.setNameStatus(group1.com.MangaSystemAndManagement.model.NameSubmissionStatus.REJECTED);
        }

        submissionRepository.save(submission);

        return savedReview;
    }

    @Override
    @Transactional
    public SubmissionReview reviewByBoard(ReviewRequest req) {
        Optional<Submission> subOpt = submissionService.findById(req.getSubmissionId());
        if (subOpt.isEmpty()) {
            throw new RuntimeException("Submission not found");
        }
        Submission submission = subOpt.get();

        Optional<Account> reviewerOpt = accountRepository.findById(req.getReviewerId());
        if (reviewerOpt.isEmpty()) {
            throw new RuntimeException("Reviewer not found");
        }
        Account reviewer = reviewerOpt.get();
        boolean isEditor = reviewer.hasRole(SystemRoleName.EDITORIAL_BOARD_MEMBER);
        boolean isLeader = reviewer.hasRole(SystemRoleName.LEADER_BOARD);
        if (!isEditor && !isLeader) {
            throw new AccessDeniedException("Only Editorial Board Members or Leader Board can vote");
        }

        // Allow: PENDING_BOARD_REVIEW, PROCESSING, or null (legacy submissions created
        // via POST /api/submissions/{userId} that only set productionStatus=PENDING, not nameStatus)
        if (submission.getNameStatus() != null
                && submission.getNameStatus() != NameSubmissionStatus.PENDING_BOARD_REVIEW
                && submission.getNameStatus() != NameSubmissionStatus.PROCESSING) {
            throw new RuntimeException("Submission must be in PENDING_BOARD_REVIEW or PROCESSING status for Board Voting");
        }

        if (req.getComment() == null || req.getComment().isBlank()) {
            throw new IllegalArgumentException("Comment is required for voting");
        }

        boolean alreadyVoted = submissionReviewService.findAll().stream()
            .anyMatch(r -> submission.getId().equals(r.getSubmissionId())
                        && reviewer.getId() == r.getReviewerId()
                        && r.getStage() == group1.com.MangaSystemAndManagement.model.ReviewStage.EDITORIAL_BOARD);
        if (alreadyVoted) {
            throw new RuntimeException("Board member has already voted for this submission");
        }

        SubmissionReview review = new SubmissionReview();
        review.setSubmission(submission);
        review.setReviewer(reviewer);
        review.setStage(group1.com.MangaSystemAndManagement.model.ReviewStage.EDITORIAL_BOARD);
        
        String decision = req.getDecision() != null ? req.getDecision().trim().toUpperCase() : "";
        if (decision.equals("APPROVE")) decision = "APPROVED";
        review.setDecision(decision);

        StringBuilder commentBuilder = new StringBuilder();
        commentBuilder.append("Notes: ").append(req.getComment());
        review.setComment(commentBuilder.toString());
        review.setReviewedAt(Instant.now());

        group1.com.MangaSystemAndManagement.dto.request.SubmissionReviewRequest reviewReq = new group1.com.MangaSystemAndManagement.dto.request.SubmissionReviewRequest();
        org.springframework.beans.BeanUtils.copyProperties(review, reviewReq);
        SubmissionReview savedReview = submissionReviewService.create(reviewReq);

        if (isLeader) {
            if ("APPROVED".equals(decision)) {
                submission.setNameStatus(group1.com.MangaSystemAndManagement.model.NameSubmissionStatus.APPROVED);
                // Project creation is now fully manual by EDITORIAL_BOARD_MEMBER
                // via POST /api/projects. No auto-create here.
            } else {
                submission.setNameStatus(group1.com.MangaSystemAndManagement.model.NameSubmissionStatus.REJECTED);
            }
        } else {
            submission.setNameStatus(group1.com.MangaSystemAndManagement.model.NameSubmissionStatus.PROCESSING);
        }

        submissionRepository.save(submission);

        return savedReview;
    }

    @Override
    @Transactional
    public Submission requestRevision(Long submissionId, Long leaderId, String comment) {
        Optional<Submission> subOpt = submissionService.findById(submissionId);
        if (subOpt.isEmpty()) {
            throw new RuntimeException("Submission not found");
        }
        Submission submission = subOpt.get();

        Optional<Account> reviewerOpt = accountRepository.findById(leaderId);
        if (reviewerOpt.isEmpty()) {
            throw new RuntimeException("Leader not found");
        }
        Account reviewer = reviewerOpt.get();
        if (!reviewer.hasRole(SystemRoleName.LEADER_BOARD)) {
            throw new AccessDeniedException("Only Leader Board can request revision");
        }

        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Comment is required for revision request");
        }

        SubmissionReview review = new SubmissionReview();
        review.setSubmission(submission);
        review.setReviewer(reviewer);
        review.setStage(group1.com.MangaSystemAndManagement.model.ReviewStage.EDITORIAL_BOARD);
        review.setDecision("REVISION");
        review.setComment("Notes: " + comment);
        review.setReviewedAt(Instant.now());

        group1.com.MangaSystemAndManagement.dto.request.SubmissionReviewRequest reviewReq = new group1.com.MangaSystemAndManagement.dto.request.SubmissionReviewRequest();
        org.springframework.beans.BeanUtils.copyProperties(review, reviewReq);
        submissionReviewService.create(reviewReq);

        submission.setNameStatus(group1.com.MangaSystemAndManagement.model.NameSubmissionStatus.REVISION);
        submissionRepository.save(submission);

        return submission;
    }

    @Override
    @Transactional
    public Submission submitToBoard(Long submissionId, Long tantouId) {
        Optional<Submission> subOpt = submissionService.findById(submissionId);
        if (subOpt.isEmpty()) {
            throw new RuntimeException("Submission not found");
        }
        Submission submission = subOpt.get();

        Optional<Account> reviewerOpt = accountRepository.findById(tantouId);
        if (reviewerOpt.isEmpty()) {
            throw new RuntimeException("Tantou Editor not found");
        }
        Account reviewer = reviewerOpt.get();
        if (!reviewer.hasRole(SystemRoleName.TANTOU_EDITOR)) {
            throw new AccessDeniedException("Only Tantou Editors can submit to the Board");
        }

        if (submission.getNameStatus() != NameSubmissionStatus.PROCESSING) {
            throw new RuntimeException("Submission must be PROCESSING to submit to board");
        }

        submission.setNameStatus(NameSubmissionStatus.PENDING_BOARD_REVIEW);
        
        group1.com.MangaSystemAndManagement.dto.request.SubmissionRequest subReq = new group1.com.MangaSystemAndManagement.dto.request.SubmissionRequest();
        org.springframework.beans.BeanUtils.copyProperties(submission, subReq);
        return submissionService.update(submission.getId(), subReq);
    }

    @Override
    @Transactional
    public Submission resubmitName(ResubmitRequest req) {
        Optional<Submission> subOpt = submissionService.findById(req.getSubmissionId());
        if (subOpt.isEmpty()) {
            throw new RuntimeException("Original submission not found");
        }
        Submission submission = subOpt.get();

        if (submission.getSubmittedBy() == null || submission.getSubmittedBy().getId() != req.getSubmittedById()) {
            throw new AccessDeniedException("Only original submitter can resubmit");
        }

        submission.setTitle(req.getTitle());
        submission.setContentUrl(req.getContentUrl());
        submission.setNameStatus(group1.com.MangaSystemAndManagement.model.NameSubmissionStatus.PENDING_BOARD_REVIEW);
        submission.setSubmittedAt(Instant.now());

        group1.com.MangaSystemAndManagement.dto.request.SubmissionRequest subReq = new group1.com.MangaSystemAndManagement.dto.request.SubmissionRequest();
        org.springframework.beans.BeanUtils.copyProperties(submission, subReq);
        return submissionService.update(submission.getId(), subReq);
    }

    @Override
    public List<Submission> listSubmissions(String status) {
        var all = submissionService.findAll();
        if (status == null || status.isBlank()) return all;
        return all.stream().filter(s -> {
            String sName = s.getNameStatus() != null ? s.getNameStatus().name() : null;
            String sProd = s.getProductionStatus() != null ? s.getProductionStatus().name() : null;
            return status.equalsIgnoreCase(sName) || status.equalsIgnoreCase(sProd);
        }).toList();
    }

    @Override
    public List<SubmissionReviewResponse> listReviewsForSubmission(Long submissionId) {
        var all = submissionReviewService.findAll();
        return all.stream().filter(r -> submissionId.equals(r.getSubmissionId())).toList();
    }

    @Override
    @Transactional
    public void assignTantouToProject(Long projectId, Long tantouId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Account tantou = accountRepository.findById(tantouId)
                .orElseThrow(() -> new RuntimeException("Tantou Editor not found"));

        if (!tantou.hasRole(SystemRoleName.TANTOU_EDITOR)) {
            throw new AccessDeniedException("The specified user is not a Tantou Editor");
        }
        
        if (project.getTantou() != null) {
            throw new RuntimeException("This project already has a Tantou assigned");
        }

        project.setTantou(tantou);
        projectRepository.save(project);
    }

    @Override
    @Transactional
    public void assignMangakaToProject(Long projectId, Long mangakaId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Account mangaka = accountRepository.findById(mangakaId)
                .orElseThrow(() -> new RuntimeException("Mangaka not found"));

        if (!mangaka.hasRole(SystemRoleName.MANGAKA)) {
            throw new AccessDeniedException("The specified user is not a Mangaka");
        }

        if (project.getMangaka() != null) {
            throw new RuntimeException("This project already has a Mangaka assigned");
        }

        project.setMangaka(mangaka);
        projectRepository.save(project);
    }
}
