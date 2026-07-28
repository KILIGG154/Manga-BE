package group1.com.MangaSystemAndManagement.service.impl;

import group1.com.MangaSystemAndManagement.TestSupportBase;
import group1.com.MangaSystemAndManagement.exception.ResourceNotFoundException;
import group1.com.MangaSystemAndManagement.model.Account;
import group1.com.MangaSystemAndManagement.model.Chapter;
import group1.com.MangaSystemAndManagement.model.ChapterStatus;
import group1.com.MangaSystemAndManagement.model.PlanStatus;
import group1.com.MangaSystemAndManagement.model.ProductionPlan;
import group1.com.MangaSystemAndManagement.model.Project;
import group1.com.MangaSystemAndManagement.model.ProjectWorkflowStatus;
import group1.com.MangaSystemAndManagement.model.SystemRoleName;
import group1.com.MangaSystemAndManagement.repository.AccountRepository;
import group1.com.MangaSystemAndManagement.repository.ChapterRepository;
import group1.com.MangaSystemAndManagement.repository.ProductionPlanRepository;
import group1.com.MangaSystemAndManagement.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

/**
 * Sprint 3 — ProjectServiceImpl.cancelProject (BA V3 §2.1 cascade).
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock ProjectRepository projectRepository;
    @Mock AccountRepository accountRepository;
    @Mock ProductionPlanRepository productionPlanRepository;
    @Mock ChapterRepository chapterRepository;

    @InjectMocks ProjectServiceImpl service;

    private static final long LEADER_ID = 1L;
    private static final long BOARD_ID = 2L;
    private static final long PROJECT_ID = 100L;
    private static final long PLAN_ID = 50L;

    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project();
        TestSupportBase.setField(project, "id", PROJECT_ID);
        project.setProjectWorkflowStatus(ProjectWorkflowStatus.ACTIVE);
    }

    @Test
    @DisplayName("Leader cancels Project; Plan cascade to CANCELLED; reason saved")
    void leaderCancelsCascading() {
        Account leader = TestSupportBase.accountWithRole(LEADER_ID, SystemRoleName.LEADER_BOARD);
        when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductionPlan plan = new ProductionPlan();
        TestSupportBase.setField(plan, "id", PLAN_ID);
        plan.setPlanStatus(PlanStatus.ACTIVE);
        plan.setProject(project);
        when(productionPlanRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.of(plan));
        when(productionPlanRepository.save(any(ProductionPlan.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(chapterRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of());

        Project result = service.cancelProject(PROJECT_ID, LEADER_ID, "Hết budget");

        assertThat(result.getProjectWorkflowStatus()).isEqualTo(ProjectWorkflowStatus.CANCELLED);
        ArgumentCaptor<ProductionPlan> planCaptor = ArgumentCaptor.forClass(ProductionPlan.class);
        verify(productionPlanRepository).save(planCaptor.capture());
        assertThat(planCaptor.getValue().getPlanStatus()).isEqualTo(PlanStatus.COMPLETED);
        assertThat(planCaptor.getValue().getActualEndDate()).isNotNull();
    }

    @Test
    @DisplayName("Editorial Board member can also cancel")
    void boardCanCancel() {
        Account board = TestSupportBase.accountWithRole(BOARD_ID, SystemRoleName.EDITORIAL_BOARD_MEMBER);
        when(accountRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productionPlanRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.empty());
        when(chapterRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of());

        Project result = service.cancelProject(PROJECT_ID, BOARD_ID, "Tác giả rút lui");

        assertThat(result.getProjectWorkflowStatus()).isEqualTo(ProjectWorkflowStatus.CANCELLED);
    }

    @Test
    @DisplayName("Tantou cannot cancel Project")
    void tantouCannotCancel() {
        Account tantou = TestSupportBase.accountWithRole(99L, SystemRoleName.TANTOU_EDITOR);
        when(accountRepository.findById(99L)).thenReturn(Optional.of(tantou));

        assertThatThrownBy(() -> service.cancelProject(PROJECT_ID, 99L, "Reason bất kỳ"))
                .isInstanceOf(AccessDeniedException.class);

        verify(projectRepository, never()).save(any());
        verify(productionPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("Missing reason -> IllegalArgumentException")
    void reasonRequired() {
        // No account stub needed — reason validation runs first.
        assertThatThrownBy(() -> service.cancelProject(PROJECT_ID, LEADER_ID, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason is required");
    }

    @Test
    @DisplayName("Cannot cancel an already-CANCELLED Project")
    void cannotCancelTwice() {
        project.setProjectWorkflowStatus(ProjectWorkflowStatus.CANCELLED);
        Account leader = TestSupportBase.accountWithRole(LEADER_ID, SystemRoleName.LEADER_BOARD);
        when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.cancelProject(PROJECT_ID, LEADER_ID, "Lý do bất kỳ"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already CANCELLED");
    }

    @Test
    @DisplayName("PUBLISHED chapters stay public — production plan stays in place when already COMPLETED")
    void planAlreadyCompletedUntouched() {
        Account leader = TestSupportBase.accountWithRole(LEADER_ID, SystemRoleName.LEADER_BOARD);

        Chapter publishedCh = new Chapter();
        TestSupportBase.setField(publishedCh, "id", 999L);
        publishedCh.setChapterStatus(ChapterStatus.PUBLISHED);

        ProductionPlan completedPlan = new ProductionPlan();
        TestSupportBase.setField(completedPlan, "id", PLAN_ID);
        completedPlan.setPlanStatus(PlanStatus.COMPLETED);
        completedPlan.setProject(project);

        when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productionPlanRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.of(completedPlan));
        when(chapterRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(publishedCh));

        service.cancelProject(PROJECT_ID, LEADER_ID, "Đã xong");

        // Verify the COMPLETED plan was NOT overwritten.
        verify(productionPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("Project not found -> ResourceNotFoundException")
    void projectNotFound() {
        Account leader = TestSupportBase.accountWithRole(LEADER_ID, SystemRoleName.LEADER_BOARD);
        when(accountRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelProject(999L, LEADER_ID, "Reason"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}