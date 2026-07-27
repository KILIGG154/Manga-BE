package group1.com.MangaSystemAndManagement.model;

/**
 * Workflow states for a SubTask assigned by a Mangaka to an Assistant.
 *
 * <p>State machine:
 * <pre>
 *   TODO ──► IN_PROGRESS
 *                │
 *                ▼ (Assistant submits rough sketch)
 *         ROUGH_SUBMITTED ◄─────────────────────────┐
 *                │                                  │
 *         Mangaka REJECT                            (Assistant resubmits rough)
 *                │                                  │
 *                ▼                                  │
 *         ROUGH_REJECTED ───────────────────────────┘
 *
 *         Mangaka APPROVE ──► ROUGH_APPROVED
 *                                  │
 *                                  ▼ (Assistant submits final)
 *                          FINAL_SUBMITTED ◄──────────────────┐
 *                                  │                          │
 *                          Mangaka REJECT                    (Assistant resubmits final)
 *                                  │                          │
 *                                  ▼                          │
 *                          FINAL_REJECTED ────────────────────┘
 *
 *                          Mangaka APPROVE ──► COMPLETED
 * </pre>
 *
 * <p>Transition rules enforced in {@code SubmissionServiceImpl}:
 * <ul>
 *   <li>ROUGH_SKETCH can only be submitted when status is {@code IN_PROGRESS} or {@code ROUGH_REJECTED}.</li>
 *   <li>FINAL can only be submitted when status is {@code ROUGH_APPROVED} or {@code FINAL_REJECTED}.</li>
 * </ul>
 */
public enum SubTaskWorkflowStatus {
    /** SubTask created but Assistant has not yet started. */
    TODO,
    /** Assistant is working — no submission yet. */
    IN_PROGRESS,
    /** Assistant uploaded a rough sketch; waiting for Mangaka review. */
    ROUGH_SUBMITTED,
    /** Mangaka rejected the rough sketch — Assistant must revise and resubmit. */
    ROUGH_REJECTED,
    /** Mangaka approved the rough sketch — Assistant may now submit the final version. */
    ROUGH_APPROVED,
    /** Assistant uploaded the final version; waiting for Mangaka review. */
    FINAL_SUBMITTED,
    /** Mangaka rejected the final version — Assistant must revise and resubmit. */
    FINAL_REJECTED,
    /** Mangaka approved the final version — SubTask is done. */
    COMPLETED
}
