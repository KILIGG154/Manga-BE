package group1.com.MangaSystemAndManagement.model;

/**
 * Workflow states for a SubTask assigned by a Mangaka to an Assistant.
 *
 * <p>State machine (Spec v2.1 – simplified, 1 round):
 * <pre>
 *   TODO ──► IN_PROGRESS
 *                │
 *                ▼ (Assistant submits)
 *           IN_PROGRESS ◄────────────────────────┐
 *                │                               │
 *         Mangaka REJECT                         │ (Assistant re-submits)
 *                │                               │
 *                ▼                               │
 *           IN_PROGRESS ─────────────────────────┘
 *
 *         Mangaka APPROVE ──► COMPLETED
 *                                  │
 *                                  ▼ (Mangaka submits TASK_LEVEL to Tantō)
 *                              Task = DONE
 * </pre>
 *
 * <p>Transition rules enforced in {@code SubmissionServiceImpl}:
 * <ul>
 *   <li>Assistant may submit any type (ROUGH_SKETCH / REVISION / FINAL) while
 *       SubTask is anything except {@code COMPLETED}.</li>
 *   <li>Mangaka APPROVE → {@code COMPLETED}. Mangaka may then submit
 *       {@code TASK_LEVEL} to Tantō once all sibling SubTasks are
 *       {@code COMPLETED}.</li>
 *   <li>Mangaka REJECT → back to {@code IN_PROGRESS} so Assistant re-submits.</li>
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
