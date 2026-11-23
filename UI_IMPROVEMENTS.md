# YÊU CẦU CẢI TIẾN GIAO DIỆN (UI/UX) – iFixPro App

**Tên dự án:** iFixPro – Hệ thống quản lý sửa chữa điện thoại và laptop  
**Phiên bản tài liệu:** 1.0  
**Phạm vi:** Customer Portal, Receptionist, Technician, Manager  
**Định hướng thiết kế:** Modern, Premium, Minimalist, Mobile First

---

## 1. Màn hình cần cải tiến (Screen / Flow)

### 1.1. Customer Portal – Giao diện khách hàng

- **Màn hình:** Customer Welcome, Repair Request, Upload Images, Request Confirmation, Repair Tracking, Repair Detail, Quotation, Feedback.
- **Vai trò người dùng (Role):** Customer.
- **Yêu cầu đặc biệt:** Khách hàng không cần đăng ký hoặc đăng nhập tài khoản.

**Luồng sử dụng:**

1. Khách hàng đến cửa hàng và quét mã QR được đặt tại quầy tiếp nhận.
2. Hệ thống tự động mở trang iFixPro Customer Portal trên trình duyệt điện thoại.
3. Hiển thị màn hình chào mừng với giao diện đẹp, chuyên nghiệp.
4. Khách hàng chọn loại thiết bị cần sửa chữa: Điện thoại, Laptop hoặc thiết bị khác.
5. Điền thông tin cá nhân và thông tin thiết bị.
6. Mô tả tình trạng lỗi và tải lên hình ảnh thực tế.
7. Kiểm tra lại thông tin và gửi yêu cầu.
8. Hệ thống tạo phiếu sửa chữa và hiển thị mã phiếu.
9. Khách hàng sử dụng liên kết theo dõi riêng để xem tiến độ sửa chữa.
10. Khách hàng có thể xem báo giá, xác nhận sửa chữa và đánh giá dịch vụ.

**Lưu ý:** Mã QR công khai tại cửa hàng chỉ mở biểu mẫu tiếp nhận. Quyền truy cập phiếu sửa chữa phải được bảo vệ bằng liên kết có token bảo mật hoặc phương thức xác thực phù hợp, chẳng hạn OTP.

### 1.2. Receptionist – Nhân viên tiếp nhận

- **Màn hình:** Receptionist Dashboard, Repair Request List, Create Repair Ticket, Ticket Detail, Customer Information.
- **Vai trò người dùng:** Receptionist.

**Chức năng:**
- Xem yêu cầu sửa chữa mới từ khách hàng.
- Kiểm tra thông tin và hình ảnh thiết bị.
- Tiếp nhận hoặc yêu cầu khách hàng bổ sung thông tin.
- Tạo hoặc cập nhật phiếu sửa chữa.
- Ghi nhận tình trạng thiết bị thực tế khi nhận máy.
- Phân công kỹ thuật viên.
- Theo dõi trạng thái xử lý.
- Bàn giao thiết bị và xác nhận hoàn tất.

### 1.3. Technician – Kỹ thuật viên

- **Màn hình:** Technician Dashboard, Assigned Repairs, Repair Detail, Diagnosis, Repair Progress.
- **Vai trò người dùng:** Technician.

**Chức năng:**
- Xem danh sách công việc được phân công.
- Xem thông tin thiết bị, hình ảnh và mô tả lỗi.
- Ghi nhận kết quả kiểm tra và chẩn đoán.
- Đề xuất linh kiện và chi phí sửa chữa.
- Cập nhật tiến độ xử lý.
- Tải lên hình ảnh trước và sau sửa chữa.
- Ghi chú kỹ thuật và kết quả kiểm tra.
- Chuyển trạng thái hoàn thành để chờ bàn giao.

### 1.4. Manager – Quản lý cửa hàng

- **Màn hình:** Manager Dashboard, Repair Management, Staff Management, Inventory, Revenue, Reports.
- **Vai trò người dùng:** Manager.

**Chức năng:**
- Theo dõi tổng số phiếu sửa chữa.
- Xem phiếu đang chờ, đang sửa và đã hoàn thành.
- Quản lý nhân viên và kỹ thuật viên.
- Theo dõi hiệu suất sửa chữa.
- Quản lý linh kiện và tồn kho.
- Theo dõi doanh thu, chi phí và lợi nhuận.
- Xem báo cáo hoạt động theo ngày, tuần, tháng.
- Quản lý cấu hình cửa hàng và mã QR tiếp nhận khách hàng.

---

## 2. Chi tiết yêu cầu thay đổi (UI/UX Changes)

### 2.1. Màu sắc & Chủ đề (Colors & Theme)

- [ ] Thiết kế lại toàn bộ giao diện theo phong cách **Premium Tech Service**, hiện đại và chuyên nghiệp.
- [ ] Sử dụng Material Design 3 làm nền tảng thiết kế cho Android App.
- [ ] Áp dụng hệ thống màu thống nhất trên toàn bộ ứng dụng.

**Bảng màu đề xuất:**

| Thành phần | Mã màu | Mục đích |
|---|---|---|
| Primary | #2563EB | Nút chính, hành động quan trọng |
| Navy | #0F172A | Header, tiêu đề, khu vực nổi bật |
| Secondary | #06B6D4 | Điểm nhấn công nghệ |
| Background | #F8FAFC | Nền tổng thể |
| Surface | #FFFFFF | Card, form, dialog |
| Text Primary | #0F172A | Nội dung chính |
| Text Secondary | #64748B | Nội dung phụ |
| Success | #10B981 | Hoàn thành, thành công |
| Warning | #F59E0B | Đang xử lý, chờ xác nhận |
| Error | #EF4444 | Lỗi, thất bại |

- [ ] Hỗ trợ Light Mode.
- [ ] Thiết kế Dark Mode cho ứng dụng nhân viên.
- [ ] Sử dụng gradient xanh navy và xanh dương cho khu vực giới thiệu dịch vụ.
- [ ] Đảm bảo độ tương phản văn bản và trạng thái đáp ứng yêu cầu accessibility.

### 2.2. Bố cục & Khoảng cách (Layout & Spacing)

- [ ] Thiết kế Mobile First, ưu tiên màn hình điện thoại.
- [ ] Sử dụng hệ thống khoảng cách 8px.
- [ ] Padding màn hình khoảng 16–20dp.
- [ ] Khoảng cách giữa các Card khoảng 12–16dp.
- [ ] Card có bo góc từ 16–24dp.
- [ ] Các thành phần form sử dụng bố cục một cột.
- [ ] Không hiển thị quá nhiều thông tin trên cùng một màn hình.
- [ ] Hạn chế cuộn ngang.
- [ ] Nút thao tác chính có chiều cao tối thiểu 48dp.
- [ ] Đảm bảo giao diện hiển thị tốt khi bàn phím được mở.
- [ ] Hỗ trợ màn hình nhỏ và chế độ chữ lớn.

**Bố cục Customer Portal:**

1. Header có logo iFixPro và nút hỗ trợ.
2. Hero Banner giới thiệu dịch vụ sửa chữa.
3. Khu vực lựa chọn loại thiết bị.
4. Biểu mẫu tiếp nhận theo từng bước.
5. Khu vực tải hình ảnh thiết bị.
6. Khu vực xác nhận thông tin.
7. Footer hiển thị thông tin hỗ trợ và chính sách bảo mật.

### 2.3. Thành phần UI (Buttons, Cards, TextFields, Lists)

#### A. Customer Welcome Screen

- [ ] Hiển thị logo iFixPro nổi bật.
- [ ] Thiết kế Hero Banner có hình điện thoại và laptop.
- [ ] Hiển thị tiêu đề: **"Thiết bị gặp vấn đề? iFixPro luôn sẵn sàng hỗ trợ!"**
- [ ] Hiển thị mô tả ngắn: "Gửi yêu cầu sửa chữa nhanh chóng, theo dõi tiến độ dễ dàng, không cần đăng nhập."
- [ ] Nút CTA nổi bật: **"Bắt đầu yêu cầu sửa chữa"**.
- [ ] Hiển thị hai nhóm dịch vụ: Sửa điện thoại và Sửa laptop.
- [ ] Bổ sung mục "Quy trình sửa chữa" với 3 bước: Gửi yêu cầu → Kiểm tra & Báo giá → Sửa chữa & Bàn giao.
- [ ] Hiển thị nút liên hệ cửa hàng.

#### B. Repair Request Form

Chia biểu mẫu thành 3 bước với thanh tiến độ ở phía trên.

**Bước 1 – Thông tin khách hàng**
- [ ] Họ và tên (bắt buộc).
- [ ] Số điện thoại (bắt buộc, có validation).
- [ ] Email (không bắt buộc).
- [ ] Loại thiết bị: Điện thoại / Laptop / Khác.

**Bước 2 – Thông tin thiết bị**
- [ ] Thương hiệu thiết bị: Apple, Samsung, Dell, HP, Asus, Lenovo hoặc Khác.
- [ ] Model thiết bị.
- [ ] Nhóm lỗi thường gặp được trình bày dưới dạng lựa chọn.
- [ ] Ô nhập mô tả tình trạng lỗi chi tiết.
- [ ] Khu vực tải lên hoặc chụp hình ảnh thiết bị.
- [ ] Cho phép xem trước và xóa ảnh trước khi gửi.
- [ ] Giới hạn tối đa 5 ảnh theo cấu hình.
- [ ] Thông báo rõ định dạng và dung lượng ảnh được hỗ trợ.
- [ ] Không yêu cầu khách hàng nhập mật khẩu mở khóa thiết bị.

**Bước 3 – Kiểm tra & Xác nhận**
- [ ] Hiển thị bản tóm tắt toàn bộ thông tin đã nhập.
- [ ] Hiển thị hình ảnh đã đính kèm.
- [ ] Có nút quay lại chỉnh sửa.
- [ ] Có checkbox đồng ý chính sách xử lý thông tin.
- [ ] Nút "Gửi yêu cầu sửa chữa".
- [ ] Hiển thị trạng thái loading trong quá trình gửi.
- [ ] Chống tạo phiếu trùng khi khách nhấn nút nhiều lần.

#### C. Request Success Screen

- [ ] Hiển thị hiệu ứng xác nhận thành công.
- [ ] Hiển thị mã phiếu sửa chữa.
- [ ] Hiển thị loại thiết bị và thời gian tiếp nhận.
- [ ] Nút "Xem tiến độ sửa chữa".
- [ ] Nút "Lưu thông tin phiếu".
- [ ] Hiển thị hướng dẫn liên hệ cửa hàng.
- [ ] Hiển thị thông báo khách hàng đã gửi yêu cầu, đang chờ nhân viên xác nhận tiếp nhận.

#### D. Repair Tracking Screen

- [ ] Thiết kế timeline theo chiều dọc.
- [ ] Hiển thị mã phiếu và tên thiết bị.
- [ ] Hiển thị trạng thái hiện tại bằng status chip.
- [ ] Hiển thị ngày giờ cập nhật gần nhất.
- [ ] Hiển thị lịch sử thay đổi trạng thái.
- [ ] Hiển thị thông tin báo giá nếu đã có.
- [ ] Cho phép xác nhận hoặc từ chối báo giá.
- [ ] Hiển thị hình ảnh quá trình sửa chữa khi được nhân viên chia sẻ.
- [ ] Hiển thị thông tin bảo hành sau khi sửa chữa hoàn tất.

**Trạng thái đề xuất:**

1. Chờ tiếp nhận.
2. Đã tiếp nhận.
3. Đang kiểm tra.
4. Chờ duyệt báo giá.
5. Đang sửa chữa.
6. Kiểm tra chất lượng.
7. Sẵn sàng bàn giao.
8. Đã bàn giao.

Các trạng thái hủy yêu cầu, từ chối báo giá hoặc không thể sửa chữa cần có luồng riêng, không được tự động coi là hoàn thành.

#### E. Receptionist / Technician / Manager Dashboard

- [ ] Thiết kế Header chào mừng theo vai trò.
- [ ] Hiển thị ảnh đại diện hoặc chữ viết tắt của nhân viên.
- [ ] Sử dụng các KPI Card trực quan.
- [ ] Bottom Navigation dành cho chức năng chính.
- [ ] Có thanh tìm kiếm phiếu sửa chữa.
- [ ] Bộ lọc theo trạng thái, ngày và kỹ thuật viên.
- [ ] Phiếu sửa chữa hiển thị dưới dạng Card có mã phiếu, thiết bị, khách hàng và trạng thái.
- [ ] Sử dụng màu trạng thái nhất quán.
- [ ] Có Empty State khi không có dữ liệu.
- [ ] Hỗ trợ Pull to Refresh.
- [ ] Có Skeleton Loading khi tải dữ liệu.
- [ ] Hiển thị snackbar hoặc toast sau thao tác thành công hoặc thất bại.

### 2.4. Icon & Hình ảnh

- [ ] Sử dụng bộ Material Symbols hoặc Lucide Icons đồng bộ.
- [ ] Icon điện thoại cho nhóm Smartphone.
- [ ] Icon laptop cho nhóm Laptop.
- [ ] Icon camera cho chức năng chụp ảnh.
- [ ] Icon upload cho chức năng tải ảnh.
- [ ] Icon wrench cho sửa chữa.
- [ ] Icon clipboard cho phiếu sửa chữa.
- [ ] Icon clock cho lịch sử và tiến độ.
- [ ] Icon shield-check cho thông tin bảo hành.
- [ ] Icon QR Code cho tính năng tiếp nhận.
- [ ] Sử dụng ảnh minh họa thiết bị có chất lượng tốt.
- [ ] Thiết kế Empty State Illustration riêng cho các trạng thái không có dữ liệu.
- [ ] Đảm bảo kích thước và phong cách icon thống nhất.

### 2.5. Hiệu ứng & Chuyển cảnh (Animations & Transitions)

- [ ] Sử dụng hiệu ứng Fade In nhẹ khi mở màn hình.
- [ ] Chuyển bước form bằng Slide Transition.
- [ ] Hiệu ứng nhấn nút nhẹ, rõ trạng thái tương tác.
- [ ] Hiệu ứng chuyển trạng thái khi hoàn tất từng bước.
- [ ] Hiển thị Progress Indicator khi tải dữ liệu.
- [ ] Hiệu ứng thành công khi tạo phiếu.
- [ ] Skeleton Loading cho danh sách phiếu.
- [ ] Animation mượt khi mở chi tiết phiếu.
- [ ] Tránh lạm dụng animation gây chậm ứng dụng.
- [ ] Hỗ trợ tùy chọn giảm chuyển động (Reduce Motion).

---

## 3. Gợi ý thiết kế hoặc Hình ảnh tham khảo (Mockups / References)

### 3.1. Phong cách tổng thể

**Design Direction:** Premium Minimal Tech App.

Tham khảo các nguyên tắc:
- Material Design 3 dành cho Android.
- Giao diện tối giản, bố cục thoáng.
- Thẻ thông tin bo góc mềm.
- Font Be Vietnam Pro hoặc Inter.
- Sử dụng hình minh họa công nghệ.
- Tập trung vào hành động chính trên từng màn hình.

### 3.2. Customer Portal – Welcome Screen

**Ý tưởng giao diện:**

Phía trên là logo iFixPro kèm lời chào:

"Chào mừng đến với iFixPro"

Hero Banner màu xanh navy kết hợp hình minh họa điện thoại và laptop, nội dung:

"Chúng tôi chăm sóc thiết bị của bạn."

Phía dưới là nút xanh dương nổi bật:

**Bắt đầu sửa chữa →**

Tiếp theo là hai Service Card:
- Sửa chữa điện thoại.
- Sửa chữa laptop.

Phía cuối có mục hướng dẫn 3 bước gửi yêu cầu và nút liên hệ hỗ trợ.

### 3.3. Repair Request – Form Screen

- Thiết kế Stepper gồm 3 bước.
- TextField bo góc, có icon phù hợp.
- Các nhóm thiết bị sử dụng Card có trạng thái được chọn.
- Lỗi thường gặp sử dụng Checkbox hoặc Choice Chip.
- Khu vực upload ảnh sử dụng khung viền nét đứt.
- Nút Next cố định ở phần dưới màn hình khi phù hợp.
- Validation hiển thị ngay dưới trường nhập liệu.

### 3.4. Repair Tracking – Timeline Screen

Thiết kế timeline dọc với các điểm trạng thái.

- Trạng thái đã hoàn thành: Màu xanh lá.
- Trạng thái đang thực hiện: Màu xanh dương.
- Trạng thái sắp tới: Màu xám.
- Trạng thái cần hành động: Màu cam.

Mỗi trạng thái hiển thị tên bước, thời gian và ghi chú tương ứng.

Nếu có báo giá mới, hiển thị Card nổi bật gồm tên linh kiện, chi phí, tổng tiền và các nút xác nhận hoặc từ chối.

### 3.5. Staff Dashboard

**Ý tưởng:**
- Header chào nhân viên.
- Dashboard KPI sử dụng các Card nhỏ.
- Thanh tìm kiếm ở đầu danh sách.
- Status Chip để phân biệt tình trạng sửa chữa.
- Bottom Navigation gồm: Tổng quan, Phiếu sửa chữa, Thông báo, Tài khoản.
- Danh sách phiếu ưu tiên hiển thị thông tin ngắn gọn, dễ quét.
- Mỗi vai trò chỉ nhìn thấy thao tác được cấp quyền.

---

## 4. Ghi chú thêm (Notes)

### 4.1. Yêu cầu trải nghiệm khách hàng

- Khách hàng không bắt buộc cài ứng dụng.
- Không cần đăng ký hoặc đăng nhập để tạo yêu cầu.
- Trang khách hàng hoạt động tốt trên Android và iOS thông qua trình duyệt.
- Thời gian nhập biểu mẫu cần được tối ưu bằng lựa chọn nhanh và gợi ý phù hợp.
- Hỗ trợ camera của điện thoại để chụp ảnh thiết bị.
- Có cơ chế thông báo khi biểu mẫu lỗi hoặc kết nối mạng không ổn định.
- Hiển thị chính sách quyền riêng tư trước khi gửi thông tin.
- Bảo vệ quyền truy cập phiếu sửa chữa của từng khách hàng.

### 4.2. Yêu cầu kỹ thuật đề xuất

**Customer Portal:**
- React + Vite.
- Tailwind CSS.
- Responsive Web Design.
- Có thể nâng cấp thành PWA.

**Android Staff App:**
- Kotlin.
- Jetpack Compose.
- Material Design 3.
- MVVM Architecture.
- Navigation Compose.

**Backend:**
- Spring Boot.
- REST API.
- Spring Security cho vai trò nhân viên.
- MySQL.
- Object Storage để lưu hình ảnh.

### 4.3. Các tiêu chí nghiệm thu (Acceptance Criteria)

**AC-01:** Khi khách quét QR hợp lệ tại cửa hàng, hệ thống mở đúng trang tiếp nhận yêu cầu mà không yêu cầu đăng nhập.

**AC-02:** Khách không thể gửi biểu mẫu nếu thiếu các trường bắt buộc.

**AC-03:** Số điện thoại phải được kiểm tra đúng định dạng được hệ thống hỗ trợ.

**AC-04:** Khách có thể chụp ảnh, chọn ảnh từ thư viện, xem trước và xóa ảnh trước khi gửi.

**AC-05:** Hệ thống phải thông báo khi ảnh vượt giới hạn số lượng, dung lượng hoặc không đúng định dạng.

**AC-06:** Khi gửi yêu cầu thành công, hệ thống tạo đúng một phiếu sửa chữa và cung cấp mã phiếu.

**AC-07:** Khi gửi thất bại, hệ thống thông báo rõ ràng và không tự ý xóa dữ liệu khách đã nhập.

**AC-08:** Khi khách xem tiến độ, hệ thống chỉ cung cấp thông tin của phiếu mà khách được phép truy cập.

**AC-09:** Nhân viên tiếp nhận có thể xem yêu cầu mới và cập nhật trạng thái theo quyền được cấp.

**AC-10:** Kỹ thuật viên có thể cập nhật chẩn đoán và tiến độ của các phiếu được phân công.

**AC-11:** Khách có thể xem và xác nhận hoặc từ chối báo giá hợp lệ, chưa hết hiệu lực.

**AC-12:** Giao diện phải đáp ứng trên màn hình điện thoại phổ biến, không xuất hiện lỗi tràn nội dung hoặc chồng lấn thành phần.

**AC-13:** Các thao tác quan trọng phải có trạng thái loading, thông báo kết quả và xử lý lỗi phù hợp.

**AC-14:** Các thao tác thay đổi trạng thái, báo giá và bàn giao phải được lưu lịch sử để truy vết.

**AC-15:** Khi người dùng nhấn gửi nhiều lần hoặc mạng gửi lại request, hệ thống không tạo phiếu trùng ngoài ý muốn.

---

## 5. Mức độ ưu tiên triển khai

### P0 – Bắt buộc cho MVP
- Customer Welcome.
- QR Entry.
- Repair Request Form.
- Image Upload.
- Request Confirmation.
- Repair Tracking.
- Receptionist Dashboard.
- Repair Ticket Detail.
- Technician Repair Progress.
- Quotation Approval.
- Validation, Responsive và trạng thái lỗi.

### P1 – Nâng cao trải nghiệm
- Dashboard KPI.
- Notifications.
- Hình ảnh trước và sau sửa chữa.
- Quản lý khách hàng.
- Lịch sử sửa chữa.
- Bảo hành điện tử.
- Dark Mode.
- Quản lý linh kiện.

### P2 – Phát triển mở rộng
- Analytics nâng cao.
- Báo cáo doanh thu.
- Đánh giá chất lượng dịch vụ.
- PWA Offline Support có kiểm soát.
- Push Notifications.
- Các tính năng hỗ trợ thông minh.

---

## 6. Kết quả mong đợi (Expected Outcome)

Sau khi triển khai cải tiến, hệ thống iFixPro cần đạt được:

1. Giao diện đồng bộ, hiện đại và chuyên nghiệp.
2. Khách hàng có thể quét QR và gửi yêu cầu mà không cần tài khoản.
3. Biểu mẫu dễ hiểu, dễ thao tác và hỗ trợ hình ảnh.
4. Người dùng dễ dàng theo dõi toàn bộ tiến độ sửa chữa.
5. Nhân viên có thể xử lý phiếu nhanh chóng.
6. Kỹ thuật viên nắm rõ công việc được phân công.
7. Quản lý theo dõi được hoạt động cửa hàng.
8. UI/UX nhất quán giữa Customer Portal và ứng dụng quản lý.
9. Hệ thống có cơ sở để mở rộng mà không cần thiết kế lại toàn bộ giao diện.
10. Các chức năng quan trọng có tiêu chí nghiệm thu rõ ràng, phục vụ kiểm thử Manual và Automation.

**Mục tiêu cuối cùng:** Đưa iFixPro trở thành hệ thống tiếp nhận và quản lý sửa chữa điện thoại, laptop có giao diện thân thiện với khách hàng, dễ vận hành cho nhân viên và chuyên nghiệp trong quản lý cửa hàng.