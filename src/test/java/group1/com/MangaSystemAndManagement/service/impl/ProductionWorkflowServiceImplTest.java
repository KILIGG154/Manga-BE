package group1.com.MangaSystemAndManagement.service.impl;

import group1.com.MangaSystemAndManagement.TestSupportBase;
import group1.com.MangaSystemAndManagement.dto.request.RecallChapterRequest;
import group1.com.MangaSystemAndManagement.dto.request.ReturnChapterRequest;
import group1.com.MangaSystemAndManagement.model.Account;
import group1.com.MangaSystemAndManagement.model.Chapter;
import group1.com.MangaSystemAndManagement.model.ChapterStatus;
import group1.com.MangaSystemAndManagement.model.FeedbackDecision;
import group1.com.MangaSystemAndManagement.model.PlanStatus;
import group1.com.MangaSystemAndManagement.model.ProductionPlan;
import group1.com.MangaSystemAndManagement.model.Project;
import group1.com.MangaSystemAndManagement.model.ProjectWorkflowStatus;
import group1.com.MangaSystemAndManagement.model.SystemRoleName;
import group1.com.MangaSystemAndManagement.model.Task;
import group1.com.MangaSystemAndManagement.model.TaskType;
import group1.com.MangaSystemAndManagement.model.TaskWorkflowStatus;
import group1.com.MangaSystemAndManagement.repository.AccountRepository;
import group1.com.MangaSystemAndManagement.repository.AssetRepository;
import group1.com.MangaSystemAndManagement.repository.ChapterRepository;
import group1.com.MangaSystemAndManagement.repository.FeedbackRepository;
import group1.com.MangaSystemAndManagement.repository.ProductionPlanRepository;
import group1.com.MangaSystemAndManagement.repository.ProjectRepository;
import group1.com.MangaSystemAndManagement.repository.SubTaskRepository;
import group1.com.MangaSystemAndManagement.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 1 + 2 — ProductionWorkflowService unit tests (BA V3 §3.1, §3.3, §3.4).
 * Focus areas: publish (Board also allowed), recall (min reason + Plan rollback),
 * return (rejection cap), override (Leader only).
 */
@ExtendWith(MockitoExtension.class)
class ProductionWorkflowServiceImplTest {

    @Mock ProjectRepository projectRepository;
    @Mock ProductionPlanRepository productionPlanRepository;
    @Mock ChapterRepository chapterRepository;
    @Mock TaskRepository taskRepository;
    @Mock SubTaskRepository subTaskRepository;
    @Mock FeedbackRepository feedbackRepository;
    @Mock AssetRepository assetRepository;
    @Mock AccountRepository accountRepository;

    @InjectMocks ProductionWorkflowServiceImpl service;

    private static final long LEADER_ID = 1L;
    private static final long BOARD_ID = 2L;
    private static final long CHAPTER_ID = 11L;
    private static final long PLAN_ID = 21L;

    private Account leader;
    private Account board;
    private Project project;
    private ProductionPlan plan;
    private Chapter publishedChapter;
    private Chapter completedChapter;

    @BeforeEach
    void setUp() {
        leader = TestSupportBase.accountWithRole(LEADER_ID, SystemRoleName.LEADER_BOARD);
        board  = TestSupportBase.accountWithRole(BOARD_ID,  SystemRoleName.EDITORIAL_BOARD_MEMBER);

        project = new Project();
        TestSupportBase.setField(project, "id", 100L);
        project.setProjectWorkflowStatus(ProjectWorkflowStatus.ACTIVE);

        plan = new ProductionPlan();
        TestSupportBase.setField(plan, "id", PLAN_ID);
        plan.setProject(project);
        plan.setPlanStatus(PlanStatus.ACTIVE);

        publishedChapter = new Chapter();
        TestSupportBase.setField(publishedChapter, "id", CHAPTER_ID);
        publishedChapter.setProductionPlan(plan);
        publishedChapter.setProject(project);
        publishedChapter.setChapterStatus(ChapterStatus.PUBLISHED);
        publishedChapter.setRecallCount(0);
        publishedChapter.setRejectionCount(0);
        publishedChapter.setPublishDate(LocalDate.now());

        completedChapter = new Chapter();
        TestSupportBase.setField(completedChapter, "id", CHAPTER_ID + 1);
        completedChapter.setProductionPlan(plan);
        completedChapter.setProject(project);
        completedChapter.setChapterStatus(ChapterStatus.COMPLETED);
        completedChapter.setRejectionCount(0);
    }

    // ---- §3.1 publishChapter: Board also allowed ----

    @Nested
    @DisplayName("publishChapter (BA V3 §3.1)")
    class PublishTests {

        @Test
        @DisplayName("EDITORIAL_BOARD_MEMBER can publish (BA V3 single-signoff)")
        void             boardCanPublish() {
            when(accountRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
            when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(publishedChapter));
            publishedChapter.setChapterStatus(ChapterStatus.COMPLETED);
            when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));
            when(productionPlanRepository.save(any(ProductionPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.publishChapter(CHAPTER_ID, BOARD_ID, LocalDate.of(2026, 8, 1));

            assertThat(publishedChapter.getChapterStatus()).isEqualTo(ChapterStatus.PUBLISHED);
            assertThat(publishedChapter.getPublishedBy()).isEqualTo(BOARD_ID);
            assertThat(publishedChapter.getPublishedAt()).isNotNull();
            assertThat(publishedChapter.getPublishDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        }

        @Test
        @DisplayName("LEADER_BOARD can publish")
        void             leaderCanPublish() {
            when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
            when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(publishedChapter));
            publishedChapter.setChapterStatus(ChapterStatus.COMPLETED);
            when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));
            when(productionPlanRepository.save(any(ProductionPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.publishChapter(CHAPTER_ID, LEADER_ID, null);

            assertThat(publishedChapter.getChapterStatus()).isEqualTo(ChapterStatus.PUBLISHED);
            assertThat(publishedChapter.getPublishedBy()).isEqualTo(LEADER_ID);
        }

        @Test
        @DisplayName("Publishing a non-COMPLETED chapter throws 409 (IllegalStateException)")
        void cannotPublishInProduction() {
            publishedChapter.setChapterStatus(ChapterStatus.IN_PRODUCTION);
            when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
            when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(publishedChapter));

            assertThatThrownBy(() -> service.publishChapter(CHAPTER_ID, LEADER_ID, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only COMPLETED");
        }
    }

    // ---- §3.4 recallChapter ----

    @Nested
    @DisplayName("recallChapter (BA V3 §3.4)")
    class RecallTests {

        @Test
        @DisplayName("Recall PUBLISHED -> IN_PRODUCTION; recallCount incremented; Tasks STAY (AI-04); Plan rolls back")
        void recallHappy() {
            plan.setPlanStatus(PlanStatus.COMPLETED);
            publishedChapter.setRecallCount(0);
            Task doneTask = new Task();
            doneTask.setTaskWorkflowStatus(TaskWorkflowStatus.DONE);
            TestSupportBase.setField(doneTask, "id", 999L);
            doneTask.setChapter(publishedChapter);

            when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
            when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(publishedChapter));
            when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));
            // AI-04: no auto-reopen; tasks stay as-is.
            when(productionPlanRepository.save(any(ProductionPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            RecallChapterRequest req = new RecallChapterRequest();
            req.setRecallReason("Trang 12 lỗi bố cục, độc giả phản ánh");

            service.recallChapter(CHAPTER_ID, LEADER_ID, req);

            assertThat(publishedChapter.getChapterStatus()).isEqualTo(ChapterStatus.IN_PRODUCTION);
            assertThat(publishedChapter.getRecallCount()).isEqualTo(1);
            assertThat(publishedChapter.getRecallReason()).contains("Trang 12 lỗi bố cục");
            // AI-04: Tasks stay DONE — Tantou now calls markTaskRevision explicitly.
            assertThat(doneTask.getTaskWorkflowStatus()).isEqualTo(TaskWorkflowStatus.DONE);
            assertThat(plan.getPlanStatus()).isEqualTo(PlanStatus.ACTIVE);
        }

        @Test
        @DisplayName("AI-07: 3rd recall blocked; chapters COMPLETED stays intact")
        void thirdRecallBlocked() {
            publishedChapter.setRecallCount(2);
            when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
            when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(publishedChapter));

            RecallChapterRequest req = new RecallChapterRequest();
            req.setRecallReason("Lần thứ 3 đã đạt giới hạn tối đa hai lần thu hồi");

            assertThatThrownBy(() -> service.recallChapter(CHAPTER_ID, LEADER_ID, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("đạt giới hạn tối đa");
            // Status remains PUBLISHED because save() was never called.
            assertThat(publishedChapter.getChapterStatus()).isEqualTo(ChapterStatus.PUBLISHED);
        }

        @Test
        @DisplayName("Plan rollback: COMPLETED -> IN_PROGRESS when chapter recalled")
        void planRollsBackFromCompleted() {
            plan.setPlanStatus(PlanStatus.COMPLETED);
            publishedChapter.setChapterStatus(ChapterStatus.PUBLISHED);
            when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
            when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(publishedChapter));
            when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));
            // AI-04: no auto-reopen; no need to stub taskRepository.
            when(productionPlanRepository.save(any(ProductionPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            RecallChapterRequest req = new RecallChapterRequest();
            req.setRecallReason("Độc giả phát hiện nhiều typo liên quan đến tên nhân vật phụ");

            service.recallChapter(CHAPTER_ID, LEADER_ID, req);

            ArgumentCaptor<ProductionPlan> planCaptor = ArgumentCaptor.forClass(ProductionPlan.class);
            verify(productionPlanRepository, times(1)).save(planCaptor.capture());
            assertThat(planCaptor.getValue().getPlanStatus()).isEqualTo(PlanStatus.ACTIVE);
        }

        @Test
        @DisplayName("Cannot recall an IN_PRODUCTION chapter")
        void cannotRecallUnpublished() {
            publishedChapter.setChapterStatus(ChapterStatus.IN_PRODUCTION);
            when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
            when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(publishedChapter));

            RecallChapterRequest req = new RecallChapterRequest();
            req.setRecallReason("Lý do hợp lệ từ 15 ký tự trở lên nhé");

            assertThatThrownBy(() -> service.recallChapter(CHAPTER_ID, LEADER_ID, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only PUBLISHED");
        }

        @Test
        @DisplayName("Tantou cannot recall (only Leader/Board)")
        void tantouCannotRecall() {
            Account tantou = TestSupportBase.accountWithRole(99L, SystemRoleName.TANTOU_EDITOR);
            when(accountRepository.findById(99L)).thenReturn(Optional.of(tantou));

            RecallChapterRequest req = new RecallChapterRequest();
            req.setRecallReason("Lý do từ 15 ký tự trở lên mới hợp lệ");

            assertThatThrownBy(() -> service.recallChapter(CHAPTER_ID, 99L, req))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ---- §3.3 returnChapterToProduction + overrideReturnLimit ----

    @Nested
    @DisplayName("returnChapterToProduction & overrideReturnLimit (BA V3 §3.3)")
    class ReturnTests {

        @Test
        @DisplayName("First return: COMPLETED -> IN_PRODUCTION; rejectionCount=1; Tasks reopened")
        void firstReturnSucceeds() {
            completedChapter.setRejectionCount(0);
            Task doneTask = new Task();
            doneTask.setTaskWorkflowStatus(TaskWorkflowStatus.DONE);
            doneTask.setChapter(completedChapter);
            TestSupportBase.setField(doneTask, "id", 555L);

            when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
            when(chapterRepository.findById(completedChapter.getId()))
                    .thenReturn(Optional.of(completedChapter));
            when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));
            // AI-04: no auto-reopen; no need to stub taskRepository.

            ReturnChapterRequest req = new ReturnChapterRequest();
            req.setRejectionReason("Lineart chưa đạt style guide");

            service.returnChapterToProduction(completedChapter.getId(), LEADER_ID, req);

            assertThat(completedChapter.getChapterStatus()).isEqualTo(ChapterStatus.IN_PRODUCTION);
            assertThat(completedChapter.getRejectionCount()).isEqualTo(1);
            assertThat(completedChapter.getRejectionReason()).isEqualTo("Lineart chưa đạt style guide");
            // AI-04: Tasks remain DONE until Tantou explicitly marks them via markTaskRevision.
            assertThat(doneTask.getTaskWorkflowStatus()).isEqualTo(TaskWorkflowStatus.DONE);
        }

        @Test
        @DisplayName("Second return also succeeds; rejectionCount=2")
        void secondReturnSucceeds() {
            completedChapter.setRejectionCount(1);
            when(accountRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
            when(chapterRepository.findById(completedChapter.getId()))
                    .thenReturn(Optional.of(completedChapter));
            when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));
            // AI-04: no auto-reopen.

            ReturnChapterRequest req = new ReturnChapterRequest();
            req.setRejectionReason("Background chi tiết quá mức cần thiết");

            service.returnChapterToProduction(completedChapter.getId(), BOARD_ID, req);

            assertThat(completedChapter.getRejectionCount()).isEqualTo(2);
            assertThat(completedChapter.getChapterStatus()).isEqualTo(ChapterStatus.IN_PRODUCTION);
        }

        @Test
        @DisplayName("Third return (no override): rejects + locks chapter into COMPLETED_NEEDS_REVIEW")
        void thirdReturnLocked() {
            completedChapter.setRejectionCount(2);
            when(accountRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
            when(chapterRepository.findById(completedChapter.getId()))
                    .thenReturn(Optional.of(completedChapter));
            when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));

            ReturnChapterRequest req = new ReturnChapterRequest();
            req.setRejectionReason("Vẫn chưa đạt sau 2 lần trả");

            assertThatThrownBy(() ->
                    service.returnChapterToProduction(completedChapter.getId(), BOARD_ID, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("COMPLETED_NEEDS_REVIEW");

            assertThat(completedChapter.getChapterStatus()).isEqualTo(ChapterStatus.COMPLETED_NEEDS_REVIEW);
            // rejectionCount should NOT be incremented when rejected.
            assertThat(completedChapter.getRejectionCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Leader can override and return even at rejectionCount=2")
        void leaderOverrides() {
            completedChapter.setRejectionCount(2);
            when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
            when(chapterRepository.findById(completedChapter.getId()))
                    .thenReturn(Optional.of(completedChapter));
            when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));
            // AI-04: no auto-reopen; planStatus = IN_PROGRESS by default setUp() — service will not save plan.

            ReturnChapterRequest req = new ReturnChapterRequest();
            req.setRejectionReason("Leader vẫn quyết trả dù đã 2 lần");

            service.overrideReturnLimit(completedChapter.getId(), LEADER_ID, req);

            assertThat(completedChapter.getChapterStatus()).isEqualTo(ChapterStatus.IN_PRODUCTION);
            assertThat(completedChapter.getRejectionCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Board member cannot override (Leader only)")
        void boardCannotOverride() {
            completedChapter.setRejectionCount(2);
            when(accountRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));

            ReturnChapterRequest req = new ReturnChapterRequest();
            req.setRejectionReason("Board cố override nhưng không được phép");

            assertThatThrownBy(() ->
                    service.overrideReturnLimit(completedChapter.getId(), BOARD_ID, req))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Only LEADER_BOARD");
        }

        @Test
        @DisplayName("Returning a non-COMPLETED chapter fails with 409 message")
        void cannotReturnNonCompleted() {
            completedChapter.setChapterStatus(ChapterStatus.IN_PRODUCTION);
            when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
            when(chapterRepository.findById(completedChapter.getId()))
                    .thenReturn(Optional.of(completedChapter));

            ReturnChapterRequest req = new ReturnChapterRequest();
            req.setRejectionReason("Reason");

            assertThatThrownBy(() ->
                    service.returnChapterToProduction(completedChapter.getId(), LEADER_ID, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only COMPLETED");
        }
    }

    // ---- §2.2 Plan PAUSED: writes must be refused ----

    @Nested
    @DisplayName("assertPlanNotPaused (Sprint 1 freeze-on-pause)")
    class FreezeTests {

        @Test
        @DisplayName("createChapter with COMPLETED plan throws IllegalStateException")
        void createChapterCompletedPlan() {
            Account tantou = TestSupportBase.accountWithRole(777L, SystemRoleName.TANTOU_EDITOR);
            plan.setPlanStatus(PlanStatus.COMPLETED);
            group1.com.MangaSystemAndManagement.dto.request.CreateChapterRequest req =
                    new group1.com.MangaSystemAndManagement.dto.request.CreateChapterRequest();
            req.setPlanId(PLAN_ID);
            req.setStartDate(LocalDate.now());
            req.setEndDate(LocalDate.now().plusDays(7));

            when(accountRepository.findById(anyLong())).thenReturn(Optional.of(tantou));
            when(productionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

            assertThatThrownBy(() -> service.createChapter(req, 777L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("COMPLETED");
            verify(chapterRepository, never()).save(any());
        }

        @Test
        @DisplayName("createChapter with CANCELLED Project throws IllegalStateException")
        void createChapterCancelledProject() {
            Account tantou = TestSupportBase.accountWithRole(778L, SystemRoleName.TANTOU_EDITOR);
            plan.setPlanStatus(PlanStatus.ACTIVE);
            project.setProjectWorkflowStatus(ProjectWorkflowStatus.CANCELLED);
            group1.com.MangaSystemAndManagement.dto.request.CreateChapterRequest req =
                    new group1.com.MangaSystemAndManagement.dto.request.CreateChapterRequest();
            req.setPlanId(PLAN_ID);
            req.setStartDate(LocalDate.now());
            req.setEndDate(LocalDate.now().plusDays(7));

            when(accountRepository.findById(anyLong())).thenReturn(Optional.of(tantou));
            when(productionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

            assertThatThrownBy(() -> service.createChapter(req, 778L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CANCELLED");
            verify(chapterRepository, never()).save(any());
        }
    }

    // ---- Decision Log 2026-07-27: AI-01 + AI-04 + AI-07 ----

    @Nested
    @DisplayName("Decision Log 2026-07-27 §AI-01 releaseNote + §AI-04 markTaskRevision + §AI-07 recall cap")
    class DecisionLogTests {

        @Test
        @DisplayName("AI-01: publishChapter with explicit releaseNote persists the note")
        void publishWithReleaseNote() {
            publishedChapter.setChapterStatus(ChapterStatus.COMPLETED);
            when(accountRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
            when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(publishedChapter));
            when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));
            when(productionPlanRepository.save(any(ProductionPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.publishChapter(
                    CHAPTER_ID, BOARD_ID,
                    LocalDate.of(2026, 8, 1),
                    "Chapter tiếp theo của arc Thanh Mẫu, ra mắt ngày 1/8");

            assertThat(publishedChapter.getReleaseNote())
                    .isEqualTo("Chapter tiếp theo của arc Thanh Mẫu, ra mắt ngày 1/8");
            assertThat(publishedChapter.getChapterStatus()).isEqualTo(ChapterStatus.PUBLISHED);
        }

        @Test
        @DisplayName("AI-01: publishChapter with NULL releaseNote -> stored as NULL")
        void publishWithNullReleaseNote() {
            publishedChapter.setChapterStatus(ChapterStatus.COMPLETED);
            when(accountRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
            when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(publishedChapter));
            when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));
            when(productionPlanRepository.save(any(ProductionPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.publishChapter(CHAPTER_ID, BOARD_ID, null, null);

            assertThat(publishedChapter.getReleaseNote()).isNull();
        }

        @Test
        @DisplayName("AI-01: blank releaseNote trims to NULL (optional means truly optional)")
        void publishWithBlankReleaseNote() {
            publishedChapter.setChapterStatus(ChapterStatus.COMPLETED);
            when(accountRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
            when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(publishedChapter));
            when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));
            when(productionPlanRepository.save(any(ProductionPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.publishChapter(CHAPTER_ID, BOARD_ID, null, "   ");

            assertThat(publishedChapter.getReleaseNote()).isNull();
        }

        @Test
        @DisplayName("AI-04: markTaskRevision flips one Task DONE -> REVISION_REQUIRED")
        void tantouMarksOneTaskRevision() {
            Account tantou = TestSupportBase.accountWithRole(777L, SystemRoleName.TANTOU_EDITOR);
            Chapter ch = new Chapter();
            TestSupportBase.setField(ch, "id", 555L);
            ch.setChapterStatus(ChapterStatus.IN_PRODUCTION);
            ch.setProductionPlan(plan);
            Task doneTask = new Task();
            doneTask.setChapter(ch);
            doneTask.setTaskWorkflowStatus(TaskWorkflowStatus.DONE);
            TestSupportBase.setField(doneTask, "id", 888L);

            when(accountRepository.findById(777L)).thenReturn(Optional.of(tantou));
            when(taskRepository.findById(888L)).thenReturn(Optional.of(doneTask));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            var req = new group1.com.MangaSystemAndManagement.dto.request.MarkTaskRevisionRequest();
            req.setTantouId(777L);
            req.setNote("Background chưa match style guide");

            var res = service.markTaskRevision(888L, req);

            assertThat(doneTask.getTaskWorkflowStatus()).isEqualTo(TaskWorkflowStatus.REVISION_REQUIRED);
            assertThat(res.getTaskWorkflowStatus()).isEqualTo(TaskWorkflowStatus.REVISION_REQUIRED);
        }

        @Test
        @DisplayName("AI-04: markTaskRevision refuses when Chapter is COMPLETED (not in production)")
        void markRevisionRequiresInProduction() {
            Account tantou = TestSupportBase.accountWithRole(777L, SystemRoleName.TANTOU_EDITOR);
            Chapter ch = new Chapter();
            TestSupportBase.setField(ch, "id", 555L);
            ch.setChapterStatus(ChapterStatus.COMPLETED);
            ch.setProductionPlan(plan);
            Task doneTask = new Task();
            doneTask.setChapter(ch);
            doneTask.setTaskWorkflowStatus(TaskWorkflowStatus.DONE);
            TestSupportBase.setField(doneTask, "id", 888L);

            when(accountRepository.findById(777L)).thenReturn(Optional.of(tantou));
            when(taskRepository.findById(888L)).thenReturn(Optional.of(doneTask));

            var req = new group1.com.MangaSystemAndManagement.dto.request.MarkTaskRevisionRequest();
            req.setTantouId(777L);

            assertThatThrownBy(() -> service.markTaskRevision(888L, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("IN_PRODUCTION");
        }

        @Test
        @DisplayName("AI-04: only Tantou or Leader may mark task for revision")
        void markRevisionRequiresTantouOrLeader() {
            Account mangaka = TestSupportBase.accountWithRole(999L, SystemRoleName.MANGAKA);
            when(accountRepository.findById(999L)).thenReturn(Optional.of(mangaka));

            var req = new group1.com.MangaSystemAndManagement.dto.request.MarkTaskRevisionRequest();
            req.setTantouId(999L);

            assertThatThrownBy(() -> service.markTaskRevision(888L, req))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("TANTOU_EDITOR or LEADER_BOARD");
        }

        @Test
        @DisplayName("AI-07: 2nd recall still succeeds; counter reaches 2")
        void secondRecallSucceeds() {
            publishedChapter.setRecallCount(1);
            when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
            when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(publishedChapter));
            when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));
            // AI-04: no auto-reopen.

            RecallChapterRequest req = new RecallChapterRequest();
            req.setRecallReason("Lần thu hồi thứ 2 trong đời chapter này tại đây");

            service.recallChapter(CHAPTER_ID, LEADER_ID, req);

            assertThat(publishedChapter.getRecallCount()).isEqualTo(2);
            assertThat(publishedChapter.getChapterStatus()).isEqualTo(ChapterStatus.IN_PRODUCTION);
        }

        @Test
        @DisplayName("AI-07 follow-up: Leader override-recall succeeds at recallCount=2")
        void leaderOverrideRecallSucceeds() {
            publishedChapter.setRecallCount(2);
            when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
            when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(publishedChapter));
            when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));

            var req = new group1.com.MangaSystemAndManagement.dto.request.OverrideRecallRequest();
            req.setLeaderId(LEADER_ID);
            req.setRecallReason("Lần thu hồi thứ 3 do leader can thiệp đặc biệt");

            service.overrideRecallChapter(CHAPTER_ID, LEADER_ID, req);

            assertThat(publishedChapter.getRecallCount()).isEqualTo(3);
            assertThat(publishedChapter.getChapterStatus()).isEqualTo(ChapterStatus.IN_PRODUCTION);
            assertThat(publishedChapter.getRecallReason()).contains("thu hồi thứ 3");
        }

        @Test
        @DisplayName("AI-07 follow-up: Board cannot override-recall (Leader only)")
        void boardCannotOverrideRecall() {
            publishedChapter.setRecallCount(2);
            when(accountRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));

            var req = new group1.com.MangaSystemAndManagement.dto.request.OverrideRecallRequest();
            req.setLeaderId(BOARD_ID);
            req.setRecallReason("Board cố override nhưng không đủ quyền can thiệp");

            assertThatThrownBy(() -> service.overrideRecallChapter(CHAPTER_ID, BOARD_ID, req))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("LEADER_BOARD");
            // Chapter status unchanged.
            assertThat(publishedChapter.getChapterStatus()).isEqualTo(ChapterStatus.PUBLISHED);
            assertThat(publishedChapter.getRecallCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("AI-07 follow-up: override-recall on unpublished chapter throws 409")
        void overrideRecallRequiresPublished() {
            publishedChapter.setChapterStatus(ChapterStatus.IN_PRODUCTION);
            publishedChapter.setRecallCount(2);
            when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
            when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(publishedChapter));

            var req = new group1.com.MangaSystemAndManagement.dto.request.OverrideRecallRequest();
            req.setLeaderId(LEADER_ID);
            req.setRecallReason("Chapter chưa publish, không thể override recall");

            assertThatThrownBy(() -> service.overrideRecallChapter(CHAPTER_ID, LEADER_ID, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PUBLISHED");
        }
    }

    // =========================================================================
    // Decision Log 2026-07-27 §AI-09: reset rejectionCount on re-complete
    // =========================================================================

    @Nested
    @DisplayName("AI-09: reset rejectionCount on re-complete")
    class RejectionResetTests {

        @Test
        @DisplayName("re-completion with rejectionCount=2 resets to 0")
        void resetRejectionOnReComplete() {
            Chapter chapter = new Chapter();
            TestSupportBase.setField(chapter, "id", 100L);
            chapter.setChapterStatus(ChapterStatus.IN_PRODUCTION);
            chapter.setRejectionCount(2);
            chapter.setProductionPlan(plan);

            Account tantou = TestSupportBase.accountWithRole(99L, SystemRoleName.TANTOU_EDITOR);
            when(accountRepository.findById(99L)).thenReturn(Optional.of(tantou));
            when(chapterRepository.findById(100L)).thenReturn(Optional.of(chapter));
            when(taskRepository.existsByChapterIdAndTaskWorkflowStatusNot(anyLong(), any())).thenReturn(false);
            when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));

            service.updateChapterStatus(100L, ChapterStatus.COMPLETED, 99L);

            assertThat(chapter.getRejectionCount()).isEqualTo(0);
            assertThat(chapter.getChapterStatus()).isEqualTo(ChapterStatus.COMPLETED);
        }

        @Test
        @DisplayName("first completion (rejectionCount=0) stays at 0")
        void firstCompletionStaysZero() {
            Chapter chapter = new Chapter();
            TestSupportBase.setField(chapter, "id", 101L);
            chapter.setChapterStatus(ChapterStatus.IN_PRODUCTION);
            chapter.setRejectionCount(0);

            Account tantou = TestSupportBase.accountWithRole(99L, SystemRoleName.TANTOU_EDITOR);
            when(accountRepository.findById(99L)).thenReturn(Optional.of(tantou));
            when(chapterRepository.findById(101L)).thenReturn(Optional.of(chapter));
            when(taskRepository.existsByChapterIdAndTaskWorkflowStatusNot(anyLong(), any())).thenReturn(false);
            when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));

            service.updateChapterStatus(101L, ChapterStatus.COMPLETED, 99L);

            assertThat(chapter.getRejectionCount()).isEqualTo(0);
        }
    }

    // =========================================================================
    // Decision Log 2026-07-27 §AI-11: isActive() helper
    // =========================================================================

    @Nested
    @DisplayName("AI-11: ProductionPlan.isActive()")
    class IsActiveTests {

        @Test
        @DisplayName("ACTIVE plan is active")
        void activeIsActive() {
            ProductionPlan p = new ProductionPlan();
            p.setPlanStatus(PlanStatus.ACTIVE);
            assertThat(p.isActive()).isTrue();
        }

        @Test
        @DisplayName("EXTENDED plan is active")
        void extendedIsActive() {
            ProductionPlan p = new ProductionPlan();
            p.setPlanStatus(PlanStatus.EXTENDED);
            assertThat(p.isActive()).isTrue();
        }

        @Test
        @DisplayName("OVERDUE plan is still active (can be extended)")
        void overdueIsActive() {
            ProductionPlan p = new ProductionPlan();
            p.setPlanStatus(PlanStatus.OVERDUE);
            assertThat(p.isActive()).isTrue();
        }

        @Test
        @DisplayName("COMPLETED plan is not active")
        void completedIsNotActive() {
            ProductionPlan p = new ProductionPlan();
            p.setPlanStatus(PlanStatus.COMPLETED);
            assertThat(p.isActive()).isFalse();
        }

        @Test
        @DisplayName("DRAFT plan is not active (until start date)")
        void draftIsNotActive() {
            ProductionPlan p = new ProductionPlan();
            p.setPlanStatus(PlanStatus.DRAFT);
            assertThat(p.isActive()).isFalse();
        }
    }

    // =========================================================================
    // Decision Log 2026-07-27 §AI-08: schedule chapter
    // =========================================================================

    @Nested
    @DisplayName("AI-08: schedule chapter + auto-publish")
    class ScheduleTests {

        @Test
        @DisplayName("Tantou schedules a COMPLETED chapter for future date")
        void tantouSchedulesCompletedChapter() {
            Chapter ch = new Chapter();
            TestSupportBase.setField(ch, "id", 200L);
            ch.setChapterStatus(ChapterStatus.COMPLETED);
            ch.setProductionPlan(plan);

            Account tantou = TestSupportBase.accountWithRole(99L, SystemRoleName.TANTOU_EDITOR);
            when(accountRepository.findById(99L)).thenReturn(Optional.of(tantou));
            when(chapterRepository.findById(200L)).thenReturn(Optional.of(ch));
            when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));

            var req = new group1.com.MangaSystemAndManagement.dto.request.ScheduleChapterRequest();
            req.setSchedulerId(99L);
            req.setPublishDate(java.time.LocalDate.now().plusDays(7));

            var res = service.scheduleChapter(200L, 99L, req);

            assertThat(ch.getChapterStatus()).isEqualTo(ChapterStatus.SCHEDULED);
            assertThat(ch.getPublishDate()).isEqualTo(java.time.LocalDate.now().plusDays(7));
            assertThat(res.getChapterStatus()).isEqualTo(ChapterStatus.SCHEDULED);
        }

        @Test
        @DisplayName("schedule with past date -> 400")
        void schedulePastDateRejected() {
            Chapter ch = new Chapter();
            TestSupportBase.setField(ch, "id", 201L);
            ch.setChapterStatus(ChapterStatus.COMPLETED);

            Account tantou = TestSupportBase.accountWithRole(99L, SystemRoleName.TANTOU_EDITOR);
            when(accountRepository.findById(99L)).thenReturn(Optional.of(tantou));
            when(chapterRepository.findById(201L)).thenReturn(Optional.of(ch));

            var req = new group1.com.MangaSystemAndManagement.dto.request.ScheduleChapterRequest();
            req.setSchedulerId(99L);
            req.setPublishDate(java.time.LocalDate.now().minusDays(1));

            assertThatThrownBy(() -> service.scheduleChapter(201L, 99L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("past");
        }

        @Test
        @DisplayName("auto-publish flips SCHEDULED with publishDate <= today to PUBLISHED")
        void autoPublishFlipsDueChapters() {
            Chapter due = new Chapter();
            TestSupportBase.setField(due, "id", 300L);
            due.setChapterStatus(ChapterStatus.SCHEDULED);
            due.setPublishDate(java.time.LocalDate.now());
            due.setProductionPlan(plan);

            when(chapterRepository.findByChapterStatusAndPublishDateLessThanEqual(
                    any(ChapterStatus.class), any(java.time.LocalDate.class)))
                    .thenReturn(java.util.List.of(due));
            when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));

            int count = service.publishDueScheduledChapters();

            assertThat(count).isEqualTo(1);
            assertThat(due.getChapterStatus()).isEqualTo(ChapterStatus.PUBLISHED);
            assertThat(due.getPublishedBy()).isEqualTo(0L);  // system
            assertThat(due.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("Helper: no due chapters -> 0")
        void noDueChaptersReturnsZero() {
            when(chapterRepository.findByChapterStatusAndPublishDateLessThanEqual(
                    any(ChapterStatus.class), any(java.time.LocalDate.class)))
                    .thenReturn(java.util.List.of());

            int count = service.publishDueScheduledChapters();

            assertThat(count).isEqualTo(0);
        }
    }
}