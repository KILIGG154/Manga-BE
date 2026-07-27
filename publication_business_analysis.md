# Báo cáo Phân tích Nghiệp vụ và Đối chiếu Codebase

## Hệ thống Manga Publishing System Backend

**Phạm vi phân tích:** thư mục `src` và tài liệu `publication_analysis.txt`  
**Mục đích:** tổng hợp hiện trạng nghiệp vụ, đối chiếu với code, xác định các khoảng trống và đề xuất mô hình workflow xuất bản phù hợp.  
**Phạm vi thực hiện:** chỉ phân tích, không triển khai hoặc chỉnh sửa code.

---

## 1. Tóm tắt điều hành

Hệ thống hiện tại đã có nền tảng cho một quy trình sản xuất manga nhiều cấp, bao gồm:

- Quản lý tài khoản và phê duyệt role.
- Quy trình gửi ý tưởng/tên truyện.
- Tạo Project và Production Plan.
- Tạo Chapter và các Task sản xuất.
- Chia Task thành SubTask cho Assistant.
- Nộp bản rough, revision và final.
- Mangaka review bản vẽ ở cấp SubTask.
- Tantou review sản phẩm ở cấp Task.
- Đánh dấu Chapter hoàn thiện.
- Leader xem danh sách Chapter hoàn thiện và thực hiện publish.

Tuy nhiên, workflow phát hành hiện tại mới dừng ở mức sơ bộ. Vấn đề cốt lõi là hệ thống đang đồng nhất hai hành động khác nhau:

1. Leader quyết định Chapter sẽ được phát hành vào một thời điểm.
2. Hệ thống thực sự mở Chapter cho độc giả đọc.

Trong code hiện tại, khi Leader truyền `publishDate`, Chapter lập tức chuyển từ `COMPLETED` sang `PUBLISHED`, kể cả khi ngày phát hành nằm trong tương lai. Vì vậy, `publishDate` đang được dùng như ngày dự kiến nhưng `PUBLISHED` lại biểu thị trạng thái public ngay lập tức.

Đề xuất trong tài liệu `publication_analysis.txt` về việc bổ sung trạng thái `SCHEDULED` là đúng hướng. Tuy nhiên, chỉ bổ sung enum là chưa đủ. Để nghiệp vụ hoàn chỉnh, cần đồng thời xử lý:

- Final editorial/release review.
- Lịch phát hành và thời điểm phát hành thực tế.
- Cơ chế tự động publish.
- Quyền truy cập nội dung cho độc giả.
- Bảo vệ file upload chưa public.
- Audit người duyệt và người lên lịch.
- Xác thực danh tính thực hiện hành động bằng JWT thay vì tin vào ID do client gửi.

---

## 2. Bản đồ nghiệp vụ hiện tại

### 2.1. Các nhóm nghiệp vụ chính

Hệ thống đang chứa ba nhóm workflow lớn.

#### Nhóm A: Tài khoản và phân quyền

- Người dùng yêu cầu OTP.
- Đăng ký tài khoản.
- Tài khoản chờ Admin hoặc Manager phê duyệt.
- Gán role hệ thống.
- Đăng nhập bằng JWT.
- Chặn đăng nhập với tài khoản chưa ở trạng thái `ACTIVE`.

#### Nhóm B: Phát triển dự án manga

- Mangaka gửi concept hoặc Name Submission.
- Editorial Board review.
- Leader quyết định kết quả cuối cùng.
- Editorial Board tạo Project.
- Tantou được gán vào Project.
- Project được activate.
- Production Plan được tạo và chờ phê duyệt.

#### Nhóm C: Sản xuất và phát hành Chapter

- Production Plan được Board hoặc Leader approve.
- Tantou tạo Chapter.
- Hệ thống tự tạo bốn Task mặc định.
- Tantou gán Chapter cho Mangaka.
- Mangaka chia nhỏ Task thành SubTask.
- Assistant thực hiện rough/final.
- Mangaka review SubTask.
- Mangaka gửi Task-level submission.
- Tantou review Task.
- Tantou xác nhận Chapter hoàn thiện.
- Leader xem Chapter hoàn thiện và publish.

### 2.2. Cây dữ liệu nghiệp vụ

```text
Project
  └── ProductionPlan
        └── Chapter
              └── Task
                    └── SubTask
                          └── Submission
                                └── SubmissionFile
```

Các quan hệ này phản ánh khá rõ trách nhiệm theo cấp:

- Project: phạm vi manga/series.
- ProductionPlan: kế hoạch và mục tiêu sản xuất.
- Chapter: đơn vị nội dung phát hành.
- Task: nhóm công việc của một Chapter.
- SubTask: phần việc giao cho Assistant.
- Submission: bằng chứng sản phẩm qua từng vòng.
- SubmissionFile: file vật lý hoặc URL của sản phẩm.

---

## 3. Luồng nghiệp vụ hiện tại theo từng giai đoạn

## 3.1. Đăng ký và phê duyệt tài khoản

Luồng hiện tại:

```text
Chưa có tài khoản
  → Gửi OTP
  → Đăng ký
  → PENDING
  → Admin/Manager review
  → ACTIVE hoặc REJECTED
  → Đăng nhập
```

Các role hệ thống chính:

- `ADMIN`
- `MANAGER`
- `TANTOU_EDITOR`
- `EDITORIAL_BOARD_MEMBER`
- `LEADER_BOARD`
- `MANGAKA`
- `ASSISTANT`

Điểm tích cực:

- Tài khoản có trạng thái rõ ràng.
- Role có cơ chế normalize alias như `TANTOR`, `EDITOR`, `LEADER`.
- Các endpoint phê duyệt account đã sử dụng `Authentication` để lấy người thực hiện thay vì nhận trực tiếp approver ID.

Điểm cần chú ý:

- `DataInitialized` tạo tài khoản mẫu với mật khẩu cố định, chỉ phù hợp cho development.
- Một số workflow khác vẫn nhận ID người thực hiện từ query parameter hoặc request body.
- Cách này không nhất quán với luồng account approval an toàn hơn.

---

## 3.2. Name Submission

Luồng hiện tại có dạng:

```text
Mangaka submit
  → PENDING_BOARD_REVIEW
  → Board member review/vote
  → PROCESSING
  → Leader quyết định
  → APPROVED hoặc REJECTED
```

Leader có thể yêu cầu revision:

```text
PROCESSING
  → REVISION
  → Mangaka resubmit
  → PENDING_BOARD_REVIEW
```

Điểm phù hợp với yêu cầu nghiệp vụ:

- Mangaka là người gửi concept.
- Board có thể tham gia review.
- Leader là người có quyết định cuối cùng.
- Không bắt buộc vote đa số để Leader mới được quyết định.

Khoảng trống:

1. `NameSubmissionRequest` chỉ chứa rất ít thông tin concept.
2. Các trường `story`, `characterDescription`, `worldSetting` có trong entity nhưng không được sử dụng đầy đủ ở luồng submit Name.
3. `Submission` đang phục vụ cả Name Submission và Production Submission.
4. `submissionType` được khai báo bắt buộc trong entity nhưng luồng Name Submission không gán rõ giá trị.
5. Có nguy cơ dữ liệu Name Submission không phù hợp với constraint của Production Submission.
6. Có hai đường tạo Project: từ workflow mới và từ CRUD thủ công.
7. Chưa có ràng buộc chắc chắn rằng một Name Submission APPROVED chỉ tạo ra một Project duy nhất.

Nhận định nghiệp vụ:

Name Submission nên được coi là một bounded context riêng với Production Submission. Việc dùng chung một entity `Submission` có thể tiếp tục được nếu có discriminator rõ ràng và rule dữ liệu chặt chẽ, nhưng hiện tại hai luồng đang còn trộn lẫn nhiều ý nghĩa.

---

## 3.3. Project và Production Plan

### Project

Có hai cách tạo Project:

1. `/api/workflow/projects`
   - Editorial Board tạo.
   - Bắt buộc gán Tantou.
   - Project bắt đầu ở `DRAFT`.

2. `/api/projects`
   - Admin hoặc Editorial Board tạo thủ công.
   - Tantou có thể được gán sau.
   - Có thêm các trường legacy như `status` dạng String.

Điểm bất nhất chính là Project có hai trường trạng thái:

- `status` kiểu String.
- `projectWorkflowStatus` kiểu enum.

Điều này tạo ra nguy cơ một Project có hai trạng thái mâu thuẫn, ví dụ:

```text
status = APPROVED
projectWorkflowStatus = DRAFT
```

Nên có một nguồn trạng thái chính cho workflow.

### Production Plan

Production Plan có hai trục trạng thái:

#### Approval status

```text
PENDING → APPROVED
PENDING → REJECTED
```

#### Plan status

```text
PLANNING → IN_PROGRESS → COMPLETED
                     └── PAUSED
```

Rule hiện tại:

- Khi Plan được tạo, `approvalStatus = PENDING`.
- Khi Board hoặc Leader approve, `approvalStatus = APPROVED`.
- Plan chuyển sang `IN_PROGRESS`.
- Chapter chỉ được tạo khi Plan đã `APPROVED`.

Đây là một rule quan trọng và hợp lý.

Khoảng trống:

- API tạo Plan chưa có kiểm tra quyền nghiệp vụ rõ ràng.
- Project có thể tự động tạo Plan rỗng khi activate.
- API tạo Plan thủ công có thể cố tạo Plan thứ hai cho cùng Project.
- Chưa có luồng reject và revise Plan đầy đủ.
- Chưa lưu người approve, thời điểm approve và lý do reject.
- Completion của Plan hiện phụ thuộc chủ yếu vào trạng thái Chapter.

---

## 3.4. Tạo và sản xuất Chapter

Khi Tantou tạo Chapter:

- Chapter được gắn với Production Plan.
- Chapter được gắn với Project.
- Chapter bắt đầu ở `BACKLOG`.
- Hệ thống tự động tạo bốn Task:
  - `NAME_WIP`
  - `LINEART`
  - `INKING`
  - `BACKGROUND`

Chapter chỉ được tạo khi Production Plan đã được approve.

Các giới hạn ngày hiện tại:

- Start date không được sau end date.
- Chapter không được bắt đầu trước ngày bắt đầu Plan.
- Chapter không được kết thúc sau ngày kết thúc Plan.

Điểm cần bổ sung về nghiệp vụ:

- Chưa kiểm tra chapter number trùng trong cùng Project.
- Chưa kiểm tra người tạo có phải Tantou phụ trách Project hay chỉ cần có role Tantou.
- `publishDate` có thể xuất hiện ngay từ lúc tạo Chapter, dù Leader chưa quyết định lịch phát hành.
- Ngày sản xuất và ngày phát hành chưa được tách rõ.

---

## 3.5. Task và SubTask

### Task

Task có các trạng thái:

```text
TODO → IN_PROGRESS → REVIEW → DONE
```

Tantou có thể tạo feedback:

- `APPROVED` → Task `DONE`.
- `REJECTED` → Task `IN_PROGRESS`.

Task có thể được assign cho:

- Mangaka.
- Assistant trong một số trường hợp.

### SubTask

SubTask dành cho Assistant và có lifecycle chi tiết:

```text
TODO
  → IN_PROGRESS
  → ROUGH_SUBMITTED
  → ROUGH_APPROVED hoặc ROUGH_REJECTED
  → FINAL_SUBMITTED
  → COMPLETED hoặc FINAL_REJECTED
```

Luồng submission:

- Assistant submit `ROUGH_SKETCH`.
- Mangaka approve hoặc reject.
- Assistant submit `FINAL` sau khi rough được approve.
- Mangaka approve final để SubTask `COMPLETED`.
- Nếu reject, Assistant có thể submit revision.

Điểm tốt:

- Có state machine tương đối rõ.
- Có optimistic locking trên SubTask.
- Có kiểm tra deadline SubTask không vượt deadline Task.
- Có giới hạn Assistant chỉ được submit cho SubTask mình được assign.
- Có giới hạn Mangaka chỉ review SubTask thuộc Task của mình.

Khoảng trống:

1. `REVISION` chưa biểu diễn rõ đang sửa rough hay final.
2. Sau khi tạo Revision, trạng thái SubTask chưa được chuyển về trạng thái chờ review tương ứng một cách rõ ràng.
3. Không kiểm tra đầy đủ Mangaka submit Task-level có phải assignee của Task không.
4. Tantou cũng có thể submit `TASK_LEVEL`, làm mờ trách nhiệm giữa Mangaka và Tantou.
5. Có thể reassign Task sau khi SubTask đã được tạo, dẫn tới dữ liệu trách nhiệm không nhất quán.
6. Một số query chỉ kiểm tra requester tồn tại, chưa kiểm tra ownership/Project membership chặt chẽ.

---

## 3.6. Review Submission và Feedback

Production Submission dùng các trạng thái:

```text
PENDING → APPROVED
PENDING → REJECTED
```

Quyền review:

- Submission cấp SubTask: Mangaka review.
- Submission cấp Task: Tantou review.

Rule reject bắt buộc có note là hợp lý vì người thực hiện cần biết phải sửa gì.

Tuy nhiên, code hiện tại không tạo `Feedback` entity khi review Submission bị reject. Note bị ghi vào `Submission.contentUrl`, tức là có thể ghi đè nội dung note ban đầu.

Hệ quả:

- Không có lịch sử feedback độc lập.
- Khó phân biệt ghi chú submit và lý do reject.
- Không theo dõi được nhiều vòng reject.
- Model và comment mô tả Feedback nhưng hành vi thực tế chưa nhất quán.

Đây là một khoảng trống audit quan trọng.

---

# 4. Phân tích chuyên sâu Publishing Workflow

## 4.1. Những gì code hiện tại đã có

Khác với nhận định ban đầu trong tài liệu, backend hiện tại đã có một phần publishing flow.

### API xem Chapter sẵn sàng publish

Endpoint:

```text
GET /api/workflow/projects/{projectId}/chapters/publishable
```

Endpoint trả về Chapter có:

```text
chapterStatus = COMPLETED
```

Role được phép xem:

- `LEADER_BOARD`
- `EDITORIAL_BOARD_MEMBER`

### API Leader publish

Endpoint:

```text
POST /api/workflow/chapters/{chapterId}/publish
```

Role được phép:

- Chỉ `LEADER_BOARD`.

Điều kiện:

- Chapter phải ở `COMPLETED`.
- Gán `publishDate`.
- Chuyển Chapter sang `PUBLISHED`.
- Nếu tất cả Chapter trong Plan đã `PUBLISHED`, Plan chuyển `COMPLETED`.

Như vậy, code đã có:

- Publish queue tối thiểu.
- Leader publish endpoint.
- Role restriction ở mức service.
- Roll-up Plan khi tất cả Chapter đã publish.

---

## 4.2. Lỗi nghiệp vụ hiện tại

Trong service publish, logic hiện tại có ý nghĩa:

```text
Leader chọn publishDate
  → lưu publishDate
  → chuyển ngay PUBLISHED
```

Nếu Leader chọn ngày tương lai, Chapter vẫn trở thành `PUBLISHED` ngay lập tức.

Ví dụ:

```text
Ngày hiện tại: 10/10
Ngày Leader chọn: 20/10
Kết quả hiện tại: Chapter PUBLISHED ngay ngày 10/10
```

Điều này làm sai cả:

- Trạng thái nghiệp vụ.
- Quyền truy cập của độc giả.
- Progress của Production Plan.
- Báo cáo số Chapter đã public.
- Logic notification.

`publishDate` hiện đang có ít nhất ba ý nghĩa tiềm ẩn:

1. Ngày dự kiến phát hành được nhập lúc tạo Chapter.
2. Ngày Leader chọn khi duyệt phát hành.
3. Ngày Chapter thực sự được public.

Ba khái niệm này nên được tách ra.

---

## 4.3. `ChapterStatus` đang trộn hai chiều trạng thái

Hiện tại enum là:

```text
BACKLOG
IN_PRODUCTION
COMPLETED
PUBLISHED
```

Các trạng thái này vừa mô tả production vừa mô tả publishing.

### Chiều production

- `BACKLOG`: chưa bắt đầu.
- `IN_PRODUCTION`: đang làm.
- `COMPLETED`: production hoàn tất.

### Chiều publishing

- Chưa được Leader duyệt.
- Đã được duyệt phát hành.
- Đã lên lịch.
- Đã public.
- Đang hold.
- Đã retract.

Nếu tiếp tục gộp hai chiều, sau này sẽ khó biểu diễn các tình huống:

- Production đã xong nhưng release chưa được duyệt.
- Đã schedule nhưng bị hold.
- Đã public nhưng cần tạm ẩn.
- Public bản cũ nhưng đang chờ thay bản mới.

Đề xuất `SCHEDULED` là cần thiết, nhưng về dài hạn có thể cần tách Production Status và Release Status thành hai trục riêng.

---

## 4.4. Final editorial review chưa được mô hình hóa đầy đủ

Tài liệu mô tả Leader hoặc Editorial Board sẽ đọc lại Chapter hoàn chỉnh trước khi phát hành.

Code hiện tại mới có các bước:

- Assistant submission review bởi Mangaka.
- Task-level review bởi Tantou.
- Tantou xác nhận Chapter `COMPLETED`.
- Leader publish.

Task-level approval của Tantou chưa nhất thiết tương đương với final editorial release review của Leader.

Final editorial review có thể cần kiểm tra:

- Lỗi nội dung.
- Lỗi chính tả hoặc lettering.
- Sai thứ tự trang.
- Thiếu trang.
- Sai metadata Chapter.
- Nội dung không phù hợp quy chuẩn.
- Bản quyền hoặc quyền sử dụng tài sản.
- Độ tuổi và cảnh báo nội dung.
- Định dạng bản cuối dùng để phân phối.

Nếu Leader chỉ bấm publish mà không có bản ghi review, hệ thống không lưu được bằng chứng rằng Chapter đã được kiểm tra trước phát hành.

---

# 5. Luồng Publishing nghiệp vụ nên hướng tới

## 5.1. Luồng tối thiểu

```text
BACKLOG
  → IN_PRODUCTION
  → COMPLETED
  → SCHEDULED
  → PUBLISHED
```

Ý nghĩa:

- `BACKLOG`: Chapter tồn tại nhưng chưa bắt đầu sản xuất.
- `IN_PRODUCTION`: đang sản xuất.
- `COMPLETED`: production đã hoàn tất, chờ xử lý phát hành.
- `SCHEDULED`: Leader đã duyệt và lên lịch, nhưng chưa public.
- `PUBLISHED`: hệ thống đã mở nội dung cho độc giả.

## 5.2. Luồng nghiệp vụ đầy đủ hơn

```text
Production Completed
  → Release Review
  → Release Approved
  → Scheduled
  → Auto Published
```

Có thể hiểu như sau:

### Production Completed

Điều kiện:

- Tất cả Task đã `DONE`.
- Tất cả SubTask cần thiết đã `COMPLETED`.
- Task-level submission đã được Tantou approve.
- Tantou xác nhận Chapter.

### Release Review

Leader kiểm tra:

- Final output.
- Metadata.
- Slot phát hành.
- Nội dung và quy định editorial.

### Scheduled

Leader chọn thời điểm phát hành tương lai.

Khi đó:

- Chapter chưa public.
- Reader chưa được đọc nội dung.
- Có thể hiển thị thông tin “sắp phát hành” nếu nghiệp vụ cho phép.
- Có thể reschedule hoặc cancel theo quyền.

### Published

Đến thời điểm được đặt lịch:

- Hệ thống kiểm tra Chapter vẫn ở `SCHEDULED`.
- Hệ thống chuyển sang `PUBLISHED`.
- Ghi lại thời điểm public thực tế.
- Nội dung được mở cho reader.
- Gửi notification nếu cần.

---

# 6. Vai trò của Board và Leader

Theo tài liệu, Leader là người quyết định cuối cùng. Đây là mô hình hợp lý nếu Board chỉ thực hiện vai trò tham vấn hoặc kiểm tra chất lượng.

Nên phân biệt rõ các vai trò:

## Editorial Board

Có thể:

- Xem Publishing Queue.
- Xem final output.
- Đưa ra nhận xét.
- Đánh dấu vấn đề cần lưu ý.
- Tham gia release review nếu doanh nghiệp yêu cầu.

Không nhất thiết phải:

- Vote từng Chapter.
- Có quyền schedule cuối cùng.
- Có quyền chuyển Chapter sang `PUBLISHED`.

## Leader Board

Có thể:

- Thực hiện final decision.
- Duyệt phát hành.
- Chọn thời điểm phát hành.
- Reschedule.
- Hold hoặc cancel lịch.
- Quyết định publish ngay trong trường hợp đặc biệt.

## Hệ thống tự động

Có trách nhiệm:

- Kiểm tra các Chapter `SCHEDULED` đến hạn.
- Chuyển sang `PUBLISHED`.
- Ghi nhận actual publication time.
- Đảm bảo nội dung chỉ mở đúng thời điểm.

---

# 7. Những dữ liệu nghiệp vụ còn thiếu cho Publishing

## 7.1. Scheduled release time

`LocalDate publishDate` chỉ lưu ngày. Nhưng tài liệu đề cập tới phát hành theo giờ, ví dụ 0h thứ Hai.

Cần thống nhất:

- Chỉ phát hành theo ngày hay theo ngày-giờ?
- Có cần phút và giây không?
- Timezone chính thức là gì?
- Có dùng timezone của nền tảng không?
- Nếu server chạy khác timezone thì xử lý thế nào?

## 7.2. Actual published time

Ngày dự kiến có thể khác thời điểm thực tế.

Nên phân biệt:

- Scheduled release date/time.
- Actual published date/time.

Nếu job chạy trễ, hệ thống phải biết Chapter thực sự public lúc nào.

## 7.3. Người lên lịch và người duyệt

Cần audit:

- Ai đã schedule?
- Schedule lúc nào?
- Ai thay đổi lịch?
- Lịch cũ là gì?
- Lịch mới là gì?
- Vì sao lịch bị thay đổi?
- Ai đưa Chapter vào hold?

## 7.4. Lịch sử quyết định

Không nên chỉ lưu trạng thái cuối cùng. Cần có lịch sử:

```text
COMPLETED → SCHEDULED
SCHEDULED → SCHEDULED  (reschedule)
SCHEDULED → ON_HOLD
ON_HOLD → SCHEDULED
SCHEDULED → PUBLISHED
```

Mỗi thay đổi nên có actor, thời gian và lý do.

## 7.5. Release exception

Cần quyết định nghiệp vụ cho các trường hợp:

- Chapter đã schedule nhưng phát hiện lỗi.
- Chapter bị hold vì vấn đề pháp lý.
- Chapter cần sửa sau final review.
- Leader muốn hủy lịch.
- Chapter đến hạn nhưng file final bị lỗi.
- Scheduler thất bại.
- Chapter đã public nhưng cần tạm ẩn.

Nếu không có trạng thái hoặc chính sách cho các trường hợp này, quy trình thực tế sẽ dễ bị kẹt.

---

# 8. Quyền truy cập nội dung và vấn đề file upload

Đây là phần quan trọng mà tài liệu gốc chưa phân tích đầy đủ.

## 8.1. Chưa thấy reader-facing publishing gate đầy đủ

Code hiện tại có endpoint Chapter chung, nhưng chưa thấy một luồng reader rõ ràng với quy tắc:

```text
Reader chỉ được xem Chapter khi chapterStatus = PUBLISHED
```

Cần phân biệt quyền của:

- Leader/Board xem Chapter nội bộ.
- Team production xem Chapter đang làm.
- Reader xem Chapter public.

Nếu dùng cùng một endpoint mà không lọc theo role và status, Chapter chưa public có nguy cơ bị trả về cho người đọc.

## 8.2. `/uploads/**` đang được mở công khai

Security config cho phép truy cập không cần authentication tới `/uploads/**`.

Web configuration cũng map thư mục upload thành static resource.

Hệ quả:

- File rough có thể bị truy cập.
- File final chưa approve có thể bị truy cập.
- File của Chapter chưa publish có thể bị truy cập.
- File nội bộ production có thể bị lộ nếu URL bị biết.

Vì vậy, thêm `SCHEDULED` nhưng vẫn để toàn bộ upload public sẽ chưa giải quyết được yêu cầu:

> Scheduled Chapter chưa được mở khóa cho độc giả.

Publishing phải bao gồm cả access control đối với file, không chỉ thay đổi trạng thái Chapter.

---

# 9. Rủi ro bảo mật và audit

## 9.1. Tin vào ID do client gửi

Nhiều endpoint nhận các trường:

- `requesterId`
- `leaderId`
- `reviewerId`
- `tantouId`
- `editorId`

Service load Account theo ID đó rồi kiểm tra role.

Điều này không chứng minh người đang gọi request thực sự là account đó.

Ví dụ về mặt nghiệp vụ:

```text
Người đăng nhập: Account A
ID gửi trong request: Leader B
Kết quả hiện tại: hệ thống xử lý như Leader B thực hiện hành động
```

Ảnh hưởng trực tiếp:

- Một user có thể giả danh Leader để publish.
- Một user có thể giả danh Mangaka để submit.
- Một user có thể giả danh Tantou để approve.
- Audit không còn đáng tin cậy.

Publishing là nghiệp vụ có hậu quả công khai, nên đây là rủi ro nghiêm trọng nhất cần được xử lý trước khi đưa vào production.

## 9.2. CRUD endpoint có thể bypass workflow

Một số endpoint CRUD cho Project, Chapter, SubmissionReview chưa thể hiện đầy đủ authorization ở mức method hoặc service.

Nếu các endpoint này được sử dụng song song với workflow mới, người dùng có thể:

- Sửa trạng thái trực tiếp.
- Xóa Chapter.
- Xóa Review.
- Thay đổi dữ liệu sau khi đã approve.
- Bypass điều kiện của workflow.

Cần xác định rõ endpoint nào là legacy chỉ dùng cho migration, endpoint nào được phép hoạt động chính thức.

## 9.3. Ownership chưa được kiểm tra đầy đủ

Một số service chỉ kiểm tra requester tồn tại hoặc có role, nhưng chưa kiểm tra:

- Có thuộc Project hay không.
- Có phải người được assign hay không.
- Có phải Tantou/Mangaka của Chapter hay không.
- Có quyền xem file cụ thể hay không.

Điều này đặc biệt đáng chú ý với Submission, Asset và SubTask.

---

# 10. Các điểm không nhất quán khác trong codebase

## 10.1. Hai hệ thống trạng thái của Project

- `status` dạng String.
- `projectWorkflowStatus` dạng enum.

Nên có một nguồn dữ liệu chính.

## 10.2. Hai hướng tạo Project

- Workflow creation.
- CRUD manual creation.

Cần tránh việc một concept APPROVED tạo thành nhiều Project.

## 10.3. ProductionPlan tự động và thủ công

Project activate có thể tự tạo Plan rỗng, trong khi API riêng cũng tạo Plan. Quan hệ one-to-one dễ gây xung đột.

## 10.4. Deadline dùng nhiều kiểu dữ liệu

- Chapter dùng `Instant` và ngày.
- Task dùng `LocalDate` + `LocalTime`.
- SubTask dùng `LocalDate` + `LocalTime`.
- Một số logic dùng `ZoneId.systemDefault()`.
- Một số helper dùng UTC.

Nếu không chuẩn hóa timezone, cùng một deadline có thể được hiểu khác nhau giữa server và client.

## 10.5. Feedback cấp Task và Feedback cấp Submission

Model cho phép Feedback gắn với Submission, nhưng logic review Submission chưa lưu Feedback tương ứng. Cần quyết định Feedback là:

- Một comment độc lập.
- Một audit record của review.
- Hay vừa là comment vừa là decision record.

## 10.6. Notification chưa hoàn thiện

Có entity Notification và service tạo notification, nhưng chưa thấy đầy đủ API để user đọc notification hoặc đánh dấu đã đọc.

Publishing nếu có auto-publish nên có notification rõ ràng:

- Chapter đã được schedule.
- Lịch phát hành thay đổi.
- Chapter đã public.
- Chapter bị hold.

---

# 11. Ưu tiên xử lý nghiệp vụ

## Mức ưu tiên 1: Bắt buộc trước khi triển khai publishing thật

1. Tách `SCHEDULED` khỏi `PUBLISHED`.
2. Không chuyển `PUBLISHED` khi release time còn ở tương lai.
3. Chỉ mở nội dung cho reader khi Chapter thực sự public.
4. Đảm bảo file nội bộ không được truy cập public trực tiếp.
5. Đối chiếu actor trong request với identity từ JWT.
6. Xác định timezone và độ chính xác của release time.
7. Có cơ chế auto-publish đến hạn.

## Mức ưu tiên 2: Bắt buộc để audit và vận hành ổn định

1. Lưu người schedule.
2. Lưu người release review.
3. Lưu actual published time.
4. Lưu lịch sử đổi trạng thái.
5. Có quy tắc reschedule, cancel và hold.
6. Lưu feedback reject độc lập.
7. Ngăn các endpoint CRUD bypass workflow.

## Mức ưu tiên 3: Chuẩn hóa toàn hệ thống

1. Hợp nhất trạng thái Project.
2. Hợp nhất hoặc phân tách rõ Name Submission và Production Submission.
3. Xử lý dứt điểm hai đường tạo Project.
4. Xử lý Plan tự động và Plan thủ công.
5. Chuẩn hóa deadline và timezone.
6. Hoàn thiện Notification workflow.
7. Bổ sung reader-facing API rõ ràng.

---

# 12. Kết luận

Tài liệu `publication_analysis.txt` đã xác định đúng lỗ hổng nghiệp vụ trung tâm: Chapter không nên chuyển thẳng từ `COMPLETED` sang `PUBLISHED` nếu thời điểm phát hành nằm trong tương lai.

Tuy nhiên, đối chiếu với code cho thấy backend đã có một phần chức năng mà tài liệu chưa cập nhật:

- Có API xem Chapter `COMPLETED`.
- Có API Leader publish.
- Có giới hạn Leader là người publish.
- Có tự động hoàn tất Production Plan sau khi tất cả Chapter đã `PUBLISHED`.

Lỗ hổng thực tế chính xác hơn là:

> Hệ thống đã có thao tác publish, nhưng chưa có scheduling thực sự và chưa có cơ chế mở nội dung theo thời điểm phát hành.

Luồng nghiệp vụ nên được hiểu như sau:

```text
Production Completed
  → Final Release Review
  → Scheduled
  → Auto Published
```

Hoặc ở dạng trạng thái tối thiểu:

```text
BACKLOG → IN_PRODUCTION → COMPLETED → SCHEDULED → PUBLISHED
```

Trong đó:

- `COMPLETED` chỉ biểu thị production hoàn tất.
- `SCHEDULED` biểu thị Leader đã quyết định lịch nhưng nội dung chưa public.
- `PUBLISHED` chỉ xuất hiện khi hệ thống thật sự mở quyền đọc.
- Scheduled release time và actual published time phải là hai dữ liệu khác nhau.
- Mọi hành động quan trọng phải gắn với identity thật từ JWT.
- File upload phải chịu cùng chính sách quyền truy cập với Chapter.

Chỉ bổ sung `SCHEDULED` mà không xử lý access control, scheduler, audit và identity sẽ tạo ra một trạng thái mới nhưng chưa giải quyết triệt để vấn đề nghiệp vụ.
