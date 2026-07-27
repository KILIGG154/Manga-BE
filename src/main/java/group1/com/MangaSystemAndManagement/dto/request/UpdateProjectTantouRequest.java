package group1.com.MangaSystemAndManagement.dto.request;

import group1.com.MangaSystemAndManagement.model.ProjectFormat;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProjectTantouRequest {
    private String genre;
    private String targetAudience;
    private ProjectFormat format;
}
