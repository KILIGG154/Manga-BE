package group1.com.MangaSystemAndManagement.service.interfaces;

import group1.com.MangaSystemAndManagement.dto.request.CreateChapterCommentRequest;
import group1.com.MangaSystemAndManagement.dto.request.CreatePlanCommentRequest;
import group1.com.MangaSystemAndManagement.dto.response.ChapterCommentResponse;
import group1.com.MangaSystemAndManagement.dto.response.PlanCommentResponse;

import java.util.List;

/**
 * Decision Log 2026-07-27 §AI-05 + §AI-12:
 * Comment thread for ProductionPlan + Chapter.
 *
 * <p>Append-only — no edit/delete (audit trail). Any project member can read/post.
 * Comment is the recommended channel when Plan is PAUSED (BA Spec V3 §2.2).</p>
 */
public interface CommentService {

    PlanCommentResponse addPlanComment(Long planId, CreatePlanCommentRequest req);

    List<PlanCommentResponse> listPlanComments(Long planId);

    ChapterCommentResponse addChapterComment(Long chapterId, CreateChapterCommentRequest req);

    List<ChapterCommentResponse> listChapterComments(Long chapterId);
}