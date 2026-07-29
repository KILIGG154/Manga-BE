package group1.com.MangaSystemAndManagement.model;

/**
 * BA V3 §1.1 — 5 trạng thái chuẩn + 1 trạng thái leo thang.
 * <ul>
 *   <li>{@link #BACKLOG} — Chapter mới tạo, Tantou chưa start. (Tái sử dụng; không cần {@code DRAFT} riêng.)</li>
 *   <li>{@link #IN_PRODUCTION} — Đang sản xuất (task/subtask/submission/review).</li>
 *   <li>{@link #COMPLETED} — Mọi Task DONE; Tantou báo xong; chờ Hội đồng xem xét.</li>
 *   <li>{@link #COMPLETED_NEEDS_REVIEW} — Đã bị Trả về ≥ 2 lần; chờ Leader override để Trả về lần 3.</li>
 *   <li>{@link #SCHEDULED} — Đã chốt ngày xuất bản, chờ CronJob tự động kích hoạt (Sprint 3).</li>
 *   <li>{@link #PUBLISHED} — Đã công khai cho độc giả.</li>
 * </ul>
 */
public enum ChapterStatus {
    BACKLOG,
    IN_PRODUCTION,
    COMPLETED,
    COMPLETED_NEEDS_REVIEW,
    SCHEDULED,
    PUBLISHED,
    OVERDUE
}
