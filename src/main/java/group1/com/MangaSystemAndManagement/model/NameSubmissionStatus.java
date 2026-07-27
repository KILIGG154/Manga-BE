package group1.com.MangaSystemAndManagement.model;

/**
 * Workflow status for a Name Submission (manga concept / storyboard submitted
 * by a Mangaka to the Editorial Board for approval).
 *
 * <p>Lifecycle:
 * <pre>
 * PENDING_BOARD_REVIEW → (Board member votes) → PROCESSING
 * PROCESSING → (Leader APPROVED)  → APPROVED
 * PROCESSING → (Leader REJECTED)  → REJECTED
 * PROCESSING → (Leader requests)  → REVISION
 * REVISION   → (Mangaka resubmits)→ PENDING_BOARD_REVIEW
 * </pre>
 */
public enum NameSubmissionStatus {
    /** Mangaka just submitted — waiting for Editorial Board votes. */
    PENDING_BOARD_REVIEW,
    /** At least one Board member has voted; waiting for Leader's final decision. */
    PROCESSING,
    /** Leader approved — Editorial Board can now create a Project. */
    APPROVED,
    /** Leader rejected — series concept is refused. */
    REJECTED,
    /** Leader requested revisions — Mangaka must resubmit. */
    REVISION
    ,PENDING
}
