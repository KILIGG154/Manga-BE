package group1.com.MangaSystemAndManagement.dto.request;

import group1.com.MangaSystemAndManagement.model.ProjectWorkflowStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProjectBoardRequest {
    private ProjectWorkflowStatus projectWorkflowStatus;
    private Long tantouId;
}
