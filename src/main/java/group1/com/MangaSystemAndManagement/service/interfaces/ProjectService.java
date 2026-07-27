package group1.com.MangaSystemAndManagement.service.interfaces;
import group1.com.MangaSystemAndManagement.dto.request.ProjectRequest;
import group1.com.MangaSystemAndManagement.model.Project;
import java.util.List;
import java.util.Optional;
public interface ProjectService {
    Project create(ProjectRequest request);
    Optional<Project> findById(Long id);
    List<Project> findAll();
    Project update(Long id, ProjectRequest request);
    void delete(Long id);

    /**
     * Assign a Tantō (Editor-in-charge) account to an existing Project.
     * @param projectId the Project to update
     * @param tantouId  the Account id of the Tantō to assign
     * @return the updated Project
     */
    Project assignTantou(Long projectId, Long tantouId);

    /**
     * Cancel a Project (BA V3 §2.1). Cascade effects:
     *  - Project.projectWorkflowStatus = CANCELLED.
     *  - ProductionPlan.planStatus        = CANCELLED (if any plan exists).
     *  - Chapter states are locked for editing; PUBLISHED chapters remain public for history.
     *
     * Allowed: LEADER_BOARD or EDITORIAL_BOARD_MEMBER.
     *
     * @param requesterId account performing the cancellation
     * @param reason      why the project was cancelled (stored on the plan as a memo)
     */
    Project cancelProject(Long projectId, Long requesterId, String reason);
}
