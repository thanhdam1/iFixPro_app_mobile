# Kế hoạch & Lộ trình Nâng cấp Giao diện trong Tương lai (UI Roadmap) - iFixPro

Tài liệu này theo dõi các kế hoạch thiết kế, nâng cấp giao diện (UI) và cải thiện trải nghiệm người dùng (UX) dự kiến thực hiện trong các phiên bản tiếp theo của ứng dụng **iFixPro**.

---

## 🎨 1. Tổng quan Định hướng Thiết kế (Design System & Theme)
- [x] **Dark Mode / Light Mode**: Hỗ trợ chuyển đổi giao diện Sáng/Tối linh hoạt (`Theme.Material3.DayNight.NoActionBar`).
- [x] **Material 3 Design Guidelines**: Chuẩn hóa màu sắc (Primary `#2563EB`, Navy `#0B1220`, Surface `#FFFFFF`), độ cong góc (Card radius 16dp, Control radius 12dp), độ đổ bóng (elevation) và typography (Plus Jakarta Sans / Be Vietnam Pro).
- [x] **Skeleton Loading & Shimmer Effects**: Tích hợp `LinearProgressIndicator` và hiệu ứng Empty State cho các danh sách.
- [x] **Micro-animations**: Thêm hiệu ứng chuyển động mượt mà cho các nút bấm, danh sách và chuyển tab.

---

## 🔑 2. Màn hình Xác thực & Tài khoản (Auth & Profile)
- [x] **Màn hình Đăng nhập (LoginActivity)**:
  - Tối ưu hiệu ứng xuất hiện Logo Vector `ic_ifixpro_mark` & thẻ đăng nhập.
  - Thêm tính năng ẩn/hiện mật khẩu dạng icon hiện đại (`password_toggle`).
  - Hỗ trợ chọn nhanh tài khoản thử nghiệm theo từng chức vụ.
- [x] **Sheet Thông tin tài khoản (ViewAccountSheet)**:
  - Cải tiến ảnh đại diện (Avatar Initials) với Badge vai trò màu sắc theo từng chức vụ.
  - Thêm thông tin liên hệ và nút Đăng xuất an toàn.

---

## 📱 3. Giao diện Khách hàng (Customer Experience)
- [x] **Trang chủ Khách hàng (CustomerHomeFragment)**:
  - Thiết kế lại 3 Card KPI tra cứu trạng thái phiếu sửa chữa (Đang sửa, Chờ báo giá, Sẵn sàng nhận).
  - Thanh tìm kiếm và bộ lọc Chip thông minh theo trạng thái phiếu.
- [x] **Màn hình Dòng thời gian sửa chữa (TicketTimelineActivity)**:
  - Hiển thị tiến trình 8 nấc thời gian từ Chờ tiếp nhận đến Đã bàn giao.
  - Tích hợp nút chấp nhận / từ chối báo giá và thông tin thiết bị chi tiết.

---

## 📋 4. Giao diện Lễ tân (Receptionist Dashboard)
- [x] **Trang danh sách phiếu (ReceptionistHomeFragment)**:
  - Bộ lọc thông minh theo trạng thái (Mới tiếp nhận, Đang sửa, Chờ giao, Hoàn tất).
  - Tìm kiếm nhanh bằng mã phiếu hoặc số điện thoại khách hàng.
- [x] **Màn hình Tạo phiếu mới (CreateTicketActivity)**:
  - Form nhập thông tin khách hàng, thiết bị và yêu cầu sửa chữa.
  - Chọn nhanh dòng máy (Điện thoại, Laptop, Máy tính bảng, Khác).
- [x] **Màn hình In hóa đơn (PrintInvoiceActivity)**:
  - Xem trước bản in (Print Preview) chuẩn định dạng hóa đơn bán hàng / biên nhận sửa chữa.

---

## 🛠️ 5. Giao diện Kỹ thuật viên (Technician Workspace)
- [x] **Màn hình Công việc (TechnicianHomeFragment)**:
  - Phân loại danh sách thiết bị cần chẩn đoán, cần thay linh kiện và chờ kiểm thử.
  - Thẻ thông tin hiển thị ưu tiên theo mức độ khẩn cấp và trạng thái phiếu.
- [x] **Màn hình Chẩn đoán & Báo giá (DiagnoseActivity)**:
  - Giao diện chọn nhanh linh kiện thay thế kèm tổng chi phí tự động tính toán.
  - Ghi chú chẩn đoán kỹ thuật và nút cập nhật báo giá cho khách hàng.
- [x] **Màn hình Quét mã QR (QRScannerActivity)**:
  - Tối ưu giao diện khung quét camera mượt mà bằng thư viện ZXing.

---

## 📊 6. Giao diện Quản lý (Manager Dashboard)
- [x] **Trang Báo cáo & Thống kê (ManagerHomeFragment)**:
  - Tích hợp biểu đồ doanh thu và thống kê phiếu sửa chữa theo ngày/tuần/tháng (`MPAndroidChart`).
  - Card chỉ số KPI doanh thu, số phiếu hoàn tất và nhân sự hoạt động.
- [x] **Màn hình Quản lý Kho linh kiện (InventoryManagementActivity)**:
  - Cảnh báo linh kiện sắp hết hàng (Low stock alert badge).
  - Giao diện thêm / sửa linh kiện và tìm kiếm kho hàng.
- [x] **Màn hình Quản lý Nhân sự (UserManagementActivity)**:
  - Danh sách tài khoản nhân viên dạng Card với Badge vai trò và thông tin liên hệ.

---

## 📌 7. Ghi chú & Đóng góp ý kiến mới
> *Điền thêm các ý tưởng nâng cấp mới vào phần này.*

- 
