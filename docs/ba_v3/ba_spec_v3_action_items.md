# BA Spec V3 — Action Items (Câu hỏi cần BA chốt)

> **Mục đích:** gom tất cả câu hỏi mở / quyết định nghiệp vụ còn dang dở từ BA Spec V3, sắp xếp theo mức độ ưu tiên để BA chốt trong buổi họp stakeholder (khoảng 1–2 giờ).
> **Mỗi câu hỏi có:** trạng thái hiện tại, đề xuất BA 3 năm, trade-off, lựa chọn cho stakeholder.

---

## 🟥 P0 — Phải chốt trước khi UAT (3 câu)

### AI-01: `Release Note` khi publish — bắt buộc hay optional?

**Trạng thái hiện tại:** Chưa có field `releaseNote` trên `Chapter`; chưa có DTO; `publishChapter` không nhận.

**BA Spec V3 nói:** *"`Release Note` (ghi chú xuất bản): Ghi chú không bắt buộc khi Hội đồng bấm Xuất bản."*

**Đề xuất BA 3 năm:**
- **Option A: Optional** — BA Spec V3 nói rõ "không bắt buộc" → implement optional, default `null`.
- **Option B: Không lưu** — bỏ qua luôn, không thêm field.
- **Option C: Optional nhưng có UI hint** — cho phép trống nhưng UI gợi ý nhập.

**Khuyến nghị:** **Option A** — optional nhưng có field để future-proof (vd: gửi email cho độc giả có note). Effort 30 phút.

**Stakeholder chốt:** ☐ Option A  ☐ Option B  ☐ Option C

---

### AI-02: Chặn đổi Tantou khi Plan PAUSED — có cần thiết không?

**Trạng thái hiện tại:** `ProjectServiceImpl.assignTantou` chưa gọi `assertPlanNotPaused`. 5 chỗ khác (`createChapter`, `assignChapter`, `assignTask`, `updateTaskStatus`, `createFeedback`) đã có guard.

**BA Spec V3 §2.2 nói:** *"CHẶN: … thay đổi Assistant/Tantou."*

**Đề xuất BA 3 năm:**
- BA Spec V3 liệt kê "đổi Assistant/Tantou" trong mục CHẶN, nên về nguyên tắc **phải chặn**.
- Lý do nghiệp vụ: trong khi Plan đang pause vì lý do X (thiếu nhân sự), nếu cho đổi Tantou → có thể bypass lý do pause.
- Effort: 15 phút (chỉ thêm 1 dòng `assertPlanNotPaused(plan)`).

**Khuyến nghị:** **Có, chặn**.

**Stakeholder chốt:** ☐ Chặn (theo BA)  ☐ Không chặn (cho phép đổi)

---

### AI-03: Auto-complete Plan có check `targetChapterCount` không?

**Trạng thái hiện tại:** `ProductionWorkflowServiceImpl.publishChapter` check `existsByProductionPlanIdAndChapterStatusNot(planId, PUBLISHED)` — tức "không còn Chapter nào ở trạng thái ≠ PUBLISHED". **KHÔNG check `totalVolumeTarget`** (field `targetChapterCount` ở BA Spec V3 = field `totalVolumeTarget` ở code).

**BA Spec V3 §2.1 nói:** *"Tự động: Khi (Số Chapter PUBLISHED == targetChapterCount) VÀ (Không còn Chapter nào ở 4 trạng thái)."*

**Hai cách hiểu:**
- **Cách A:** `targetChapterCount` là **mục tiêu** — Plan chỉ COMPLETED khi đạt mục tiêu. Nếu Tantou tạo 5 chapter nhưng target = 12 → Plan chỉ COMPLETED khi 12/12 PUBLISHED.
- **Cách B:** `targetChapterCount` là **tổng số chapter kế hoạch** — không khác "tổng số chapter thực tế" nếu Tantou tạo đúng số chapter theo kế hoạch.

**Đề xuất BA 3 năm:**
- Nếu `totalVolumeTarget` có sẵn từ đầu Plan → dùng Cách A (check cả 2 điều kiện).
- Nếu `totalVolumeTarget` chưa chốt lúc tạo Plan → dùng Cách B (chỉ check "all PUBLISHED").

**Khuyến nghị:** Hỏi stakeholder: BA V3 nói rõ `targetChapterCount` là **con số cố định từ đầu** hay **số chapter Tantou tạo ra**? Nếu cố định → enforce check; nếu dynamic → giữ code hiện tại.

**Stakeholder chốt:** ☐ targetChapterCount cố định → enforce  ☐ Dynamic → giữ code hiện tại

---

## 🟧 P1 — Quan trọng, chốt trong 2 sprint tới (4 câu)

### AI-04: Tantou chọn Task cần sửa sau Return/Recall

**Trạng thái hiện tại:** Code tự động set **tất cả** Task `DONE → REVISION_REQUIRED`. Chưa có endpoint cho Tantou chọn Task cụ thể.

**BA Spec V3 §4.2 nói:** *"Tantou vào Chapter chủ động chọn đúng Task cần sửa và chuyển trạng thái Task đó sang REVISION_REQUIRED."*

**Đề xuất BA 3 năm:**
- BA V3 rõ ràng nói "Tantou **chủ động chọn**" — không phải "auto".
- Code hiện tại đang **sai tinh thần BA**: auto-reopen tất cả có thể gây ra task thừa mà Mangaka phải làm lại vô ích.

**Khuyến nghị:**
- **Option A (đúng BA):** Khi Return/Recall, set Chapter = IN_PRODUCTION, **không touch Task**. Tantou phải vào chọn Task nào cần sửa.
- **Option B (hiện tại):** Auto-reopen tất cả Task DONE. Tantou không cần chọn.
- **Option C (hybrid):** Auto-reopen, nhưng Tantou có thể set Task DONE trở lại nếu không cần sửa.

**Effort:**
- Option A: đổi ~20 dòng code trong `doReturn` + `recallChapter` (bỏ loop reopen).
- Option B: giữ nguyên.
- Option C: thêm endpoint `POST /tasks/{id}/keep-done` (1 giờ).

**Stakeholder chốt:** ☐ Option A  ☐ Option B  ☐ Option C

---

### AI-05: Cho phép Comment trao đổi khi Plan PAUSED

**Trạng thái hiện tại:** ✅ IMPLEMENTED (Sprint 5 — 2026-07-27 02:15).
- Entity `PlanComment` + `ChapterComment` (gộp AI-12).
- 4 endpoint: POST/GET `/plans/{id}/comments` + POST/GET `/chapters/{id}/comments`.
- 8 unit test pass.
- UI spec tại `docs/decision_log/ui_spec_comment_thread_2026_07_27.md`.
- Roles: Tantou/Board/Leader/Admin/Mangaka/Assistant (Manager bị chặn).

**Đề xuất BA 3 năm:**
- Cần tạo entity mới + 2 endpoint POST/GET.
- Effort: 4–8 giờ (entity + service + controller + migration + test).
- Có thể defer nếu stakeholder chấp nhận "không có comment trong sprint này".

**Stakeholder chốt:** ✅ Implement Comment (sprint 5) — DONE 2026-07-27.

---

### AI-06: URL prefix `/api/v1/` theo BA đề xuất

**Trạng thái hiện tại:** Code dùng `/api/workflow/...`. BA Spec V3 §1.2 đề xuất `/api/v1/...`.

**Trade-off:**
- **Đổi prefix:** nhất quán BA, nhưng phải update toàn bộ FE integration docs.
- **Giữ prefix `/api/workflow/`:** code hiện tại chạy ổn, đỡ phải đổi.

**Khuyến nghị:** Hỏi stakeholder. Nếu chưa có FE triển khai → đổi sang `/api/v1/` ngay. Nếu FE đã chạy `/api/workflow/` → giữ.

**Stakeholder chốt:** ☐ Đổi `/api/v1/`  ☐ Giữ `/api/workflow/`

---

### AI-07: `recallCount` có giới hạn tối đa không?

**Trạng thái hiện tại:** Code không giới hạn → 1 chapter có thể bị recall vô hạn lần.

**BA Spec V3 §3.4 KHÔNG nói giới hạn.**

**Đề xuất BA 3 năm:**
- **Có** giới hạn → bảo vệ nghiệp vụ (không cho "đùa" với độc giả).
- Số lần đề xuất: **2 lần** (giống rejection). Sau 2 lần recall → khóa ở trạng thái leo thang (vd: thêm enum `RECALL_NEEDS_REVIEW`).
- Hoặc **không** giới hạn → để Leader/Board tự quyết.

**Khuyến nghị:** **Giới hạn 2 lần, leo thang lần 3 cần Leader override.** Effort: ~2 giờ.

**Stakeholder chốt:** ☐ Có giới hạn (2 lần + override)  ☐ Không giới hạn

---

## 🟨 P2 — Tốt nếu chốt được, không chốt vẫn OK (5 câu)

### AI-08: `SCHEDULED → PUBLISHED` scheduler — actor nào trigger?

**Trạng thái hiện tại:** ✅ IMPLEMENTED (Sprint 6 — 2026-07-27 02:30).
- Endpoint `POST /chapters/{id}/schedule` (Tantou/Board/Leader).
- Spring `@Scheduled` cron job (default 5 phút, override `manga.publish.cron`).
- Manual trigger `POST /chapters/publish-scheduled`.
- Index migration SQL trên `(Status, PublishDate)`.
- 4 unit test pass.

**Đề xuất BA 3 năm:**
- Cần endpoint `POST /chapters/{id}/schedule?publishDate={yyyy-MM-dd}` (Tantou/Board quyền).
- Cần Spring `@Scheduled` job chạy mỗi phút (hoặc dùng Quartz) kiểm tra chapter SCHEDULED có `publishDate <= now`.
- Effort: 4–6 giờ.

**Stakeholder chốt:** ✅ Implement scheduler (sprint 6) — DONE 2026-07-27.

---

### AI-09: `rejectionCount` reset khi re-complete?

**Trạng thái hiện tại:** ✅ IMPLEMENTED (Sprint 6 — 2026-07-27 02:30).
- Trong `ProductionWorkflowServiceImpl.updateChapterStatus(...)`: khi chapter `IN_PRODUCTION → COMPLETED` và `rejectionCount > 0`, reset về 0.
- 2 unit test pass.

**BA Spec V3 §3.3 KHÔNG nói reset.**

**Đề xuất BA 3 năm:**
- **Reset về 0** khi chapter `IN_PRODUCTION → COMPLETED` lại → cho chapter "cơ hội thứ 3" hợp lý hơn.
- **Giữ nguyên** → tích lũy, nhưng có thể vô hạn lần override.

**Khuyến nghị:** **Reset về 0 khi re-complete.** Đúng tinh thần Agile. Effort: 5 phút.

**Stakeholder chốt:** ✅ Reset về 0 — DONE 2026-07-27.

---

### AI-10: DROP column `approvalStatus` — khi nào?

**Trạng thái hiện tại:** ✅ IMPLEMENTED (Sprint 8 follow-up — 2026-07-27 02:39).
- Field `PlanApprovalStatus approvalStatus` XÓA khỏi `ProductionPlan.java`.
- Enum `PlanApprovalStatus` XÓA khỏi `model/`.
- Field `approvalStatus` XÓA khỏi `ProductionPlanResponse`.
- Migration `DROP COLUMN ProductionPlan.approval_status` (SQL Server + H2).
- 6 chỗ tham chiếu đã clean (service, DataInit, test).
- 61/61 test pass (không cần test mới vì chỉ xóa dead code).

**BA Spec V3 §5.2 nói:** *"Giữ cột trong DB khoảng 2 sprint để fallback, sau đó DROP column."*

**Đề xuất BA 3 năm:**
- Đánh dấu TODO + lên lịch DROP cho sprint 8 (sau 2 sprint production).
- Effort: 30 phút (viết migration script).

**Stakeholder chốt:** ✅ DROP sprint 8 — DONE 2026-07-27.

---

### AI-11: Helper `isActive()` trên ProductionPlan

**Trạng thái hiện tại:** ✅ IMPLEMENTED (Sprint 6 — 2026-07-27 02:30).
- Method `boolean isActive()` trên entity `ProductionPlan`.
- Trả về `true` khi `planStatus IN [IN_PROGRESS, PAUSED]`.
- 4 unit test pass.

**Code rải rác `planStatus == IN_PROGRESS || == PAUSED`** đã được giữ nguyên (backward-compat). Helper dùng cho FE/dashboard khi cần filter "Active Plans".

**BA Spec V3 §1.2 định nghĩa:** *"Active Plan: Production Plan thuộc Project có planStatus IN [IN_PROGRESS, PAUSED]."*

**Stakeholder chốt:** ✅ Có thêm — DONE 2026-07-27.

---

### AI-12: Comment entity cho CHAPTER (không chỉ Plan)

**Trạng thái hiện tại:** ✅ IMPLEMENTED (Sprint 5 — gộp cùng AI-05).
- Entity `ChapterComment` + endpoint POST/GET `/chapters/{id}/comments`.
- Dùng khi Tantou/Team cần thảo luận Task cần sửa sau Return/Recall.

**Đề xuất:** Tương tự AI-05 nhưng cho Chapter.

**Stakeholder chốt:** ✅ Implement Chapter comment — DONE.

---

## ⏳ Defer (làm trong sprint sau release production) (2 câu)

### AI-13: Audit/log trail

**Trạng thái hiện tại:** BA Spec V3 đã ghi rõ "tạm bỏ qua khối audit".

**Sprint nào:** Sau release production. Effort: lớn (1–2 sprint).

---

### AI-14: Email / In-app notification

**Trạng thái hiện tại:** Không có.

**Sprint nào:** Sprint 5 hoặc sau.

---

## Bảng tổng hợp để in ra cho stakeholder

| ID | Câu hỏi | Mức ưu tiên | Effort | Đề xuất BA |
|---|---|:---:|:---:|:---:|
| AI-01 | `Release Note` optional? | 🟥 P0 | 30 phút | Option A — Optional có field |
| AI-02 | Chặn đổi Tantou khi PAUSED? | 🟥 P0 | 15 phút | Có, chặn |
| AI-03 | Auto-complete có check targetChapterCount? | 🟥 P0 | 1 giờ | Tùy stakeholder |
| AI-04 | Tantou chọn Task sau Return/Recall? | 🟧 P1 | 1–2 giờ | Option A — đúng BA |
| AI-05 | Comment trao đổi khi PAUSED? | 🟧 P1 | 4–8 giờ | Sprint 5 |
| AI-06 | URL prefix `/api/v1/`? | 🟧 P1 | 1 giờ | Hỏi stakeholder |
| AI-07 | `recallCount` có giới hạn? | 🟧 P1 | 2 giờ | 2 lần + override |
| AI-08 | Scheduler SCHEDULED → PUBLISHED? | 🟨 P2 | 4–6 giờ | Sprint 5 |
| AI-09 | `rejectionCount` reset khi re-complete? | 🟨 P2 | 5 phút | Reset về 0 |
| AI-10 | DROP `approvalStatus` khi nào? | 🟨 P2 | 30 phút | Sprint 8 |
| AI-11 | Helper `isActive()`? | 🟨 P2 | 5 phút | Có |
| AI-12 | Chapter Comment? | 🟨 P2 | 4–8 giờ | Sprint 5 |
| AI-13 | Audit/log | ⏳ | lớn | Sau release |
| AI-14 | Notification | ⏳ | lớn | Sau release |

---

## Lịch sử

- **2026-07-27**: File tạo lần đầu — gap chính thức theo BA Spec V3.
- **2026-07-27 00:30**: Stakeholder chốt AI-01/02/03/04/06/07.
- **2026-07-27 02:15**: Sprint 5 implement AI-05 + AI-12 (Comment).
- **2026-07-27 02:30**: Sprint 6 implement AI-08 + AI-09 + AI-11.
- **2026-07-27 02:39**: Sprint 8 implement AI-10 (DROP approvalStatus).
- **Sprint tiếp theo**: cập nhật sau khi stakeholder chốt.