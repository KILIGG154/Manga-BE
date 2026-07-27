# Implementation Roadmap — Áp dụng BA V3 vào codebase

**Nguồn yêu cầu:** `ba_production_publishing_gap.md` (đối chiếu BA V3 ↔ codebase).  
**Phạm vi:** main flow (loại trừ audit).  
**Nguyên tắc:** mỗi sprint phải build được, không phá flow cũ, có migration script.

---

## Tổng quan

| Sprint | Phạm vi | Trạng thái hiện tại |
| --- | --- | --- |
| **Sprint 1 (cốt lõi Plan)** | Bỏ pre-approval Plan, mở quyền publish cho `EDITORIAL_BOARD_MEMBER`, pause/resume Plan + `pauseReason`, Force Close Plan, recall Chapter + `recallReason` + `recallCount`, migration script `PlanApprovalStatus` | Đang thực hiện |
| **Sprint 2 (state machine Chapter)** | Thêm `ChapterStatus.SCHEDULED`, `COMPLETED_NEEDS_REVIEW`, endpoint return + `rejectionCount` + override lần 3 cho Leader, `TaskWorkflowStatus.REVISION_REQUIRED`, optimistic locking `Chapter.version` | Sau Sprint 1 |
| **Sprint 3 (edge case)** | Cascade `Project.CANCELLED` → Plan + khóa Chapter, tích hợp scheduler (nếu BA chốt), nối tiếp audit | Sau Sprint 2 |

---

## Sprint 1 — Main flow cốt lõi (kế hoạch này)

### 1.1 Bỏ pre-approval Plan (BA V3 §1, §5)

**Mục tiêu:** Plan khởi tạo → `planStatus = IN_PROGRESS` ngay, không cần qua bước approve.

**File thay đổi:**

| File | Thay đổi |
| --- | --- |
| `model/ProductionPlan.java` | Thêm field `pausedBy (Long)`, `pausedAt (Instant)`, `pauseReason (Text)`; đánh `@Deprecated` field `approvalStatus`. |
| `model/PlanStatus.java` | Thêm `CANCELLED`. |
| `service/impl/ProductionPlanServiceImpl.java` | `createProductionPlan`: set `planStatus = IN_PROGRESS`, không set `PlanApprovalStatus.PENDING`. |
| `controller/ProductionPlanController.java` | Xóa hoặc đánh deprecated endpoint `POST /production-plans/{id}/approve`. |
| `service/interfaces/ProductionPlanService.java` | Đánh deprecated `approveProductionPlan`. |
| `config/DataInitialized.java` | Giữ nguyên (chỉ init role + account). |

### 1.2 Mở quyền publish cho `EDITORIAL_BOARD_MEMBER` (BA V3 §3.1)

**Mục tiêu:** cả Leader và Board đều publish được.

**File thay đổi:**

| File | Thay đổi |
| --- | --- |
| `service/impl/ProductionWorkflowServiceImpl.java` (dòng 569–604) | `publishChapter`: cho phép `LEADER_BOARD` **hoặc** `EDITORIAL_BOARD_MEMBER`. |

### 1.3 Pause / Resume Plan (BA V3 §2.2)

**Mục tiêu:** endpoint pause/resume, lưu `pausedBy/pausedAt/pauseReason`, đóng băng submit khi Plan = `PAUSED`.

**File thay đổi:**

| File | Thay đổi |
| --- | --- |
| `model/ProductionPlan.java` | (đã thêm field ở 1.1) |
| `dto/request/PausePlanRequest.java` (mới) | `{ reason: String }`. |
| `dto/response/ProductionPlanResponse.java` | (đã có — kiểm tra expose field pause). |
| `service/interfaces/ProductionPlanService.java` | Thêm method `pausePlan(planId, requesterId, reason)` và `resumePlan(planId, requesterId)`. |
| `service/impl/ProductionPlanServiceImpl.java` | Implement `pausePlan` (set `planStatus=PAUSED`, lưu pausedBy/At/Reason); `resumePlan` (set `IN_PROGRESS`, reset pause). |
| `controller/ProductionPlanController.java` | Endpoint `POST /production-plans/{id}/pause`, `POST /production-plans/{id}/resume`. |
| `service/impl/ProductionWorkflowServiceImpl.java` (`createChapter`, `assignTask`, submit) | Thêm check `plan.planStatus == PAUSED` → 409. |

### 1.4 Force Close Plan (BA V3 §2.1)

**Mục tiêu:** Leader/Board bấm "Kết thúc sớm Plan" + nhập lý do.

**File thay đổi:**

| File | Thay đổi |
| --- | --- |
| `dto/request/ForceClosePlanRequest.java` (mới) | `{ reason: String }`. |
| `service/interfaces/ProductionPlanService.java` | Thêm `forceClosePlan(planId, requesterId, reason)`. |
| `service/impl/ProductionPlanServiceImpl.java` | Implement: chỉ `LEADER_BOARD`/`EDITORIAL_BOARD_MEMBER`; set `planStatus = COMPLETED`; lưu lý do ở `pauseReason` (tái sử dụng) hoặc field mới `forceCloseReason` (thêm nếu cần tách). |
| `controller/ProductionPlanController.java` | Endpoint `POST /production-plans/{id}/force-close`. |

### 1.5 Recall Chapter (BA V3 §3.4)

**Mục tiêu:** endpoint `recallChapter` với `recallReason` ≥ 15 ký tự, tăng `recallCount`, mở khóa Task, lùi Plan `COMPLETED → IN_PROGRESS`.

**File thay đổi:**

| File | Thay đổi |
| --- | --- |
| `model/Chapter.java` | Thêm `recallCount (int, default 0)`, `recallReason (Text)`. |
| `dto/request/RecallChapterRequest.java` (mới) | `{ recallReason: String @Size(min=15) }`. |
| `service/interfaces/ProductionWorkflowService.java` | Thêm `recallChapter(chapterId, requesterId, request)`. |
| `service/impl/ProductionWorkflowServiceImpl.java` | Implement: chỉ Leader/Board; `PUBLISHED → IN_PRODUCTION`; tăng `recallCount`; mở khóa Task (set các Task `DONE → IN_PROGRESS` hoặc giữ nguyên — đã có `TaskWorkflowStatus.REVISION_REQUIRED` chưa thì đợi Sprint 2); nếu Plan = COMPLETED → set IN_PROGRESS. |
| `controller/ProductionWorkflowController.java` | Endpoint `POST /api/workflow/chapters/{chapterId}/recall`. |

### 1.6 Migration `PlanApprovalStatus` (BA V3 §5)

**Mục tiêu:** convert dữ liệu cũ sang `planStatus` mới, đánh dấu `@Deprecated`.

**File thay đổi:**

| File | Thay đổi |
| --- | --- |
| `model/ProductionPlan.java` | Thêm `@Deprecated` lên field `approvalStatus`. |
| `config/DataInitialized.java` (hoặc tạo `config/PlanApprovalMigration.java` riêng) | Thêm method `migratePlanApprovalStatus()` chạy sau `initRoles`: `PENDING → IN_PROGRESS`, `APPROVED → IN_PROGRESS`, `REJECTED → PAUSED` với `pauseReason = "Hồ sơ bị Reject từ hệ thống cũ"`. |

### 1.7 Checklist Sprint 1 (đánh dấu khi xong)

- [ ] Thêm field `pausedBy`, `pausedAt`, `pauseReason` vào `ProductionPlan.java`.
- [ ] Thêm `CANCELLED` vào `PlanStatus.java`.
- [ ] Sửa `createProductionPlan`: set `planStatus=IN_PROGRESS`, không set `PENDING`.
- [ ] Sửa `publishChapter`: cho phép `EDITORIAL_BOARD_MEMBER`.
- [ ] Thêm endpoint `POST /production-plans/{id}/pause` (+ DTO + service).
- [ ] Thêm endpoint `POST /production-plans/{id}/resume`.
- [ ] Thêm endpoint `POST /production-plans/{id}/force-close` (+ DTO + service).
- [ ] Thêm field `recallCount`, `recallReason` vào `Chapter.java`.
- [ ] Thêm endpoint `POST /api/workflow/chapters/{chapterId}/recall` (+ DTO + service).
- [ ] Đóng băng submit khi Plan = `PAUSED` (check ở `createChapter`, submit).
- [ ] Đánh `@Deprecated` field `approvalStatus`.
- [ ] Migration script chuyển `PlanApprovalStatus` cũ.
- [ ] Build + compile thành công.

---

## Sprint 2 — State machine Chapter & reject (dự kiến)

| # | Hạng mục | File chính |
| --- | --- | --- |
| 1 | Thêm `ChapterStatus.SCHEDULED`, `COMPLETED_NEEDS_REVIEW`, giữ nguyên `BACKLOG` hoặc thêm `DRAFT` | `ChapterStatus.java` |
| 2 | Endpoint `returnChapterToProduction` + `rejectionCount` + giới hạn 2 lần | `ProductionWorkflowController.java`, `Chapter.java` |
| 3 | Override lần 3 cho `LEADER_BOARD` | Endpoint mới |
| 4 | `TaskWorkflowStatus.REVISION_REQUIRED` | `TaskWorkflowStatus.java` |
| 5 | Optimistic locking `Chapter.version` (`@Version` Long) | `Chapter.java` |
| 6 | Xử lý rollback Plan `COMPLETED → IN_PROGRESS` khi Return | `ProductionWorkflowServiceImpl.java` |

---

## Sprint 3 — Edge case (dự kiến)

| # | Hạng mục | File chính |
| --- | --- | --- |
| 1 | `ProjectWorkflowStatus.CANCELLED` + cascade Plan/Chapter | `ProjectWorkflowStatus.java`, `ProductionWorkflowServiceImpl.java` |
| 2 | Scheduler `SCHEDULED → PUBLISHED` (nếu BA chốt có luồng này) | Job/CronJob mới |
| 3 | Audit/log trail (sprint tiếp theo, ngoài main flow) | TBD |

---

## Câu hỏi mở cần BA chốt trước khi Sprint 2

1. `rejectionCount` có reset khi Chapter re-complete không?
2. "Khóa Chapter" khi `Project.CANCELLED` cụ thể là cấm cái gì?
3. "Mở khóa Task" khi Recall = reset Task `DONE → IN_PROGRESS` hay chỉ cho phép set sang `REVISION_REQUIRED`?
4. `recallCount` có max không?
5. `SCHEDULED` do actor nào lên lịch, scheduler ở đâu?
6. `targetChapterCount` là tổng số chapter hay con số riêng?
7. Comment trao đổi khi `PAUSED` lưu ở bảng nào?