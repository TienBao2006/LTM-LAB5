# LAB 5: HỆ THỐNG JAVA RMI ĐỒNG BỘ DỮ LIỆU 2 CHIỀU (TWO-WAY REPLICATION)
## Học phần: Lập Trình Mạng (Computer Network Programming)

---

## 1. MỤC TIÊU VÀ BỐI CẢNH DỰ ÁN
Dự án xây dựng hệ sinh thái **Java RMI (Remote Method Invocation)** hỗ trợ đầy đủ các thao tác CRUD (Thêm, Xóa, Sửa, Xem) với cơ chế **Đồng bộ hóa 2 chiều tự động (Active - Active Two-Way Replication)** giữa 2 Server độc lập.
- Client có thể kết nối đến **Server 1** hoặc **Server 2** để thực hiện ghi dữ liệu.
- Khi một Server nhận yêu cầu ghi, nó lưu cục bộ và lập tức đóng vai trò RMI Client chuyển phát sang Server đối tác qua mạng.
- **Cơ chế chống lặp vô hạn (Loopback Prevention)**: Phân tách rõ ràng giữa phương thức Client gọi (`addStudent`, `updateStudent`, `deleteStudent`) và phương thức đồng bộ nội bộ giữa 2 Server (`syncAdd`, `syncUpdate`, `syncDelete`). Khi Server đối tác nhận lệnh `sync...`, nó chỉ cập nhật dữ liệu cục bộ và **không được gọi ngược lại**.
- **Cơ chế chịu lỗi (Fault-Tolerance)**: Nếu Server đối tác tắt máy hoặc ngắt kết nối mạng LAN, Server hiện tại vẫn hoàn tất thao tác ghi cho Client một cách an toàn mà không làm sập ứng dụng.

---

## 2. CẤU TRÚC THƯ MỤC DỰ ÁN

```
lab5/
├── src/                          # Mã nguồn Java
│   ├── common/                   # Entity và Remote Interface dùng chung
│   │   ├── Student.java          # Đối tượng Entity (implements Serializable)
│   │   └── StudentSyncService.java # Remote Interface RMI
│   ├── server/                   # Mã nguồn phía Server
│   │   ├── StudentSyncServiceImpl.java # Triển khai CRUD & logic Replication
│   │   └── ServerMain.java       # Khởi chạy RMI Registry & bind Service
│   ├── client/                   # Mã nguồn phía Client
│   │   └── ClientForm.java       # Giao diện Java Swing & xử lý sự kiện RMI
│   └── test/                     # Kiểm thử tự động
│       └── TestReplication.java  # Kịch bản kiểm thử 7 bước đồng bộ 2 chiều
│
├── bin/                          # Thư mục chứa các tệp .class đã biên dịch
├── compile.bat                   # File biên dịch toàn bộ dự án
├── run_server1.bat               # Khởi động nhanh Server 1 (Port 1099)
├── run_server2.bat               # Khởi động nhanh Server 2 (Port 1100)
├── run_client.bat                # Khởi động giao diện Client Swing
├── test_simulation.bat           # Chạy kịch bản kiểm thử tự động
├── HUONG_DAN_VAN_DAP.md          # Cẩm nang 10 câu hỏi & câu trả lời vấn đáp chi tiết
└── README.md                     # Tài liệu hướng dẫn cài đặt & vận hành
```

---

## 3. HƯỚNG DẪN CHẠY THỰC TẾ

### 🔹 KỊCH BẢN 1: CHẠY THỬ TRÊN CÙNG MỘT MÁY TÍNH (LOCALHOST)

1. **Biên dịch mã nguồn**:
   Nhấp đúp vào `compile.bat` (hoặc mở Terminal chạy: `javac -encoding UTF-8 -d bin src/common/*.java src/server/*.java src/client/*.java src/test/*.java`).

2. **Khởi động Server 1**:
   Nhấp đúp `run_server1.bat`
   *(Lắng nghe cổng `1099`, cấu hình đối tác là `1100`)*.

3. **Khởi động Server 2**:
   Nhấp đúp `run_server2.bat`
   *(Lắng nghe cổng `1100`, cấu hình đối tác là `1099`)*.

4. **Khởi động Client**:
   Nhấp đúp `run_client.bat`.
   - Trên Client bấm nút **"Nối Server 1 (1099)"**: Thêm sinh viên mới `SV004` -> Bấm Thêm mới.
   - Bấm nút **"Nối Server 2 (1100)"**: Quan sát bảng dữ liệu -> Sinh viên `SV004` đã xuất hiện ngay trên Server 2 (Chứng minh đồng bộ thành công)!
   - Sửa thông tin `SV004` trên Server 2 -> Nối lại Server 1 -> Thông tin đã được cập nhật sang Server 1!

5. **Chạy kiểm thử tự động toàn diện**:
   Nhấp đúp `test_simulation.bat`.
   Kịch bản sẽ tự động chạy 7 bài kiểm thử (Add, Sync, Update, Reverse Sync, Loopback check, Fault Tolerance, Delete).

---

### 🔹 KỊCH BẢN 2: CHẠY TRÊN 2 MÁY THỰC VÀ MÁY ẢO QUA MẠNG LAN (BRIDGED ADAPTER)

#### Giả lập địa chỉ IP mạng LAN mẫu:
- **Máy vật lý 1 (Server 1)**: IP `192.168.1.100`
- **Máy vật lý 2 (Server 2)**: IP `192.168.1.101`
- **Máy ảo (VM Client)**: IP `192.168.1.102` (Network Adapter chọn **Bridged Adapter** để có cùng dải IP mạng LAN).

#### Các bước thực hiện:

1. **Trên Máy vật lý 1 (Server 1)**:
   Mở Command Prompt tại thư mục `lab5/`:
   ```cmd
   java "-Dfile.encoding=UTF-8" -cp "bin" server.ServerMain 1099 1100 192.168.1.100 192.168.1.101 "Server 1"
   ```
   *Giải thích tham số:*
   - Cổng lắng nghe: `1099`
   - Cổng đối tác: `1100`
   - IP của Server 1: `192.168.1.100` (sẽ được gán vào `java.rmi.server.hostname`)
   - IP của Server 2: `192.168.1.101`
   - Tên định danh: `"Server 1"`

2. **Trên Máy vật lý 2 (Server 2)**:
   Mở Command Prompt tại thư mục `lab5/`:
   ```cmd
   java "-Dfile.encoding=UTF-8" -cp "bin" server.ServerMain 1100 1099 192.168.1.101 192.168.1.100 "Server 2"
   ```
   *Giải thích tham số:*
   - Cổng lắng nghe: `1100`
   - Cổng đối tác: `1099`
   - IP của Server 2: `192.168.1.101`
   - IP của Server 1: `192.168.1.100`
   - Tên định danh: `"Server 2"`

3. **Trên Máy ảo (VM Client)**:
   Mở ClientForm:
   ```cmd
   java "-Dfile.encoding=UTF-8" -cp "bin" client.ClientForm
   ```
   - Nhập ô Server IP: `192.168.1.100`, Port: `1099` -> Bấm **Kết nối** (Kết nối tới Server 1).
   - Thao tác Thêm/Sửa/Xóa.
   - Đổi ô Server IP sang: `192.168.1.101`, Port: `1100` -> Bấm **Kết nối** (Chuyển sang Server 2).
   - Quan sát danh sách dữ liệu khớp 100% giữa 2 Server.

---

## 4. LƯU Ý QUAN TRỌNG KHI CHẠY MẠNG LAN / VẤN ĐÁP

1. **Cấu hình Tường lửa (Windows Firewall)**:
   - Trên Máy 1 và Máy 2, nếu Client không kết nối được (gặp lỗi `Connection refused` hoặc `Connection timed out`), hãy mở cổng hoặc tắt tạm Windows Firewall:
     ```powershell
     netsh advfirewall firewall add rule name="Java RMI 1099" dir=in action=allow protocol=TCP localport=1099
     netsh advfirewall firewall add rule name="Java RMI 1100" dir=in action=allow protocol=TCP localport=1100
     ```

2. **Tại sao phải thiết lập `java.rmi.server.hostname`?**:
   - Khi chạy RMI qua mạng LAN hoặc máy ảo, nếu không thiết lập thuộc tính này, Java RMI sẽ mặc định đăng ký địa chỉ stub là `127.0.0.1` hoặc IP card mạng ảo (VirtualBox/VMware Host-Only).
   - Khi Client từ xa lookup lấy Stub về, Client sẽ cố kết nối vào `127.0.0.1` của chính máy Client dẫn tới lỗi `ConnectException`.
   - Lệnh `System.setProperty("java.rmi.server.hostname", localHost)` đảm bảo Stub chứa đúng IP mạng LAN thật của máy chủ.

