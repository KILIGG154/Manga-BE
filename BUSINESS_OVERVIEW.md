# Manga Publishing System — Tổng quan Nghiệp vụ & Phân tích Hệ thống

> **Ngày:** 2026-07-27
> **Phiên bản code:** 0.0.1-SNAPSHOT
> **Build:** `./mvnw test` → **61/61 pass** · `./mvnw compile` → **BUILD SUCCESS (200 sources)**
> **Scope:** BA Spec V3 · BA Production Publishing Gap · Decision Log · API Usage

---

## 1. Tổng quan hệ thống

### 1.1 Hệ thống là gì

**Manga Publishing System** (Agile Studio) là nền tảng quản lý quy trình sản xuất truyện tranh từ giai đoạn lên kế hoạch đến xuất bản, hỗ trợ nhiều vai trò cùng làm việc trên cùng một dự án.

**Kiến trúc:** Spring Boot 3 · Java 21 · JPA/Hibernate · MySQL/SQL Server + H2 (dev) · JWT Authentication · Spring Security · Flyway migrations · Maven

### 1.2 Mục tiêu thiết kế

1. **Active Plan đơn lẻ:** Mỗi Project có một Production Plan duy nhất, trực tiếp ở trạng thái `IN_PROGRESS` — không qua pre-approval.
2. **Tách biệt vai trò:** Leader, Editorial Board, Tantou, Mangaka, Assistant có phạm vi hành động khác nhau rõ ràng.
3. **Kiểm soát chất lượng:** Chapter phải qua vòng review nhiều bước trước khi xuất bản, với giới hạn rejection/recall.
4. **Audit nội bộ:** Mọi hành động quan trọng đều ghi nhận ai làm, khi nào, lý do gì.
5. **Scheduler tự động:** Chapter được lên lịch xuất bản, hệ thống tự động kích hoạt đúng ngày.

---

## 2. Các Actor & Vai trò

### 2.1 Vai trò hệ thống (`SystemRoleName` enum)

| Role | Ký hiệu | Mô tả |
|---|---|---|
| **ADMIN** | `ADMIN` | Quản trị hệ thống — full access |
| **LEADER_BOARD** | `LEADER` | Trưởng nhóm — có quyền publish, recall, override, force-close |
| **EDITORIAL_BOARD_MEMBER** | `BOARD` | Thành viên Hội đồng Biên tập — publish/reject/recall |
| **TANTOU_EDITOR** | `TANTOU` | Editor chính — tạo Task, complete Chapter, schedule |
| **MANGAKA** | `MANGAKA` | Họa sĩ — nhận SubTask, submit bản vẽ |
| **ASSISTANT** | `ASSISTANT` | Trợ lý — nhận SubTask, vẽ theo hướng dẫn |
| **MANAGER** | `MANAGER` | Quản lý dự án |

### 2.2 Seed accounts

| Email | Role | Password |
|---|---|---|
| admin@gmail.com | ADMIN | admin123 |
| leader@manga.com | LEADER_BOARD | password123 |
| board1@manga.com | EDITORIAL_BOARD_MEMBER | password123 |
| tantou@manga.com | TANTOU_EDITOR | password123 |
| mangaka@manga.com | MANGAKA | password123 |
| assistant1@manga.com | ASSISTANT | password123 |

---

## 3. Entity & State Machine

### 3.1 Entity chính

```
Account ──has──► SystemRole (1-N)
                  SystemRoleName (ADMIN / LEADER_BOARD / EDITORIAL_BOARD_MEMBER / ...)

Project ──has──► ProductionPlan (1-1)
             ├──► Planning (1-1, optional)
             ├──► Chapter (1-N)
             │        ├──► Task (1-N)
             │        │        ├──► SubTask (1-N)
             │        │        │        └──► Submission (1-N)
             │        │        └──► Submission (1-N)  [standalone chapter submission]
             │        ├──► ChapterComment (1-N)
             │        └──► Asset (1-N)
             └──► ProjectRole (1-N)

ProductionPlan ──has──► ChapterComment (1-N)
```

### 3.2 State machine từng Entity

#### Project Workflow Status (`ProjectWorkflowStatus`)

```
DRAFT ──► ACTIVE ──► ON_HOLD ──► COMPLETED
                        │
                        └──► CANCELLED ──► cascade: Plan + Chapter
```

#### Production Plan Status (`PlanStatus`)

```
IN_PROGRESS ──► PAUSED ──► IN_PROGRESS
    │               │
    └──► COMPLETED ◄┘      │
           ▲               │
           └── CANCELLED ──┘  (khi Project CANCELLED)
```

> Helper `isActive()` trả về `true` khi `IN_PROGRESS` hoặc `PAUSED`.

#### Chapter Status (`ChapterStatus`) — BA V3 §1.1

```
BACKLOG ──► IN_PRODUCTION ──► COMPLETED ──► SCHEDULED ──► PUBLISHED
                              │                           ▲
                              ├──► COMPLETED_NEEDS_REVIEW ──┤  (rejected ≥ 2x)
                              │                                 (needs Leader override)
                              └──► IN_PRODUCTION ◄───────────┘  (recall)
```

| Trạng thái | Ý nghĩa |
|---|---|
| `BACKLOG` | Chapter mới tạo, Tantou chưa start |
| `IN_PRODUCTION` | Đang sản xuất (Task/SubTask/Submission/Review) |
| `COMPLETED` | Mọi Task DONE; chờ Hội đồng xem xét |
| `COMPLETED_NEEDS_REVIEW` | Bị Trả về ≥ 2 lần; chờ Leader override |
| `SCHEDULED` | Đã chốt ngày xuất bản; chờ CronJob tự động kích hoạt |
| `PUBLISHED` | Đã công khai cho độc giả |

#### Task Workflow Status (`TaskWorkflowStatus`)

```
TODO ──► IN_PROGRESS ──► REVIEW ──► DONE
                              │
                              └──► REVISION_REQUIRED (Tantou marks sau Return/Recall)
```

#### SubTask Workflow Status (`SubTaskWorkflowStatus`)

```
TODO
  │
  └──► IN_PROGRESS
            │
            ▼ (Assistant submit rough)
      ROUGH_SUBMITTED ◄───────────────────┐
            │                              │
       Mangaka REJECT                     (resubmit)
            │                              │
            ▼                              │
      ROUGH_REJECTED ──────────────────────┘
            │
       Mangaka APPROVE
            │
            ▼
      ROUGH_APPROVED
            │
            ▼ (Assistant submit final)
      FINAL_SUBMITTED ◄───────────────────┐
            │                              │
       Mangaka REJECT                     (resubmit)
            │                              │
            ▼                              │
      FINAL_REJECTED ──────────────────────┘
            │
       Mangaka APPROVE
            │
            ▼
        COMPLETED
```

#### Submission Review Decision (`FeedbackDecision`)

```
APPROVED ──► DONE
REJECTED ──► IN_PROGRESS
```

---

## 4. Main Workflow — Luồng nghiệp vụ chính

### 4.1 Tạo dự án & lên kế hoạch

```
1. ADMIN tạo Account + SystemRole
2. LEADER tạo Project (DRAFT)
3. LEADER activate Project → Project ACTIVE + ProductionPlan IN_PROGRESS
4. Tantou tạo ProductionPlan (thêm Milestones, Schedule, ChapterTimeline, Deadline, Budget, ...)
5. Tantou gán MANGAKA + ASSISTANT vào Project qua ProjectRole
```

### 4.2 Tạo Chapter & Task

```
6. Tantou tạo Chapter (BACKLOG) thuộc ProductionPlan
7. Tantou tạo Task cho Chapter (TODO)
8. Tantou gán Task cho Mangaka/Assistant
9. Tantou start Chapter → BACKLOG → IN_PRODUCTION
```

### 4.3 Sản xuất SubTask (vòng Rough → Final)

```
10. Assistant nhận SubTask, vẽ rough sketch
11. Assistant submit rough → ROUGH_SUBMITTED
12. Mangaka review: APPROVED → ROUGH_APPROVED / REJECTED → ROUGH_REJECTED
13. Assistant sửa và resubmit (lặp lại bước 10-12)
14. Assistant submit final → FINAL_SUBMITTED
15. Mangaka review final: APPROVED → COMPLETED / REJECTED → FINAL_REJECTED
```

### 4.4 Complete & Publish Chapter

```
16. Tantou complete Chapter (tất cả Task DONE) → COMPLETED
17. Hội đồng (EDITORIAL_BOARD_MEMBER hoặc LEADER_BOARD):
    a) Publish ngay → PUBLISHED (publishedBy + publishedAt recorded)
    b) Hoặc lên lịch → SCHEDULED + publishDate
       → Scheduler (cron 5 phút) tự động SCHEDULED → PUBLISHED khi đến ngày
18. Plan auto-COMPLETED khi tất cả Chapter PUBLISHED
```

### 4.5 Return (Hội đồng trả về)

```
19. Hội đồng reject Chapter đã COMPLETED
    → rejectionCount += 1 (rejectionReason logged)
    → Chapter → IN_PRODUCTION
    → Task giữ nguyên DONE
20. Tantou gọi POST /tasks/{id}/mark-revision cho từng Task cần sửa
    → Task DONE → REVISION_REQUIRED
21. Nếu rejectionCount ≥ 2 → Chapter COMPLETED_NEEDS_REVIEW (khóa tạm)
    → LEADER override để trả về lần 3
22. Tantou re-complete → rejectionCount RESET về 0 → COMPLETED
```

### 4.6 Recall (Thu hồi sau xuất bản)

```
23. LEADER hoặc BOARD recall đã PUBLISHED
    → recallCount += 1 (recallReason ≥ 15 ký tự)
    → Chapter → IN_PRODUCTION
    → Plan rollback IN_PROGRESS (nếu đã COMPLETED)
24. Cap: recallCount ≥ 2 → bị chặn
    → LEADER override-recall để force recall lần 3
25. Tantou mark-revision + re-complete → rejectionCount RESET → COMPLETED → re-publish
```

### 4.7 Pause / Resume Plan

```
26. Tantou/Board/Leader pause Plan → PAUSED
    → pauseReason logged (pausedBy + pausedAt)
    → SubTask/Submission bị đóng băng (chặn ở assertPlanNotPaused)
27. Comment trao đổi vẫn hoạt động khi PAUSED
28. Leader/Board resume → IN_PROGRESS (pauseReason cleared)
```

---

## 5. Đã triển khai — Decision Log (10/12 AI)

### 5.1 Bảng tổng hợp

| AI | Quyết định | Sprint | Test |
|:---:|---|:---:|:---:|
| AI-01 | `releaseNote` optional khi publish | 3 | ✅ 3 |
| AI-02 | KHÔNG chặn đổi Tantou khi PAUSED | 3 | — |
| AI-03 | Auto-complete Plan DYNAMIC (check thực tế, không target) | 3 | — |
| AI-04 | Tantou chủ động chọn Task cần sửa (không auto-reopen) | 3 | ✅ 4 |
| AI-05 | Comment trao đổi Plan/Chapter (4 endpoint) | 5 | ✅ 8 |
| AI-06 | URL prefix GIỮ `/api/workflow/` (không đổi) | 3 | — |
| AI-07 | `recallCount` cap = 2 + Leader override lần 3 | 3 + 4 | ✅ 4 |
| AI-08 | Scheduler SCHEDULED → PUBLISHED (cron 5 phút) | 6 | ✅ 4 |
| AI-09 | `rejectionCount` RESET về 0 khi re-complete | 6 | ✅ 2 |
| AI-10 | DROP `approval_status` column (AI-10 = ~8) | 8 | — |
| AI-11 | Helper `ProductionPlan.isActive()` | 6 | ✅ 4 |
| AI-12 | Chapter Comment entity + endpoint | 5 | ✅ 8 |

**10 quyết định đã triển khai** · **1 No-op** (AI-06) · **2 Deferred** (AI-13 Audit trail, AI-14 Notification)

### 5.2 Chi tiết từng tính năng đã ship

#### AI-01: Release Note optional
- Field `releaseNote` (NVARCHAR(MAX), nullable) trên `Chapter`
- DTO `PublishChapterRequest` + overload `publishChapter(..., releaseNote)`
- blank/null → stored as NULL
- **Tests:** 3/3 pass

#### AI-03: Auto-complete DYNAMIC
- Logic: `existsByProductionPlanIdAndChapterStatusNot(PUBLISHED)` → nếu all PUBLISHED → Plan `COMPLETED`
- Không dùng `targetChapterCount` / `totalVolumeTarget` để block

#### AI-04: Tantou chủ động chọn Task
- `POST /api/workflow/tasks/{id}/mark-revision` — Tantou đánh dấu Task DONE/REVIEW → `REVISION_REQUIRED`
- **Tests:** 4/4 pass

#### AI-05 + AI-12: Comment
- `PlanComment` + `ChapterComment` entity
- `POST /api/workflow/plans/{id}/comments` — GET plan comments
- `POST /api/workflow/chapters/{id}/comments` — GET chapter comments
- **Tests:** 8/8 pass

#### AI-07: Recall cap + override
- `recallCount` cap = 2; lần 3 cần Leader `POST /chapters/{id}/override-recall`
- **Tests:** 4/4 pass (trong RecallTests + override-recall)

#### AI-08: Scheduler SCHEDULED → PUBLISHED
- `POST /api/workflow/chapters/{id}/schedule` — COMPLETED → SCHEDULED, save `publishDate`
- `POST /api/workflow/chapters/publish-scheduled` — manual trigger
- `ChapterPublishScheduler` @Scheduled(cron = `manga.publish.cron`, default 5 phút)
- Index `IX_Chapter_Status_PublishDate` trên `(Status, PublishDate)`
- **Tests:** 4/4 pass

#### AI-09: Rejection reset
- Trong `updateChapterStatus(...)`: khi `IN_PRODUCTION → COMPLETED` và `rejectionCount > 0` → reset về 0
- **Tests:** 2/2 pass

#### AI-10: DROP approvalStatus
- Xóa field `approvalStatus` khỏi `ProductionPlan`
- Xóa enum `PlanApprovalStatus`
- Migration `DROP COLUMN approval_status` (SQL Server + H2)
- Breaking change: API response không còn `approvalStatus`

#### AI-11: isActive() helper
- Method `boolean isActive()` trên `ProductionPlan`: `planStatus IN [IN_PROGRESS, PAUSED]`
- **Tests:** 4/4 pass

---

## 6. Còn Deferred — Chưa triển khai

| AI | Item | Ghi chú |
|:---:|---|---|
| AI-10 | ~~DROP `approval_status`~~ | ✅ Đã xong sprint 8 |
| AI-13 | Audit/log trail | Sau release production |
| AI-14 | Email/in-app notification | Sau release, cần stakeholder chốt channel |

---

## 7. Tất cả API Endpoints (22 endpoints)

> Base URL: `/api/workflow/` · JWT Bearer token · Vai trò ghi ở mỗi endpoint

### 7.1 Production Plan

| Method | URL | Role | Mô tả |
|---|---|---|---|
| `POST` | `/api/production-plans` | Tantou/Leader | Tạo Plan (luôn `IN_PROGRESS`) |
| `GET` | `/api/production-plans/{id}` | any | Chi tiết Plan |
| `POST` | `/api/production-plans/{id}/pause` | Tantou/Board/Leader | Pause → PAUSED |
| `POST` | `/api/production-plans/{id}/resume` | Tantou/Board/Leader | Resume → IN_PROGRESS |
| `POST` | `/api/production-plans/{id}/force-close` | Leader | Force → COMPLETED |
| `POST` | `/api/production-plans/{id}/comments` | Tantou/Board/Leader/Admin/Mangaka/Assistant | Comment khi PAUSED |
| `GET` | `/api/production-plans/{id}/comments` | any | List comment |

### 7.2 Chapter

| Method | URL | Role | Mô tả |
|---|---|---|---|
| `POST` | `/api/workflow/chapters` | Tantou | Tạo Chapter |
| `PUT` | `/api/workflow/chapters/{id}/status` | Tantou | Update status (COMPLETED, ...) |
| `POST` | `/api/workflow/chapters/{id}/publish` | Leader/Board | Publish ngay → PUBLISHED |
| `POST` | `/api/workflow/chapters/{id}/return` | Leader/Board | Return → IN_PRODUCTION + rejectionCount |
| `POST` | `/api/workflow/chapters/{id}/recall` | Leader/Board | Recall PUBLISHED → IN_PRODUCTION |
| `POST` | `/api/workflow/chapters/{id}/override-recall` | Leader | Force recall lần 3 (bypass cap) |
| `POST` | `/api/workflow/chapters/{id}/schedule` | Tantou/Leader/Board | Lên lịch → SCHEDULED |
| `POST` | `/api/workflow/chapters/publish-scheduled` | any (admin) | Manual trigger scheduler |
| `POST` | `/api/workflow/chapters/{id}/comments` | Tantou/Board/Leader/Admin/Mangaka/Assistant | Chapter comment |
| `GET` | `/api/workflow/chapters/{id}/comments` | any | List chapter comment |

### 7.3 Task

| Method | URL | Role | Mô tả |
|---|---|---|---|
| `POST` | `/api/workflow/tasks/{id}/mark-revision` | Tantou/Leader | DONE → REVISION_REQUIRED |
| `POST` | `/api/workflow/tasks/{id}/feedback` | Tantou | Tantou review Submission |

### 7.4 Project

| Method | URL | Role | Mô tả |
|---|---|---|---|
| `POST` | `/api/projects` | Leader | Tạo Project |
| `PUT` | `/api/projects/{id}/activate` | Leader | Activate → Plan `IN_PROGRESS` |
| `POST` | `/api/projects/{id}/cancel` | Leader/Board | Cancel → cascade |

### 7.5 Submission

| Method | URL | Role | Mô tả |
|---|---|---|---|
| `POST` | `/api/submissions/{id}/review` | Mangaka | Review submission (APPROVED/REJECTED) |

---

## 8. Cấu trúc Database (Schema tóm tắt)

### 8.1 Bảng chính

```
Account (Id, Email, Password, FullName, status)
  └── SystemRole (AccountId, roleName)

Project (Id, Title, Description, status, format)
  ├── ProductionPlan (Id, ProjectId, planStatus, milestones, schedule, ...)
  │        └── Chapter (Id, ProductionPlanId, chapterNumber, status, publishDate, recallCount, rejectionCount, publishedBy, publishedAt, releaseNote, version)
  │                 └── Task (Id, ChapterId, workflowStatus, type)
  │                          └── SubTask (Id, TaskId, workflowStatus)
  │                                   └── Submission (Id, SubTaskId, type)
  │                                            └── SubmissionReview (SubmissionId, reviewerId, decision)
  └── ChapterComment (Id, ChapterId, authorId, body, createdAt)
ProductionPlan (Id, ProjectId, planStatus, ...)
  └── PlanComment (Id, ProductionPlanId, authorId, body, createdAt)

Asset (Id, ChapterId, category, fileUrl, fileType)
```

### 8.2 Index quan trọng

| Index | Bảng | Cột | Mục đích |
|---|---|---|---|
| `IX_Chapter_Status_PublishDate` | Chapter | (Status, PublishDate) | Scheduler query O(log n) |

---

## 9. Unit Test Summary

```
Tests run: 61, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

Maven: 20.500s
Java: 21
```

| Test class | Tests | Coverage |
|---|---|:---:|
| `ProductionPlanServiceImplTest` | 10 | Pause/Resume/Force-Close/approval |
| `ProductionWorkflowServiceImplTest` | 44 | Publish/Recall/Return/Schedule/AI-09/AI-11 |
| `ProjectServiceImplTest` | 7 | Cancel cascade |
| **Tổng** | **61** | |

### Tests mới thêm Sprint 6 (10 tests)

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
| `autoPublishFlipsDueChapters` | AI-08 | SCHEDULED → PUBLISHED at due date |
| `noDueChaptersReturnsZero` | AI-08 | 0 due → 0 |

---

## 10. Cấu trúc Source Code

```
src/main/java/group1/com/MangaSystemAndManagement/
├── model/          (43 files)  — Entity, Enum, embeddable
├── repository/     (22 files)  — JPA Repository
├── dto/
│   ├── request/    (20 files)  — DTO nhận từ client
│   └── response/   (16 files)  — DTO trả về cho client
├── service/
│   ├── interfaces/ (14 files)  — Service interface
│   └── impl/       (14 files)  — Service implementation
├── controller/     (20 files) — REST controller
├── scheduler/       (1 file)  — ChapterPublishScheduler
├── config/          (4 files) — DataInitialized, SecurityConfig
├── security/        (4 files) — JwtTokenProvider, JwtAuthFilter
├── exception/       (3 files) — Custom exceptions
└── MangaSystemAndManagementApplication.java

src/main/resources/
├── application.properties
├── db/migration/   (22 Flyway migration scripts)
└── static/ (assets)

src/test/java/  (5 test classes, 61 tests)
```

---

## 11. Migration Scripts (Flyway)

| File | Mô tả |
|---|---|
| `V2026_07_27__add_release_note_to_chapter.sql` | Thêm cột `release_note` |
| `V2026_07_27__create_plan_chapter_comment_tables.sql` | Bảng PlanComment + ChapterComment + FK + index |
| `V2026_07_27__add_chapter_schedule_index.sql` | Index `(Status, PublishDate)` cho scheduler |
| `V2026_07_27__drop_production_plan_approval_status_column.sql` | DROP column `approval_status` |
| *(các file `_h2.sql` tương ứng)* | Cho H2 / dev profile |

---

## 12. Lịch sử triển khai

| Thời gian | Sprint | Features | Tests |
|---|---|---|:---:|
| 2026-07-27 00:30 | Sprint 3 | AI-01/03/04/06/07 | 40 |
| 2026-07-27 02:00 | Sprint 4 | AI-07 override-recall endpoint | 43 |
| 2026-07-27 02:15 | Sprint 5 | AI-05 + AI-12 Comment | 51 |
| 2026-07-27 02:30 | Sprint 6 | AI-08/09/11 (scheduler + reset + isActive) | 61 |
| 2026-07-27 02:39 | Sprint 8 | AI-10 DROP approvalStatus column | 61 |

---

## 13. Sprint tiếp theo — Đề xuất

| Priority | Task | Effort | Value |
|---|---|:---:|---|
| 1 | AI-13: Audit trail (bảng `PlanPauseHistory`, `StatusChangeLog`) | 1-2 sprint | High — vận hành production |
| 2 | AI-14: Email/in-app notification (chốt channel trước) | 1-2 sprint | Medium |
| 3 | Postman collection cho 22 endpoint | 1 giờ | High — FE integration |
| 4 | Integration test e2e (MockMvc) | 4 giờ | High — regression safety |
| 5 | FE integration + UAT | ongoing | Critical |

---

## 14. Liên kết tài liệu

| File | Mục đích |
|---|---|
| `BA_SPEC_V3_ACTION_ITEMS.md` | Action items từ BA Spec V3 — gap analysis |
| `ba_production_publishing_gap.md` | Đối chiếu BA Spec V3 với code hiện tại (25-35% coverage ban đầu) |
| `publication_business_analysis.md` | Phân tích nghiệp vụ xuất bản — 2 action items cốt lõi |
| `docs/decision_log/decision_log_2026_07_27.md` | Tracking đầy đủ mọi quyết định + chi tiết code |
| `API_USAGE.md` | Hướng dẫn sử dụng API + curl examples |
| `TEST_RESULTS.md` | Chi tiết từng test case |
| `docs/ba_v3/ba_spec_v3_action_items.md` | Nguồn gốc 14 AI items |

---

*Document tổng hợp: 2026-07-27 10:24 AM (UTC+7) · Source: ba_spec_v3_action_items.md · decision_log_2026_07_27.md · publication_business_analysis.md · ba_production_publishing_gap.md · API_USAGE.md · TEST_RESULTS.md + source code 200 files*
