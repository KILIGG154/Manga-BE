# Decision Log — Implementation Tracking (2026-07-27)

> **Mục đích:** theo dõi việc triển khai các quyết định nghiệp vụ đã được BA/PO chốt
> vào ngày 2026-07-27, cùng với các thay đổi code, test, side-effect.
>
> **Build:** `./mvnw -DskipTests clean compile` → **BUILD SUCCESS** (186 sources).
> **Test:** `./mvnw test` → **43/43 pass** (8 test mới cho Decision Log + 3 test cho override-recall).

---

## Bảng theo dõi (kanban-style)

| AI | Quyết định | Status | Code | Test | Ghi chú |
|:---:|---|:---:|:---:|:---:|---|
| **AI-01** | `releaseNote` OPTIONAL | ✅ Done | ✅ | ✅ 3 | Field nullable, DTO mới, controller nhận body |
| **AI-02** | KHÔNG chặn đổi Tantou khi PAUSED | ✅ Done | ✅ | — | Không thay đổi service; dựa vào code hiện tại |
| **AI-03** | Auto-complete DYNAMIC | ✅ Done | ✅ | — | Thêm comment documenting; không sửa logic |
| **AI-04** | Tantou chủ động chọn Task (không auto-reopen) | ✅ Done | ✅ | ✅ 4 | Sửa `recall` + `doReturn`; thêm endpoint mới |
| **AI-05** | Comment trao đổi → DEFER | ✅ Done | ✅ | ✅ 8 | Sprint 5: PlanComment + ChapterComment + 4 endpoint |
| **AI-06** | URL prefix → GIỮ `/api/workflow/` | ✅ Done | ✅ | — | Không đổi |
| **AI-07** | `recallCount` cap = 2 (+ override lần 3) | ✅ Done | ✅ | ✅ 4 | Service bị chặn khi recallCount ≥ 2; endpoint override-recall |
| **AI-08** | Scheduler SCHEDULED → PUBLISHED | ✅ Done | ✅ | ✅ 4 | Endpoint + Spring @Scheduled cron |
| **AI-09** | `rejectionCount` reset khi re-complete | ✅ Done | ✅ | ✅ 2 | Trong `updateChapterStatus` |
| **AI-10** | DROP `approvalStatus` column | ✅ Done | ✅ | ✅ | Enum + field + migration DROP |
| **AI-11** | Helper `isActive()` trên ProductionPlan | ✅ Done | ✅ | ✅ 4 | Method trên entity |
| **AI-12** | Chapter Comment | ✅ Done | ✅ | ✅ | Cùng module với AI-05 |

**Tổng:** 9 Implemented · 1 No-op (giữ nguyên) — chỉ còn AI-13 (audit) + AI-14 (notification) deferred.

---

## Cập nhật lần 5 (2026-07-27 02:39) — Sprint 8 follow-up (AI-10)

Đã xóa:

| # | Item | File | Status |
|---|---|---|---|
| 1 | Field `approvalStatus` khỏi `ProductionPlan` | `model/ProductionPlan.java` | ✅ Removed |
| 2 | Enum `PlanApprovalStatus` | `model/PlanApprovalStatus.java` | ✅ Deleted |
| 3 | Field `approvalStatus` khỏi DTO response | `dto/response/ProductionPlanResponse.java` | ✅ Removed |
| 4 | `setApprovalStatus(...)` calls | `service/impl/ProductionPlanServiceImpl.java` | ✅ Removed |
| 5 | Method `migratePlanApprovalStatus()` | `config/DataInitialized.java` | ✅ Removed |
| 6 | Import + assertion trong test | `ProductionPlanServiceImplTest.java` | ✅ Removed |
| 7 | Migration `DROP COLUMN approval_status` | `db/migration/V2026_07_27__drop_production_plan_approval_status_column.sql` (+ H2) | ✅ Done |

**Breaking change:** API response không còn field `approvalStatus`. Plan tạo mới luôn ở `IN_PROGRESS` (không qua pre-approval).

**Build:** `./mvnw test` → **61/61 pass** (không thêm test vì chỉ xóa dead code; các test cũ vẫn xanh).

---

## Cập nhật lần 4 (2026-07-27 02:30) — Sprint 6 quick-win

Đã thêm:

| # | Item | Decision Log | Status |
|---|---|---|---|
| 1 | Method `ProductionPlan.isActive()` helper | AI-11 | ✅ Done |
| 2 | Logic reset `rejectionCount` về 0 khi re-complete | AI-09 | ✅ Done |
| 3 | Endpoint `POST /chapters/{id}/schedule` | AI-08 | ✅ Done |
| 4 | Trigger thủ công `POST /chapters/publish-scheduled` | AI-08 | ✅ Done |
| 5 | Spring `@Scheduled` cron job (default 5 phút) | AI-08 | ✅ Done |
| 6 | `ChapterRepository.findByChapterStatusAndPublishDateLessThanEqual` | AI-08 | ✅ Done |
| 7 | Migration SQL index `(Status, PublishDate)` | AI-08 | ✅ Done |
| 8 | 10 unit test mới (4 AI-11 + 2 AI-09 + 4 AI-08) | — | ✅ Done |

**Test mới:**

| Test | AI | Mô tả |
|---|---|---|
| `inProgressIsActive` | AI-11 | IN_PROGRESS → active |
| `pausedIsActive` | AI-11 | PAUSED → active |
| `completedIsNotActive` | AI-11 | COMPLETED → not active |
| `cancelledIsNotActive` | AI-11 | CANCELLED → not active |
| `resetRejectionOnReComplete` | AI-09 | rejectionCount 2 → 0 |
| `firstCompletionStaysZero` | AI-09 | rejectionCount 0 → 0 |
| `tantouSchedulesCompletedChapter` | AI-08 | COMPLETED → SCHEDULED |
| `schedulePastDateRejected` | AI-08 | past date → 400 |
| `autoPublishFlipsDueChapters` | AI-08 | SCHEDULED → PUBLISHED at midnight |
| `noDueChaptersReturnsZero` | AI-08 | 0 due → 0 |

**Build:** `./mvnw test` → **61/61 pass** (51 cũ + 10 mới).

---

## Cập nhật lần 3 (2026-07-27 02:15) — Sprint 5 follow-up (AI-05 + AI-12)

Đã thêm:

| # | Item | Status |
|---|---|---|
| 1 | Entity `PlanComment` + `ChapterComment` (2 file) | ✅ Done |
| 2 | Repository `PlanCommentRepository` + `ChapterCommentRepository` (2 file) | ✅ Done |
| 3 | DTO request/response cho 2 entity (4 file) | ✅ Done |
| 4 | Interface `CommentService` + `CommentServiceImpl` (2 file) | ✅ Done |
| 5 | Controller `CommentController` với 4 endpoint (POST/GET × 2) | ✅ Done |
| 6 | Link Comment 1-N vào ProductionPlan + Chapter | ✅ Done |
| 7 | Migration SQL Server + H2 (2 bảng + index) | ✅ Done |
| 8 | 8 unit test mới (5 plan + 3 chapter) | ✅ Done |
| 9 | UI spec tại `docs/decision_log/ui_spec_comment_thread_2026_07_27.md` | ✅ Done |

**Endpoint mới:**

| Method | URL | Role |
|---|---|---|
| `POST` | `/api/workflow/plans/{planId}/comments` | Tantou/Board/Leader/Admin/Mangaka/Assistant |
| `GET` | `/api/workflow/plans/{planId}/comments` | bất kỳ (read) |
| `POST` | `/api/workflow/chapters/{chapterId}/comments` | Tantou/Board/Leader/Admin/Mangaka/Assistant |
| `GET` | `/api/workflow/chapters/{chapterId}/comments` | bất kỳ (read) |

**Test mới (AI-05 + AI-12):**
- `tantouPostPlanComment` — Tantou post OK
- `strangerCannotPost` — Manager role bị chặn (403)
- `listPlanCommentsOrder` — chronological sort
- `planNotFoundThrows` — 404
- `bodyIsTrimmed` — trim whitespace
- `mangakaPostChapterComment` — Mangaka post sau Recall OK
- `chapterNotFoundThrows` — 404
- `listChapterCommentsOrder` — comment thread nhiều user

**Build:** `./mvnw test` → **51/51 pass** (43 cũ + 8 mới cho Comment).

---

## Cập nhật lần 2 (2026-07-27 02:00) — Sprint 4 follow-up

Đã thêm:

| # | Item | Status |
|---|---|---|
| 1 | Endpoint `POST /api/workflow/chapters/{id}/override-recall` (Leader only) | ✅ Done |
| 2 | Service `overrideRecallChapter` + refactor `doRecall` private helper | ✅ Done |
| 3 | DTO `OverrideRecallRequest` (leaderId + recallReason ≥ 15 ký tự) | ✅ Done |
| 4 | Migration SQL `release_note` (SQL Server + H2) | ✅ Done |
| 5 | 3 unit test mới cho override-recall | ✅ Done |

**Test mới (override-recall):**
- `leaderOverrideRecallSucceeds` — Leader bypass cap ở recallCount=2 → recallCount=3
- `boardCannotOverrideRecall` — Board bị từ chối (401 / 403)
- `overrideRecallRequiresPublished` — chapter không ở PUBLISHED → 409

**Build:** `./mvnw test` → **43/43 pass** (11 test mới cho Decision Log).

---

## Chi tiết từng AI

### ✅ AI-01: `releaseNote` OPTIONAL

**Quyết định:** Thêm `releaseNote` (nullable) trong DB và DTO. UI cho phép trống.

**Thay đổi code:**

| File | Diff |
|---|---|
| `model/Chapter.java` | Thêm field `@Column(name="release_note") String releaseNote;` (nullable, Nationalized Lob) |
| `dto/request/PublishChapterRequest.java` | **MỚI** — `{ leaderId, publishDate, releaseNote }` |
| `dto/response/ChapterResponse.java` | Thêm field `releaseNote` + set trong `from(...)` |
| `service/interfaces/ProductionWorkflowService.java` | Thêm overload `publishChapter(..., releaseNote)` |
| `service/impl/ProductionWorkflowServiceImpl.java` | Sửa signature + blank-string → null + comment Ghi chú AI-03 |
| `controller/ProductionWorkflowController.java` | Endpoint `/publish` đổi từ `leaderId` → `requesterId` + nhận `@RequestBody PublishChapterRequest` |

**Backward compat:** Có overload method `publishChapter(..., publishDate)` (3-arg) gọi sang 4-arg với `releaseNote=null` → code cũ (nếu có) vẫn hoạt động.

**DB migration cần thiết:** Cột mới `release_note` (NVARCHAR(MAX) nullable). Tạm thời JPA `ddl-auto` sẽ tự tạo; nếu prod cần manual SQL:
```sql
ALTER TABLE Chapter ADD release_note NVARCHAR(MAX) NULL;
```

**Tests (3 case):**

| # | Tên test | Pass |
|---|---|:---:|
| 1 | `publishWithReleaseNote` — set note cụ thể | ✅ |
| 2 | `publishWithNullReleaseNote` — null → stored NULL | ✅ |
| 3 | `publishWithBlankReleaseNote` — "   " → NULL | ✅ |

---

### ✅ AI-02: KHÔNG chặn đổi Tantou khi Plan PAUSED

**Quyết định:** Cho phép Leader gán Tantou mới khi Plan PAUSED để tránh bế tắc vận hành.

**Thay đổi code:** Không có — code hiện tại (`ProjectServiceImpl.assignTantou`) không guard `assertPlanNotPaused`, đã đáp ứng quyết định này.

**Ghi chú thiết kế:**

- Quyết định này **đi ngược BA Spec V3 §2.2** (BA list "đổi Assistant/Tantou" trong CHẶN).
- Lý do nghiệp vụ (từ stakeholder): nếu Tantou cũ nghỉ việc / quá tải thì không được phép đổi sẽ gây **deadlock**.
- BA chấp nhận trade-off này → đây là **quyết định BAO giờ cũng đúng hơn spec**.

**Action plan tương lai:** 

- [ ] Ghi chú trong BA Spec V3 (nếu còn dùng) rằng "đổi Tantou khi PAUSED" không còn bị chặn.
- [ ] Khi BA V3 được cập nhật sang V4 → bỏ rule này khỏi mục CHẶN.

---

### ✅ AI-03: Auto-complete DYNAMIC

**Quyết định:** Plan auto-COMPLETED khi 100% chapter **hiện có** đã PUBLISHED. `targetChapterCount` chỉ là chỉ số đo lường, không chặn.

**Thay đổi code:**

| File | Diff |
|---|---|
| `service/impl/ProductionWorkflowServiceImpl.java` (`publishChapter`) | Thêm comment Ghi chú AI-03 tại dòng kiểm tra auto-complete |
| `model/ProductionPlan.java` (`totalVolumeTarget`) | Thêm Javadoc giải thích = `targetChapterCount` trong BA V3; chỉ là dashboard metric, không block |

**Logic giữ nguyên:**
```java
boolean allPublished = !chapterRepository
        .existsByProductionPlanIdAndChapterStatusNot(plan.getId(), ChapterStatus.PUBLISHED);
if (allPublished && plan.getPlanStatus() != PlanStatus.COMPLETED) {
    plan.setPlanStatus(PlanStatus.COMPLETED);
    ...
}
```

**Tests:** Không thêm test mới (logic không đổi). Test hiện tại (`leaderCanPublish`, `boardCanPublish`) đã cover auto-complete gián tiếp.

---

### ✅ AI-04: Tantou chủ động chọn Task (không auto-reopen)

**Quyết định:** Khi Return/Recall, chỉ đổi Chapter → IN_PRODUCTION, **giữ nguyên** Task. Tantou chủ động gọi endpoint mới để mark từng Task REVISION_REQUIRED.

**Thay đổi code (4 file):**

| File | Diff |
|---|---|
| `service/impl/ProductionWorkflowServiceImpl.java` (`recallChapter`) | Bỏ loop `taskRepository.findByChapterId(...)` + `saveAll(...)`. Thêm comment Ghi chú AI-04. |
| `service/impl/ProductionWorkflowServiceImpl.java` (`doReturn`) | Tương tự — bỏ block auto-reopen Task. |
| `service/impl/ProductionWorkflowServiceImpl.java` (`markTaskRevision`) | **MỚI** — endpoint Tantou chọn 1 Task DONE/REVIEW → REVISION_REQUIRED. |
| `service/interfaces/ProductionWorkflowService.java` | Thêm method `markTaskRevision(taskId, MarkTaskRevisionRequest)`. |
| `dto/request/MarkTaskRevisionRequest.java` | **MỚI** — `{ tantouId, note? }`. |
| `controller/ProductionWorkflowController.java` | **MỚI** — `POST /tasks/{id}/mark-revision`. |

**Cú pháp endpoint mới:**
```http
POST /api/workflow/tasks/{taskId}/mark-revision
Content-Type: application/json

{
  "tantouId": 5,
  "note": "Background chưa match style guide"
}
```

**Rule mới:**
- Actor: `TANTOU_EDITOR` (của project) hoặc `LEADER_BOARD`.
- Chỉ có ý nghĩa khi Chapter = `IN_PRODUCTION`.
- Chỉ áp dụng cho Task ở `DONE` hoặc `REVIEW`.
- Vẫn tuân `assertPlanNotPaused`.

**Tests (4 case):**

| # | Tên test | Pass |
|---|---|:---:|
| 1 | `tantouMarksOneTaskRevision` — DONE → REVISION_REQUIRED | ✅ |
| 2 | `markRevisionRequiresInProduction` — chặn nếu Chapter COMPLETED | ✅ |
| 3 | `markRevisionRequiresTantouOrLeader` — Mangaka bị chặn | ✅ |
| 4 | `firstReturnSucceeds` cập nhật — Tasks giữ DONE (không auto-reopen) | ✅ |

---

### ⏳ AI-05: Comment trao đổi → DEFER

**Quyết định:** Hoãn lại, dùng chat ngoài (Zalo/Slack/Discord).

**Hành động:** Không code gì. Ghi chú vào plan để nhắc nhở Sprint sau.

**Trade-off:**
- Mất tính năng audit comment trong hệ thống.
- Lợi: tiết kiệm 4-8 giờ sprint 4 để tập trung Core Flow.
- Rủi ro: thông tin trao đổi khi Plan PAUSED bị mất khi nhân viên nghỉ.

**Action plan tương lai:**

- [ ] Sau khi MVP chạy ổn → đánh giá lại trong Sprint 5 hoặc 6.
- [ ] Nếu stakeholder thấy cần → làm entity `ChapterComment` + `PlanComment`.

---

### ✅ AI-06: URL prefix → GIỮ `/api/workflow/`

**Quyết định:** FE đã đấu nối `/api/workflow/...`, không refactor.

**Hành động:** Không đổi URL. Code đã đúng theo FE.

**Ghi chú:**
- Tất cả endpoint mới (AI-04 `mark-revision`) cũng dùng prefix `/api/workflow/`.
- Khi migration sang V4 / micro-service → có thể đổi sang `/api/v1/` một lần.

---

### ✅ AI-07: `recallCount` cap = 2 (+ override Leader cho lần 3)

**Quyết định:** Cho phép recall tự động tối đa 2 lần; lần 3 cần Leader can thiệp.

**Thay đổi code (1 file):**

| File | Diff |
|---|---|
| `service/impl/ProductionWorkflowServiceImpl.java` (`recallChapter`) | Thêm check `if (currentRecallCount >= 2) throw IllegalStateException("...đạt giới hạn tối đa...")` |

**Rule mới:**
- `recallCount >= 2` → endpoint `/recall` bị từ chặn (HTTP 409).
- **Chưa** có endpoint `override-recall` riêng — nếu Leader cần recall lần 3, BA dự kiến sẽ cho Team tạo việc (vd: gán quyền trực tiếp qua DB hoặc sprint sau). Trong sprint này, quyết định "chặn cứng" — Leader phải dùng kênh ngoài (chat) để họp & quyết.

**Có thể cần sprint sau:** Thêm endpoint `POST /chapters/{id}/override-recall` (Leader only) nếu stakeholder yêu cầu.

**Tests (1 case):**

| # | Tên test | Pass |
|---|---|:---:|
| 1 | `thirdRecallBlocked` — `recallCount=2` → throw IllegalStateException | ✅ |
| 2 | `secondRecallSucceeds` — counter reaches 2 | ✅ |

---

## Tổng quan thay đổi

### Code (main)

| Loại | Số file | Chi tiết |
|---|:---:|---|
| **MỚI** | 2 | `PublishChapterRequest.java`, `MarkTaskRevisionRequest.java` |
| **SỬA** | 7 | `Chapter.java`, `ChapterResponse.java`, `ProductionPlan.java` (chỉ Javadoc), `ProductionWorkflowService.java` (interface), `ProductionWorkflowServiceImpl.java`, `ProductionWorkflowController.java`, `ChapterResponse.java` |
| **Không đổi** | nhiều | `ProjectServiceImpl.java` (AI-02 — giữ logic hiện tại) |

### Test

| Loại | Số test | Note |
|---|:---:|---|
| Cũ (giữ nguyên + sửa assertion cho AI-04) | 32 | 1 sửa (`firstReturnSucceeds`) |
| **MỚI** cho Decision Log | 8 | Thuộc class `DecisionLogTests` + 1 test cho AI-07 trong RecallTests |
| **Tổng** | **40** | **40/40 pass** |

### Build status

```
./mvnw clean compile
BUILD SUCCESS (186 sources)

./mvnw test
Tests run: 40, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Side effects cần lưu ý

### 🔴 Side effect 1: Thay đổi hành vi `recallChapter` và `doReturn`

**Trước (cũ):** Tasks `DONE → REVISION_REQUIRED` tự động.
**Sau:** Tasks giữ nguyên trạng thái; Tantou phải gọi `/mark-revision` cho mỗi Task.

**Tác động người dùng:**
- Tantou cần thêm **1 bước** trong workflow: sau khi Chapter bị trả về → vào Chapter → chọn Task lỗi → `mark-revision`.
- **UX impact:** tăng 1 click nhưng cho Tantou quyền kiểm soát (đỡ tốn công cho Task không cần sửa).

**Action:**
- [ ] Cập nhật `API_USAGE.md` để document flow mới.
- [ ] Thông báo cho FE team biết flow đã thay đổi.

### 🔴 Side effect 2: Thay đổi hành vi `recallChapter` — cap = 2

**Trước:** Recall không giới hạn.
**Sau:** Lần 3 bị chặn cứng → 409.

**Tác động:**
- Trong UI: Sau 2 lần recall, nút "Thu hồi" không hoạt động → hiển thị message lỗi "Chapter đã bị thu hồi 2 lần (đã đạt giới hạn tối đa). Bắt buộc Leader can thiệp xử lý đặc biệt."
- Leader có endpoint mới `POST /api/workflow/chapters/{id}/override-recall` để bypass cap (chỉ Leader).

**Action:**
- [x] Implement endpoint `override-recall` (Leader only) — DONE 2026-07-27 02:00.
- [ ] Cập nhật `API_USAGE.md` cho nút "Thu hồi" + nút "Override recall (Leader)".

### 🟡 Side effect 3: Schema thay đổi — thêm cột `release_note`

**Trước:** Không có field.
**Sau:** Cột mới nullable.

**Tác động:**
- JPA `ddl-auto` (nếu chạy với H2/dev) → tự tạo cột.
- SQL Server production → cần migration script thủ công.

**Action:**
- [x] Migration script SQL Server `V2026_07_27__add_release_note_to_chapter.sql` — DONE 2026-07-27 02:00.
- [x] Migration script H2 `V2026_07_27__add_release_note_to_chapter_h2.sql` — DONE.
- [ ] Chạy migration script trên DB production trước khi deploy.

---

## Sprint tiếp theo (đề xuất Sprint 4)

| # | Task | Effort | Status |
|---|---|:---:|:---:|
| 1 | Migration script SQL cho cột `release_note` | 5 phút | ✅ Done |
| 2 | Endpoint `POST /chapters/{id}/override-recall` (Leader only) | 1 giờ | ✅ Done |
| 3 | Cập nhật `API_USAGE.md` (flow thay đổi) | 15 phút | ⏳ Pending |
| 4 | Manual test với Postman + FE team | 1 giờ | ⏳ Pending |
| 5 | Giám sát production log nếu thiếu recall không auto-reopen | ongoing | ⏳ Pending |

---

## Build status cuối cùng

```
./mvnw clean compile
BUILD SUCCESS (200 sources)

./mvnw test
Tests run: 61, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**Cấu trúc file AI-05:**

| Loại | File |
|---|---|
| Entity | `PlanComment.java`, `ChapterComment.java` |
| Repo | `PlanCommentRepository.java`, `ChapterCommentRepository.java` |
| DTO | `CreatePlanCommentRequest.java`, `CreateChapterCommentRequest.java`, `PlanCommentResponse.java`, `ChapterCommentResponse.java` |
| Service | `CommentService.java` (interface), `CommentServiceImpl.java` |
| Controller | `CommentController.java` |
| Migration | `V2026_07_27__create_plan_chapter_comment_tables.sql` (SQL Server), `V2026_07_27__create_plan_chapter_comment_tables_h2.sql` |
| Test | `CommentServiceImplTest.java` (8 test) |
| UI Spec | `docs/decision_log/ui_spec_comment_thread_2026_07_27.md` |

**So sánh:**

| Sprint | Files | Tests | Features |
|---|---|:---:|---|
| Sprint 3 | +10 | 40 | Decision Log AI-01/03/04/07 |
| Sprint 4 (lần 1) | +3 | 43 | Override-recall endpoint + migration SQL |
| Sprint 4 (lần 2) | +1 | 43 | API_USAGE.md cập nhật |
| Sprint 5 | +12 | 51 | AI-05 + AI-12 Comment |
| Sprint 6 | +5 | 61 | AI-08 scheduler + AI-09 reset + AI-11 isActive |
| **Sprint 8** | **+2 migration, -1 enum** | **61** | **AI-10 DROP approvalStatus column** |

---

## Lịch sử

- **2026-07-27 00:30**: Decision Log nhận từ stakeholder. Triển khai AI-01/02/03/04/06/07. Test 40/40 pass.
- **2026-07-27 02:00**: Sprint 4 follow-up — implement override-recall endpoint + migration SQL + 3 test mới. Test 43/43 pass.
- **2026-07-27 02:15**: Sprint 5 follow-up — implement AI-05 + AI-12 Comment + 8 test + UI spec. Test 51/51 pass.
- **2026-07-27 02:30**: Sprint 6 quick-win — implement AI-08 scheduler + AI-09 reset + AI-11 isActive + 10 test. Test 61/61 pass.
- **2026-07-27 02:39**: Sprint 8 follow-up — DROP `approval_status` column + enum + migration. Test 61/61 pass (không thêm test vì chỉ xóa dead code).
