package group1.com.MangaSystemAndManagement.model;

/**
 * BA V3 §2.1: ON_HOLD is a project-level pause (Tantou quyết). CANCELLED is terminal:
 * the Project (and its Plan) are killed. Publsihed chapters remain public for historical
 * record, but no new chapter/task/submission can be created.
 */
public enum ProjectWorkflowStatus {
    DRAFT,
    ACTIVE,
    ON_HOLD,
    COMPLETED,
    CANCELLED
}
