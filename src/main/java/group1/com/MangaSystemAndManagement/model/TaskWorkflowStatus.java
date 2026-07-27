package group1.com.MangaSystemAndManagement.model;

/**
 * BA V3 §4.2: REVISION_REQUIRED is set by Tantou on a Task after a Chapter is
 * returned or recalled, to mark which specific Task needs rework.
 */
public enum TaskWorkflowStatus {
    TODO,
    IN_PROGRESS,
    REVIEW,
    REVISION_REQUIRED,
    DONE
}
