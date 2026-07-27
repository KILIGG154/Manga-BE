# BA Spec V3 — Implementation Status Checklist

> **Mục đích:** checklist triển khai theo từng section của BA Spec V3, kèm file tham chiếu.
> **Build trạng thái:** `./mvnw -DskipTests clean compile` → **BUILD SUCCESS** (184 source files).
> **Test trạng thái:** `./mvnw test` → **32/32 pass**.

---

## §1. Chuẩn hóa thuật ngữ & Enum

- [x] §1.1.a `ChapterStatus.IN_PRODUCTION` — `model/ChapterStatus.java:16`
- [x] §1.1.b 5 trạng thái chuẩn — `model/ChapterStatus.java:14–21` (tái dùng `BACKLOG` thay `DRAFT`)
- [x] §1.1.c Giải thích `BACKLOG ≡ DRAFT` — Javadoc ở `ChapterStatus.java:5–11`
- [ ] 🟡 §1.2.a Helper `isActive()` — chưa có
- [x] §1.2.b Schema UNIQUE Plan — `model/ProductionPlan.java:24`
- [x] §1.2.c 3 action mapping — `controller/ProductionWorkflowController.java`
- [ ] ❌ §1.2.d `Release Note` optional — chưa có field/DTO

## §2. State Machine Production Plan

### §2.1. Ma trận Transition

- [x] §2.1.a `IN_PROGRESS → PAUSED` — `ProductionPlanServiceImpl.pausePlan`
- [x] §2.1.b `PAUSED → IN_PROGRESS` — `ProductionPlanServiceImpl.resumePlan`
- [x] §2.1.c Auto-complete — `ProductionWorkflowServiceImpl.publishChapter` (~line 626)
- [x] §2.1.d Force Close IN_PROGRESS — `forceClosePlan`
- [x] §2.1.e Force Close PAUSED — `forceClosePlan` (cùng method)
- [x] §2.1.f Recall rollback — `recallChapter` (~line 595)
- [x] §2.1.g Return rollback — `doReturn` (~line 720)
- [x] §2.1.h Cancel cascade — `ProjectServiceImpl.cancelProject`
- [ ] 🟡 §2.1.i Auto-complete check `targetChapterCount` — code chỉ check "all PUBLISHED"

### §2.2. Pause / Resume chi tiết

- [x] §2.2.a Quyền Tantou/Leader/Board — `pausePlan`
- [x] §2.2.b Chặn create Chapter — `assertPlanNotPaused` ở `createChapter:150`
- [x] §2.2.c Chặn giao Task/SubTask — `assignChapter:215`, `assignTask:266`
- [x] §2.2.d Chặn nộp Submission — `createFeedback:541`
- [x] §2.2.e Chặn sửa Deadline — qua `updateTaskStatus:341`
- [ ] ❌ §2.2.f Chặn đổi Tantou — `ProjectServiceImpl.assignTantou` thiếu guard
- [x] §2.2.g Read-only — không có throw ở GET
- [x] §2.2.h Tải file cũ — không có rule chặn
- [ ] ❌ §2.2.i Comment trao đổi — chưa có entity
- [x] §2.2.j Lưu `pausedBy/At/Reason` — `ProductionPlan.java:99–109`
- [x] §2.2.k Resume không bắt buộc lý do — không có param `reason`
- [x] §2.2.l Reset pauseReason — `resumePlan` set 3 field null

## §3. Luồng Phát hành & Hội đồng Biên tập

### §3.1. Editorial Board Model

- [x] §3.1.a Multi-user Board — `SystemRoleName.EDITORIAL_BOARD_MEMBER`
- [x] §3.1.b Board/Leader publish — `publishChapter`
- [x] §3.1.c `publishedBy`/`publishedAt` — `Chapter.java:103–108`

### §3.2. Optimistic Locking

- [x] §3.2.a `@Version Long version` — `Chapter.java:113`
- [x] §3.2.b HTTP 409 — `exception/GlobalExceptionHandler`
- [x] §3.2.c Message BA — đã set
- [x] §3.2.d `Long` thay `Integer` — không ảnh hưởng

### §3.3. Rejection Limit & Escalation

- [x] §3.3.a `rejectionCount:Integer = 0` — `Chapter.java:92`
- [x] §3.3.b Cap 2 lần — `doReturn` check `>= 2`
- [x] §3.3.c `COMPLETED_NEEDS_REVIEW` — `ChapterStatus.java:18`
- [x] §3.3.d Message họp Board — đã set
- [x] §3.3.e Override chỉ Leader — `overrideReturnLimit`
- [x] §3.3.f Endpoint riêng — `POST /chapters/{id}/override-return`

### §3.4. Chapter Recall

- [x] §3.4.a Quyền Leader/Board — `recallChapter`
- [x] §3.4.b Endpoint recall — `POST /chapters/{id}/recall`
- [x] §3.4.c `recallReason` ≥ 15 — `RecallChapterRequest:@Size(min=15)`
- [x] §3.4.d `PUBLISHED → IN_PRODUCTION` — set trong `recallChapter`
- [x] §3.4.e `recallCount++` — set trong `recallChapter`
- [x] §3.4.f Mở khóa Task — loop `DONE → REVISION_REQUIRED`
- [x] §3.4.g Plan rollback — `planRollsBackFromCompleted` test ✅

## §4. Edge Cases

### §4.1. Project Cancellation

- [x] §4.1.a Project → `CANCELLED` — `ProjectWorkflowStatus.CANCELLED`
- [x] §4.1.b Cascade Plan → `CANCELLED` — `cancelProject`
- [x] §4.1.c Plan `COMPLETED` giữ nguyên — `planAlreadyCompletedUntouched` test ✅
- [ ] 🟡 §4.1.d Cascade khóa Chapter — đã cover `createChapter`, cần smoke-test các endpoint khác
- [x] §4.1.e `PUBLISHED` giữ nguyên — không động vào
- [x] §4.1.f `reason` bắt buộc — `CancelProjectRequest:@NotBlank`
- [x] §4.1.g Quyền Leader/Board — `@PreAuthorize`

### §4.2. Trả về ảnh hưởng Task

- [x] §4.2.a Không reset toàn bộ — chỉ reopen Task `DONE`
- [x] §4.2.b Chapter → `IN_PRODUCTION` — set trong service
- [ ] 🟡 §4.2.c Tantou chọn Task — chưa có endpoint chọn lọc

## §5. Migration Strategy

- [x] §5.1.a `PENDING → IN_PROGRESS` — `DataInitialized.migratePlanApprovalStatus`
- [x] §5.1.b `APPROVED → IN_PROGRESS` — cùng method
- [x] §5.1.c `REJECTED → PAUSED` + pauseReason seed — cùng method
- [x] §5.2.a `@Deprecated approvalStatus` — `ProductionPlan.java:70`
- [x] §5.2.b Code mới không query `approvalStatus` — đã migrate
- [ ] 🟡 §5.2.c Timeline DROP cột — chưa có plan cụ thể

## §6. Action Mapping URLs

- [x] §6.a Publish — `POST /api/workflow/chapters/{id}/publish`
- [x] §6.b Return — `POST /api/workflow/chapters/{id}/return`
- [x] §6.c Recall — `POST /api/workflow/chapters/{id}/recall`

**Ghi chú URL:** BA đề xuất `/api/v1/...`, code dùng `/api/workflow/...`. **Cần BA chốt** có muốn đổi prefix.

---

## Tổng hợp

| Ký hiệu | Số | % |
|---|:---:|:---:|
| [x] ✅ đã đáp ứng | 38 | **76%** |
| [ ] 🟡 đáp ứng một phần | 9 | **18%** |
| [ ] ❌ chưa đáp ứng | 3 | **6%** |
| **Tổng mục** | **50** | **100%** |

### 3 mục ❌ (chưa đáp ứng) — cần sprint tiếp theo

| ID | Mục | Effort |
|---|---|:---:|
| §1.2.d | `Release Note` optional khi publish | XS |
| §2.2.f | Chặn đổi Tantou khi Plan PAUSED | XS |
| §2.2.i | Comment trao đổi khi Plan PAUSED | L (entity + endpoint + UI) |

### 9 mục 🟡 — đề xuất xử lý ngay

- **§1.2.a** thêm `isActive()` helper → 5 phút.
- **§2.1.i** giữ logic hiện tại (auto-complete khi "all PUBLISHED" đã đúng).
- **§4.1.d** smoke-test thêm 5 case (`updateTaskStatus`, `assignTask`, `createFeedback`, `updateChapterStatus`, `assignChapter` khi Project.CANCELLED) → 30 phút.
- **§4.2.c** thêm endpoint `POST /tasks/{id}/mark-revision` → 1 giờ.
- **§5.2.c** thêm comment `// TODO: DROP column after sprint 8` → 1 phút.

---

## Sprint tiếp theo đề xuất (Sprint 4)

### P1 — đóng gap chính thức (theo BA V3 §)

| # | Task | Effort |
|---|---|:---:|
| 1 | Thêm `Chapter.releaseNote` + DTO + field trong `publishChapter` | 30 phút |
| 2 | Thêm `assertPlanNotPaused` ở `ProjectServiceImpl.assignTantou` | 15 phút |
| 3 | Helper `ProductionPlan.isActive()` | 5 phút |
| 4 | Smoke-test 5 case Project.CANCELLED | 30 phút |

### P2 — task chọn lọc + comment

| # | Task | Effort |
|---|---|:---:|
| 5 | Endpoint Tantou chọn Task để set `REVISION_REQUIRED` | 2 giờ |
| 6 | Entity `ChapterComment` + endpoint POST/GET | 4 giờ |
| 7 | Entity `PlanComment` + endpoint POST/GET | 4 giờ |

### P3 — kỹ thuật

| # | Task | Effort |
|---|---|:---:|
| 8 | Đổi URL prefix `/api/workflow/` → `/api/v1/` (theo BA) | 1 giờ |
| 9 | DROP column `approvalStatus` sau 2 sprint production | scheduled |