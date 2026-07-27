package group1.com.MangaSystemAndManagement.model;

/**
 * Workflow status for a Production Submission — any file submitted by an
 * {@code ASSISTANT} (ROUGH_SKETCH / FINAL) or a {@code MANGAKA} (TASK_LEVEL)
 * within the production pipeline.
 *
 * <p>Lifecycle:
 * <pre>
 * PENDING → (Mangaka / Tantō reviews) → APPROVED | REJECTED
 * </pre>
 * When REJECTED, a {@link Feedback} record linked to the same Submission
 * stores the reviewer's comment.
 */
public enum ProductionSubmissionStatus {
    /** Submission has been uploaded and is awaiting review. */
    PENDING,
    /** Reviewer approved the submission — triggers the next SubTask/Task state. */
    APPROVED,
    /** Reviewer rejected the submission — Assistant/Mangaka must revise. */
    REJECTED
}
