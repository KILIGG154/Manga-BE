package group1.com.MangaSystemAndManagement.service.impl;

import group1.com.MangaSystemAndManagement.TestSupportBase;
import group1.com.MangaSystemAndManagement.dto.request.CreateProductionPlanRequest;
import group1.com.MangaSystemAndManagement.dto.request.ExtendProductionPlanRequest;
import group1.com.MangaSystemAndManagement.exception.ResourceNotFoundException;
import group1.com.MangaSystemAndManagement.model.Account;
import group1.com.MangaSystemAndManagement.model.ChapterStatus;
import group1.com.MangaSystemAndManagement.model.PlanStatus;
import group1.com.MangaSystemAndManagement.model.ProductionPlan;
import group1.com.MangaSystemAndManagement.model.Project;
import group1.com.MangaSystemAndManagement.model.SystemRoleName;
import group1.com.MangaSystemAndManagement.repository.AccountRepository;
import group1.com.MangaSystemAndManagement.repository.ChapterRepository;
import group1.com.MangaSystemAndManagement.repository.PlanExtensionLogRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests aligned with Technical Spec v2.1 §4.
 */
@ExtendWith(MockitoExtension.class)
class ProductionPlanServiceImplTest {

    @Mock ProductionPlanRepository productionPlanRepository;
    @Mock ProjectRepository projectRepository;
    @Mock AccountRepository accountRepository;
    @Mock ChapterRepository chapterRepository;
    @Mock PlanExtensionLogRepository planExtensionLogRepository;

    @InjectMocks ProductionPlanServiceImpl service;

    private static final long TANTOU_ID = 100L;
    private static final long LEADER_ID = 200L;
    private static final long PLAN_ID = 1L;
    private static final long PROJECT_ID = 10L;

    private Account tantou;
    private Account leader;
    private Project project;
    private ProductionPlan plan;

    @BeforeEach
    void setUp() {
        tantou = TestSupportBase.accountWithRole(TANTOU_ID, SystemRoleName.TANTOU_EDITOR);
        leader = TestSupportBase.accountWithRole(LEADER_ID, SystemRoleName.LEADER_BOARD);

        project = new Project();
        TestSupportBase.setField(project, "id", PROJECT_ID);
        project.setTitle("Project X");

        plan = new ProductionPlan();
        plan.setProject(project);
        plan.setTitle("Project X - Production Plan 07/2026");
        plan.setStartDate(LocalDate.now().minusDays(5));
        plan.setEndDate(LocalDate.now().plusDays(20));
        plan.setDeadlineDate(LocalDate.now().plusDays(25));
        plan.setPublishDate(LocalDate.now().plusDays(30));
        TestSupportBase.setField(plan, "id", PLAN_ID);
    }

    // ---- §4.1 createProductionPlan ----

    @Nested
    @DisplayName("createProductionPlan (Spec v2.1 §4.1)")
    class CreateTests {

        @Test
        @DisplayName("BR-02: startDate <= today → status = ACTIVE")
        void activeWhenStartInPast() {
            when(accountRepository.findById(TANTOU_ID)).thenReturn(Optional.of(tantou));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(productionPlanRepository.existsByProjectIdAndTitle(PROJECT_ID, "P1")).thenReturn(false);
            when(productionPlanRepository.save(any(ProductionPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CreateProductionPlanRequest req = new CreateProductionPlanRequest();
            req.setTitle("P1");
            req.setStartDate(LocalDate.now().minusDays(1));
            req.setEndDate(LocalDate.now().plusDays(20));
            req.setDeadlineDate(LocalDate.now().plusDays(25));
            req.setPublishDate(LocalDate.now().plusDays(30));

            ProductionPlan result = service.createProductionPlan(PROJECT_ID, TANTOU_ID, req);

            assertThat(result.getPlanStatus()).isEqualTo(PlanStatus.ACTIVE);
            assertThat(result.getCreatedBy()).isEqualTo(TANTOU_ID);
        }

        @Test
        @DisplayName("BR-02: startDate > today → status = DRAFT")
        void draftWhenStartInFuture() {
            when(accountRepository.findById(TANTOU_ID)).thenReturn(Optional.of(tantou));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(productionPlanRepository.existsByProjectIdAndTitle(PROJECT_ID, "Future")).thenReturn(false);
            when(productionPlanRepository.save(any(ProductionPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CreateProductionPlanRequest req = new CreateProductionPlanRequest();
            req.setTitle("Future");
            req.setStartDate(LocalDate.now().plusDays(10));
            req.setEndDate(LocalDate.now().plusDays(40));
            req.setDeadlineDate(LocalDate.now().plusDays(45));
            req.setPublishDate(LocalDate.now().plusDays(50));

            ProductionPlan result = service.createProductionPlan(PROJECT_ID, TANTOU_ID, req);

            assertThat(result.getPlanStatus()).isEqualTo(PlanStatus.DRAFT);
        }

        @Test
        @DisplayName("BR-01: less than 20 days duration → IllegalArgumentException")
        void rejectsTooShortDuration() {
            when(accountRepository.findById(TANTOU_ID)).thenReturn(Optional.of(tantou));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

            CreateProductionPlanRequest req = new CreateProductionPlanRequest();
            req.setTitle("Short");
            req.setStartDate(LocalDate.now());
            req.setEndDate(LocalDate.now().plusDays(10));
            req.setDeadlineDate(LocalDate.now().plusDays(15));
            req.setPublishDate(LocalDate.now().plusDays(20));

            assertThatThrownBy(() -> service.createProductionPlan(PROJECT_ID, TANTOU_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("20 ngày");
        }

        @Test
        @DisplayName("BR-03: duplicate (project, title) → IllegalArgumentException")
        void rejectsDuplicateTitle() {
            when(accountRepository.findById(TANTOU_ID)).thenReturn(Optional.of(tantou));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(productionPlanRepository.existsByProjectIdAndTitle(PROJECT_ID, "Dup")).thenReturn(true);

            CreateProductionPlanRequest req = new CreateProductionPlanRequest();
            req.setTitle("Dup");
            req.setStartDate(LocalDate.now());
            req.setEndDate(LocalDate.now().plusDays(25));
            req.setDeadlineDate(LocalDate.now().plusDays(30));
            req.setPublishDate(LocalDate.now().plusDays(35));

            assertThatThrownBy(() -> service.createProductionPlan(PROJECT_ID, TANTOU_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tồn tại");
        }
    }

    // ---- §4.2 extendProductionPlan ----

    @Nested
    @DisplayName("extendProductionPlan (Spec v2.1 §4.2)")
    class ExtendTests {

        @Test
        @DisplayName("Tantou can extend an ACTIVE plan; status -> EXTENDED + log created")
        void extendsActivePlan() {
            plan.setPlanStatus(PlanStatus.ACTIVE);
            when(accountRepository.findById(TANTOU_ID)).thenReturn(Optional.of(tantou));
            when(productionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
            when(productionPlanRepository.save(any(ProductionPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(planExtensionLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ExtendProductionPlanRequest req = new ExtendProductionPlanRequest();
            req.setNewEndDate(LocalDate.now().plusDays(45));
            req.setReasonCode("RESOURCE_SHORTAGE");

            ProductionPlan result = service.extendProductionPlan(PLAN_ID, TANTOU_ID, req);

            assertThat(result.getPlanStatus()).isEqualTo(PlanStatus.EXTENDED);
            assertThat(result.getEndDate()).isEqualTo(LocalDate.now().plusDays(45));

            ArgumentCaptor<group1.com.MangaSystemAndManagement.model.PlanExtensionLog> captor =
                    ArgumentCaptor.forClass(group1.com.MangaSystemAndManagement.model.PlanExtensionLog.class);
            verify(planExtensionLogRepository).save(captor.capture());
            assertThat(captor.getValue().getReasonCode()).isEqualTo("RESOURCE_SHORTAGE");
        }

        @Test
        @DisplayName("Cannot extend a DRAFT plan")
        void rejectsDraftPlan() {
            plan.setPlanStatus(PlanStatus.DRAFT);
            when(accountRepository.findById(TANTOU_ID)).thenReturn(Optional.of(tantou));
            when(productionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

            ExtendProductionPlanRequest req = new ExtendProductionPlanRequest();
            req.setNewEndDate(LocalDate.now().plusDays(60));
            req.setReasonCode("OTHER");

            assertThatThrownBy(() -> service.extendProductionPlan(PLAN_ID, TANTOU_ID, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DRAFT");
        }

        @Test
        @DisplayName("newEndDate must be > current endDate")
        void rejectsEarlierEndDate() {
            plan.setPlanStatus(PlanStatus.ACTIVE);
            when(accountRepository.findById(TANTOU_ID)).thenReturn(Optional.of(tantou));
            when(productionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

            ExtendProductionPlanRequest req = new ExtendProductionPlanRequest();
            req.setNewEndDate(plan.getEndDate().minusDays(1));
            req.setReasonCode("OTHER");

            assertThatThrownBy(() -> service.extendProductionPlan(PLAN_ID, TANTOU_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lớn hơn");
        }
    }

    // ---- §4.3 completeProductionPlan ----

    @Nested
    @DisplayName("completeProductionPlan (Spec v2.1 §4.3)")
    class CompleteTests {

        @Test
        @DisplayName("BR-05: blocks when any chapter is not PUBLISHED")
        void blocksWhenChaptersIncomplete() {
            plan.setPlanStatus(PlanStatus.ACTIVE);
            when(accountRepository.findById(TANTOU_ID)).thenReturn(Optional.of(tantou));
            when(productionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
            when(chapterRepository.countByProductionPlanIdAndChapterStatusNot(PLAN_ID, ChapterStatus.PUBLISHED))
                    .thenReturn(2L);

            assertThatThrownBy(() -> service.completeProductionPlan(PLAN_ID, TANTOU_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("2");
        }

        @Test
        @DisplayName("100% chapters PUBLISHED → COMPLETED + actualEndDate")
        void completesWhenAllPublished() {
            plan.setPlanStatus(PlanStatus.ACTIVE);
            when(accountRepository.findById(TANTOU_ID)).thenReturn(Optional.of(tantou));
            when(productionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
            when(chapterRepository.countByProductionPlanIdAndChapterStatusNot(PLAN_ID, ChapterStatus.PUBLISHED))
                    .thenReturn(0L);
            when(productionPlanRepository.save(any(ProductionPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ProductionPlan result = service.completeProductionPlan(PLAN_ID, TANTOU_ID);

            assertThat(result.getPlanStatus()).isEqualTo(PlanStatus.COMPLETED);
            assertThat(result.getActualEndDate()).isEqualTo(LocalDate.now());
        }
    }

    // ---- §5 scheduled jobs ----

    @Nested
    @DisplayName("Scheduler jobs (Spec v2.1 §5)")
    class SchedulerTests {

        @Test
        @DisplayName("promoteDraftPlansToActive flips DRAFT → ACTIVE")
        void promotesDrafts() {
            ProductionPlan p = new ProductionPlan();
            TestSupportBase.setField(p, "id", 7L);
            p.setPlanStatus(PlanStatus.DRAFT);

            when(productionPlanRepository.findByPlanStatusAndStartDateLessThanEqual(
                    PlanStatus.DRAFT, LocalDate.now())).thenReturn(List.of(p));

            int n = service.promoteDraftPlansToActive(LocalDate.now());
            assertThat(n).isEqualTo(1);
            assertThat(p.getPlanStatus()).isEqualTo(PlanStatus.ACTIVE);
        }

        @Test
        @DisplayName("markOverduePlans flips ACTIVE/EXTENDED → OVERDUE")
        void marksOverdue() {
            ProductionPlan p = new ProductionPlan();
            TestSupportBase.setField(p, "id", 8L);
            p.setPlanStatus(PlanStatus.ACTIVE);

            when(productionPlanRepository.findByPlanStatusInAndEndDateBefore(
                    java.util.EnumSet.of(PlanStatus.ACTIVE, PlanStatus.EXTENDED), LocalDate.now()))
                    .thenReturn(List.of(p));

            int n = service.markOverduePlans(LocalDate.now());
            assertThat(n).isEqualTo(1);
            assertThat(p.getPlanStatus()).isEqualTo(PlanStatus.OVERDUE);
        }
    }

    // ---- auth helpers ----

    @Test
    @DisplayName("Account not found → ResourceNotFoundException")
    void rejectsMissingAccount() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.completeProductionPlan(PLAN_ID, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Assistant cannot complete → AccessDenied")
    void assistantCannotComplete() {
        Account assistant = TestSupportBase.accountWithRole(500L, SystemRoleName.ASSISTANT);
        when(accountRepository.findById(500L)).thenReturn(Optional.of(assistant));

        assertThatThrownBy(() ->
                service.completeProductionPlan(PLAN_ID, 500L))
                .isInstanceOf(AccessDeniedException.class);

        verify(productionPlanRepository, never()).save(any());
    }
}