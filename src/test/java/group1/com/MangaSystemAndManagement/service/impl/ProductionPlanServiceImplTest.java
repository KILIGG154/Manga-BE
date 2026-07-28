package group1.com.MangaSystemAndManagement.service.impl;

import group1.com.MangaSystemAndManagement.TestSupportBase;
import group1.com.MangaSystemAndManagement.dto.request.ForceClosePlanRequest;
import group1.com.MangaSystemAndManagement.dto.request.PausePlanRequest;
import group1.com.MangaSystemAndManagement.dto.request.ProductionPlanRequest;
import group1.com.MangaSystemAndManagement.exception.ResourceNotFoundException;
import group1.com.MangaSystemAndManagement.model.Account;
import group1.com.MangaSystemAndManagement.model.PlanStatus;
import group1.com.MangaSystemAndManagement.model.ProductionPlan;
import group1.com.MangaSystemAndManagement.model.Project;
import group1.com.MangaSystemAndManagement.model.SystemRoleName;
import group1.com.MangaSystemAndManagement.repository.AccountRepository;
import group1.com.MangaSystemAndManagement.repository.ProductionPlanRepository;
import group1.com.MangaSystemAndManagement.repository.ProjectRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 1 + 3 — ProductionPlanService unit tests (BA V3 §1, §2.1, §2.2).
 */
@ExtendWith(MockitoExtension.class)
class ProductionPlanServiceImplTest {

    @Mock ProductionPlanRepository productionPlanRepository;
    @Mock ProjectRepository projectRepository;
    @Mock AccountRepository accountRepository;

    @InjectMocks ProductionPlanServiceImpl service;

    private static final long TANTOU_ID = 100L;
    private static final long LEADER_ID = 200L;
    private static final long BOARD_ID = 300L;
    private static final long PLAN_ID = 1L;

    private Account tantou;
    private Account leader;
    private Account board;
    private Project project;
    private ProductionPlan plan;

    @BeforeEach
    void setUp() {
        tantou = TestSupportBase.accountWithRole(TANTOU_ID, SystemRoleName.TANTOU_EDITOR);
        leader = TestSupportBase.accountWithRole(LEADER_ID, SystemRoleName.LEADER_BOARD);
        board  = TestSupportBase.accountWithRole(BOARD_ID,  SystemRoleName.EDITORIAL_BOARD_MEMBER);

        project = new Project();
        TestSupportBase.setField(project, "id", 10L);

        plan = new ProductionPlan();
        plan.setProject(project);
        plan.setPlanStatus(PlanStatus.IN_PROGRESS);
        TestSupportBase.setField(plan, "id", PLAN_ID);
    }

    // ---- 1.1 createProductionPlan: must default to IN_PROGRESS ----

    @Nested
    @DisplayName("createProductionPlan (BA V3 §1)")
    class CreateTests {

        @Test
        @DisplayName("BA V3 §1: Plan defaults to IN_PROGRESS; no more pre-approval PENDING")
        void createsPlanInProgressWithoutPending() {
            when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
            when(productionPlanRepository.save(any(ProductionPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ProductionPlanRequest req = new ProductionPlanRequest();
            req.setMilestones("M1");
            req.setChapterTimeline("C1");

            ProductionPlan saved = service.createProductionPlan(10L, req);

            ArgumentCaptor<ProductionPlan> captor = ArgumentCaptor.forClass(ProductionPlan.class);
            verify(productionPlanRepository).save(captor.capture());
            ProductionPlan written = captor.getValue();

            assertThat(written.getPlanStatus()).isEqualTo(PlanStatus.IN_PROGRESS);
            // Decision Log 2026-07-27 §AI-10: approvalStatus removed.
            assertThat(saved.getPlanStatus()).isEqualTo(PlanStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Project not found -> RuntimeException")
        void failsWhenProjectMissing() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createProductionPlan(99L, new ProductionPlanRequest()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Project not found");

            verify(productionPlanRepository, never()).save(any());
        }
    }

    // ---- 1.2 pause / resume ----

    @Nested
    @DisplayName("pausePlan / resumePlan (BA V3 §2.2)")
    class PauseResumeTests {

        @Test
        @DisplayName("Tantou can pause; reason persisted; status -> PAUSED")
        void tantouCanPause() {
            when(accountRepository.findById(TANTOU_ID)).thenReturn(Optional.of(tantou));
            when(productionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
            when(productionPlanRepository.save(any(ProductionPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PausePlanRequest req = new PausePlanRequest();
            req.setReason("Thiếu nhân sự tháng 7");

            ProductionPlan result = service.pausePlan(PLAN_ID, TANTOU_ID, req);

            assertThat(result.getPlanStatus()).isEqualTo(PlanStatus.PAUSED);
            assertThat(result.getPauseReason()).isEqualTo("Thiếu nhân sự tháng 7");
            assertThat(result.getPausedBy()).isEqualTo(TANTOU_ID);
            assertThat(result.getPausedAt()).isNotNull();
        }

        @Test
        @DisplayName("Resume resets pause fields; status -> IN_PROGRESS")
        void resumeClearsPause() {
            plan.setPlanStatus(PlanStatus.PAUSED);
            plan.setPausedBy(TANTOU_ID);
            plan.setPauseReason("Old reason");

            when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
            when(productionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
            when(productionPlanRepository.save(any(ProductionPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ProductionPlan result = service.resumePlan(PLAN_ID, LEADER_ID);

            assertThat(result.getPlanStatus()).isEqualTo(PlanStatus.IN_PROGRESS);
            assertThat(result.getPausedBy()).isNull();
            assertThat(result.getPausedAt()).isNull();
            assertThat(result.getPauseReason()).isNull();
        }

        @Test
        @DisplayName("Cannot pause a COMPLETED plan")
        void cannotPauseTerminalPlan() {
            plan.setPlanStatus(PlanStatus.COMPLETED);
            when(accountRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
            when(productionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

            assertThatThrownBy(() ->
                    service.pausePlan(PLAN_ID, BOARD_ID, new PausePlanRequest() {{ setReason("x"); }}))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("terminal");
        }

        @Test
        @DisplayName("Resume on non-PAUSED throws IllegalStateException")
        void resumeOnlyFromPaused() {
            plan.setPlanStatus(PlanStatus.IN_PROGRESS);
            when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
            when(productionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

            assertThatThrownBy(() -> service.resumePlan(PLAN_ID, LEADER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only PAUSED plans can be resumed");
        }

        @Test
        @DisplayName("Assistant (no permission) cannot pause -> AccessDenied")
        void assistantCannotPause() {
            Account assistant = TestSupportBase.accountWithRole(500L, SystemRoleName.ASSISTANT);
            when(accountRepository.findById(500L)).thenReturn(Optional.of(assistant));

            assertThatThrownBy(() ->
                    service.pausePlan(PLAN_ID, 500L, new PausePlanRequest() {{ setReason("x"); }}))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ---- 1.3 force-close ----

    @Nested
    @DisplayName("forceClosePlan (BA V3 §2.1)")
    class ForceCloseTests {

        @Test
        @DisplayName("Leader can force-close a PAUSED plan with reason")
        void leaderCanForceClose() {
            plan.setPlanStatus(PlanStatus.PAUSED);
            plan.setPauseReason("old reason");
            when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
            when(productionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
            when(productionPlanRepository.save(any(ProductionPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ForceClosePlanRequest req = new ForceClosePlanRequest();
            req.setReason("Hết budget");

            ProductionPlan result = service.forceClosePlan(PLAN_ID, LEADER_ID, req);

            assertThat(result.getPlanStatus()).isEqualTo(PlanStatus.COMPLETED);
            assertThat(result.getPauseReason()).isEqualTo("Hết budget");
        }

        @Test
        @DisplayName("Cannot force-close from DRAFT-ish (PLANNING no longer exists) or CANCELLED")
        void cannotForceCloseFromCancelled() {
            plan.setPlanStatus(PlanStatus.CANCELLED);
            when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
            when(productionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

            ForceClosePlanRequest req = new ForceClosePlanRequest();
            req.setReason("test");

            assertThatThrownBy(() -> service.forceClosePlan(PLAN_ID, LEADER_ID, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Force-close");
        }

        @Test
        @DisplayName("Tantou cannot force-close (only Leader/Board)")
        void tantouCannotForceClose() {
            when(accountRepository.findById(TANTOU_ID)).thenReturn(Optional.of(tantou));

            assertThatThrownBy(() ->
                    service.forceClosePlan(PLAN_ID, TANTOU_ID, new ForceClosePlanRequest() {{ setReason("x"); }}))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }
}