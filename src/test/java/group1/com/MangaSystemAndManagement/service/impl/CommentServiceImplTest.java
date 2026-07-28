package group1.com.MangaSystemAndManagement.service.impl;

import group1.com.MangaSystemAndManagement.TestSupportBase;
import group1.com.MangaSystemAndManagement.dto.request.CreateChapterCommentRequest;
import group1.com.MangaSystemAndManagement.dto.request.CreatePlanCommentRequest;
import group1.com.MangaSystemAndManagement.model.Account;
import group1.com.MangaSystemAndManagement.model.Chapter;
import group1.com.MangaSystemAndManagement.model.ChapterComment;
import group1.com.MangaSystemAndManagement.model.ChapterStatus;
import group1.com.MangaSystemAndManagement.model.PlanComment;
import group1.com.MangaSystemAndManagement.model.PlanStatus;
import group1.com.MangaSystemAndManagement.model.ProductionPlan;
import group1.com.MangaSystemAndManagement.model.SystemRoleName;
import group1.com.MangaSystemAndManagement.repository.AccountRepository;
import group1.com.MangaSystemAndManagement.repository.ChapterCommentRepository;
import group1.com.MangaSystemAndManagement.repository.ChapterRepository;
import group1.com.MangaSystemAndManagement.repository.PlanCommentRepository;
import group1.com.MangaSystemAndManagement.repository.ProductionPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock PlanCommentRepository planCommentRepository;
    @Mock ChapterCommentRepository chapterCommentRepository;
    @Mock ProductionPlanRepository productionPlanRepository;
    @Mock ChapterRepository chapterRepository;
    @Mock AccountRepository accountRepository;

    @InjectMocks CommentServiceImpl service;

    private static final long PLAN_ID = 21L;
    private static final long CHAPTER_ID = 11L;
    private static final long TANTOU_ID = 4L;
    private static final long STRANGER_ID = 999L;

    private ProductionPlan plan;
    private Chapter chapter;
    private Account tantou;

    @BeforeEach
    void setUp() {
        plan = new ProductionPlan();
        TestSupportBase.setField(plan, "id", PLAN_ID);
        plan.setPlanStatus(PlanStatus.ACTIVE);

        chapter = new Chapter();
        TestSupportBase.setField(chapter, "id", CHAPTER_ID);
        chapter.setProductionPlan(plan);

        tantou = TestSupportBase.accountWithRole(TANTOU_ID, SystemRoleName.TANTOU_EDITOR);
        tantou.setFirstName("Trang");
        tantou.setLastName("Nguyen");
    }

    @Nested
    @DisplayName("Plan comment (§AI-05)")
    class PlanCommentTests {

        @Test
        @DisplayName("Tantou post comment on plan -> comment persisted")
        void tantouPostPlanComment() {
            when(productionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
            when(accountRepository.findById(TANTOU_ID)).thenReturn(Optional.of(tantou));
            when(planCommentRepository.save(any(PlanComment.class)))
                    .thenAnswer(inv -> {
                        PlanComment c = inv.getArgument(0);
                        TestSupportBase.setField(c, "id", 555L);
                        return c;
                    });

            CreatePlanCommentRequest req = new CreatePlanCommentRequest();
            req.setAuthorId(TANTOU_ID);
            req.setBody("Đề xuất: reschedule chapter 12 sang tuần sau vì thiếu assistant");

            var res = service.addPlanComment(PLAN_ID, req);

            assertThat(res.getBody()).contains("Đề xuất");
            assertThat(res.getAuthorName()).isEqualTo("Trang Nguyen");
            assertThat(res.getPlanId()).isEqualTo(PLAN_ID);
        }

        @Test
        @DisplayName("Outside allowed roles -> AccessDenied")
        void strangerCannotPost() {
            // MANAGER is not in comment-post whitelist (only Tantou/Mangaka/Assistant/Board/Leader/Admin).
            Account stranger = TestSupportBase.accountWithRole(STRANGER_ID, SystemRoleName.MANAGER);
            when(productionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
            when(accountRepository.findById(STRANGER_ID)).thenReturn(Optional.of(stranger));

            CreatePlanCommentRequest req = new CreatePlanCommentRequest();
            req.setAuthorId(STRANGER_ID);
            req.setBody("Tôi là manager, không nên được phép comment theo policy");

            assertThatThrownBy(() -> service.addPlanComment(PLAN_ID, req))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Tantou");
            verify(planCommentRepository, never()).save(any());
        }

        @Test
        @DisplayName("List plan comments sorts by createdAt asc")
        void listPlanCommentsOrder() {
            PlanComment c1 = new PlanComment();
            TestSupportBase.setField(c1, "id", 1L);
            c1.setProductionPlan(plan);
            c1.setAuthorId(TANTOU_ID);
            c1.setBody("first");
            c1.setCreatedAt(java.time.Instant.now());

            PlanComment c2 = new PlanComment();
            TestSupportBase.setField(c2, "id", 2L);
            c2.setProductionPlan(plan);
            c2.setAuthorId(TANTOU_ID);
            c2.setBody("second");
            c2.setCreatedAt(java.time.Instant.now().plusSeconds(60));

            when(productionPlanRepository.existsById(PLAN_ID)).thenReturn(true);
            when(planCommentRepository.findByProductionPlanIdOrderByCreatedAtAsc(PLAN_ID))
                    .thenReturn(List.of(c1, c2));

            var res = service.listPlanComments(PLAN_ID);

            assertThat(res).hasSize(2);
            assertThat(res.get(0).getBody()).isEqualTo("first");
            assertThat(res.get(1).getBody()).isEqualTo("second");
        }

        @Test
        @DisplayName("Plan not found -> RuntimeException")
        void planNotFoundThrows() {
            when(productionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.empty());

            CreatePlanCommentRequest req = new CreatePlanCommentRequest();
            req.setAuthorId(TANTOU_ID);
            req.setBody("any body");

            assertThatThrownBy(() -> service.addPlanComment(PLAN_ID, req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("ProductionPlan not found");
        }

        @Test
        @DisplayName("Body is trimmed before save")
        void bodyIsTrimmed() {
            when(productionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
            when(accountRepository.findById(TANTOU_ID)).thenReturn(Optional.of(tantou));
            when(planCommentRepository.save(any(PlanComment.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CreatePlanCommentRequest req = new CreatePlanCommentRequest();
            req.setAuthorId(TANTOU_ID);
            req.setBody("   Leading and trailing whitespace   ");

            var res = service.addPlanComment(PLAN_ID, req);

            assertThat(res.getBody()).isEqualTo("Leading and trailing whitespace");
        }
    }

    @Nested
    @DisplayName("Chapter comment (§AI-12)")
    class ChapterCommentTests {

        @Test
        @DisplayName("Mangaka post comment on chapter after Recall")
        void mangakaPostChapterComment() {
            Account mangaka = TestSupportBase.accountWithRole(8L, SystemRoleName.MANGAKA);
            mangaka.setFirstName("Linh");
            mangaka.setLastName("Pham");
            chapter.setChapterStatus(ChapterStatus.IN_PRODUCTION);

            when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter));
            when(accountRepository.findById(8L)).thenReturn(Optional.of(mangaka));
            when(chapterCommentRepository.save(any(ChapterComment.class)))
                    .thenAnswer(inv -> {
                        ChapterComment c = inv.getArgument(0);
                        TestSupportBase.setField(c, "id", 666L);
                        return c;
                    });

            CreateChapterCommentRequest req = new CreateChapterCommentRequest();
            req.setAuthorId(8L);
            req.setBody("Tantou ơi background trang 12 cần làm lại, em xác nhận");

            var res = service.addChapterComment(CHAPTER_ID, req);

            assertThat(res.getBody()).contains("background trang 12");
            assertThat(res.getAuthorName()).isEqualTo("Linh Pham");
            assertThat(res.getChapterId()).isEqualTo(CHAPTER_ID);
        }

        @Test
        @DisplayName("Chapter not found -> RuntimeException")
        void chapterNotFoundThrows() {
            when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.empty());

            CreateChapterCommentRequest req = new CreateChapterCommentRequest();
            req.setAuthorId(TANTOU_ID);
            req.setBody("any");

            assertThatThrownBy(() -> service.addChapterComment(CHAPTER_ID, req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Chapter not found");
        }

        @Test
        @DisplayName("List chapter comments returns chronological order")
        void listChapterCommentsOrder() {
            ChapterComment c1 = new ChapterComment();
            TestSupportBase.setField(c1, "id", 1L);
            c1.setChapter(chapter);
            c1.setAuthorId(TANTOU_ID);
            c1.setBody("Tantou: chọn task X");
            c1.setCreatedAt(java.time.Instant.now());

            ChapterComment c2 = new ChapterComment();
            TestSupportBase.setField(c2, "id", 2L);
            c2.setChapter(chapter);
            c2.setAuthorId(8L);
            c2.setBody("Mangaka: ok em sửa");
            c2.setCreatedAt(java.time.Instant.now().plusSeconds(120));

            when(chapterRepository.existsById(CHAPTER_ID)).thenReturn(true);
            when(chapterCommentRepository.findByChapterIdOrderByCreatedAtAsc(CHAPTER_ID))
                    .thenReturn(List.of(c1, c2));

            var res = service.listChapterComments(CHAPTER_ID);

            assertThat(res).hasSize(2);
            assertThat(res.get(0).getAuthorId()).isEqualTo(TANTOU_ID);
            assertThat(res.get(1).getAuthorId()).isEqualTo(8L);
        }
    }
}