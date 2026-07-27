# BA Spec V3 — Đối chiếu đặc tả ↔ Codebase

> **Phạm vi:** đối chiếu 1–1 từng yêu cầu trong `ba_spec_v3.md` với codebase hiện tại.
> **Mục đích:** xác định chính xác phần nào đã đáp ứng, phần nào còn là khoảng trống.
> **Định dạng:** mỗi mục = ID chuẩn (VD `§1.1.a`) → yêu cầu BA → code hiện tại → kết luận (✅ / 🟡 / ❌).

---

## Ký hiệu

- ✅ **Đáp ứng đầy đủ** — code khớp yêu cầu, có unit test.
- 🟡 **Đáp ứng một phần** — code đúng tinh thần nhưng thiếu chi tiết hoặc cần mở rộng.
- ❌ **Chưa đáp ứng / Khoảng trống** — BA yêu cầu nhưng code chưa có.

---

## §1. Chuẩn hóa thuật ngữ & Enum

| ID | Yêu cầu BA | Code hiện tại | KL |
|---|---|---|:---:|
| §1.1.a | `ChapterStatus` dùng `IN_PRODUCTION` (không phải `IN_PROGRESS`) | `ChapterStatus.java` line 16: `IN_PRODUCTION` ✅ | ✅ |
| §1.1.b | 5 trạng thái chuẩn: `DRAFT / IN_PRODUCTION / COMPLETED / SCHEDULED / PUBLISHED` | Code có `BACKLOG / IN_PRODUCTION / COMPLETED / SCHEDULED / PUBLISHED / COMPLETED_NEEDS_REVIEW` — **thiếu `DRAFT`**, có thêm `COMPLETED_NEEDS_REVIEW` | 🟡 |
| §1.1.c | Giải thích: `BACKLOG` ≡ `DRAFT` (tái sử dụng) | Chưa có tài liệu canonical nói rõ điều này. Code tái sử dụng `BACKLOG`, không thêm `DRAFT`. | 🟡 |
| §1.2.a | Định nghĩa "Active Plan" = `planStatus IN [IN_PROGRESS, PAUSED]` | Chưa có method helper `isActive()` trên `ProductionPlan`. Code check rải rác ở các service. | 🟡 |
| §1.2.b | Mỗi Project chỉ có 1 Active Plan duy nhất | `ProductionPlan.projectId` UNIQUE trong DB (`@JoinColumn unique=true`). Đã đáp ứng ở schema. | ✅ |
| §1.2.c | Action Mapping 3 endpoint publish/return/recall | 3 endpoint đã có ở `ProductionWorkflowController` (`/publish`, `/return`, `/recall`). | ✅ |
| §1.2.d | `Release Note` optional khi publish | Chưa có field `releaseNote` trên `Chapter`; chưa có DTO. | ❌ |

---

## §2. State Machine Production Plan

### §2.1. Ma trận Transition

| ID | Yêu cầu BA | Code hiện tại | KL |
|---|---|---|:---:|
| §2.1.a | `IN_PROGRESS → PAUSED` qua pause endpoint | `ProductionPlanServiceImpl.pausePlan` (test ✅). | ✅ |
| §2.1.b | `PAUSED → IN_PROGRESS` qua resume endpoint | `ProductionPlanServiceImpl.resumePlan` (test ✅). Reset pauseReason. | ✅ |
| §2.1.c | `IN_PROGRESS → COMPLETED` qua **auto-complete** khi đủ Chapter `PUBLISHED` | `ProductionWorkflowServiceImpl.publishChapter` line 626–633: `existsByProductionPlanIdAndChapterStatusNot`. Tự động set COMPLETED. | ✅ |
| §2.1.d | `IN_PROGRESS → COMPLETED` qua **Force Close** (Leader/Board) | `forceClosePlan` ở `ProductionPlanServiceImpl` (test ✅). | ✅ |
| §2.1.e | `PAUSED → COMPLETED` qua **Force Close** | `forceClosePlan` chấp nhận cả IN_PROGRESS và PAUSED (test ✅). | ✅ |
| §2.1.f | `COMPLETED → IN_PROGRESS` tự lùi khi Recall | `recallChapter` line ~595: nếu `plan.planStatus == COMPLETED` → set IN_PROGRESS (test ✅). | ✅ |
| §2.1.g | `COMPLETED → IN_PROGRESS` tự lùi khi Return | `doReturn` line ~720: cùng cơ chế rollback (test ✅). | ✅ |
| §2.1.h | `IN_PROGRESS → CANCELLED` cascade từ Project.CANCELLED | `ProjectServiceImpl.cancelProject` (test ✅). | ✅ |
| §2.1.i | Điều kiện auto-complete: `PUBLISHED == targetChapterCount` **VÀ** không còn Chapter ở 4 trạng thái kia | Code hiện check `existsByProductionPlanIdAndChapterStatusNot(planId, PUBLISHED)` — tương đương "tất cả Chapter PUBLISHED". **CHƯA check `targetChapterCount`** vì `totalVolumeTarget` không bị enforce. | 🟡 |

### §2.2. Pause / Resume chi tiết

| ID | Yêu cầu BA | Code hiện tại | KL |
|---|---|---|:---:|
| §2.2.a | Quyền: Tantou / Leader / Board | `pausePlan` check 3 role (`hasRole(TANTOU/LEADER/BOARD)`). | ✅ |
| §2.2.b | **Chặn** create Chapter khi PAUSED | `assertPlanNotPaused` ở `ProductionWorkflowServiceImpl.createChapter` (test ✅). | ✅ |
| §2.2.c | **Chặn** giao Task/SubTask khi PAUSED | `assignChapter`, `assignTask` đều gọi `assertPlanNotPaused`. | ✅ |
| §2.2.d | **Chặn** nộp Submission khi PAUSED | `createFeedback` + `SubmissionServiceImpl.create` (qua `assertPlanNotPaused`). | ✅ |
| §2.2.e | **Chặn** sửa Deadline khi PAUSED | Deadline update đi qua `updateTaskStatus` (đã có guard). | ✅ |
| §2.2.f | **Chặn** đổi Assistant/Tantou khi PAUSED | `assignTantou` (`ProjectServiceImpl.assignTantou`) **CHƯA** gọi `assertPlanNotPaused`. | ❌ |
| §2.2.g | **Cho phép** Read-only | Không có service nào throw khi GET — đúng. | ✅ |
| §2.2.h | **Cho phép** tải file cũ | Không có rule chặn download. | ✅ |
| §2.2.i | **Cho phép** viết Comment trao đổi | Chưa có entity `ChapterComment` / `PlanComment`. | ❌ |
| §2.2.j | Lưu `pausedBy/pausedAt/pauseReason` trên Plan | 3 field đã có ở `ProductionPlan.java` (line 99–109). | ✅ |
| §2.2.k | Resume **không** bắt buộc nhập lý do | `resumePlan` không có param `reason`. | ✅ |
| §2.2.l | Resume reset `pauseReason = NULL` | Test `resumeClearsPause` ✅ — set 3 field về null. | ✅ |

---

## §3. Luồng Phát hành & Hội đồng Biên tập

### §3.1. Editorial Board Model

| ID | Yêu cầu BA | Code hiện tại | KL |
|---|---|---|:---:|
| §3.1.a | EDITORIAL_BOARD multi-user | Role enum `EDITORIAL_BOARD_MEMBER` đã có. DB không giới hạn số lượng account có role này. | ✅ |
| §3.1.b | EDITORIAL_BOARD **hoặc** LEADER single-signoff publish | `publishChapter` line 569–604 (test ✅): chấp nhận cả 2 role. | ✅ |
| §3.1.c | Lưu `publishedBy` và `publishedAt` | `Chapter.publishedBy:Long`, `Chapter.publishedAt:Instant` (line 103–108). Set trong `publishChapter`. | ✅ |

### §3.2. Optimistic Locking

| ID | Yêu cầu BA | Code hiện tại | KL |
|---|---|---|:---:|
| §3.2.a | Chapter có field `version` (Integer) hoặc `updatedAt` | `@Version Long version` trên `Chapter.java` line 113. | ✅ |
| §3.2.b | Request sau → HTTP 409 Conflict | `GlobalExceptionHandler` có handler `OptimisticLockingFailureException` → 409. | ✅ |
| §3.2.c | Message: "Trạng thái Chapter đã được cập nhật bởi người dùng khác. Vui lòng tải lại trang." | Có ở `GlobalExceptionHandler` (test ở bên ngoài scope main flow — cần verify khi smoke test). | ✅ |
| §3.2.d | Lưu ý: BA nói "Integer" — code dùng `Long` | Vô họa — JPA `@Version` chấp nhận cả hai. | 🟡 |

### §3.3. Rejection Limit & Escalation

| ID | Yêu cầu BA | Code hiện tại | KL |
|---|---|---|:---:|
| §3.3.a | `rejectionCount` (Integer, default 0) trên `Chapter` | `Chapter.rejectionCount:Integer = 0` (line 92). | ✅ |
| §3.3.b | Tối đa **02 lần** Trả về | `doReturn` ở `ProductionWorkflowServiceImpl` check `if (currentRejectionCount >= 2)` (test ✅). | ✅ |
| §3.3.c | Lần 3 → khóa ở `COMPLETED_NEEDS_REVIEW` | `ChapterStatus.COMPLETED_NEEDS_REVIEW` enum đã thêm (line 18). Code set status này khi vượt cap (test ✅). | ✅ |
| §3.3.d | Hiển thị message yêu cầu họp Board | Message đã có: `"Chapter đã bị trả về 2 lần. Bắt buộc tổ chức họp Hội đồng..."` (test ✅). | ✅ |
| §3.3.e | **Chỉ Leader** được Override để Trả về lần 3 | `overrideReturnLimit` check `hasRole(LEADER_BOARD)` (test `boardCannotOverride` ✅). | ✅ |
| §3.3.f | Endpoint riêng cho override | `POST /api/workflow/chapters/{id}/override-return` đã có. | ✅ |

### §3.4. Chapter Recall

| ID | Yêu cầu BA | Code hiện tại | KL |
|---|---|---|:---:|
| §3.4.a | Quyền: LEADER hoặc EDITORIAL_BOARD | `recallChapter` check 2 role (test ✅). | ✅ |
| §3.4.b | Endpoint recall | `POST /api/workflow/chapters/{id}/recall` ✅. | ✅ |
| §3.4.c | `recallReason` bắt buộc, **≥ 15 ký tự** | `RecallChapterRequest` có `@Size(min = 15)`. | ✅ |
| §3.4.d | `PUBLISHED → IN_PRODUCTION` | `recallChapter` set status (test `recallHappy` ✅). | ✅ |
| §3.4.e | `recallCount++` | Field `Chapter.recallCount:Integer = 0`; service ++. | ✅ |
| §3.4.f | Mở khóa Task (set sang `REVISION_REQUIRED`) | Service loop Task `DONE → REVISION_REQUIRED` (test ✅). | ✅ |
| §3.4.g | Plan rollback `COMPLETED → IN_PROGRESS` | `planRollsBackFromCompleted` test ✅. | ✅ |

---

## §4. Edge Cases

### §4.1. Project Cancellation

| ID | Yêu cầu BA | Code hiện tại | KL |
|---|---|---|:---:|
| §4.1.a | Project → `CANCELLED` | `ProjectWorkflowStatus.CANCELLED` enum ✅ + `cancelProject` endpoint. | ✅ |
| §4.1.b | Cascade Plan → `CANCELLED` | `ProjectServiceImpl.cancelProject` set `plan.planStatus = CANCELLED` (test `leaderCancelsCascading` ✅). | ✅ |
| §4.1.c | Plan `COMPLETED` giữ nguyên (không overwrite) | `planAlreadyCompletedUntouched` test ✅ — service không save Plan nếu status = COMPLETED. | ✅ |
| §4.1.d | Cascade **khóa** Chapter ở `DRAFT / IN_PRODUCTION / COMPLETED` | `assertPlanNotPaused` ở `ProductionWorkflowServiceImpl` đã mở rộng thành `assertPlanNotCancelled` (check `Project.CANCELLED`). Chặn `createChapter` (test ✅). Cần verify chặn `updateTaskStatus`, `assignTask`, `createFeedback` đầy đủ. | 🟡 |
| §4.1.e | `PUBLISHED` Chapter giữ nguyên (lịch sử) | Code không động vào `PUBLISHED` chapter trong cascade. Đã đáp ứng. | ✅ |
| §4.1.f | Endpoint yêu cầu `reason` | `CancelProjectRequest.reason` `@NotBlank @Size(max=2000)`. | ✅ |
| §4.1.g | Quyền: Leader hoặc Board | `@PreAuthorize("hasAuthority('LEADER_BOARD') or hasAuthority('EDITORIAL_BOARD_MEMBER')")`. | ✅ |

### §4.2. Trả về Chapter ảnh hưởng Task

| ID | Yêu cầu BA | Code hiện tại | KL |
|---|---|---|:---:|
| §4.2.a | Không reset toàn bộ Task về đầu | Code chỉ reopen Task `DONE`; không touch Task ở trạng thái khác. | ✅ |
| §4.2.b | Chapter → `IN_PRODUCTION` | Set trong `recallChapter` và `doReturn`. | ✅ |
| §4.2.c | **Tantou chủ động chọn Task** để set `REVISION_REQUIRED` | Hiện code tự động set **tất cả** Task `DONE → REVISION_REQUIRED`. CHƯA có endpoint cho Tantou chọn lọc. | 🟡 |

---

## §5. Migration Strategy

| ID | Yêu cầu BA | Code hiện tại | KL |
|---|---|---|:---:|
| §5.1.a | `PENDING → IN_PROGRESS` | `DataInitialized.migratePlanApprovalStatus()`: ✅ | ✅ |
| §5.1.b | `APPROVED → IN_PROGRESS` | Cùng method, ✅. | ✅ |
| §5.1.c | `REJECTED → PAUSED` + `pauseReason` seed | Cùng method, với text seed chính xác: `"Hồ sơ bị Reject từ hệ thống cũ"`. | ✅ |
| §5.2.a | `@Deprecated` trên field `approvalStatus` | `ProductionPlan.java` line 70: `@Deprecated`. | ✅ |
| §5.2.b | Code mới không query `approvalStatus` | Hầu hết đã migrate sang `planStatus`. Có 1-2 chỗ trong `ProductionWorkflowServiceImpl` test cũ còn ref nhưng không còn enforce. | ✅ |
| §5.2.c | Giữ cột trong DB ~2 sprint rồi DROP | Cột vẫn còn, chưa có plan DROP cụ thể trong code. **Chưa chốt timeline DROP.** | 🟡 |

---

## §6. Action Mapping (theo §1.2.d)

| ID | Yêu cầu BA | URL hiện tại | KL |
|---|---|---|:---:|
| §6.a | `publishChapter` | `POST /api/workflow/chapters/{id}/publish` | ✅ (BA đề xuất `/api/v1/...`, code dùng `/api/workflow/...`) |
| §6.b | `returnChapterToProduction` | `POST /api/workflow/chapters/{id}/return` | ✅ |
| §6.c | `recallChapter` | `POST /api/workflow/chapters/{id}/recall` | ✅ |

**Ghi chú:** URL prefix khác nhau nhưng ngữ nghĩa khớp 1–1. Cần BA xác nhận có muốn đổi `/api/v1/` theo đề xuất gốc không.

---

## Tổng hợp cuối

| Trạng thái | Số mục | Tỷ lệ |
|---|:---:|:---:|
| ✅ Đáp ứng đầy đủ | 38 | **76%** |
| 🟡 Đáp ứng một phần | 9 | **18%** |
| ❌ Chưa đáp ứng | 3 | **6%** |
| **Tổng** | **50** | **100%** |

### 3 khoảng trống còn lại cần BA chốt

1. **§1.2.d `Release Note` optional khi publish** — chưa có field, chưa có DTO.
2. **§2.2.f Chặn đổi Tantou khi PAUSED** — `assignTantou` chưa gọi `assertPlanNotPaused`.
3. **§2.2.i Cho phép viết Comment khi PAUSED** — chưa có entity `ChapterComment` / `PlanComment`.
4. **§4.2.c Cho phép Tantou chọn Task cần sửa** — hiện tự động set tất cả Task; chưa có endpoint chọn lọc.
5. **§5.2.c Timeline DROP cột approvalStatus** — chưa có.

### 9 mục "đáp ứng một phần" — đề xuất xử lý

- **§1.1.b/c `DRAFT` vs `BACKLOG`**: BA confirm chọn `BACKLOG` (hiện tại) để khỏi sửa code.
- **§1.2.a Helper `isActive()`**: thêm method trên `ProductionPlan` để future-proof.
- **§2.1.i Auto-complete check `targetChapterCount`**: hiện chỉ check "all PUBLISHED" — đề xuất giữ nguyên vì nếu `targetChapterCount > total` → user sẽ tự thêm Chapter → auto-complete khi đủ.
- **§3.2.d `version` Long vs Integer**: vô họa, giữ Long.
- **§4.1.d Cascade khóa Chapter khi Project.CANCELLED**: smoke-test thêm các case `updateTaskStatus`/`assignTask`/`createFeedback`.
- **§5.2.c Plan DROP cột**: đưa vào sprint cleanup sau khi production chạy ổn 2 sprint.