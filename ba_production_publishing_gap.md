# Báo cáo Đối chiếu BA V3 — Production Plan & Mô hình Xuất bản Nội bộ (Agile Studio)

**Phạm vi đối chiếu:** thư mục `src` của dự án và tài liệu BA V3 do người dùng cung cấp.  
**Mục tiêu:** xác định main flow nào đã đáp ứng, main flow nào còn là khoảng trống, theo đúng thuật ngữ và enum trong codebase.  
**Giới hạn đối chiếu:** theo yêu cầu của người dùng, **tạm bỏ qua khối audit/log**; chỉ tập trung vào luồng nghiệp vụ chính.  
**Phạm vi thực hiện:** chỉ phân tích, không sinh code.

---

## 1. Tóm tắt điều hành

BA V3 (bản đã đầy đủ chi tiết) quyết định chuyển sang **mô hình Studio Agile** với bốn trụ cột:

1. Bỏ pre-approval Plan (`PENDING`/`APPROVED`/`REJECTED`), chuyển sang cơ chế **Active Plan** đơn lẻ với state machine `PLANNING → IN_PROGRESS ⇄ PAUSED → COMPLETED / CANCELLED`.
2. Tách **Hội đồng Biên tập** (`EDITORIAL_BOARD_MEMBER`) khỏi **Leader** (`LEADER_BOARD`), cả hai đều có quyền publish/reject/recall ở cấp Chapter (single-signoff).
3. Chuẩn hóa vòng đời Chapter với 5 trạng thái: `DRAFT / IN_PRODUCTION / COMPLETED / SCHEDULED / PUBLISHED`; thêm trạng thái đặc biệt `COMPLETED_NEEDS_REVIEW` để leo thang khi bị Trả về ≥ 2 lần.
4. Bổ sung cơ chế **Thu hồi** Chapter đã xuất bản (Recall) với `recallReason` ≥ 15 ký tự, đồng thời có chiến lược migration dữ liệu `PlanApprovalStatus` cũ.

### Đánh giá tổng thể main flow

| Nhóm yêu cầu BA V3 (main flow) | Mức đáp ứng |
| --- | --- |
| Bỏ pre-approval Plan, set `IN_PROGRESS` ngay khi Active Plan được khởi tạo | Một phần (Project activate đã tạo Plan = `PLANNING`, chưa bỏ được `PlanApprovalStatus.PENDING` ngay khi khởi tạo, chưa mở quyền create Chapter khi Plan = `IN_PROGRESS` mà không cần `APPROVED`) |
| Pause / Resume Plan + lưu `pauseReason` + đóng băng submit | Không đáp ứng (thiếu endpoint, thiếu `pauseReason`/`pausedBy`/`pausedAt`, thiếu rule chặn submit khi `PAUSED`) |
| Force Close Plan + tự động `COMPLETED` khi đủ Chapter `PUBLISHED` | Một phần (auto-complete khi mọi Chapter `PUBLISHED` đã có; force-close bằng tay chưa có) |
| Editorial Board `EDITORIAL_BOARD_MEMBER` được publish/reject/recall | Không đáp ứng (`publishChapter` chỉ cho `LEADER_BOARD`; chưa có endpoint reject; chưa có endpoint recall) |
| Chapter state machine 5 trạng thái + `SCHEDULED` + `COMPLETED_NEEDS_REVIEW` | Không đáp ứng (code mới có `BACKLOG/IN_PRODUCTION/COMPLETED/PUBLISHED`; thiếu `DRAFT/SCHEDULED/COMPLETED_NEEDS_REVIEW`) |
| Rejection limit 2 lần, leo thang lần 3 | Không đáp ứng (chưa có `rejectionCount`, chưa có `COMPLETED_NEEDS_REVIEW`, chưa có override của Leader) |
| Recall Chapter + `recallCount` + `recallReason` (min 15 ký tự) | Không đáp ứng (chưa có endpoint recall, chưa có `recallReason`/`recallCount`) |
| Project `CANCELLED` → Plan `CANCELLED` + khóa Chapter `DRAFT/IN_PRODUCTION/COMPLETED` | Không đáp ứng (`ProjectWorkflowStatus` hiện là `DRAFT/ACTIVE/ON_HOLD/COMPLETED`, chưa có `CANCELLED`; chưa có rule cascade) |
| Trả về Chapter → Tantou chuyển đúng Task sang `REVISION_REQUIRED` | Không đáp ứng (chưa có endpoint reject, chưa có logic chuyển Task sang `REVISION_REQUIRED`) |
| Optimistic locking cho xung đột publish/reject | Không đáp ứng (chưa có `version`/`updatedAt` ở `Chapter`) |
| Migration `PlanApprovalStatus` cũ → `IN_PROGRESS`/`PAUSED` | Không đáp ứng (chưa có migration script, chưa có `@Deprecated` trên `approvalStatus`) |
| `pauseReason` reset về NULL khi Resume | Không đáp ứng |

**Kết luận tổng main flow:** hệ thống hiện đáp ứng khoảng **25–35%** yêu cầu BA V3 (phần auto-complete Plan khi mọi Chapter `PUBLISHED` và quan hệ Project-Plan 1-1). Phần còn lại đều là khoảng trống cần xây mới.

### Chấm điểm BA V3 dưới góc BA 3 năm (thang 100)

| # | Tiêu chí | Điểm /10 | Diễn giải |
| --- | --- | ---: | --- |
| 1 | **Tính nhất quán nội tại** (giữa các phần BA, enum, thuật ngữ) | 5 | §1.1 dùng `IN_PROGRESS` cho Chapter, codebase dùng `IN_PRODUCTION` → xung đột thuật ngữ ngay trong tài liệu. §2.1 hình state machine có nhánh `PAUSED → COMPLETED ⬅ Force Close` nhưng narrative chỉ nói "Force Close áp dụng cả khi IN_PROGRESS hoặc PAUSED" — không thống nhất giữa vẽ và nói. `COMPLETED` vừa gọi "terminal mềm" vừa có rule tự lùi `IN_PROGRESS` → nên gọi thẳng `soft-state`. |
| 2 | **Tính đầy đủ state machine** | 6 | Mạnh: có auto-complete theo `targetChapterCount`, có force close, có branch recall/return. Yếu: diagram thiếu arrow `PAUSED → IN_PROGRESS` ở narrative (chỉ có ở dòng trên cùng); `COMPLETED → IN_PROGRESS` nhảy ngược không nằm trong diagram; `PAUSED → CANCELLED` (khi Project hủy) — BA không vẽ. |
| 3 | **Tính chặt chẽ rule nghiệp vụ** (điều kiện biên) | 7 | Mạnh: rejection count + leo thang + override, recall min 15 ký tự, cascade Project CANCELLED, migration script cụ thể. Yếu: `rejectionCount` không nói có reset khi chapter re-complete không; "khóa Chapter" khi `CANCELLED` không định nghĩa cụ thể (cấm submit? cấm update status? cấm cả tải file?); `SCHEDULED` rất mơ hồ — không có actor, không có endpoint "Lên lịch", không nói rõ scheduler ở đâu; "Mở khóa Task" khi Recall — không nói reset Task từ `DONE` về `IN_PROGRESS` hay chỉ mở quyền chuyển sang `REVISION_REQUIRED`. |
| 4 | **Tính khả thi hạ tầng kỹ thuật** (DEV có thể code thẳng) | 6 | Ưu: gần như mọi rule đều ánh xạ được sang entity + service + controller (xem §5). Nhược: BA §3.2 nói "version (Integer) **hoặc** updatedAt (Timestamp)" — đây là hai cơ chế khác nhau, JPA `@Version` dùng `Long`, không phải Integer; nên chốt một cơ chế; `targetChapterCount` chưa rõ là tổng số chapter hay con số riêng; Comment trao đổi khi Plan PAUSED — BA ghi tính năng nhưng không chỉ bảng nào lưu (chưa có `ChapterComment`/`PlanComment`); `releaseNote` không bắt buộc — cần chốt để chốt schema. |
| 5 | **Mức độ sẵn sàng đưa cho DEV triển khai** | 5 | Có **10 câu hỏi mở** ở §6 mà BA V3 chưa trả lời. 10 câu này nằm ở các điểm then chốt của main flow — đặc biệt là vai của Leader vs Board ở Cancel Project, "khóa Chapter" là cụ thể gì, Comment trao đổi lưu bảng nào. Một BA 3 năm sẽ không bàn giao DEV khi còn 10 câu hỏi mở; cần một buổi chốt trước. |
| 6 | **Tính truy vết được** (audit, dấu vết quyết định) | 5 | Đã bị người dùng chủ động loại khỏi phạm vi đối chiếu — nhưng BA V3 vẫn còn audit ngầm trong state machine. Cụ thể: Pause Plan chỉ lưu `pausedBy/pausedAt/pauseReason` (lưu trực tiếp trên Plan) → nếu Plan bị pause nhiều lần sẽ mất dấu lý do lần trước (chỉ giữ lần cuối). Cần BA chốt: lưu lịch sử pause/resume thành bảng riêng (`PlanPauseHistory`), hay chỉ giữ trạng thái hiện tại? |
| | **Tổng** | **34/60 → ~57/100** | |

**Tách riêng hai khía cạnh:**

- **Tinh thần BA V3** (Agile, Active Plan đơn lẻ, Editorial Board multi-user, soft-terminal Plan, recall có trách nhiệm): **8/10**. Thể hiện tư duy sản phẩm tốt — biết rằng `COMPLETED` không nên là terminal cứng, biết rejection cần giới hạn, biết recall cần chứng cớ tối thiểu 15 ký tự.
- **Đặc tả BA V3** (rule, enum, edge case, mơ hồ thuật ngữ): **5/10**. Vẫn còn 10 câu hỏi mở, thiếu 4–5 rule biên, còn xung đột thuật ngữ `IN_PROGRESS` vs `IN_PRODUCTION` ngay trong tài liệu.

**So với BA bản trước** (chấm 6/10 ở lần đối chiếu đầu): BA V3 tiến bộ rõ. Tiến bộ cụ thể: có state machine diagram, có rejection limit + leo thang, có Recall + `recallCount` + `recallReason`, có optimistic locking, có migration script, có state `COMPLETED_NEEDS_REVIEW` làm cầu nối rõ ràng.

**So với một BA đủ chuẩn để đưa thẳng cho DEV** (cần ~75/100): còn thiếu khoảng **18 điểm**, phân bổ:

1. **Thuật ngữ & enum** (mất ~3 điểm): chốt `IN_PRODUCTION` vs `IN_PROGRESS`, chốt `BACKLOG` vs `DRAFT`, bỏ phương án "version Integer hoặc updatedAt" — chốt một cơ chế.
2. **Rule nghiệp vụ còn lủng** (mất ~8 điểm): reset `rejectionCount`, "khóa" cụ thể, "Mở khóa Task" cụ thể, scheduler/SCHEDULED rõ actor, Cancel Project rõ role, Comment lưu bảng nào, audit pause lịch sử hay trạng thái hiện tại.
3. **Diagram & narrative đồng bộ** (mất ~3 điểm): diagram thiếu resume, diagram thiếu `PAUSED → CANCELLED`, narrative nói "force close từ IN_PROGRESS/PAUSED" mà diagram chỉ vẽ một chiều.
4. **Một số giá trị biên chưa chốt** (mất ~4 điểm): `recallCount` có max không, bao nhiêu Leader ký mới Cancel được, có đa Leader ký ở Cancel không.

---

## 2. Chuẩn hóa thuật ngữ & mapping enum thực tế

### 2.1. Bảng mapping enum giữa BA V3 và codebase

| Khái niệm BA V3 | Enum trong BA V3 | Enum hiện tại trong code | Ghi chú |
| --- | --- | --- | --- |
| Role Hội đồng | (định nghĩa mới: multi-user) | `EDITORIAL_BOARD_MEMBER` | Đã có sẵn — trùng khớp tinh thần BA |
| Role Leader | (định nghĩa mới) | `LEADER_BOARD` | Đã có sẵn — trùng khớp |
| Role Tantou | (định nghĩa mới) | `TANTOU_EDITOR` | Đã có sẵn |
| Role Mangaka | (định nghĩa mới) | `MANGAKA` | Đã có sẵn |
| Role Assistant | (định nghĩa mới) | `ASSISTANT` | Đã có sẵn |
| Role Manager | (không đề cập trong BA V3) | `MANAGER` | Tồn tại trong code, BA V3 chưa định nghĩa |
| Chapter: DRAFT | `DRAFT` | Không có (mới tạo = `BACKLOG`) | Cần quyết: tái sử dụng `BACKLOG` hay thêm `DRAFT`? |
| Chapter: IN_PRODUCTION | `IN_PRODUCTION` | `IN_PRODUCTION` | Trùng khớp |
| Chapter: COMPLETED | `COMPLETED` | `COMPLETED` | Trùng khớp |
| Chapter: SCHEDULED | `SCHEDULED` | Không có | Khoảng trống |
| Chapter: PUBLISHED | `PUBLISHED` | `PUBLISHED` | Trùng khớp |
| Chapter: COMPLETED_NEEDS_REVIEW | `COMPLETED_NEEDS_REVIEW` | Không có | Khoảng trống (BA §3.3) |
| Plan: PLANNING | `PLANNING` | `PlanStatus.PLANNING` | Đã có |
| Plan: IN_PROGRESS | `IN_PROGRESS` | `PlanStatus.IN_PROGRESS` | Đã có |
| Plan: PAUSED | `PAUSED` | `PlanStatus.PAUSED` | Đã có |
| Plan: COMPLETED | `COMPLETED` | `PlanStatus.COMPLETED` | Đã có |
| Plan: CANCELLED | `CANCELLED` | Không có | Khoảng trống (BA §2.1, §4.1) |
| Plan Approval cũ | `PENDING/APPROVED/REJECTED` | `PlanApprovalStatus.{PENDING,APPROVED,REJECTED}` | BA muốn loại bỏ |
| Project: ACTIVE | `ACTIVE` | `ProjectWorkflowStatus.ACTIVE` | Đã có |
| Project: CANCELLED | (định nghĩa mới) | Không có (chỉ `DRAFT/ACTIVE/ON_HOLD/COMPLETED`) | Khoảng trống (BA §4.1) |

### 2.2. Action Mapping theo BA V3 (chốt với DEV)

| Hành động BA | Method Java đề xuất | Endpoint đề xuất |
| --- | --- | --- |
| Xuất bản Chapter | `publishChapter` | `POST /api/v1/chapters/{id}/publish` |
| Trả về sản xuất | `returnChapterToProduction` | `POST /api/v1/chapters/{id}/return` |
| Thu hồi Chapter | `recallChapter` | `POST /api/v1/chapters/{id}/recall` |
| Pause Plan | `pausePlan` | `POST /api/v1/plans/{id}/pause` |
| Resume Plan | `resumePlan` | `POST /api/v1/plans/{id}/resume` |
| Force Close Plan | `forceClosePlan` | `POST /api/v1/plans/{id}/force-close` |
| Cancel Project | `cancelProject` | `POST /api/v1/projects/{id}/cancel` |
| Override Reject (Leader) | `overrideReturnLimit` | `POST /api/v1/chapters/{id}/override-return` |

**Lưu ý đặc biệt:** BA V3 viết `IN_PROGRESS` cho cả Plan và Chapter. Codebase dùng `IN_PRODUCTION` cho Chapter. Hai bên nên **giữ `IN_PRODUCTION` cho Chapter** (đã dùng ổn trong `ChapterStatus`) để không phá vỡ dữ liệu cũ. Cần thống nhất trong tài liệu: BA V3 có thể ghi rõ "Chapter dùng `IN_PRODUCTION` thay vì `IN_PROGRESS`" để tránh hiểu nhầm khi đọc chéo.

---

## 3. Đối chiếu chi tiết theo luồng main flow

### 3.1. State machine `ProductionPlan`

**BA V3 §2.1:**

```text
[Khởi tạo] ──► IN_PROGRESS ◄──── (Resume) ────► PAUSED
                     │                             │
                     ├────────► COMPLETED ◄────────┘ (Chỉ qua Force Close)
                     │
                     └────────► CANCELLED (Khi Project bị hủy)
```

#### 3.1.1. Khởi tạo & điều kiện `IN_PROGRESS`

**BA V3:**
- Project `ACTIVE` → Plan tự khởi tạo (`PLANNING`) → Tantou điền xong → chuyển `IN_PROGRESS` ngay, không qua pre-approval.
- Tantou có thể tạo Chapter khi Plan = `IN_PROGRESS` (không cần `APPROVED`).

**Hiện trạng code:**

```java
// ProductionWorkflowServiceImpl.activateProject (dòng 60-84)
project.setProjectWorkflowStatus(ProjectWorkflowStatus.ACTIVE);
// Create empty ProductionPlan linked to Project
ProductionPlan plan = new ProductionPlan();
plan.setProject(project);
plan.setPlanStatus(PlanStatus.PLANNING);
productionPlanRepository.save(plan);
```

```java
// ProductionWorkflowServiceImpl.createChapter (dòng 175-179)
// Guard: production plan must be APPROVED before creating chapters
if (plan.getApprovalStatus() != PlanApprovalStatus.APPROVED) {
    throw new IllegalStateException("Cannot create chapters: ...");
}
```

```java
// ProductionPlanServiceImpl.createProductionPlan (dòng 44)
plan.setApprovalStatus(PlanApprovalStatus.PENDING);
```

**Khoảng trống:**

| # | Yêu cầu BA V3 | Hiện trạng | Khoảng trống |
| --- | --- | --- | --- |
| 3.1.1.a | Bỏ pre-approval; Plan = `IN_PROGRESS` ngay khi khởi tạo | `createProductionPlan` set `PlanApprovalStatus.PENDING`; `activateProject` tạo Plan = `PLANNING`; `approveProductionPlan` mới chuyển sang `IN_PROGRESS` | Cần đổi `createProductionPlan` mặc định `IN_PROGRESS`, bỏ guard `PlanApprovalStatus.APPROVED` trong `createChapter`, đánh dấu `approvalStatus` deprecated |
| 3.1.1.b | Tantou có thể tạo Chapter khi Plan = `IN_PROGRESS` | `createChapter` chỉ cho `TANTOU_EDITOR` (đúng actor) nhưng block bởi guard `APPROVED` | Sau khi bỏ guard, đã đáp ứng |

#### 3.1.2. Pause / Resume

**BA V3 §2.2:**

- Quyền: Tantou, Leader, Board.
- Pause: `IN_PROGRESS → PAUSED`, lưu `pausedBy`, `pausedAt`, `pauseReason`.
- Đóng băng khi `PAUSED`:
  - Chặn: tạo Chapter, giao Task/SubTask, nộp Submission (Rough/Final), sửa Deadline, đổi Assistant/Tantou.
  - Cho phép: Read-only, tải file cũ, viết Comment.
- Resume: `PAUSED → IN_PROGRESS`, không bắt buộc nhập lý do, reset `pauseReason` về NULL.

**Hiện trạng code:**
- `ProductionPlan` hiện không có `pausedBy`/`pausedAt`/`pauseReason`.
- Không có endpoint `/plans/{id}/pause`, `/plans/{id}/resume`.
- `ProductionWorkflowServiceImpl.createChapter` chỉ check `PlanApprovalStatus.APPROVED`, không check `planStatus`.
- `ProductionWorkflowServiceImpl.updateTaskStatus`, `assignChapter`, `assignTask`, `createFeedback` không check `planStatus`.

**Khoảng trống:**

| # | Yêu cầu BA V3 | Hiện trạng | Khoảng trống |
| --- | --- | --- | --- |
| 3.1.2.a | Lưu `pausedBy/pausedAt/pauseReason` | Thiếu cả ba trường | Thêm cột DB + setter trên `ProductionPlan` |
| 3.1.2.b | Endpoint Pause / Resume | Không có | Tạo `POST /plans/{id}/pause`, `POST /plans/{id}/resume` (lấy actor từ JWT) |
| 3.1.2.c | Đóng băng submit khi `PAUSED` | Không có rule nào | Thêm check `plan.planStatus == PAUSED` ở `createChapter`, `assignTask`, `assignSubTask`, `submitRough/Final`, `updateTaskDeadline` |
| 3.1.2.d | Resume reset `pauseReason = NULL` | Không có logic | Clear ba trường pause ở service `resumePlan` |

#### 3.1.3. Auto-complete & Force Close

**BA V3 §2.1:**
- Auto-complete: khi `chapter.PUBLISHED == targetChapterCount` VÀ không còn Chapter ở `DRAFT/IN_PRODUCTION/COMPLETED/SCHEDULED`.
- Force Close: Leader/Board bấm "Kết thúc sớm" + nhập lý do (áp dụng cả khi `IN_PROGRESS` hoặc `PAUSED`).
- `COMPLETED` là terminal mềm: nếu Hội đồng Recall/Return Chapter thuộc Plan, `planStatus` tự lùi `IN_PROGRESS`.

**Hiện trạng code:**

```java
// ProductionWorkflowServiceImpl.updateChapterStatus (dòng 528-536)
// Auto-complete the ProductionPlan when all its chapters are PUBLISHED
if (status == ChapterStatus.PUBLISHED && chapter.getProductionPlan() != null) {
    ProductionPlan plan = chapter.getProductionPlan();
    boolean allPublished = !chapterRepository
            .existsByProductionPlanIdAndChapterStatusNot(plan.getId(), ChapterStatus.PUBLISHED);
    if (allPublished && plan.getPlanStatus() != PlanStatus.COMPLETED) {
        plan.setPlanStatus(PlanStatus.COMPLETED);
        productionPlanRepository.save(plan);
    }
}
```

**Khoảng trống:**

| # | Yêu cầu BA V3 | Hiện trạng | Khoảng trống |
| --- | --- | --- | --- |
| 3.1.3.a | Auto-complete khi đủ Chapter `PUBLISHED` | Đã có logic khi set Chapter = `PUBLISHED` | Đã đáp ứng (có thể cần đảm bảo `publishChapter` cũng trigger đúng — hiện tại `publishChapter` không gọi updateChapterStatus, có auto-complete riêng ở dòng 593-601, đúng) |
| 3.1.3.b | Force Close + lý do | Không có endpoint | Cần `POST /plans/{id}/force-close` + lý do; chuyển Plan sang `COMPLETED` |
| 3.1.3.c | `COMPLETED` tự lùi `IN_PROGRESS` khi Recall/Return | Chưa có (vì chưa có Recall/Return) | Sẽ đáp ứng đồng thời khi implement §3.2.4 và §3.2.5 |
| 3.1.3.d | `targetChapterCount` là điều kiện auto-complete | Code hiện check `existsByProductionPlanIdAndChapterStatusNot(planId, PUBLISHED)` (tức zero Chapter ≠ PUBLISHED) | Logic hiện tại không cần `targetChapterCount` vì check "all chapters are PUBLISHED" tương đương điều kiện BA. Tuy nhiên BA V3 nói "đủ `targetChapterCount` VÀ không còn Chapter ở 4 trạng thái khác" — hai điều kiện này cùng nghĩa nếu `targetChapterCount == tổng số Chapter`. Cần BA xác nhận `targetChapterCount` có phải tổng số Chapter không, hay là con số riêng (vd: 12 chapter dù chỉ có 8 Chapter hiện tại). Nếu là con số riêng → cần trường `targetChapterCount` trên `ProductionPlan` (hiện đã có) |

### 3.2. Vòng đời Chapter & quyền Hội đồng

#### 3.2.1. Chuẩn hóa 5 trạng thái Chapter

**BA V3 §1.1:** `DRAFT / IN_PRODUCTION / COMPLETED / SCHEDULED / PUBLISHED`.

**Hiện trạng `ChapterStatus.java`:** `BACKLOG / IN_PRODUCTION / COMPLETED / PUBLISHED`.

**Khoảng trống:**

| # | Yêu cầu BA V3 | Hiện trạng | Khoảng trống |
| --- | --- | --- | --- |
| 3.2.1.a | Thêm `DRAFT` | Không có | Có thể thêm giá trị mới hoặc tái sử dụng `BACKLOG` |
| 3.2.1.b | Thêm `SCHEDULED` | Không có | Cần thêm giá trị và rule: Chapter `SCHEDULED` khi Hội đồng bấm "Lên lịch xuất bản" (chờ CronJob publish) |
| 3.2.1.c | Bỏ `BACKLOG` | Có `BACKLOG` | Cần quyết: thay bằng `DRAFT` (đổi tên) hay giữ cả hai (một dùng cho chưa phân Task, một cho chưa start)? BA V3 chưa rõ |

#### 3.2.2. Quyền Publish

**BA V3 §3.1:** `LEADER_BOARD` hoặc `EDITORIAL_BOARD_MEMBER` đều có quyền publish đơn phương.

**Hiện trạng code (`ProductionWorkflowServiceImpl.publishChapter` dòng 569-604):**

```java
if (!leader.hasRole(SystemRoleName.LEADER_BOARD)) {
    throw new AccessDeniedException("Only LEADER_BOARD can publish a chapter");
}
```

**Khoảng trống:**

| # | Yêu cầu BA V3 | Hiện trạng | Khoảng trống |
| --- | --- | --- | --- |
| 3.2.2.a | Cả `LEADER_BOARD` và `EDITORIAL_BOARD_MEMBER` đều được publish | Chỉ cho `LEADER_BOARD` | Đổi điều kiện thành `LEADER_BOARD OR EDITORIAL_BOARD_MEMBER` |

#### 3.2.3. Release Note (tùy chọn)

**BA V3 §1.2:** Release Note không bắt buộc khi Hội đồng bấm Xuất bản.

**Hiện trạng:** `publishChapter(chapterId, leaderId, publishDate)` không có Release Note.

**Khoảng trống:**

| # | Yêu cầu BA V3 | Hiện trạng | Khoảng trống |
| --- | --- | --- | --- |
| 3.2.3.a | Hỗ trợ `releaseNote` tùy chọn | Thiếu | Thêm DTO `ReleaseChapterRequest { releaseNote: String? }`, lưu vào trường mới trên `Chapter` |

> **Ghi chú audit:** vì người dùng tạm bỏ qua khối audit, có thể **không** lưu `releaseNote` lên DB ngay từ đầu; chỉ thêm API input + response echo để dễ mở rộng sau.

#### 3.2.4. Trả về sản xuất (`returnChapterToProduction`)

**BA V3 §1.2, §3.3:**
- Endpoint: `POST /api/v1/chapters/{id}/return`.
- Actor: `LEADER_BOARD` hoặc `EDITORIAL_BOARD_MEMBER`.
- Input bắt buộc: `rejectionReason` (Text).
- State: `COMPLETED → IN_PRODUCTION`.
- Tăng `rejectionCount` lên 1.
- Nếu `rejectionCount == 2` và tiếp tục bấm Trả về → khóa Chapter ở trạng thái `COMPLETED_NEEDS_REVIEW`, không cho trả về tự động, hiện thông báo "Chapter đã bị trả về 2 lần. Bắt buộc tổ chức họp Hội đồng để chốt phương án.".
- Override: chỉ Leader mới được mở khóa để Trả về lần 3.

**Hiện trạng code:**
- Không có endpoint return.
- `Chapter` không có `rejectionCount`.
- Không có `COMPLETED_NEEDS_REVIEW`.

**Khoảng trống:**

| # | Yêu cầu BA V3 | Hiện trạng | Khoảng trống |
| --- | --- | --- | --- |
| 3.2.4.a | Endpoint `POST /chapters/{id}/return` với `rejectionReason` | Không có | Tạo endpoint + service |
| 3.2.4.b | `rejectionCount` tăng theo | Thiếu | Thêm cột DB + setter |
| 3.2.4.c | Sau 2 lần → `COMPLETED_NEEDS_REVIEW` | Thiếu | Thêm giá trị enum `ChapterStatus.COMPLETED_NEEDS_REVIEW`, logic chặn ở service |
| 3.2.4.d | Override lần 3 chỉ cho Leader | Thiếu | Endpoint `POST /chapters/{id}/override-return`, actor = `LEADER_BOARD` |

#### 3.2.5. Thu hồi Chapter đã xuất bản (`recallChapter`)

**BA V3 §3.4:**
- Quyền: `LEADER_BOARD` hoặc `EDITORIAL_BOARD_MEMBER`.
- Endpoint: `POST /api/v1/chapters/{id}/recall`.
- Input: `recallReason` bắt buộc, min 15 ký tự.
- State: `PUBLISHED → IN_PRODUCTION`.
- Tăng `recallCount` lên 1.
- Mở khóa mọi Task trong Chapter.
- Nếu Plan đang `COMPLETED`, planStatus tự lùi `IN_PROGRESS`.

**Hiện trạng code:**
- Không có endpoint recall.
- `Chapter` không có `recallCount`/`recallReason`.

**Khoảng trống:**

| # | Yêu cầu BA V3 | Hiện trạng | Khoảng trống |
| --- | --- | --- | --- |
| 3.2.5.a | Endpoint `POST /chapters/{id}/recall` với `recallReason` (min 15) | Không có | Tạo endpoint + DTO với validation `@Size(min=15)` |
| 3.2.5.b | Tăng `recallCount` | Thiếu | Thêm cột DB |
| 3.2.5.c | Mở khóa Task | Không có logic | Trong service recall: với mỗi Task, đảm bảo không ở `DONE_LOCKED`/`PUBLISHED` (trạng thái hiện không có, cần BA xác nhận "khóa" là gì — có thể chỉ cần reset subtask về `IN_PROGRESS`) |
| 3.2.5.d | Plan `COMPLETED → IN_PROGRESS` nếu Plan thuộc Chapter đang ở `COMPLETED` | Thiếu | Thêm rule ở service recall: kiểm tra plan.planStatus == COMPLETED → set về IN_PROGRESS |

#### 3.2.6. Optimistic locking cho xung đột publish/reject

**BA V3 §3.2:**
- `Chapter` có trường `version` (Integer) hoặc `updatedAt` (Timestamp).
- Request đến sau → HTTP 409 Conflict với thông báo "Trạng thái Chapter đã được cập nhật bởi người dùng khác".

**Hiện trạng:** `Chapter.java` không có `version`/`updatedAt`.

**Khoảng trống:**

| # | Yêu cầu BA V3 | Hiện trạng | Khoảng trống |
| --- | --- | --- | --- |
| 3.2.6.a | Có `version`/`updatedAt` để so sánh lúc request | Thiếu | Thêm `@Version` Long hoặc cột `updatedAt` Instant |
| 3.2.6.b | HTTP 409 nếu version cũ | Thiếu | JPA tự ném `OptimisticLockException`; bắt ở ControllerAdvice trả 409 |

### 3.3. Cascade khi Project `CANCELLED`

**BA V3 §4.1:**
- Project `CANCELLED` → Plan `CANCELLED`.
- Khóa toàn bộ Chapter ở `DRAFT/IN_PRODUCTION/COMPLETED`.
- Chapter `PUBLISHED` giữ nguyên (lịch sử).

**Hiện trạng:**
- `ProjectWorkflowStatus` không có `CANCELLED`.
- `PlanStatus` không có `CANCELLED`.

**Khoảng trống:**

| # | Yêu cầu BA V3 | Hiện trạng | Khoảng trống |
| --- | --- | --- | --- |
| 3.3.a | Thêm `ProjectWorkflowStatus.CANCELLED` | Thiếu | Thêm giá trị enum |
| 3.3.b | Thêm `PlanStatus.CANCELLED` | Thiếu | Thêm giá trị enum |
| 3.3.c | Endpoint `POST /projects/{id}/cancel` + cascade | Thiếu | Tạo endpoint + service; cascade khóa Chapter |
| 3.3.d | Khóa Chapter = "không cho thao tác gì" | Cần định nghĩa "khóa" là gì — BA V3 không nói rõ | Ghi nhận câu hỏi mở (xem §6) |

### 3.4. Trả về ảnh hưởng Task

**BA V3 §4.2:**
- Khi Chapter `COMPLETED → IN_PRODUCTION` (do Return/Recall):
  - Không reset toàn bộ Task.
  - Chapter = `IN_PRODUCTION`.
  - Tantou chủ động chọn Task cần sửa, chuyển sang `REVISION_REQUIRED` (Yêu cầu làm lại) → gán cho Mangaka/Assistant.

**Hiện trạng:**
- `TaskWorkflowStatus` hiện có (cần xem nội dung cụ thể).
- Chưa có `REVISION_REQUIRED` (cần xác nhận).
- Chưa có endpoint reject nên không có dịp kích hoạt rule.

**Khoảng trống:**

| # | Yêu cầu BA V3 | Hiện trạng | Khoảng trống |
| --- | --- | --- | --- |
| 3.4.a | Task có trạng thái `REVISION_REQUIRED` | Cần xác nhận trong `TaskWorkflowStatus` | Nếu chưa có, thêm giá trị enum |
| 3.4.b | Logic "Tantou chọn Task cần sửa" | Hiện Tantou đã có thể set bất kỳ Task nào qua `updateTaskStatus` | Cần thêm rule chỉ cho phép set `REVISION_REQUIRED` khi Chapter vừa được Return/Recall; hoặc mở cho Tantou set tự do (BA nên chốt) |

### 3.5. Migration dữ liệu cũ

**BA V3 §5.1:**
- `PlanApprovalStatus.PENDING` → `planStatus = IN_PROGRESS`.
- `PlanApprovalStatus.APPROVED` → `planStatus = IN_PROGRESS`.
- `PlanApprovalStatus.REJECTED` → `planStatus = PAUSED`, gán `pauseReason = "Hồ sơ bị Reject từ hệ thống cũ"`.

**Hiện trạng:** chưa có migration script.

**Khoảng trống:**

| # | Yêu cầu BA V3 | Hiện trạng | Khoảng trống |
| --- | --- | --- | --- |
| 3.5.a | Migration script chuyển `approvalStatus` cũ sang `planStatus` mới | Thiếu | Viết Flyway/Liquibase script hoặc Java migration bean chạy `@PostConstruct`/CommandLineRunner |
| 3.5.b | `@Deprecated` trên cột `approvalStatus` | Chưa có | Annotate field + cột DB đánh dấu deprecation |
| 3.5.c | Giữ cột ~2 sprint rồi DROP | Thiếu | Lên plan trong sprint backlog |

---

## 4. Tổng hợp khoảng trống main flow

Gom tất cả khoảng trống ở §3 vào bảng ưu tiên (main flow, không bao gồm audit/log):

| STT | Main flow | Khoảng trống | Độ phức tạp DEV |
| --- | --- | --- | --- |
| 1 | Bỏ pre-approval Plan | Đổi default `createProductionPlan`; bỏ guard `APPROVED` ở `createChapter`; thêm `PlanApprovalStatus@Deprecated`; viết migration script | Trung bình |
| 2 | Pause / Resume Plan | Thêm `pausedBy/pausedAt/pauseReason` trên `ProductionPlan`; 2 endpoint mới; rule đóng băng submit ở `createChapter/assignTask/assignSubTask/submit/updateDeadline` | Cao |
| 3 | Force Close Plan | Endpoint + lý do; logic chuyển `PlanStatus.COMPLETED` | Thấp |
| 4 | Tự lùi Plan khi Recall/Return | Rule ở service recall/return | Trung bình |
| 5 | Mở quyền publish cho `EDITORIAL_BOARD_MEMBER` | Đổi điều kiện ở `publishChapter` | Thấp |
| 6 | Thêm `ChapterStatus.SCHEDULED` + `COMPLETED_NEEDS_REVIEW` (và `DRAFT` nếu BA chốt) | Enum + rule | Trung bình |
| 7 | Endpoint `returnChapterToProduction` + `rejectionCount` + giới hạn 2 lần | Service + DTO + enum + cột DB | Cao |
| 8 | Override lần 3 cho Leader | Endpoint + actor check | Trung bình |
| 9 | Endpoint `recallChapter` + `recallReason` (min 15) + `recallCount` | Service + DTO + validation + cột DB | Trung bình |
| 10 | Optimistic locking `Chapter.version` | Thêm cột + JPA `@Version` + ControllerAdvice trả 409 | Thấp |
| 11 | Cascade `Project.CANCELLED` → Plan + Chapter khóa | Enum + endpoint + service cascade | Trung bình |
| 12 | `TaskWorkflowStatus.REVISION_REQUIRED` | Enum + rule chuyển trạng thái | Thấp |
| 13 | Migration script `PlanApprovalStatus` cũ | Flyway script | Trung bình |

---

## 5. Đề xuất cấu trúc thay đổi (chỉ phân tích, chưa code)

### 5.1. Model

- `ProductionPlan`:
  - Thêm: `pausedBy` (Long, FK Account), `pausedAt` (Instant), `pauseReason` (Text).
  - `@Deprecated` lên field `approvalStatus`.
  - Enum `PlanStatus`: thêm `CANCELLED`.
- `Chapter`:
  - Thêm: `rejectionCount` (int, default 0), `recallCount` (int, default 0), `recallReason` (Text), `rejectionReason` (Text), `releaseNote` (Text, optional), `version` (`@Version` Long, default 0) hoặc `updatedAt`.
  - Enum `ChapterStatus`: thêm `SCHEDULED`, `COMPLETED_NEEDS_REVIEW`. Cân nhắc thêm `DRAFT` hoặc tái dùng `BACKLOG`.
- `Project`:
  - Enum `ProjectWorkflowStatus`: thêm `CANCELLED`.
- `Task`:
  - Enum `TaskWorkflowStatus`: thêm `REVISION_REQUIRED` (nếu chưa có).

### 5.2. Service

- `ProductionPlanService`:
  - `createProductionPlan`: đổi default sang `PlanStatus.IN_PROGRESS`, không set `PlanApprovalStatus.PENDING` nữa.
  - Thêm: `pausePlan`, `resumePlan`, `forceClosePlan`.
  - Helper: `assertNotPaused(plan)` để các service khác gọi.
- `ProductionWorkflowService` (hoặc tách thành `ChapterReleaseService`):
  - `publishChapter`: đổi actor check, thêm `releaseNote`.
  - Thêm: `returnChapterToProduction`, `overrideReturnLimit`, `recallChapter`.
  - Bỏ guard `PlanApprovalStatus.APPROVED` ở `createChapter`.
  - Thêm check `plan.planStatus == PAUSED` ở các hàm submit.
- `ProjectService` (nếu có) hoặc mới:
  - `cancelProject`: cascade `plan → CANCELLED`, khóa Chapter (rule cần BA xác nhận "khóa" cụ thể).

### 5.3. Controller

- `ProductionPlanController`: thêm `POST /plans/{id}/pause`, `/resume`, `/force-close`.
- `ChapterController` (hoặc mới `ChapterReleaseController`): thêm `POST /chapters/{id}/return`, `/override-return`, `/recall`, đổi `POST /chapters/{id}/publish`.
- `ProjectController`: thêm `POST /projects/{id}/cancel`.

### 5.4. Security / RBAC

- Actor lấy từ `Authentication` (JWT) cho mọi endpoint mới.
- Role mapping BA V3:
  - Publish/Return/Recall: `LEADER_BOARD` hoặc `EDITORIAL_BOARD_MEMBER`.
  - Pause/Resume Plan: `TANTOU_EDITOR`, `LEADER_BOARD`, `EDITORIAL_BOARD_MEMBER`.
  - Force Close Plan: `LEADER_BOARD` hoặc `EDITORIAL_BOARD_MEMBER`.
  - Override Return: chỉ `LEADER_BOARD`.
  - Cancel Project: `LEADER_BOARD` (hoặc BA V3 cần xác nhận có cho `EDITORIAL_BOARD_MEMBER` không).

### 5.5. DTO

- `PausePlanRequest { reason: String }`.
- `ForceClosePlanRequest { reason: String }`.
- `ReleaseChapterRequest { releaseNote: String? }`.
- `ReturnChapterRequest { rejectionReason: String }`.
- `RecallChapterRequest { recallReason: String @Size(min=15) }`.
- `CancelProjectRequest { reason: String? }`.

### 5.6. Migration

- Viết Flyway script (hoặc Java migration bean) để convert dữ liệu cũ:
  - `approvalStatus = PENDING` → `planStatus = IN_PROGRESS`.
  - `approvalStatus = APPROVED` → `planStatus = IN_PROGRESS`.
  - `approvalStatus = REJECTED` → `planStatus = PAUSED`, `pauseReason = "Hồ sơ bị Reject từ hệ thống cũ"`.
- Giữ cột `approvalStatus` khoảng 2 sprint, đánh dấu `@Deprecated` ở entity, sau đó DROP.

---

## 6. Câu hỏi mở cần BA xác nhận trước khi DEV triển khai

Các câu dưới đây BA V3 chưa nói rõ, DEV không thể tự quyết:

1. **Chapter.DRAFT vs BACKLOG:** tái sử dụng `BACKLOG` (đổi tên) hay thêm `DRAFT` giữ song song? Ý nghĩa nghiệp vụ có khác không (`BACKLOG` hiện dùng cho Chapter mới tạo chưa start)?
2. **SCHEDULED do ai kích hoạt?** BA V3 §1.1 nói "đã chốt ngày xuất bản, chờ CronJob tự động kích hoạt". Tức là có bước "Lên lịch" riêng (Hội đồng chọn ngày tương lai → `SCHEDULED`), và một scheduler job sẽ chuyển sang `PUBLISHED` khi tới giờ. BA V3 chưa mô tả endpoint "Lên lịch" và cơ chế scheduler. Cần xác nhận có trong main flow không.
3. **"Khóa Chapter" khi Project `CANCELLED` nghĩa là gì?** Cụ thể: cấm submit? cấm update status? cấm tải file? cần định nghĩa "khóa" trước khi code.
4. **"Mở khóa Task" khi Recall:** "mở khóa" tương ứng với trạng thái nào? Reset Task từ `DONE` về `IN_PROGRESS`, hay chỉ cho phép set sang `REVISION_REQUIRED`? BA V3 không nói rõ.
5. **`rejectionCount` có reset khi Return thành công không?** (Sau khi Tantou sửa xong và bấm "Hoàn thành" lại thành `COMPLETED`, biến đếm có reset về 0 hay giữ nguyên?)
6. **`SCHEDULED → PUBLISHED` có qua bước Hội đồng duyệt lần cuối không?** Hay scheduler tự publish mà không cần actor? BA V3 không nói.
7. **`recallCount` có giới hạn tối đa không?** (Ví dụ: chỉ cho Recall tối đa 1 lần, sau đó phải tạo Chapter mới.)
8. **Permission "Cancel Project" thuộc role nào?** BA V3 không nói — Leader hay cả Hội đồng?
9. **Comment trao đổi khi Plan `PAUSED`:** "Cho phép viết Comment trao đổi" — endpoint/bảng nào lưu Comment hiện tại? Có cần thêm bảng `ChapterComment`/`PlanComment`?
10. **`publishedBy` là `userId` cá nhân hay cả `EDITORIAL_BOARD_MEMBER` group?** Nếu Hội đồng là multi-user, vẫn lưu userId người bấm, hay cần thêm `groupRole`?

---

## 7. Kết luận

BA V3 (bản đầy đủ) đã chặt hơn bản trước ở nhiều điểm: thêm `COMPLETED_NEEDS_REVIEW`, `recallCount`, optimistic locking, force-close, migration script. **Chấm tổng thể: 57/100** dưới góc BA 3 năm — tinh thần 8/10, đặc tả 5/10. Vẫn còn **10 câu hỏi mở** ở §6 cần BA chốt trước khi đưa cho DEV.

Về mặt main flow (loại trừ audit), hệ thống hiện đáp ứng khoảng **25–35%** yêu cầu BA V3. Khoảng trống phân bổ:

- **Nhóm trung bình** (làm trước, ~1–2 sprint): bỏ pre-approval, mở quyền publish cho Editorial Board, thêm `pauseReason` + endpoint pause/resume, thêm `recallReason` + recall endpoint, auto-complete Plan đã có sẵn.
- **Nhóm cao** (cần thêm 1–2 sprint): state machine Chapter 5 trạng thái + `COMPLETED_NEEDS_REVIEW`, rejection limit + override, cascade `Project.CANCELLED`, optimistic locking.
- **Nhóm thấp** (làm nhanh, song song): Force Close Plan, `REVISION_REQUIRED` enum, migration script.

Khuyến nghị lộ trình:

1. **Sprint 1 (main flow cốt lõi)**: bỏ pre-approval (#1), mở quyền publish (#5), thêm pause/resume + `pauseReason` (#2), endpoint recall (#9), Force Close (#3), migration script (#13).
2. **Sprint 2 (state machine Chapter + reject)**: thêm `SCHEDULED/COMPLETED_NEEDS_REVIEW` (#6), endpoint return + rejection limit + override (#7, #8), `REVISION_REQUIRED` (#12), optimistic locking (#10).
3. **Sprint 3 (edge case)**: cascade `Project.CANCELLED` (#11), tích hợp scheduler (nếu BA xác nhận), nối tiếp audit/log ở sprint sau.

### Khuyến nghị nâng BA V3 lên 75/100 (đủ chuẩn bàn giao DEV)

- Một buổi chốt 10 câu hỏi mở với stakeholder (~2 giờ, không đụng chạm tinh thần BA).
- Một bản vẽ state machine đầy đủ (PlantUML hoặc Mermaid) có chú thích rule cho mỗi arrow → có thể làm file `ba_state_diagram.md` kèm theo.
- Một bảng quyết định (decision table) cho 5 câu edge case: reset `rejectionCount`, "khóa" cụ thể, "Mở khóa Task" cụ thể, scheduler/SCHEDULED, audit pause lịch sử hay trạng thái hiện tại.

Nếu muốn, tôi có thể tạo thêm:

- `ba_v3_scorecard.md` — chấm chi tiết từng dòng BA V3 theo thang 100.
- `ba_v3_open_questions.md` — gom 10 câu hỏi mở dưới dạng gửi stakeholder.
- Hoặc vào chế độ Plan để soạn checklist API + sequence diagram cho Sprint 1.