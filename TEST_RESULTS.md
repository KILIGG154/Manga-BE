# TEST_RESULTS — Kết quả chạy unit test (3 sprint)

> Tất cả test được viết với JUnit 5 + Mockito + AssertJ (có sẵn trong `spring-boot-starter-webmvc-test`).  
> Ngày chạy: 2026-07-27.

---

## Tổng kết

```
Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**32 tests, 32 pass, 0 fail.** Thời gian chạy: ~14.5s.

---

## Chi tiết từng class test

### 1. `ProductionPlanServiceImplTest` (10 tests)

| # | Test | Nghiệp vụ | Kết quả |
|---|---|---|---|
| 1 | `createsPlanInProgressWithoutPending` | Plan tạo ra vào `IN_PROGRESS` ngay, không cần pre-approval | ✅ |
| 2 | `failsWhenProjectMissing` | Trả exception nếu Project không tồn tại | ✅ |
| 3 | `tantouCanPause` | Tantou pause Plan, `pauseReason` lưu vào DB | ✅ |
| 4 | `resumeClearsPause` | Resume reset hết `pausedBy/At/Reason` | ✅ |
| 5 | `cannotPauseTerminalPlan` | Plan `COMPLETED/CANCELLED` không thể pause | ✅ |
| 6 | `resumeOnlyFromPaused` | Resume từ trạng thái khác PAUSED → exception | ✅ |
| 7 | `assistantCannotPause` | Assistant không có quyền pause (AccessDenied) | ✅ |
| 8 | `leaderCanForceClose` | Leader force-close Plan PAUSED → COMPLETED, lý do lưu | ✅ |
| 9 | `cannotForceCloseFromCancelled` | Force-close từ CANCELLED → exception | ✅ |
| 10 | `tantouCannotForceClose` | Tantou không được force-close (chỉ Leader/Board) | ✅ |

### 2. `ProductionWorkflowServiceImplTest` (15 tests)

| # | Test | Nghiệp vụ | Kết quả |
|---|---|---|---|
| 1 | `boardCanPublish` | **EDITORIAL_BOARD_MEMBER được publish** (BA V3 §3.1 mở quyền) | ✅ |
| 2 | `leaderCanPublish` | LEADER_BOARD publish được (giữ rule cũ) | ✅ |
| 3 | `cannotPublishInProduction` | Chapter không ở COMPLETED thì publish → 409 | ✅ |
| 4 | `recallHappy` | Recall PUBLISHED → IN_PRODUCTION, `recallCount++`, Task `DONE → REVISION_REQUIRED`, Plan rollback | ✅ |
| 5 | `planRollsBackFromCompleted` | Plan `COMPLETED → IN_PROGRESS` khi chapter bị recall | ✅ |
| 6 | `cannotRecallUnpublished` | Recall chapter không `PUBLISHED` → 409 | ✅ |
| 7 | `tantouCannotRecall` | Tantou không được recall (chỉ Leader/Board) | ✅ |
| 8 | `firstReturnSucceeds` | Lần return 1: COMPLETED → IN_PRODUCTION, `rejectionCount=1`, Task reopen | ✅ |
| 9 | `secondReturnSucceeds` | Lần return 2: vẫn success, `rejectionCount=2` | ✅ |
| 10 | `thirdReturnLocked` | Lần return 3 (no override): chapter khóa `COMPLETED_NEEDS_REVIEW`, không increment count | ✅ |
| 11 | `leaderOverrides` | Leader vượt cap: chapter trở lại `IN_PRODUCTION`, `rejectionCount=3` | ✅ |
| 12 | `boardCannotOverride` | Board KHÔNG override được (Leader only) | ✅ |
| 13 | `cannotReturnNonCompleted` | Return chapter không COMPLETED → 409 | ✅ |
| 14 | `createChapterPaused` | PAUSED plan chặn write operations (IllegalStateException) | ✅ |
| 15 | `createChapterCancelledProject` | CANCELLED project chặn write operations | ✅ |

### 3. `ProjectServiceImplTest` (7 tests)

| # | Test | Nghiệp vụ | Kết quả |
|---|---|---|---|
| 1 | `leaderCancelsCascading` | Leader cancel Project → Plan tự động `CANCELLED` cascade, lý do lưu | ✅ |
| 2 | `boardCanCancel` | EDITORIAL_BOARD_MEMBER cũng được cancel | ✅ |
| 3 | `tantouCannotCancel` | Tantou không được cancel (chỉ Leader/Board) | ✅ |
| 4 | `reasonRequired` | Cancel thiếu lý do → IllegalArgumentException | ✅ |
| 5 | `cannotCancelTwice` | Cancel Project đã `CANCELLED` → IllegalStateException | ✅ |
| 6 | `planAlreadyCompletedUntouched` | Plan đã `COMPLETED` thì KHÔNG bị overwrite bởi cascade | ✅ (giữ nguyên lịch sử) |
| 7 | `projectNotFound` | Project không tồn tại → ResourceNotFoundException | ✅ |

---

## Helper file

`src/test/java/group1/com/MangaSystemAndManagement/TestSupportBase.java` — utility:

- `accountWithRole(id, SystemRoleName...)` — tạo `Account` mock với role, dùng reflection để set `id` (vì field `@Id` không có setter).
- `setField(target, fieldName, value)` — helper set field private thông qua reflection, đệ quy lên class cha.

Đây là nơi giúp test tránh phải khởi tạo EntityManager thật (chỉ cần Mockito stub).

---

## Quyết định thiết kế test

1. **Dùng `@InjectMocks` + `@Mock`**: test layer service, không phải controller. Plan ban đầu đề cập controller test nhưng controller chỉ làm try/catch — validation đã test ở service.
2. **Mockito strict stubbing**: phát hiện stub thừa. Một số lỗi ban đầu (`UnnecessaryStubbingException`, `NullPointerException` do `chapterRepository.save` trả `null`, `AccessDenied` do sai role) đã được sửa và ghi nhận.
3. **`@Nested` + `@DisplayName`**: surefire output chia nhóm rõ ràng theo BA V3 section.
4. **Không dùng `@SpringBootTest`**: giữ test thuần unit, không cần context Spring nặng. Mock tất cả 8 repo của `ProductionWorkflowServiceImpl`.

---

## Cách chạy lại

```bash
cd "C:/Users/Kilig/Downloads/manga-publishing-system-backend"
./mvnw test
# Hoặc chạy 1 class:
./mvnw test -Dtest=ProductionWorkflowServiceImplTest
# Hoặc 1 method:
./mvnw test -Dtest=ProductionPlanServiceImplTest#leaderCanForceClose
```

---

## Còn lại (TODO test - ngoài scope main flow)

Các flow chưa cover test (dành cho sprint sau):

| Chưa test | Lý do |
|---|---|
| `approveProductionPlan` (deprecated) | Không còn ý nghĩa BA V3, giữ cho tương thích |
| `updateChapterStatus` roll-up DONE | Sẽ test trong integration test sau |
| `createFeedback` flow | Phức tạp vì feedback ảnh hưởng Task |
| `createProject` / `activateProject` | Đã có sẵn, không nằm trong 3 sprint |
| Migration `PlanApprovalStatus` | Cần integration test với H2 để test thật |

**Khuyến nghị:** Khi có thời gian, viết thêm `DataInitializedMigrationTest` (test migration với H2 test DB) và 1 `SubmissionFlowIntegrationTest` end-to-end.