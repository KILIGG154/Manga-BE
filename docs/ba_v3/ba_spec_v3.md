================================================================================
          TÀI LIỆU PHÂN TÍCH NGHIỆP VỤ ĐẶC TẢ CHI TIẾT (BA SPECIFICATION V3)
          Phân hệ: Production Plan & Mô hình Xuất bản Nội bộ (Agile Studio)
================================================================================

> **File gốc:** BA Specification V3 do stakeholder cung cấp.
> **Mục đích:** lưu trữ canonical để tham chiếu, đối chiếu với code và tài liệu
> kỹ thuật (gap analysis, implementation status, action items — tất cả đặt cùng
> thư mục `docs/ba_v3/`).

--------------------------------------------------------------------------------

1. CHUẨN HÓA THUẬT NGỮ & MAPPING ENUM CODEBASE
--------------------------------------------------------------------------------

1.1. Chuẩn hóa Enum Trạng thái Chapter (Khắc phục xung đột Enum)
- Codebase sử dụng: ChapterStatus.IN_PRODUCTION (Thay vì IN_PROGRESS).
- Danh sách trạng thái chuẩn cấp Chapter:
  * DRAFT: Chapter mới tạo, chưa phân công Task.
  * IN_PRODUCTION: Đang trong quá trình vẽ, nộp bài, review Task/SubTask.
  * COMPLETED: Đội sản xuất đã hoàn thành 100% Task. Chờ Hội đồng xem xét.
  * SCHEDULED: Đã chốt ngày xuất bản (Chờ CronJob tự động kích hoạt).
  * PUBLISHED: Đã công khai nội dung cho độc giả.

1.2. Định nghĩa Khái niệm & Thuật ngữ Nghiệp vụ
- Active Plan (Plan đang hoạt động): Là Production Plan thuộc một Project có 
  planStatus IN [IN_PROGRESS, PAUSED]. Mỗi Project chỉ có duy nhất 01 Active Plan.
- Release Note (ghi chú xuất bản): Ghi chú không bắt buộc khi Hội đồng bấm Xuất bản.
- Rejection Reason (lý do trả về): Lý do bắt buộc khi Hội đồng từ chối xuất bản.
- Recall Reason (lý do thu hồi): Lý do bắt buộc khi Hội đồng thu hồi Chapter đã đăng.
- Action Mapping (Mã hóa chức năng cho DEV):
  * Xuất bản Chapter: publishChapter (API: POST /api/v1/chapters/{id}/publish)
  * Trả về sản xuất: returnChapterToProduction (API: POST /api/v1/chapters/{id}/return)
  * Thu hồi Chapter: recallChapter (API: POST /api/v1/chapters/{id}/recall)

--------------------------------------------------------------------------------

2. ĐẶC TẢ CHI TIẾT STATE MACHINE & VÒNG ĐỜI PRODUCTION PLAN
--------------------------------------------------------------------------------

2.1. Ma trận Chuyển đổi Trạng thái Plan (State Transition Rules)

  [Khởi tạo] ──► IN_PROGRESS ◄──── (Resume) ────► PAUSED
                     │                             │
                     ├────────► COMPLETED ◄────────┘ (Chỉ qua Force Close)
                     │
                     └────────► CANCELLED (Khi Project bị hủy)

- Luồng Pause / Resume:
  * IN_PROGRESS -> PAUSED: Kích hoạt khi bấm Pause Plan.
  * PAUSED -> IN_PROGRESS: Kích hoạt khi bấm Resume Plan.
- Điều kiện Plan chuyển sang COMPLETED (Hoàn tất):
  * Tự động (Auto-complete): Khi (Số Chapter PUBLISHED == targetChapterCount) 
    VÀ (Không còn Chapter nào ở trạng thái DRAFT, IN_PRODUCTION, COMPLETED, SCHEDULED).
  * Thủ công (Force Close): Leader/Board bấm "Kết thúc sớm Plan" + nhập lý do 
    (Áp dụng cả khi Plan đang IN_PROGRESS hoặc PAUSED).
- Tính chất Terminal (Trạng thái cuối):
  * COMPLETED là trạng thái "Terminal mềm". Nếu Plan đã COMPLETED nhưng Hội đồng 
    thực hiện Thu hồi (Recall) hoặc Trả về (Return) một Chapter thuộc Plan đó, 
    planStatus tự động chuyển ngược lại sang IN_PROGRESS.

2.2. Quy tắc Pause / Resume Chi tiết
- Quyền thực hiện: Tantou (người phụ trách Project), Leader, hoặc Hội đồng (Board).
- Thao tác bị ĐÓNG BĂNG khi Plan ở trạng thái PAUSED:
  * CHẶN: Tạo Chapter mới, giao Task/SubTask mới, nộp Submission (Rough/Final), 
    chỉnh sửa Deadline, thay đổi Assistant/Tantou.
  * CHO PHÉP: Xem dữ liệu (Read-only), tải file cũ, viết Comment trao đổi.
- Lưu trữ Thông tin Pause hiện tại (Trực tiếp trên bảng ProductionPlan):
  * pausedBy (UserID), pausedAt (Timestamp), pauseReason (Text).
- Resume Plan: Không bắt buộc nhập lý do. Reset các trường pauseReason về NULL.

--------------------------------------------------------------------------------

3. ĐẶC TẢ CHI TIẾT LUỒNG PHÁT HÀNH & HỘI ĐỒNG BIÊN TẬP
--------------------------------------------------------------------------------

3.1. Mô hình Hội đồng Biên tập (Editorial Board Model)
- Vai trò: Dùng Role hệ thống EDITORIAL_BOARD. Đây là nhóm đa người dùng (Multi-user).
- Thẩm quyền xuất bản: Bất kỳ User nào có role EDITORIAL_BOARD hoặc LEADER đều có 
  quyền thao tác đơn phương (Single-signoff) trên giao diện Lịch sản xuất.
- Lưu vết đơn giản trên Chapter: `publishedBy` (UserID) và `publishedAt` (Timestamp).

3.2. Chống Xung đột Dữ liệu (Concurrency & Optimistic Locking)
- Mỗi Chapter bắt buộc có trường version (Integer) hoặc updatedAt (Timestamp).
- Khi User A (Hội đồng 1) bấm "Xuất bản" đồng thời User B (Hội đồng 2) bấm "Trả về":
  * Request nào đến server trước sẽ xử lý thành công và tăng version lên 1.
  * Request đến sau sẽ bị từ chối với lỗi HTTP 409 Conflict ("Trạng thái Chapter 
    đã được cập nhật bởi người dùng khác. Vui lòng tải lại trang").

3.3. Giới hạn Trả về Sản xuất (Rejection Limits & Escalation)
- Đếm số lần từ chối: Bảng Chapter bổ sung trường rejectionCount (Integer, Mặc định = 0).
- Giới hạn: Tối đa 02 lần Trả về cho mỗi Chapter.
- Xử lý khi vượt giới hạn (Lần 3):
  * Nếu rejectionCount == 2 và Hội đồng tiếp tục bấm "Trả về sản xuất", hệ thống 
    KHÔNG cho phép trả về tự động.
  * Hệ thống khóa Chapter ở trạng thái COMPLETED_NEEDS_REVIEW và hiển thị thông báo: 
    "Chapter đã bị trả về 2 lần. Bắt buộc tổ chức họp Hội đồng để chốt phương án."
  * Chỉ duy nhất Leader/Trưởng ban Biên tập mới có quyền Override mở khóa để Trả về lần 3.

3.4. Thu hồi Chapter đã Phát hành (Chapter Recall)
- Quyền hạn: Chỉ LEADER hoặc EDITORIAL_BOARD.
- Luồng thực hiện:
  1. Trên Chapter đang PUBLISHED, bấm nút "Thu hồi Chapter".
  2. Hệ thống bắt buộc nhập recallReason (Độ dài tối thiểu 15 ký tự).
  3. Cập nhật Chapter status: PUBLISHED -> IN_PRODUCTION.
  4. Tăng trường recallCount + 1.
  5. Toàn bộ Task trong Chapter được mở khóa lại để sửa đổi.
  6. Nếu Plan đang COMPLETED, planStatus tự động lùi về IN_PROGRESS.

--------------------------------------------------------------------------------

4. XỬ LÝ CÁC TÌNH HUỐNG NGOẠI LỆ (EDGE CASES)
--------------------------------------------------------------------------------

4.1. Dự án bị Hủy (Project Cancellation)
- Khi Project bị chuyển sang trạng thái CANCELLED:
  * Active Plan tương ứng tự động đổi planStatus = CANCELLED.
  * Tất cả Chapter ở trạng thái DRAFT, IN_PRODUCTION, COMPLETED bị khóa hoàn toàn.
  * Các Chapter đã PUBLISHED vẫn giữ nguyên trạng thái dữ liệu lịch sử.

4.2. Trả về Chapter ảnh hưởng đến Task bên trong
- Khi Chapter chuyển từ COMPLETED quay lại IN_PRODUCTION (do bị Trả về hoặc Thu hồi):
  * Hệ thống không reset toàn bộ Task về ban đầu.
  * Trạng thái Chapter = IN_PRODUCTION.
  * Tantou vào Chapter chủ động chọn đúng Task cần sửa và chuyển trạng thái Task 
    đó sang REVISION_REQUIRED (Yêu cầu làm lại) để gán cho Mangaka/Assistant.

--------------------------------------------------------------------------------

5. CHIẾN LƯỢC CHUYỂN ĐỔI DỮ LIỆU CŨ (MIGRATION STRATEGY)
--------------------------------------------------------------------------------

Khi triển khai code mới (bỏ luồng Pre-approval PENDING/APPROVED/REJECTED của Plan):

5.1. Quy tắc Convert Dữ liệu Database (Data Migration Script)
- Đối với ProductionPlan có approvalStatus cũ:
  * PENDING  --> Chuyển planStatus = IN_PROGRESS.
  * APPROVED --> Chuyển planStatus = IN_PROGRESS.
  * REJECTED --> Chuyển planStatus = PAUSED, gán pauseReason = "Hồ sơ bị Reject từ hệ thống cũ".

5.2. Deprecation Plan
- Đánh dấu cột approvalStatus trong Database là Deprecated (@Deprecated trong Entity).
- Giữ cột approvalStatus trong DB khoảng 2 sprint để fallback nếu cần, sau đó DROP column.
- Toàn bộ Code logic mới chỉ query và ghi nhận theo trường planStatus.
================================================================================