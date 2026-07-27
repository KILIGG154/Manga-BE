# CHANGES — Tổng hợp tất cả file đã sửa trong 3 sprint

> Roadmap từ `IMPLEMENTATION_ROADMAP.md`. Áp dụng `ba_production_publishing_gap.md` (BA V3) vào codebase.
> 3 sprint × 3 commit tương ứng. Tất cả file compile sạch (BUILD SUCCESS, 184 source files).

---

## Sprint 1 — Main flow cốt lõi

### File mới tạo

| File | Mục đích |
|---|---|
| `src/main/java/group1/com/MangaSystemAndManagement/dto/request/PausePlanRequest.java` | DTO `POST /production-plans/{id}/pause`: `{ reason }`, `@NotBlank @Size(max=1000)` |
| `src/main/java/group1/com/MangaSystemAndManagement/dto/request/ForceClosePlanRequest.java` | DTO `POST /production-plans/{id}/force-close`: `{ reason }` |
| `src/main/java/group1/com/MangaSystemAndManagement/dto/request/RecallChapterRequest.java` | DTO `POST /workflow/chapters/{chapterId}/recall`: `{ recallReason }`, `@NotBlank @Size(min=15, max=2000)` |

### File sửa

| File | Thay đổi |
|---|---|
| `model/PlanStatus.java` | Bỏ `PLANNING`, thêm `CANCELLED`, đổi default thành `IN_PROGRESS`; thêm Javadoc |
| `model/ProductionPlan.java` | Thêm 3 field: `pausedBy:Long`, `pausedAt:Instant`, `pauseReason:Text`; đổi default `planStatus=IN_PROGRESS`; thêm annotation `@Deprecated` cho `approvalStatus` |
| `model/Chapter.java` | Thêm 2 field: `recallCount:int (default 0)`, `recallReason:Text` |
| `service/interfaces/ProductionPlanService.java` | Thêm `pausePlan`, `resumePlan`, `forceClosePlan`; đánh `@Deprecated` `approveProductionPlan` |
| `service/impl/ProductionPlanServiceImpl.java` | Implement 3 method; `createProductionPlan` set `IN_PROGRESS` + `APPROVED` (backward-compat); `@Transactional` cho các method có side-effect |
| `service/impl/ProductionWorkflowServiceImpl.java` | Mở quyền publish cho `EDITORIAL_BOARD_MEMBER`; thêm helper `assertPlanNotPaused(...)` |
| `service/impl/ProductionWorkflowServiceImpl.java` (Sprint 1 nối tiếp) | Gọi `assertPlanNotPaused` ở 6 chỗ: `createChapter`, `assignChapter`, `assignTask`, `updateTaskStatus`, `createFeedback`, `updateChapterStatus` |
| `controller/ProductionPlanController.java` | Thêm 3 endpoint mới; `@Deprecated approve` |
| `controller/ProductionWorkflowController.java` | Thêm `POST /api/workflow/chapters/{chapterId}/recall` |
| `config/DataInitialized.java` | Thêm method `migratePlanApprovalStatus()` chạy sau `initRoles`: PENDING/APPROVED → IN_PROGRESS, REJECTED → PAUSED với pauseReason seeded |

### Endpoint mới ở Sprint 1

```
POST /api/production-plans/{id}/pause       (Tantou/Leader/Board)
POST /api/production-plans/{id}/resume      (Tantou/Leader/Board)
POST /api/production-plans/{id}/force-close (Leader/Board)
POST /api/workflow/chapters/{id}/recall     (Leader/Board)
```

### Endpoint đánh dấu deprecated

```
POST /api/production-plans/{id}/approve     → SPRINT 2 đã có thể xóa hoàn toàn
```

---

## Sprint 2 — State machine Chapter & reject

### File mới tạo

| File | Mục đích |
|---|---|
| `src/main/java/group1/com/MangaSystemAndManagement/dto/request/ReturnChapterRequest.java` | DTO `POST /workflow/chapters/{chapterId}/return`: `{ rejectionReason }` |

### File sửa

| File | Thay đổi |
|---|---|
| `model/ChapterStatus.java` | Thêm `COMPLETED_NEEDS_REVIEW`, `SCHEDULED`; tái sử dụng `BACKLOG` (không cần `DRAFT`); thêm Javadoc đầy đủ |
| `model/TaskWorkflowStatus.java` | Thêm `REVISION_REQUIRED` |
| `model/Chapter.java` | Thêm `rejectionCount` (default 0), `rejectionReason`, `publishedBy:Long`, `publishedAt:Instant`, `version:Long` với `@Version` |
| `service/interfaces/ProductionWorkflowService.java` | Thêm `returnChapterToProduction`, `overrideReturnLimit` |
| `service/impl/ProductionWorkflowServiceImpl.java` | Implement private `doReturn(...)` (share giữa return + override); mở khóa Task `DONE → REVISION_REQUIRED`; rollback Plan `COMPLETED → IN_PROGRESS`; cập nhật `publishChapter` lưu `publishedBy/At` |
| `controller/ProductionWorkflowController.java` | Thêm 2 endpoint `return` + `override-return` |
| `exception/GlobalExceptionHandler.java` | Thêm handler `OptimisticLockingFailureException` → HTTP 409 với message BA V3 §3.2 |

### Endpoint mới ở Sprint 2

```
POST /api/workflow/chapters/{chapterId}/return            (Leader/Board)
POST /api/workflow/chapters/{chapterId}/override-return   (Leader only)
```

### Rule nghiệp vụ mới

- **Giới hạn return 2 lần**: chapter bị return tự động 2 lần thì lần thứ 3 bị chặn, set trạng thái `COMPLETED_NEEDS_REVIEW`, buộc họp Board.
- **Override chỉ Leader**: `LEADER_BOARD` có thể vượt qua giới hạn.
- **Optimistic locking**: mỗi lần write vào `Chapter` phải khớp `version` → nếu không khớp trả 409.
- **Reopen Task**: tất cả Task `DONE` chuyển thành `REVISION_REQUIRED` khi chapter bị recall hoặc return.

---

## Sprint 3 — Edge case

### File mới tạo

| File | Mục đích |
|---|---|
| `src/main/java/group1/com/MangaSystemAndManagement/dto/request/CancelProjectRequest.java` | DTO `POST /projects/{projectId}/cancel`: `{ reason }` |

### File sửa

| File | Thay đổi |
|---|---|
| `model/ProjectWorkflowStatus.java` | Thêm `CANCELLED` |
| `repository/ChapterRepository.java` | Thêm method `findByProjectId(Long)` |
| `service/interfaces/ProjectService.java` | Thêm `cancelProject(projectId, requesterId, reason)` |
| `service/impl/ProjectServiceImpl.java` | Inject `ProductionPlanRepository` + `ChapterRepository`; implement `cancelProject` (cascade Plan → CANCELLED, guard `status` trùng) |
| `controller/ProjectController.java` | Thêm endpoint `POST /{projectId}/cancel` với `@PreAuthorize` cho Leader/Board |
| `service/impl/ProductionWorkflowServiceImpl.java` | Mở rộng `assertPlanNotPaused` thành `assertPlanNotPaused` cũ + check `Project.CANCELLED` (chỉ chặn write) |

### Endpoint mới ở Sprint 3

```
POST /api/projects/{projectId}/cancel   (Leader/Board)
```

### Quy tắc cascade

- Project → `CANCELLED`: bắt buộc có `reason`.
- Plan → tự động `CANCELLED` (ghi lý do vào `pauseReason` nếu Plan chưa `COMPLETED`).
- **Khóa** mọi chapter chưa `PUBLISHED` (chỉ chặn write qua guard `assertPlanNotPaused`).
- **Giữ** chapter đã `PUBLISHED` nguyên trạng (lịch sử).

---

## Tổng số file thay đổi

| Loại | Số lượng |
|---|---|
| File mới tạo (DTO + roadmap) | 7 |
| File sửa (model + service + controller + config + exception) | 17 |
| File tài liệu | 3 (`IMPLEMENTATION_ROADMAP.md`, `CHANGES.md`, `API_USAGE.md`) |
| **Tổng** | **27** |

Build kết quả cuối cùng:

```bash
./mvnw -DskipTests clean compile
# BUILD SUCCESS — 184 source files compiled
```