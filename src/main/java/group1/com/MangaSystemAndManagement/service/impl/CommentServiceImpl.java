package group1.com.MangaSystemAndManagement.service.impl;

import group1.com.MangaSystemAndManagement.dto.request.CreateChapterCommentRequest;
import group1.com.MangaSystemAndManagement.dto.request.CreatePlanCommentRequest;
import group1.com.MangaSystemAndManagement.dto.response.ChapterCommentResponse;
import group1.com.MangaSystemAndManagement.dto.response.PlanCommentResponse;
import group1.com.MangaSystemAndManagement.model.Account;
import group1.com.MangaSystemAndManagement.model.Chapter;
import group1.com.MangaSystemAndManagement.model.ChapterComment;
import group1.com.MangaSystemAndManagement.model.PlanComment;
import group1.com.MangaSystemAndManagement.model.ProductionPlan;
import group1.com.MangaSystemAndManagement.model.SystemRoleName;
import group1.com.MangaSystemAndManagement.repository.AccountRepository;
import group1.com.MangaSystemAndManagement.repository.ChapterCommentRepository;
import group1.com.MangaSystemAndManagement.repository.ChapterRepository;
import group1.com.MangaSystemAndManagement.repository.PlanCommentRepository;
import group1.com.MangaSystemAndManagement.repository.ProductionPlanRepository;
import group1.com.MangaSystemAndManagement.service.interfaces.CommentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Decision Log 2026-07-27 §AI-05 + §AI-12:
 * Comment thread for ProductionPlan + Chapter.
 *
 * <p>Allowed roles: any active project member — Tantou, Mangaka, Assistant, Board, Leader, Admin.
 * The endpoint is intentionally liberal: anyone who can view the Plan/Chapter can comment.</p>
 */
@Service
public class CommentServiceImpl implements CommentService {

    private final PlanCommentRepository planCommentRepository;
    private final ChapterCommentRepository chapterCommentRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final ChapterRepository chapterRepository;
    private final AccountRepository accountRepository;

    public CommentServiceImpl(
            PlanCommentRepository planCommentRepository,
            ChapterCommentRepository chapterCommentRepository,
            ProductionPlanRepository productionPlanRepository,
            ChapterRepository chapterRepository,
            AccountRepository accountRepository) {
        this.planCommentRepository = planCommentRepository;
        this.chapterCommentRepository = chapterCommentRepository;
        this.productionPlanRepository = productionPlanRepository;
        this.chapterRepository = chapterRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public PlanCommentResponse addPlanComment(Long planId, CreatePlanCommentRequest req) {
        ProductionPlan plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("ProductionPlan not found: " + planId));
        Account author = getAuthorAndValidateRole(req.getAuthorId());

        PlanComment c = new PlanComment();
        c.setProductionPlan(plan);
        c.setAuthorId(author.getId());
        c.setAuthorName(resolveAuthorName(author));
        c.setBody(req.getBody().trim());
        c = planCommentRepository.save(c);
        return PlanCommentResponse.from(c);
    }

    @Override
    public List<PlanCommentResponse> listPlanComments(Long planId) {
        if (!productionPlanRepository.existsById(planId)) {
            throw new RuntimeException("ProductionPlan not found: " + planId);
        }
        return planCommentRepository.findByProductionPlanIdOrderByCreatedAtAsc(planId).stream()
                .map(PlanCommentResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChapterCommentResponse addChapterComment(Long chapterId, CreateChapterCommentRequest req) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found: " + chapterId));
        Account author = getAuthorAndValidateRole(req.getAuthorId());

        ChapterComment c = new ChapterComment();
        c.setChapter(chapter);
        c.setAuthorId(author.getId());
        c.setAuthorName(resolveAuthorName(author));
        c.setBody(req.getBody().trim());
        c = chapterCommentRepository.save(c);
        return ChapterCommentResponse.from(c);
    }

    @Override
    public List<ChapterCommentResponse> listChapterComments(Long chapterId) {
        if (!chapterRepository.existsById(chapterId)) {
            throw new RuntimeException("Chapter not found: " + chapterId);
        }
        return chapterCommentRepository.findByChapterIdOrderByCreatedAtAsc(chapterId).stream()
                .map(ChapterCommentResponse::from)
                .collect(Collectors.toList());
    }

    private Account getAuthorAndValidateRole(Long authorId) {
        Account author = accountRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + authorId));
        boolean isMember = author.hasRole(SystemRoleName.TANTOU_EDITOR)
                || author.hasRole(SystemRoleName.MANGAKA)
                || author.hasRole(SystemRoleName.ASSISTANT)
                || author.hasRole(SystemRoleName.LEADER_BOARD)
                || author.hasRole(SystemRoleName.EDITORIAL_BOARD_MEMBER)
                || author.hasRole(SystemRoleName.ADMIN);
        if (!isMember) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Author phải là thành viên dự án (Tantou/Mangaka/Assistant/Board/Leader/Admin).");
        }
        return author;
    }

    private String resolveAuthorName(Account a) {
        String first = a.getFirstName() == null ? "" : a.getFirstName();
        String last = a.getLastName() == null ? "" : a.getLastName();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? "User#" + a.getId() : name;
    }
}