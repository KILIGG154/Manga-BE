# API Usage — Hướng dẫn test & validate (BA V3 + Decision Log 2026-07-27)

> **Mục tiêu:** Hướng dẫn chạy được **toàn bộ main flow** từ lúc tạo Account → Project → Plan → Chapter → Task → SubTask → Submission → Publish.
> Mỗi API có: URL, role, validate, status code, ví dụ curl, ý nghĩa nghiệp vụ.
>
> **Build:** `./mvnw test` → **61/61 pass** · **200 source files**
> **Decision Log:** 10/12 AI implemented (AI-13 Audit + AI-14 Notification deferred)

---

## §0. Tổng quan Main Flow (End-to-End Journey)

### 0.1 Sơ đồ luồng chính

```
PHASE 1: SETUP (Admin/Leader)
  Account → Project → ProjectRole → ProductionPlan

PHASE 2: SẢN XUẤT (Tantou + Mangaka + Assistant)
  Chapter → Task → SubTask → Submission → Review → [lather, rinse, repeat]

PHASE 3: REVIEW & PUBLISH (Tantou → Board/Leader)
  Tantou complete Chapter → Board publish/return → [repeat]

PHASE 4: SCHEDULE & AUTO-PUBLISH (Scheduler)
  COMPLETED → SCHEDULED → (cron) → PUBLISHED

PHASE 5: QUALITY CONTROL (Board/Leader)
  Return/Recall → mark-revision → re-complete → re-publish

PHASE 6: CLOSE
  Force-close / Cancel Project
```

### 0.2 Danh sách endpoints theo thứ tự main flow

| # | Phase | Method | URL | Vai trò | Mô tả |
|---|---|---|---|---|---|
| 1 | Setup | `POST` | `/api/auth/register` | Admin | Tạo Account |
| 2 | Setup | `POST` | `/api/projects` | Leader | Tạo Project |
| 3 | Setup | `POST` | `/api/projects/{id}/roles` | Leader | Gán Mangaka/Assistant |
| 4 | Setup | `POST` | `/api/projects/{id}/production-plan` | Tantou | Tạo ProductionPlan (IN_PROGRESS) |
| 5 | Sản xuất | `POST` | `/api/workflow/chapters` | Tantou | Tạo Chapter (BACKLOG) |
| 6 | Sản xuất | `POST` | `/api/workflow/tasks` | Tantou | Tạo Task |
| 7 | Sản xuất | `POST` | `/api/workflow/subtasks` | Tantou | Tạo SubTask |
| 8 | Sản xuất | `POST` | `/api/submissions` | Assistant | Tạo Submission (rough/final) |
| 9 | Sản xuất | `POST` | `/api/submissions/{id}/review` | Mangaka | Review Submission |
| 10 | Review | `PUT` | `/api/workflow/chapters/{id}/status` | Tantou | Complete Chapter (COMPLETED) |
| 11 | Publish | `POST` | `/api/workflow/chapters/{id}/publish` | Board/Leader | Publish ngay |
| 12 | Publish | `POST` | `/api/workflow/chapters/{id}/schedule` | Tantou | Lên lịch (SCHEDULED) |
| 13 | QC | `POST` | `/api/workflow/chapters/{id}/return` | Board/Leader | Return về IN_PRODUCTION |
| 14 | QC | `POST` | `/api/workflow/chapters/{id}/recall` | Board/Leader | Recall PUBLISHED |
| 15 | QC | `POST` | `/api/workflow/tasks/{id}/mark-revision` | Tantou | Đánh dấu Task cần sửa |
| 16 | QC | `POST` | `/api/workflow/chapters/{id}/override-recall` | Leader only | Override recall cap |
| 17 | Pause | `POST` | `/api/production-plans/{id}/pause` | Tantou/Board/Leader | Pause Plan |
| 18 | Resume | `POST` | `/api/production-plans/{id}/resume` | Tantou/Board/Leader | Resume Plan |
| 19 | Close | `POST` | `/api/production-plans/{id}/force-close` | Leader/Board | Force-close Plan |
| 20 | Close | `POST` | `/api/projects/{id}/cancel` | Leader/Board | Cancel Project (cascade) |

---

## §1. QUẢN LÝ ACCOUNT & SETUP

### 1.1 Đăng ký Account (Admin)

```
POST /api/auth/register
Content-Type: application/json

{
  "email": "tantou2@manga.com",
  "password": "password123",
  "fullName": "Tantou Editor 2",
  "systemRole": "TANTOU_EDITOR"
}
```

- **Roles**: ADMIN only.
- **Validate**: email unique, password min 6 chars, role must match `SystemRoleName` enum.
- **Trả về**: `201 Created` + Account object (password hidden).

### 1.2 Gán vai trò cho Account (Admin)

```
POST /api/system-roles/assign
Content-Type: application/json

{
  "accountId": 5,
  "roleName": "MANGAKA"
}
```

- **Roles**: ADMIN.
- **Trả về**: `200 OK` + Account với role mới.

### 1.3 Gán Project Role (Leader gán Mangaka/Assistant vào Project)

```
POST /api/projects/{projectId}/roles
Content-Type: application/json

{
  "memberId": 6,
  "role": "MANGAKA"
}
```

- **Roles**: LEADER_BOARD.
- **Validate**: `memberId` phải là account có trong hệ thống; `role` phải là `MANGAKA` hoặc `ASSISTANT`.
- **Trả về**: `200 OK` + ProjectRole object.
- **Ý nghĩa nghiệp vụ**: trước khi tạo SubTask, Leader phải gán Mangaka/Assistant vào Project.

---

## §2. QUẢN LÝ PROJECT & PRODUCTION PLAN

### 2.1 Tạo Project

```
POST /api/projects
Content-Type: application/json

{
  "title": "Thanh Mẫu - Tập 1",
  "description": "Truyện tranh fantasy 10 tập, target 18 tuổi trở lên",
  "format": "MANGA"
}
```

- **Roles**: LEADER_BOARD.
- **Validate**: `title` `@NotBlank`, max 200; `format` must match `ProjectFormat` enum (`MANGA`, `MANHWA`, `WEBTOON`, `COMIC`).
- **Trả về**: `201 Created` + Project (`projectWorkflowStatus = DRAFT`).

### 2.2 Activate Project (tạo ProductionPlan)

```
PUT /api/projects/{projectId}/status?tantouId={tantouId}
```

- **Roles**: LEADER_BOARD.
- **Validate**: Project phải ở `DRAFT`.
- **Hiệu ứng**:
  - Project `DRAFT → ACTIVE`.
  - Tự động tạo ProductionPlan ở `planStatus = IN_PROGRESS` (không qua pre-approval — AI-10).
  - Tantou được gán làm editor chính.
- **Trả về**: `200 OK` + Project + Plan.

### 2.3 Tạo ProductionPlan chi tiết

```
POST /api/projects/{projectId}/production-plan
Content-Type: application/json

{
  "milestones": "Chương 1-3 Q1 2026, chương 4-6 Q2 2026",
  "schedule": "3 tuần/chương, 2 chapter/tuần",
  "chapterTimeline": "Tập 1: 20 chapter, mỗi chapter 20 trang",
  "deadline": "2026-12-31T23:59:59Z",
  "resources": "1 Tantou, 1 Mangaka, 2 Assistant",
  "budget": 50000000,
  "assistantAllocation": "1 assistant mỗi 10 trang",
  "priority": "HIGH",
  "risk": "Thiếu assistant mùa cao điểm"
}
```

- **Roles**: TANTOU_EDITOR, LEADER_BOARD.
- **Validate**: `deadline` must be future; `budget` >= 0; `priority` in `LOW/MEDIUM/HIGH/URGENT`.
- **Hiệu ứng**: Plan tạo ở `IN_PROGRESS` (AI-10: no pre-approval).
- **Trả về**: `201 Created` + ProductionPlan.

### 2.4 Pause Plan

```
POST /api/production-plans/{id}/pause?requesterId={tantouOrLeaderId}
Content-Type: application/json

{ "reason": "Thiếu nhân sự tháng 7" }
```

- **Roles**: TANTOU_EDITOR, LEADER_BOARD, EDITORIAL_BOARD_MEMBER.
- **Validate**: Plan không ở `COMPLETED`/`CANCELLED`; không được `PAUSED` rồi.
- **Hiệu ứng**: Plan → `PAUSED`; mọi write vào chapter/task bị chặn 409.
- **Comment**: Comment trao đổi vẫn hoạt động khi PAUSED (§2.7).

### 2.5 Resume Plan

```
POST /api/production-plans/{id}/resume?requesterId={id}
```

- **Roles**: TANTOU_EDITOR, LEADER_BOARD, EDITORIAL_BOARD_MEMBER.
- **Validate**: Plan phải đang `PAUSED`.
- **Hiệu ứng**: Plan → `IN_PROGRESS`; `pausedBy`/`pausedAt`/`pauseReason` = null.
- **Trả về**: `200 OK` + Plan.

### 2.6 Force-Close Plan

```
POST /api/production-plans/{id}/force-close?requesterId={leaderOrBoardId}
Content-Type: application/json

{ "reason": "Dự án hết budget, kết thúc sớm" }
```

- **Roles**: LEADER_BOARD, EDITORIAL_BOARD_MEMBER.
- **Validate**: Plan đang `IN_PROGRESS` hoặc `PAUSED`.
- **Hiệu ứng**: Plan → `COMPLETED` (terminal soft-state).
- **Trả về**: `200 OK` + Plan.

### 2.7 Plan Comments (khi PAUSED hoặc bình thường)

```
POST /api/workflow/plans/{planId}/comments
Content-Type: application/json

{
  "authorId": 4,
  "body": "Đề xuất: reschedule chapter 12 sang tuần sau"
}
```

- **Roles**: TANTOU_EDITOR, MANGAKA, ASSISTANT, EDITORIAL_BOARD_MEMBER, LEADER_BOARD, ADMIN.
- **Validate**: `body` 1-4000 chars (auto trim); `authorId` must be valid account.
- **Trả về**: `201 Created` + PlanCommentResponse.

```
GET /api/workflow/plans/{planId}/comments
```

- **Roles**: bất kỳ (read-only).
- **Trả về**: `200 OK` + `List<PlanCommentResponse>` sorted `createdAt ASC`.

---

## §3. CHAPTER WORKFLOW

### 3.1 Tạo Chapter

```
POST /api/workflow/chapters
Content-Type: application/json

{
  "productionPlanId": 1,
  "chapterNumber": 1,
  "title": "Chương 1: Khởi đầu"
}
```

- **Roles**: TANTOU_EDITOR.
- **Validate**: `chapterNumber` unique trong Plan; Plan không `PAUSED`/`CANCELLED`.
- **Hiệu ứng**: Chapter tạo ở `BACKLOG`.
- **Trả về**: `201 Created` + Chapter.

### 3.2 Update Chapter Status (Tantou điều khiển trạng thái)

```
PUT /api/workflow/chapters/{chapterId}/status?tantouId={tantouId}
Content-Type: application/json

{ "status": "IN_PRODUCTION" }
```

- **Roles**: TANTOU_EDITOR.
- **Validate**: Tantou phải thuộc Project; valid status transition.
- **Status hợp lệ**: `BACKLOG → IN_PRODUCTION`, `IN_PRODUCTION → COMPLETED`.
- **Hiệu ứng đặc biệt**:
  - `IN_PRODUCTION → COMPLETED`: nếu `rejectionCount > 0` → **reset về 0** (AI-09).
- **Trả về**: `200 OK` + Chapter.

### 3.3 Publish Chapter (Board/Leader)

```
POST /api/workflow/chapters/{chapterId}/publish?requesterId={id}
Content-Type: application/json

{
  "leaderId": 3,
  "publishDate": "2026-08-01",
  "releaseNote": "Chapter đầu tiên ra mắt độc giả"
}
```

- **Roles**: LEADER_BOARD **hoặc** EDITORIAL_BOARD_MEMBER.
- **Validate**: Chapter phải `COMPLETED`; không trùng `PUBLISHED`.
- **Body (AI-01 — releaseNote OPTIONAL)**: tất cả fields đều optional.
  - `releaseNote` blank/null → stored NULL.
  - `publishDate` null → defaults to today.
- **Hiệu ứng**: Chapter → `PUBLISHED`; `publishedBy`, `publishedAt`, `releaseNote` recorded.
- **Auto-complete Plan (AI-03)**: nếu tất cả chapters = PUBLISHED → Plan `IN_PROGRESS → COMPLETED`.
- **Trả về**: `200 OK` + Chapter.

### 3.4 Schedule Chapter (Tantou lên lịch — AI-08)

```
POST /api/workflow/chapters/{chapterId}/schedule?requesterId={tantouId}
Content-Type: application/json

{
  "schedulerId": 4,
  "publishDate": "2026-08-01"
}
```

- **Roles**: TANTOU_EDITOR, LEADER_BOARD, EDITORIAL_BOARD_MEMBER.
- **Validate**:
  - `publishDate` must be future (past date → 400).
  - Chapter phải `COMPLETED` hoặc `SCHEDULED` (để re-schedule).
- **Hiệu ứng**: Chapter → `SCHEDULED`, lưu `publishDate`.
- **Scheduler**: Spring `@Scheduled` (cron configurable, default 5 phút):
  - Query `findByChapterStatusAndPublishDateLessThanEqual(SCHEDULED, today)`.
  - Auto-flip → `PUBLISHED`, `publishedBy=0` (system), `publishedAt=now`.
- **Manual trigger**: `POST /api/workflow/chapters/publish-scheduled` → trả về số chapter đã publish.

### 3.5 Return Chapter (Board/Leader trả về)

```
POST /api/workflow/chapters/{chapterId}/return?requesterId={boardOrLeaderId}
Content-Type: application/json

{ "rejectionReason": "Tỉ lệ nhân vật chưa đúng style guide" }
```

- **Roles**: LEADER_BOARD, EDITORIAL_BOARD_MEMBER.
- **Validate**: Chapter phải `COMPLETED`.
- **Logic (AI-04 + AI-09)**:
  - `rejectionCount < 2`: Chapter → `IN_PRODUCTION`, `rejectionCount++`; Tasks giữ nguyên; Plan rollback nếu cần.
  - `rejectionCount >= 2`: Chapter → `COMPLETED_NEEDS_REVIEW` → **409** (cần Leader override §3.7).
  - Re-complete: `rejectionCount` reset về 0 (AI-09).
- **Trả về**: `200 OK` hoặc `409` với message.

### 3.6 Recall Chapter (Board/Leader thu hồi đã publish)

```
POST /api/workflow/chapters/{chapterId}/recall?requesterId={boardOrLeaderId}
Content-Type: application/json

{ "recallReason": "Trang 12 lỗi bố cục, độc giả phản ánh" }
```

- **Roles**: LEADER_BOARD, EDITORIAL_BOARD_MEMBER.
- **Validate**:
  - Chapter phải `PUBLISHED`.
  - `recallReason` min 15 ký tự.
  - `recallCount < 2` (AI-07 cap = 2). Nếu `>= 2` → **409**.
- **Hiệu ứng**: Chapter → `IN_PRODUCTION`; `recallCount++`; Plan rollback nếu COMPLETED.
- **KHÔNG auto-reopen Tasks** (AI-04) — Tantou phải gọi `/mark-revision` từng Task.
- **Trả về**: `200 OK` + Chapter.

### 3.7 Override Recall (Leader bypass cap — AI-07)

```
POST /api/workflow/chapters/{chapterId}/override-recall?requesterId={leaderId}
Content-Type: application/json

{
  "leaderId": 3,
  "recallReason": "Leader can thiệp lần 3 do lỗi nghiêm trọng"
}
```

- **Roles**: **Chỉ LEADER_BOARD** (Board bị 403).
- **Validate**: Chapter phải `PUBLISHED`; `recallReason` min 15.
- **Hiệu ứng**: bypass cap, Chapter → `IN_PRODUCTION`, `recallCount++`.
- **Trả về**: `200 OK` + Chapter.

### 3.8 Chapter Comments

```
POST /api/workflow/chapters/{chapterId}/comments
Content-Type: application/json

{ "authorId": 8, "body": "Mangaka xác nhận background trang 12 cần sửa" }
```

```
GET /api/workflow/chapters/{chapterId}/comments
```

- Tương tự §2.7 (Plan Comments).

---

## §4. TASK & SUBTASK WORKFLOW

### 4.1 Tạo Task (Tantou giao việc cho Mangaka)

```
POST /api/workflow/tasks
Content-Type: application/json

{
  "chapterId": 1,
  "title": "Vẽ trang 1-5: cảnh mở đầu",
  "type": "LINEART"
}
```

- **Roles**: TANTOU_EDITOR.
- **Validate**: Chapter không `PAUSED`/`CANCELLED`; `type` in `LINEART/COLORING/BACKGROUND/LETTERING`.
- **Hiệu ứng**: Task tạo ở `TODO`.
- **Trả về**: `201 Created` + Task.

### 4.2 Gán Task cho Mangaka/Assistant

```
POST /api/workflow/tasks/{taskId}/assign
Content-Type: application/json

{ "assigneeId": 6 }
```

- **Roles**: TANTOU_EDITOR.
- **Validate**: Assignee phải có `MANGAKA` hoặc `ASSISTANT` role trong Project.
- **Hiệu ứng**: Task → `IN_PROGRESS` (nếu đang `TODO`).
- **Trả về**: `200 OK` + Task.

### 4.3 Mark Task for Revision (Tantou chọn Task cần sửa — AI-04)

```
POST /api/workflow/tasks/{taskId}/mark-revision
Content-Type: application/json

{
  "tantouId": 4,
  "note": "Background chưa match style guide, cần sửa lại"
}
```

- **Roles**: TANTOU_EDITOR, LEADER_BOARD.
- **Validate**:
  - Chapter phải `IN_PRODUCTION` (sau Return/Recall).
  - Task phải `DONE` hoặc `REVIEW`.
  - Plan không `PAUSED`.
- **Hiệu ứng**: Task → `REVISION_REQUIRED`.
- **Trả về**: `200 OK` + Task.
- **Workflow đầy đủ**:
  1. Board gọi `/return` → chapter → IN_PRODUCTION.
  2. Tantou xem lý do → gọi `/mark-revision` cho từng Task cần sửa.
  3. Tasks → `REVISION_REQUIRED` → Team sửa → Tantou update → DONE.

### 4.4 Tantou Review Submission (Tantou duyệt bài của Mangaka)

```
POST /api/workflow/tasks/{taskId}/feedback
Content-Type: application/json

{
  "tantouId": 4,
  "decision": "APPROVED"
}
```

- **Roles**: TANTOU_EDITOR.
- **Validate**: Task phải `REVIEW` (Submission đã submitted).
- **Hiệu ứng**:
  - `APPROVED`: Task → `DONE`.
  - `REJECTED`: Task → `IN_PROGRESS`, Mangaka sửa lại.
- **Trả về**: `200 OK` + Task.

### 4.5 Tạo SubTask (Tantou chia nhỏ Task)

```
POST /api/workflow/subtasks
Content-Type: application/json

{
  "taskId": 1,
  "title": "Rough sketch - trang 1",
  "assignedToId": 7
}
```

- **Roles**: TANTOU_EDITOR.
- **Hiệu ứng**: SubTask tạo ở `TODO`.
- **Trả về**: `201 Created` + SubTask.

### 4.6 SubTask Status Transitions

```
PUT /api/workflow/subtasks/{subtaskId}/status?assistantId={id}
Content-Type: application/json

{ "status": "IN_PROGRESS" }
```

Valid transitions (theo `SubTaskWorkflowStatus`):

| Từ | Đến | Actor |
|---|---|---|
| `TODO` | `IN_PROGRESS` | Assistant |
| `IN_PROGRESS` | `ROUGH_SUBMITTED` | Assistant (submit rough) |
| `ROUGH_REJECTED` | `ROUGH_SUBMITTED` | Assistant (resubmit rough) |
| `ROUGH_SUBMITTED` | `ROUGH_APPROVED` | Mangaka review |
| `ROUGH_SUBMITTED` | `ROUGH_REJECTED` | Mangaka review |
| `ROUGH_APPROVED` | `FINAL_SUBMITTED` | Assistant (submit final) |
| `FINAL_REJECTED` | `FINAL_SUBMITTED` | Assistant (resubmit final) |
| `FINAL_SUBMITTED` | `COMPLETED` | Mangaka review |
| `FINAL_SUBMITTED` | `FINAL_REJECTED` | Mangaka review |

---

## §5. SUBMISSION WORKFLOW (Mangaka duyệt bài)

### 5.1 Tạo Submission (Assistant gửi bài)

```
POST /api/submissions
Content-Type: application/json

{
  "subTaskId": 1,
  "type": "ROUGH"
}
```

- **Roles**: ASSISTANT.
- **Validate**: SubTask phải ở trạng thái hợp lệ:
  - `ROUGH` chỉ khi SubTask `IN_PROGRESS` hoặc `ROUGH_REJECTED`.
  - `FINAL` chỉ khi SubTask `ROUGH_APPROVED` hoặc `FINAL_REJECTED`.
- **Hiệu ứng**: Submission tạo, SubTask → `ROUGH_SUBMITTED`/`FINAL_SUBMITTED`.
- **Trả về**: `201 Created` + Submission.

### 5.2 Review Submission (Mangaka duyệt)

```
POST /api/submissions/{submissionId}/review
Content-Type: application/json

{
  "reviewerId": 6,
  "decision": "APPROVED",
  "feedback": "Tốt, lineart chuẩn rồi"
}
```

- **Roles**: MANGAKA.
- **Validate**: Submission tồn tại.
- **Hiệu ứng**:
  - `APPROVED`:
    - SubTask `ROUGH_SUBMITTED` → `ROUGH_APPROVED`.
    - SubTask `FINAL_SUBMITTED` → `COMPLETED`.
    - → Task `REVIEW` (Tantou duyệt tiếp).
  - `REJECTED`:
    - SubTask `ROUGH_SUBMITTED` → `ROUGH_REJECTED`.
    - SubTask `FINAL_SUBMITTED` → `FINAL_REJECTED`.
- **Trả về**: `200 OK` + SubmissionReview.

---

## §6. CANCEL PROJECT (Cascade)

### 6.1 Cancel Project

```
POST /api/projects/{projectId}/cancel?requesterId={leaderOrBoardId}
Content-Type: application/json

{ "reason": "Tác giả rút lui, không có người thay thế" }
```

- **Roles**: LEADER_BOARD, EDITORIAL_BOARD_MEMBER.
- **Validate**: Project không `CANCELLED`.
- **Cascade hiệu ứng**:
  - `Project` → `CANCELLED`.
  - `ProductionPlan` → `CANCELLED` (trừ khi đã `COMPLETED`).
  - Mọi write vào Chapter/Task bị chặn 409.
  - **Chapter đã `PUBLISHED` vẫn truy cập được** (lịch sử).
- **Trả về**: `200 OK` + Project.

---

## §7. QUẢN LÝ PLAN (Pause / Resume / Force-Close)

> Các endpoint này đã được mô tả ở §2.4–2.6. Tóm tắt nhanh:

| Method | URL | Vai trò | Hiệu ứng |
|---|---|---|---|
| `POST` | `/api/production-plans/{id}/pause` | Tantou/Board/Leader | → `PAUSED`, freeze write |
| `POST` | `/api/production-plans/{id}/resume` | Tantou/Board/Leader | → `IN_PROGRESS` |
| `POST` | `/api/production-plans/{id}/force-close` | Leader/Board | → `COMPLETED` |
| `POST` | `/api/workflow/plans/{id}/comments` | Tantou/Board/Leader/... | Tạo comment (works when PAUSED) |
| `GET` | `/api/workflow/plans/{id}/comments` | any | List comments |

**Helper `isActive()` (AI-11):** `plan.isActive()` = `planStatus IN [IN_PROGRESS, PAUSED]`. Dùng cho dashboard filter.

---

## §8. QUẢN LÝ COMMENT

### 8.1 Plan Comments

| Method | URL | Roles | Mô tả |
|---|---|---|---|
| `POST` | `/api/workflow/plans/{planId}/comments` | Tantou/Board/Leader/Mangaka/Assistant/Admin | Tạo comment |
| `GET` | `/api/workflow/plans/{planId}/comments` | any | List (sort `createdAt ASC`) |

### 8.2 Chapter Comments

| Method | URL | Roles | Mô tả |
|---|---|---|---|
| `POST` | `/api/workflow/chapters/{chapterId}/comments` | Tantou/Board/Leader/Mangaka/Assistant/Admin | Tạo comment |
| `GET` | `/api/workflow/chapters/{chapterId}/comments` | any | List (sort `createdAt ASC`) |

---

## §9. QUICK-START — Một curl script chạy được main flow

> Chạy script này từ terminal (Linux/macOS/PowerShell) sau khi `./mvnw spring-boot:run`.
>
> **Cổng mặc định:** `6003`. Đổi bằng: `export BASE_URL=http://localhost:XXXX/api`
>
> **Lưu ý NCLOB:** Nếu gặp lỗi 500 `Could not extract column... NCLOB`, cần chạy migration:
> ```powershell
> powershell -ExecutionPolicy Bypass -File fix_nclob.ps1
> ```

### Tùy chọn 1: Script bash (Linux/macOS/WSL)

```bash
#!/bin/bash
# =============================================================================
# Quick-Start: Chạy Main Flow từ A-Z bằng curl
# Prerequisites:
#   - Server đang chạy tại localhost:6003
#   - Database đã apply migration fix_nclob.ps1
# =============================================================================

BASE="${BASE_URL:-http://localhost:6003/api}"
AUTH="Content-Type: application/json"

echo "=== Manga System Quick-Start ==="

# Login
ADMIN_TOKEN=$(curl -s -X POST "$BASE/auth/login" \
  -H "$AUTH" \
  -d '{"email":"admin@gmail.com","password":"admin123"}' \
  | grep -o '"token":"[^"]*"' | sed 's/"token":"\(.*\)"/\1/')
echo "Admin token: ${ADMIN_TOKEN:0:30}..."

TANTOU_TOKEN=$(curl -s -X POST "$BASE/auth/login" \
  -H "$AUTH" \
  -d '{"email":"tantou@manga.com","password":"password123"}' \
  | grep -o '"token":"[^"]*"' | sed 's/"token":"\(.*\)"/\1/')

BOARD_TOKEN=$(curl -s -X POST "$BASE/auth/login" \
  -H "$AUTH" \
  -d '{"email":"board1@manga.com","password":"password123"}' \
  | grep -o '"token":"[^"]*"' | sed 's/"token":"\(.*\)"/\1/')

# Create project
PROJECT_RESP=$(curl -s -X POST "$BASE/projects" \
  -H "$AUTH" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"title":"QuickStart Manga","description":"Test manga","format":"MANGA"}')
PROJECT_ID=$(echo $PROJECT_RESP | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
echo "Project: $PROJECT_ID"

# Activate
curl -s -X PUT "$BASE/projects/$PROJECT_ID/status" \
  -H "$AUTH" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"status":"ACTIVE"}' > /dev/null

# Create plan
PLAN_RESP=$(curl -s -X POST "$BASE/projects/$PROJECT_ID/production-plans" \
  -H "$AUTH" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"milestones":"Chuong 1-3","schedule":"3 tuan","deadline":"2026-12-31","budget":50000000}')
PLAN_ID=$(echo $PLAN_RESP | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
echo "Plan: $PLAN_ID"

# Create chapter
CHAPTER_RESP=$(curl -s -X POST "$BASE/workflow/chapters?requesterId=156" \
  -H "$AUTH" \
  -H "Authorization: Bearer $TANTOU_TOKEN" \
  -d "{\"planId\":$PLAN_ID,\"chapterNumber\":1,\"title\":\"Chuong 1\"}")
CHAPTER_ID=$(echo $CHAPTER_RESP | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
echo "Chapter: $CHAPTER_ID"

# Start chapter IN_PRODUCTION
curl -s -X PUT "$BASE/workflow/chapters/$CHAPTER_ID/status?requesterId=156&status=IN_PRODUCTION" \
  -H "$AUTH" \
  -H "Authorization: Bearer $TANTOU_TOKEN" \
  -d '{}' | grep -o '"chapterStatus":"[^"]*"'

# Board publishes
curl -s -X POST "$BASE/workflow/chapters/$CHAPTER_ID/publish?requesterId=153" \
  -H "$AUTH" \
  -H "Authorization: Bearer $BOARD_TOKEN" \
  -d '{"leaderId":153,"publishDate":"2026-08-01"}' | grep -o '"chapterStatus":"[^"]*"'

echo "=== Done ==="
```

### Tùy chọn 2: Script Node.js (Windows/macOS/Linux)

```bash
# Windows/macOS/Linux — chạy script Node.js
node test_flow.js
# Hoặc chỉ định port:
BASE_URL=http://localhost:6003/api node test_flow.js
```

### Tùy chọn 3: PowerShell (Windows)

```powershell
# Windows PowerShell — chạy từng bước
$BASE = "http://localhost:6003/api"

# Login
$admin = Invoke-RestMethod -Method Post -Uri "$BASE/auth/login" `
  -Body '{"email":"admin@gmail.com","password":"admin123"}' -ContentType "application/json"
$adminToken = $admin.data.token

# Create project
$proj = Invoke-RestMethod -Method Post -Uri "$BASE/projects" `
  -Body '{"title":"QuickStart","description":"Test","format":"MANGA"}' `
  -ContentType "application/json" -Headers @{Authorization="Bearer $adminToken"}
$projId = $proj.data.id

# Activate
Invoke-RestMethod -Method Put -Uri "$BASE/projects/$projId/status" `
  -Body '{"status":"ACTIVE"}' -ContentType "application/json" `
  -Headers @{Authorization="Bearer $adminToken"} | Out-Null

# ... tiếp tục các bước tương tự
```

### Database Migration Note (NCLOB Fix)

Nếu gặp lỗi HTTP 500 `Could not extract column... NCLOB`, cần chạy:

```powershell
# Windows
powershell -ExecutionPolicy Bypass -File fix_nclob.ps1

# Sau đó restart server và chạy lại
```

# ── PHASE 1: Setup ──────────────────────────────────────────────────────────

echo "=== PHASE 1: Admin tạo Account cho Mangaka + Assistant ==="

# Admin đăng nhập → lấy token
ADMIN_TOKEN=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@gmail.com","password":"admin123"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

echo "Admin token: ${ADMIN_TOKEN:0:30}..."

# Admin tạo Mangaka account (nếu chưa có)
curl -s -X POST "$BASE/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"mangaka2@manga.com","password":"password123","fullName":"Họa sĩ 2","systemRole":"MANGAKA"}' \
  | grep -o '"id":[0-9]*' | head -1

# Admin gán role MANGAKA cho account mangaka@manga.com
curl -s -X POST "$BASE/system-roles/assign" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"accountId":7,"roleName":"MANGAKA"}'

echo ""
echo "=== PHASE 2: Leader tạo Project + ProductionPlan ==="

# Leader đăng nhập
LEADER_TOKEN=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"leader@manga.com","password":"password123"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Tạo project
PROJECT_RESP=$(curl -s -X POST "$BASE/projects" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LEADER_TOKEN" \
  -d '{"title":"Thanh Mẫu - Tập 1","description":"Truyện fantasy 10 tập","format":"MANGA"}')
PROJECT_ID=$(echo $PROJECT_RESP | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "Project created: ID=$PROJECT_ID"

# Activate project (tự động tạo ProductionPlan IN_PROGRESS)
curl -s -X PUT "$BASE/projects/$PROJECT_ID/status?tantouId=4" \
  -H "Authorization: Bearer $LEADER_TOKEN"

# Gán Mangaka vào Project
curl -s -X POST "$BASE/projects/$PROJECT_ID/roles" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LEADER_TOKEN" \
  -d '{"memberId":7,"role":"MANGAKA"}'

# Tantou đăng nhập
TANTOU_TOKEN=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"tantou@manga.com","password":"password123"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

echo ""
echo "=== PHASE 3: Tantou tạo Chapter + Task + SubTask ==="

# Tantou tạo chapter
CHAPTER_RESP=$(curl -s -X POST "$BASE/workflow/chapters" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TANTOU_TOKEN" \
  -d "{\"productionPlanId\":$PROJECT_ID,\"chapterNumber\":1,\"title\":\"Chương 1\"}")
CHAPTER_ID=$(echo $CHAPTER_RESP | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "Chapter created: ID=$CHAPTER_ID"

# Start chapter: BACKLOG → IN_PRODUCTION
curl -s -X PUT "$BASE/workflow/chapters/$CHAPTER_ID/status?tantouId=4" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TANTOU_TOKEN" \
  -d '{"status":"IN_PRODUCTION"}'

# Tantou tạo task
TASK_RESP=$(curl -s -X POST "$BASE/workflow/tasks" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TANTOU_TOKEN" \
  -d "{\"chapterId\":$CHAPTER_ID,\"title\":\"Vẽ trang 1-5: cảnh mở đầu\",\"type\":\"LINEART\"}")
TASK_ID=$(echo $TASK_RESP | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "Task created: ID=$TASK_ID"

# Gán task cho Mangaka
curl -s -X POST "$BASE/workflow/tasks/$TASK_ID/assign" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TANTOU_TOKEN" \
  -d '{"assigneeId":7}'

# Tạo subtask
SUBTASK_RESP=$(curl -s -X POST "$BASE/workflow/subtasks" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TANTOU_TOKEN" \
  -d "{\"taskId\":$TASK_ID,\"title\":\"Rough sketch trang 1\",\"assignedToId\":7}")
SUBTASK_ID=$(echo $SUBTASK_RESP | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "SubTask created: ID=$SUBTASK_ID"

echo ""
echo "=== PHASE 4: Mangaka review Submission ==="

# Assistant đăng nhập
ASST_TOKEN=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"assistant1@manga.com","password":"password123"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Assistant bắt đầu subtask
curl -s -X PUT "$BASE/workflow/subtasks/$SUBTASK_ID/status?assistantId=8" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ASST_TOKEN" \
  -d '{"status":"IN_PROGRESS"}'

# Assistant submit rough
SUBM_RESP=$(curl -s -X POST "$BASE/submissions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ASST_TOKEN" \
  -d "{\"subTaskId\":$SUBTASK_ID,\"type\":\"ROUGH\"}")
SUBM_ID=$(echo $SUBM_RESP | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "Submission created: ID=$SUBM_ID"

# Mangaka review → APPROVED
curl -s -X POST "$BASE/submissions/$SUBM_ID/review" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LEADER_TOKEN" \
  -d '{"reviewerId":7,"decision":"APPROVED","feedback":"Tốt, chuẩn rồi"}'

echo ""
echo "=== PHASE 5: Tantou complete Chapter → Board publish ==="

# Tantou complete chapter
curl -s -X PUT "$BASE/workflow/chapters/$CHAPTER_ID/status?tantouId=4" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TANTOU_TOKEN" \
  -d '{"status":"COMPLETED"}'

# Board đăng nhập
BOARD_TOKEN=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"board1@manga.com","password":"password123"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Board publish
echo "Board publish chapter $CHAPTER_ID..."
curl -s -X POST "$BASE/workflow/chapters/$CHAPTER_ID/publish?requesterId=5" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $BOARD_TOKEN" \
  -d '{"leaderId":5,"publishDate":"2026-08-01","releaseNote":"Chapter 1 ra mắt"}'

echo ""
echo "=== MAIN FLOW DONE ==="
echo "Chapter $CHAPTER_ID is PUBLISHED"
```

---

## §10. EDGE-CASE RESPONSES

### 10.1 Optimistic Locking Conflict

```
HTTP/1.1 409 Conflict
{
  "message": "Trạng thái Chapter đã được cập nhật bởi người dùng khác. Vui lòng tải lại trang.",
  "errorCode": "STALE_ENTITY"
}
```

### 10.2 Plan PAUSED Guard

Mọi write (create chapter/task, submit, update status) bị chặn:

```
HTTP/1.1 409 Conflict
{
  "message": "Production Plan <id> is PAUSED. Submissions are frozen. Resume the plan first."
}
```

### 10.3 Recall Cap Reached

```
HTTP/1.1 409 Conflict
{
  "message": "Chapter đã bị thu hồi 2 lần (đã đạt giới hạn tối đa). Bắt buộc Leader can thiệp xử lý đặc biệt."
}
```

### 10.4 Return Limit Reached

```
HTTP/1.1 409 Conflict
{
  "message": "Chapter đã bị trả về 2 lần. Chuyển sang trạng thái COMPLETED_NEEDS_REVIEW. Cần Leader họp Board."
}
```

---

## §11. CHECKLIST TEST THỦ CÔNG

### Phase 1 — Setup
- [ ] `POST /auth/register` → 201 + Account
- [ ] `POST /projects` → 201 + Project
- [ ] `PUT /projects/{id}/status` → Project ACTIVE + Plan IN_PROGRESS
- [ ] `POST /projects/{id}/roles` → Mangaka/Assistant gán vào Project
- [ ] `POST /projects/{id}/production-plan` → Plan IN_PROGRESS

### Phase 2 — Production
- [ ] `POST /workflow/chapters` → 201 + Chapter BACKLOG
- [ ] `PUT /chapters/{id}/status → IN_PRODUCTION` → 200
- [ ] `POST /workflow/tasks` → 201 + Task TODO
- [ ] `POST /tasks/{id}/assign` → 200 + Task IN_PROGRESS
- [ ] `POST /workflow/subtasks` → 201 + SubTask TODO
- [ ] `PUT /subtasks/{id}/status → IN_PROGRESS` → 200
- [ ] `POST /submissions` → 201 + Submission ROUGH
- [ ] `POST /submissions/{id}/review → APPROVED` → SubTask ROUGH_APPROVED

### Phase 3 — Review & Publish
- [ ] `PUT /chapters/{id}/status → COMPLETED` → 200 (rejectionCount = 0 → 0)
- [ ] `POST /chapters/{id}/publish` → 200 + PUBLISHED
- [ ] Auto-complete: Plan IN_PROGRESS → COMPLETED (khi tất cả chapter PUBLISHED)

### Phase 4 — QC (Return/Recall)
- [ ] `POST /chapters/{id}/return` → 200 + IN_PRODUCTION (rejectionCount: 0 → 1)
- [ ] `POST /tasks/{id}/mark-revision` → 200 + REVISION_REQUIRED
- [ ] `PUT /chapters/{id}/status → COMPLETED` → rejectionCount RESET về 0 (AI-09)
- [ ] Return lần 2 → rejectionCount: 0 → 2
- [ ] Return lần 3 → 409 COMPLETED_NEEDS_REVIEW
- [ ] `POST /chapters/{id}/override-recall` (Board) → 403
- [ ] `POST /chapters/{id}/override-recall` (Leader) → 200 + IN_PRODUCTION
- [ ] Recall lần 1 → 200 + IN_PRODUCTION (recallCount: 0 → 1)
- [ ] Recall lần 2 → 200 + IN_PRODUCTION (recallCount: 1 → 2)
- [ ] Recall lần 3 → 409 cap reached
- [ ] Override-recall (Leader) → 200 + recallCount: 2 → 3

### Phase 5 — Scheduler
- [ ] `POST /chapters/{id}/schedule` (past date) → 400
- [ ] `POST /chapters/{id}/schedule` (future) → 200 + SCHEDULED
- [ ] `POST /chapters/publish-scheduled` → Integer (số chapter đã publish)

### Phase 6 — Pause/Resume/Close
- [ ] `POST /plans/{id}/pause` → 200 + PAUSED
- [ ] `POST /plans/{id}/comments` khi PAUSED → 201 (comment vẫn hoạt động)
- [ ] Tạo chapter khi PAUSED → 409
- [ ] `POST /plans/{id}/resume` → 200 + IN_PROGRESS
- [ ] `POST /plans/{id}/force-close` → 200 + COMPLETED
- [ ] `POST /projects/{id}/cancel` → 200 + CANCELLED (Plan + Chapter cascade)
- [ ] Chapter PUBLISHED vẫn truy cập được sau Cancel

### Phase 7 — AI Decisions
- [ ] AI-01: publish không releaseNote → releaseNote = null
- [ ] AI-01: publish có releaseNote → releaseNote stored
- [ ] AI-02: đổi Tantou khi PAUSED → 200 (KHÔNG bị chặn)
- [ ] AI-03: Plan target=10 nhưng chỉ 5 chapter → publish 5/5 → Plan auto-COMPLETED

### Phase 8 — Concurrency
- [ ] 2 tab cùng publish → tab sau nhận 409 STALE_ENTITY

**Total: 44 assertions · Build: 61/61 pass**

---

## §12. QUICK REFERENCE — Decision Log (10/12 AI)

| AI | Quyết định | Sprint | § |
|:---:|---|:---:|---|
| AI-01 | `releaseNote` optional | 3 | §3.3 |
| AI-02 | KHÔNG chặn đổi Tantou khi PAUSED | 3 | §2.4 |
| AI-03 | Auto-complete DYNAMIC (check thực tế, không target) | 3 | §3.3 |
| AI-04 | Tantou chủ động chọn Task (không auto-reopen) | 3 | §4.3 |
| AI-05 | Plan Comment (works when PAUSED) | 5 | §2.7 |
| AI-06 | URL prefix GIỮ `/api/workflow/` | 3 | (all) |
| AI-07 | `recallCount` cap = 2 + Leader override lần 3 | 3+4 | §3.6–3.7 |
| AI-08 | Scheduler SCHEDULED → PUBLISHED (cron 5 phút) | 6 | §3.4 |
| AI-09 | `rejectionCount` RESET khi re-complete | 6 | §3.5 |
| AI-10 | DROP `approval_status` column | 8 | Breaking change notice |
| AI-11 | Helper `ProductionPlan.isActive()` | 6 | §7 |
| AI-12 | Chapter Comment entity | 5 | §8.2 |

**Deferred:** AI-13 (Audit trail) · AI-14 (Email/notification) — sprint sau release.

---

*Document: 2026-07-27 · Source: API_USAGE.md + BUSINESS_OVERVIEW.md · Build: 61/61 pass*


> Áp dụng cho 3 sprint vừa code + Decision Log 2026-07-27. Mỗi API có: URL, role được phép, validate, status code trả về, ví dụ curl, ý nghĩa nghiệp vụ.
>
> **Quyết định nghiệp vụ mới (Decision Log 2026-07-27):**
> - **AI-01**: `releaseNote` OPTIONAL khi publish.
> - **AI-02**: Cho phép đổi Tantou khi Plan PAUSED (không chặn).
> - **AI-03**: Auto-complete Plan DYNAMIC (theo số chapter hiện có, không check `totargetChapterCount`).
> - **AI-04**: Tantou chủ động chọn Task cần sửa (không auto-reopen). Endpoint mới: `/tasks/{id}/mark-revision`.
> - **AI-07**: `recallCount` cap = 2; lần 3 cần `override-recall` (Leader only).

---

## 0. Chuẩn bị

```bash
# Chạy service
./mvnw spring-boot:run

# Swagger UI
# http://localhost:8080/swagger-ui/index.html
```

### Tài khoản seed (đã có sẵn nhờ `DataInitialized`)

| Email | Role | Mật khẩu |
|---|---|---|
| `admin@gmail.com` | ADMIN | `admin123` |
| `leader@manga.com` | LEADER_BOARD | `password123` |
| `board1@manga.com`, `board2@manga.com`, `board3@manga.com` | EDITORIAL_BOARD_MEMBER | `password123` |
| `tantou@manga.com` | TANTOU_EDITOR | `password123` |
| `mangaka@manga.com` | MANGAKA | `password123` |
| `assistant1@manga.com` ... `assistant3@manga.com` | ASSISTANT | `password123` |

---

## 1. QUẢN LÝ PLAN (Sprint 1)

### 1.1 Pause Plan

```
POST /api/production-plans/{id}/pause?requesterId={tantouOrLeaderId}
Content-Type: application/json

{ "reason": "Thiếu nhân sự tháng 7" }
```

- **Roles**: TANTOU_EDITOR, LEADER_BOARD, EDITORIAL_BOARD_MEMBER.
- **Validate**:
  - `reason` `@NotBlank`, max 1000 ký tự.
  - Plan phải tồn tại.
  - Plan không ở `COMPLETED` / `CANCELLED` (terminal).
  - Plan không được `PAUSED` rồi.
- **Trả về**:
  - `200 OK` + body Plan (PlanStatus = `PAUSED`, pausedBy/pausedAt/Reason đã ghi).
  - `403` nếu sai role.
  - `409` nếu Plan ở trạng thái không hợp lệ.
- **Ý nghĩa nghiệp vụ (BA V3 §2.2)**: mọi submit/create/assign/update đều bị chặn 409 cho đến khi Resume. Read-only (dashboard, asset) vẫn xem được.

**Test curl:**

```bash
curl -X POST "http://localhost:8080/api/production-plans/1/pause?requesterId=4" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{ "reason": "Tantou nghỉ phép tháng 7, thiếu người review" }'
```

### 1.2 Resume Plan

```
POST /api/production-plans/{id}/resume?requesterId={id}
```

- **Roles**: TANTOU_EDITOR, LEADER_BOARD, EDITORIAL_BOARD_MEMBER.
- **Validate**: Plan phải đang `PAUSED`.
- **Trả về**: `200 OK` + Plan (status = `IN_PROGRESS`, pausedBy/pausedAt/Reason = null).
- **409** nếu Plan không `PAUSED`.

**Test:**

```bash
curl -X POST "http://localhost:8080/api/production-plans/1/resume?requesterId=4" \
  -H "Authorization: Bearer <token>"
```

### 1.3 Force-Close Plan

```
POST /api/production-plans/{id}/force-close?requesterId={leaderOrBoardId}
Content-Type: application/json

{ "reason": "Dự án hết budget, kết thúc sớm" }
```

- **Roles**: LEADER_BOARD, EDITORIAL_BOARD_MEMBER.
- **Validate**:
  - `reason` `@NotBlank`, max 1000.
  - Plan đang `IN_PROGRESS` hoặc `PAUSED`.
- **Trả về**: `200 OK` + Plan (status = `COMPLETED`, pauseReason = lý do).
- **403** sai role, **409** trạng thái không hợp lệ.

**Test:**

```bash
curl -X POST "http://localhost:8080/api/production-plans/1/force-close?requesterId=3" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{ "reason": "Board quyết định ngừng dự án" }'
```

---

## 2. WORKFLOW PUBLISH (Sprint 1 + 2)

### 2.1 Publish Chapter (sửa — Decision Log §AI-01)

```
POST /api/workflow/chapters/{chapterId}/publish?requesterId={id}&publishDate={yyyy-MM-dd}
Content-Type: application/json

{
  "releaseNote": "Chapter tiếp theo của arc Thanh Mẫu, ra mắt ngày 1/8"
}
```

- **Roles (BA V3 §3.1)**: LEADER_BOARD **hoặc** EDITORIAL_BOARD_MEMBER.
- **Validate**: Chapter phải `COMPLETED`; không được trùng `PUBLISHED`.
- **Body (MỚI)**: `PublishChapterRequest { leaderId, publishDate, releaseNote }` — **TẤT CẢ optional** (Decision Log §AI-01).
  - `releaseNote` blank `null` → stored NULL.
  - `publishDate` blank → defaults to today.
  - Không bắt buộc nhập body.
- **Trả về**: `200 OK` + Chapter (status=`PUBLISHED`, `publishDate`, `publishedBy`, `publishedAt`, **`releaseNote`** đã ghi).
- **Auto-complete Plan (Decision Log §AI-03)**: nếu tất cả chapters của Plan = PUBLISHED, Plan tự động `COMPLETED` (DYNAMIC — không check `targetChapterCount`).
- **403** sai role, **409** trạng thái không hợp lệ.

**Test curl (minimal — không cần body):**

```bash
curl -X POST "http://localhost:8080/api/workflow/chapters/1/publish?requesterId=3" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>"
```

**Test curl (có releaseNote + publishDate):**

```bash
curl -X POST "http://localhost:8080/api/workflow/chapters/1/publish?requesterId=3&publishDate=2026-08-01" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{ "releaseNote": "Chapter tiếp theo của arc Thanh Mẫu, ra mắt ngày 1/8" }'
```

### 2.2 Recall Chapter (sửa — Decision Log §AI-04 + §AI-07)

```
POST /api/workflow/chapters/{chapterId}/recall?requesterId={leaderOrBoardId}
Content-Type: application/json

{ "recallReason": "Phát hiện sai lệch bố cục trang 12, cần vẽ lại" }
```

- **Roles**: LEADER_BOARD, EDITORIAL_BOARD_MEMBER.
- **Validate**:
  - `recallReason` `@NotBlank`, **min 15 ký tự** (BA V3 §3.4).
  - Chapter phải `PUBLISHED`.
  - **`recallCount < 2`** (Decision Log §AI-07 — cap = 2 lần). Nếu `recallCount >= 2` → 409.
- **Hiệu ứng** (sửa theo §AI-04):
  - Chapter `PUBLISHED → IN_PRODUCTION`.
  - `recallCount++`.
  - **KHÔNG auto-reopen Tasks** (Tantou chủ động chọn Task sau; xem §2.6).
  - Nếu Plan đang `COMPLETED` → rollback `IN_PROGRESS`.
- **Trả về**: `200 OK` + Chapter.
- **403** sai role; **400** nếu recallReason < 15 char; **409** Chapter không `PUBLISHED`, **hoặc recallCount >= 2**.

**Test curl:**

```bash
curl -X POST "http://localhost:8080/api/workflow/chapters/1/recall?requesterId=3" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{ "recallReason": "Trang 12 lỗi bố cục, độc giả phản ánh" }'
```

**Test expectations (sprint 4):**
- Lần 1: 200 OK, chapter → IN_PRODUCTION, recallCount=1.
- Lần 2: 200 OK, recallCount=2.
- Lần 3 (Board): **409** `"Chapter đã bị thu hồi 2 lần (đã đạt giới hạn tối đa). Bắt buộc Leader can thiệp xử lý đặc biệt."`
- Lần 3 (Leader dùng `/override-recall`): 200 OK, recallCount=3.

### 2.3 Return Chapter (sửa — Decision Log §AI-04)

```
POST /api/workflow/chapters/{chapterId}/return?requesterId={leaderOrBoardId}
Content-Type: application/json

{ "rejectionReason": "Tỉ lệ nhân vật chưa đúng style guide" }
```

- **Roles**: LEADER_BOARD, EDITORIAL_BOARD_MEMBER.
- **Validate**: Chapter phải `COMPLETED`.
- **Logic** (sửa theo §AI-04):
  - Nếu `rejectionCount < 2`: chapter `COMPLETED → IN_PRODUCTION`, ++count; **KHÔNG auto-reopen Tasks** (Tantou chọn sau bằng `/mark-revision`); rollback Plan nếu cần.
  - Nếu `rejectionCount >= 2`: chapter `COMPLETED → COMPLETED_NEEDS_REVIEW`, **không cho return tự động**, trả 409 với message buộc họp Board. Leader dùng `/override-return` (§2.4) để bypass.
- **Trả về**: `200 OK` hoặc `409` với message họp.

### 2.4 Override Return Limit

```
POST /api/workflow/chapters/{chapterId}/override-return?requesterId={leaderId}
Content-Type: application/json

{ "rejectionReason": "Leader quyết định vẫn cần làm lại lineart dù đã 2 lần" }
```

- **Roles**: **Chỉ LEADER_BOARD** (không cho Board).
- **Validate**: Chapter phải ở `COMPLETED` hoặc `COMPLETED_NEEDS_REVIEW`.
- **Hiệu ứng**: bỏ qua giới hạn 2, force chuyển về `IN_PRODUCTION`, ++rejectionCount. KHÔNG auto-reopen Tasks (§AI-04).
- **403** nếu không phải Leader; **409** nếu trạng thái sai.

### 2.5 Override Recall Limit (MỚI — Decision Log §AI-07 follow-up)

```
POST /api/workflow/chapters/{chapterId}/override-recall?requesterId={leaderId}
Content-Type: application/json

{
  "leaderId": 3,
  "recallReason": "Lần thu hồi thứ 3 do leader can thiệp xử lý đặc biệt"
}
```

- **Roles**: **Chỉ LEADER_BOARD** (Board bị từ chối).
- **Validate**:
  - `leaderId` `@NotBlank`.
  - `recallReason` `@NotBlank`, **min 15 ký tự**.
  - Chapter phải `PUBLISHED` (không áp dụng cho chapter đã IN_PRODUCTION).
  - **`recallCount` có thể ≥ 2** (đây là mục đích của override).
- **Hiệu ứng**: bỏ qua cap `recallCount`, force `PUBLISHED → IN_PRODUCTION`, `recallCount++`. KHÔNG auto-reopen Tasks (§AI-04). Rollback Plan nếu cần.
- **403** sai role (Board bị chặn); **409** nếu chapter không `PUBLISHED`; **400** nếu recallReason < 15 char.

**Test curl:**

```bash
curl -X POST "http://localhost:8080/api/workflow/chapters/1/override-recall?requesterId=3" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{ "leaderId": 3, "recallReason": "Lần thu hồi thứ 3 do leader can thiệp xử lý đặc biệt" }'
```

### 2.6 Mark Task for Revision (MỚI — Decision Log §AI-04)

> Sau khi Chapter được Return/Recall, Tantou chủ động chọn **từng Task** cần sửa và gọi endpoint này để set `REVISION_REQUIRED`. KHÔNG có auto-reopen.

```
POST /api/workflow/tasks/{taskId}/mark-revision?requesterId={tantouId}
Content-Type: application/json

{
  "tantouId": 4,
  "note": "Background chưa match style guide"
}
```

- **Roles**: **TANTOU_EDITOR** (của project) hoặc **LEADER_BOARD**.
- **Validate**:
  - `tantouId` required.
  - Chapter chứa Task phải `IN_PRODUCTION` (sau Return/Recall).
  - Task phải ở `DONE` hoặc `REVIEW`.
  - Plan không được `PAUSED` (nếu PAUSED → 409 qua `assertPlanNotPaused`).
- **Hiệu ứng**: `Task.DONE/REVIEW → REVISION_REQUIRED`.
- **Trả về**: `200 OK` + Task (status = REVISION_REQUIRED).
- **403** sai role; **409** Chapter không IN_PRODUCTION, Plan PAUSED, Task status sai.

**Test curl:**

```bash
curl -X POST "http://localhost:8080/api/workflow/tasks/888/mark-revision" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{ "tantouId": 4, "note": "Background chưa match style guide" }'
```

**Workflow đầy đủ (sprint 4+):**

1. Board gọi `/return` hoặc `/recall` → chapter → IN_PRODUCTION, Tasks giữ nguyên.
2. Tantou vào Chapter → xem lý do → chọn Task lỗi → gọi `/mark-revision` cho từng Task.
3. Tasks chuyển sang `REVISION_REQUIRED` → Team sửa → đẩy lại.

### 2.7 Plan Comments (MỚI — Decision Log §AI-05)

> Hệ thống thảo luận trong Plan — đặc biệt hữu ích khi Plan PAUSED (BA V3 §2.2 yêu cầu "có thể bình luận" khi freeze).

#### POST /api/workflow/plans/{planId}/comments

```
POST /api/workflow/plans/{planId}/comments
Content-Type: application/json

{
  "authorId": 4,
  "body": "Đề xuất: reschedule chapter 12 sang tuần sau vì thiếu assistant"
}
```

- **Roles**: TANTOU_EDITOR, MANGAKA, ASSISTANT, EDITORIAL_BOARD_MEMBER, LEADER_BOARD, ADMIN.
- **Validate**: `body` `@NotBlank`, 1-4000 chars (auto trim).
- **Trả về**: `200 OK` + `PlanCommentResponse { id, planId, authorId, authorName, body, createdAt }`.
- **403** nếu author không ở role được phép; **404** nếu plan không tồn tại.

#### GET /api/workflow/plans/{planId}/comments

- **Roles**: bất kỳ ai có role trong hệ thống (đọc).
- **Trả về**: `200 OK` + `List<PlanCommentResponse>` sorted by `createdAt ASC`.

### 2.8 Chapter Comments (MỚI — Decision Log §AI-12)

#### POST /api/workflow/chapters/{chapterId}/comments

```
POST /api/workflow/chapters/{chapterId}/comments
Content-Type: application/json

{
  "authorId": 8,
  "body": "Mangaka xác nhận background trang 12 cần sửa"
}
```

- **Roles**: giống §2.7.
- **Validate**: `body` 1-4000 chars.
- **Trả về**: `200 OK` + `ChapterCommentResponse`.

#### GET /api/workflow/chapters/{chapterId}/comments

- **Trả về**: `200 OK` + `List<ChapterCommentResponse>` sorted by `createdAt ASC`.

**Workflow đầy đủ với Comment:**

```
Plan bị PAUSED:
  1. Tantou gọi POST /plans/{id}/comments → trao đổi giải pháp
  2. Mangaka/Assistant reply (cùng endpoint)
  3. Leader đọc, quyết → bấm Resume

Chapter bị Return/Recall:
  1. Tantou gọi POST /chapters/{id}/comments → note lý do cụ thể
  2. Mangaka reply acknowledge
  3. Tantou gọi POST /tasks/{taskId}/mark-revision → chọn Task cần sửa
```

**UI spec chi tiết:** xem `docs/decision_log/ui_spec_comment_thread_2026_07_27.md`.

### 2.9 Schedule Chapter (MỚI — Decision Log §AI-08)

> Lên lịch xuất bản. Chapter COMPLETED → SCHEDULED, scheduler sẽ tự động SCHEDULED → PUBLISHED.

```
POST /api/workflow/chapters/{chapterId}/schedule?requesterId={id}
Content-Type: application/json

{
  "schedulerId": 4,
  "publishDate": "2026-08-01"
}
```

- **Roles**: TANTOU_EDITOR, LEADER_BOARD, EDITORIAL_BOARD_MEMBER.
- **Validate**:
  - `schedulerId` `@NotNull`.
  - `publishDate` `@NotNull`, must not be in the past.
  - Chapter status phải `COMPLETED` (hoặc đã `SCHEDULED` để re-schedule).
- **Hiệu ứng**: chapter → `SCHEDULED`, lưu `publishDate`.
- **Scheduler**: Spring `@Scheduled` cron job mỗi 5 phút (configurable qua `manga.publish.cron`) sẽ tự động:
  - Query `findByChapterStatusAndPublishDateLessThanEqual(SCHEDULED, today)`.
  - Flip từng chapter → PUBLISHED, set `publishedBy=0` (system), `publishedAt=now`.
  - Roll-up Plan COMPLETED nếu tất cả chapters đã PUBLISHED.
- **Trả về**: `200 OK` + Chapter (status = SCHEDULED).
- **403** sai role; **400** publishDate trong quá khứ; **409** chapter không ở COMPLETED/SCHEDULED.

**Manual trigger (admin/cron):**

```
POST /api/workflow/chapters/publish-scheduled
```

- **Roles**: any (dùng nội bộ; production nên bảo vệ bằng Spring Security rule).
- **Trả về**: `200 OK` + số chapter đã publish (Integer).

**Test curl:**

```bash
# Schedule chapter 11 cho 1/8/2026
curl -X POST "http://localhost:8080/api/workflow/chapters/11/schedule?requesterId=4" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{ "schedulerId": 4, "publishDate": "2026-08-01" }'

# Manual trigger scheduler (Leader)
curl -X POST "http://localhost:8080/api/workflow/chapters/publish-scheduled"
```

**Application properties (override cron):**

```properties
# Default: every 5 minutes
manga.publish.cron=0 0/5 * * * *
```

### 2.10 Helper — Plan re-completion resets rejectionCount (MỚI — Decision Log §AI-09)

> Khi Tantou set Chapter `IN_PRODUCTION → COMPLETED` lại (sau Return), `rejectionCount` tự động reset về 0. Đúng tinh thần Agile: chapter "có cơ hội thứ 3".

- **Code đã tự động**: gọi `POST /chapters/{id}/status` body `{"status":"COMPLETED"}` (Tantou) → nếu `rejectionCount > 0`, reset về 0.
- **Lý do**: cho chapter "fresh budget" 2 lần return, tránh trường hợp bị tích lũy vô hạn.

### 2.11 Helper — ProductionPlan.isActive() (MỚI — Decision Log §AI-11)

> Method `boolean isActive()` trên entity `ProductionPlan`. Trả về `true` khi `planStatus IN [IN_PROGRESS, PAUSED]`.

- **Dùng cho**: dashboard "My Active Plans", filter dropdown, check `isActive()` thay vì inline check.
- **Không có endpoint**: là helper, không cần controller.

### 2.12 ProductionPlan.approvalStatus removed (Sprint 8 — Decision Log §AI-10)

> **Breaking change:** Per BA Spec V3 §5.2, the legacy `approval_status` column has been kept unused for 2 sprints and is now **DROPPED** entirely.

**Changes:**

| Removed | Where | Replaced by |
|---|---|---|
| Field `PlanApprovalStatus approvalStatus` | `ProductionPlan.java` | `PlanStatus` (always non-null) |
| Enum `PlanApprovalStatus` | `model/PlanApprovalStatus.java` | n/a — file deleted |
| Field `approvalStatus` | `ProductionPlanResponse.java` | n/a — field deleted |
| `setApprovalStatus(...)` calls | `ProductionPlanServiceImpl` | n/a — direct `setPlanStatus(IN_PROGRESS)` |
| Method `migratePlanApprovalStatus()` | `DataInitialized.java` | n/a — migration completed in earlier sprint |

**Migration:** `V2026_07_27__drop_production_plan_approval_status_column.sql` (SQL Server + H2 variant) — DROP COLUMN `ProductionPlan.approval_status`.

**API impact:**

- `GET /plans/{id}` response no longer contains `approvalStatus` field.
- `POST /projects/{id}/production-plan` always creates a Plan in `IN_PROGRESS` (no pre-approval).
- `POST /plans/{id}/approve` still exists (marked `@Deprecated`) but only sets `planStatus = IN_PROGRESS`.

---

## 3. PROJECT CANCEL (Sprint 3)

### 3.1 Cancel Project

```
POST /api/projects/{projectId}/cancel?requesterId={leaderOrBoardId}
Content-Type: application/json

{ "reason": "Dự án không đạt chỉ tiêu sau 2 sprint review" }
```

- **Roles**: LEADER_BOARD, EDITORIAL_BOARD_MEMBER.
- **Validate**: `reason` `@NotBlank`, max 2000; Project chưa `CANCELLED`.
- **Cascade**:
  - `Project.projectWorkflowStatus = CANCELLED`.
  - `ProductionPlan.planStatus = CANCELLED` (nếu có, trừ khi đã `COMPLETED`).
  - `Plan.pauseReason = "Project cancelled: <reason>"`.
  - Mọi write vào chapter/task bị chặn 409 (qua `assertPlanNotPaused`).
- **Giữ nguyên** chapter đã `PUBLISHED` (lịch sử).
- **Trả về**: `200 OK` + Project.

**Test:**

```bash
curl -X POST "http://localhost:8080/api/projects/1/cancel?requesterId=3" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{ "reason": "Tác giả rút lui, không có người thay thế" }'
```

---

## 4. EDGE-CASE RESPONSES

### 4.1 Optimistic locking conflict (Sprint 2 §3.2)

Khi 2 user cùng sửa 1 chapter, request sau sẽ nhận:

```json
HTTP/1.1 409 Conflict
{
  "status": 409,
  "message": "Trạng thái Chapter đã được cập nhật bởi người dùng khác. Vui lòng tải lại trang.",
  "error": "Conflict",
  "errorCode": "STALE_ENTITY"
}
```

### 4.2 Plan PAUSED guard (Sprint 1)

Mọi call dưới đây trả `409`:

- `POST /api/workflow/chapters` (Tantou cố tạo chapter mới).
- `POST /api/workflow/chapters/{id}/assign` (gán Mangaka).
- `POST /api/workflow/tasks/{id}/assign`.
- `PUT /api/workflow/tasks/{id}/status`.
- `POST /api/workflow/tasks/{id}/feedback`.

```json
{ "message": "Production Plan <id> is PAUSED. Submissions are frozen. Resume the plan first." }
```

### 4.3 Project CANCELLED guard (Sprint 3)

Tương tự như trên, mọi write chặn với message:

```json
{ "message": "Project <id> is CANCELLED. Cannot mutate chapters/tasks." }
```

---

## 5. CHECKLIST TEST THỦ CÔNG (smoke test 20 phút)

1. Login với `tantou@manga.com`, tạo Project mới qua `POST /api/projects`.
2. Activate Project qua `PUT /api/workflow/projects/{id}/status?tantouId=4` → Plan = `IN_PROGRESS` ngay.
3. Tạo 1 Chapter qua `POST /api/workflow/chapters` (Plan = `IN_PROGRESS` đã mở quyền).
4. Gán Mangaka qua `POST /api/workflow/chapters/{id}/assign`.
5. Tantou update Task status đến `DONE`.
6. Tantou set Chapter = `COMPLETED`.
7. **Publish không releaseNote** (Decision Log §AI-01): login board (`board1@manga.com`) → `POST /publish` không body → ✅ 200, `releaseNote=null`.
8. **Publish có releaseNote**: `POST /publish` body `{ "releaseNote": "Ghi chú xuất bản" }` → ✅ 200, `releaseNote` lưu.
9. **Recall lần 1** (§AI-04): `POST /recall` `recallReason` ≥ 15 char → 200, chapter → IN_PRODUCTION, **Tasks giữ nguyên DONE** (không auto-reopen).
10. **Tantou mark-revision** (§AI-04): `POST /tasks/{id}/mark-revision` cho 1 Task cụ thể → ✅ 200, Task → REVISION_REQUIRED.
11. Re-complete chapter → `POST /return` 2 lần liên tiếp → lần 3 expect 409 `COMPLETED_NEEDS_REVIEW`.
12. Leader `POST /override-return` → ✅ 200, chapter → IN_PRODUCTION.
13. **Recall cap 2** (§AI-07): publish lại → `POST /recall` 2 lần đầu OK → lần 3 expect 409 `"...đã đạt giới hạn tối đa..."`.
14. Leader `POST /override-recall` → ✅ 200, recallCount=3. Board gọi `/override-recall` → expect 403.
15. Leader pause Plan → tạo Chapter mới → expect 409.
16. **AI-02**: trong khi Plan PAUSED, Leader đổi Tantou → ✅ 200 (KHÔNG bị chặn).
17. Resume → tạo lại Chapter → ✅.
18. Force-close Plan với lý do → ✅ Plan = COMPLETED.
19. Cancel Project → Plan = `CANCELLED`, tạo Chapter mới → 409.
20. Cancel 1 project khác đã có 1 chapter PUBLISHED → chapter PUBLISHED vẫn truy cập được.
21. Mở 2 tab cùng publish chapter → tab sau expect 409 (optimistic lock).
22. **Auto-complete dynamic** (§AI-03): tạo Plan với `targetChapterCount=10` nhưng chỉ tạo 5 Chapter → publish 5/5 → Plan auto-COMPLETED (KHÔNG chờ đủ 10).

---

## 6. CHECKLIST TEST TỰ ĐỘNG (đề xuất)

Hiện tại **43/43 unit test pass** (sprint 4 status). Phân bố:

| Class test | Test | Cần cover | Status |
|---|---|---|:---:|
| `ProductionPlanServiceImplTest` | 10 | pause/resume/force-close (cả state hợp lệ lẫn không hợp lệ) | ✅ |
| `ProductionWorkflowServiceImplTest` | 26 | publish (board + leader + releaseNote), recall (cap 2 + override), return (đếm rejectionCount), override-return, mark-revision (Tantou chọn), rollback Plan | ✅ |
| `ProjectServiceImplTest` | 7 | cancelProject cascade | ✅ |

**Test coverage mới (Decision Log 2026-07-27):**

| Test | Decision Log | Status |
|---|---|:---:|
| `publishWithReleaseNote` | AI-01 | ✅ |
| `publishWithNullReleaseNote` | AI-01 | ✅ |
| `publishWithBlankReleaseNote` | AI-01 | ✅ |
| `tantouMarksOneTaskRevision` | AI-04 | ✅ |
| `markRevisionRequiresInProduction` | AI-04 | ✅ |
| `markRevisionRequiresTantouOrLeader` | AI-04 | ✅ |
| `firstReturnSucceeds` (cập nhật — Tasks giữ) | AI-04 | ✅ |
| `thirdRecallBlocked` | AI-07 | ✅ |
| `secondRecallSucceeds` | AI-07 | ✅ |
| `leaderOverrideRecallSucceeds` | AI-07 follow-up | ✅ |
| `boardCannotOverrideRecall` | AI-07 follow-up | ✅ |
| `overrideRecallRequiresPublished` | AI-07 follow-up | ✅ |

**Test cần viết thêm (sprint 5+):**

| Test | Bối cảnh |
|---|---|
| `releaseNoteColumnExistsMigrationTest` | Test Flyway migration tạo cột `release_note` |
| `optimisticLockingConcurrencyTest` | 2 thread update cùng Chapter, expect 1 thành công + 1 thất bại 409 |
| `submissionFlowIntegrationTest` | e2e từ "tạo Chapter" → "publish + recall + mark-revision" |
| `dynamicAutoCompleteTest` | Plan với `targetChapterCount > số chapter thực tế` → auto-complete khi đủ |
| `recallCapSixthTest` | Verify Board cannot override-recall (RBAC chi tiết) |