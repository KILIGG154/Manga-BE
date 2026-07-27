package group1.com.MangaSystemAndManagement.model;

public enum PlanStatus {
    IN_PROGRESS,    // Plan created (or resumed) — chapters are being produced
    PAUSED,         // Temporarily halted (Leader / Board decision)
    COMPLETED,      // All chapters in this plan have been PUBLISHED, or Force-Close was invoked
    CANCELLED       // Plan cancelled because its parent Project was cancelled
}
