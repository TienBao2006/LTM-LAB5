# CẨM NANG ÔN TẬP & TRẢ LỜI VẤN ĐÁP MÔN LẬP TRÌNH MẠNG
## ĐỀ TÀI: HỆ THỐNG JAVA RMI ĐỒNG BỘ DỮ LIỆU 2 CHIỀU (LAB 5 - TWO-WAY REPLICATION)

Tài liệu này tổng hợp toàn bộ các câu hỏi lý thuyết chuyên sâu, các tình huống giao tiếp mạng thực tế và giải thích chi tiết mã nguồn để sinh viên đạt điểm tuyệt đối khi trả lời vấn đáp Lab 5.

---

### ❓ CÂU HỎI 1: JAVA RMI LÀ GÌ? KIẾN TRÚC VÀ NGUYÊN LÝ HOẠT ĐỘNG CỦA JAVA RMI NHƯ THẾ NÀO?
**Vị trí code:** [`lab5/src/common/StudentSyncService.java`](file:///c:/Users/ADMIN/Python/LTM/lab5/src/common/StudentSyncService.java) và [`lab5/src/server/StudentSyncServiceImpl.java`](file:///c:/Users/ADMIN/Python/LTM/lab5/src/server/StudentSyncServiceImpl.java)

**Trả lời:**
- **Định nghĩa**:
  - **RMI (Remote Method Invocation)** là một cơ chế phân tán cấp cao của nền tảng Java cho phép một đối tượng chạy trên một Máy ảo Java (JVM) này có thể trực tiếp gọi phương thức của một đối tượng đang nằm trên một JVM khác (dù ở khác tiến trình trên cùng máy hoặc trên các máy tính khác nhau qua mạng LAN/Internet).
- **Kiến trúc 3 thành phần cốt lõi**:
  1. **RMI Registry (Mặc định cổng 1099)**: Đóng vai trò như một danh bạ đăng ký dịch vụ (Naming/Lookup Service). Server đăng ký một tên định danh gắn với đối tượng từ xa (`registry.rebind("StudentSyncService", serviceImpl)`). Client truy vấn vào danh bạ này để lấy về đối tượng đại diện (`lookup("StudentSyncService")`).
  2. **Stub (Client Proxy)**: Là đối tượng đại diện nằm ở phía Client, triển khai cùng một Remote Interface với Server. Khi Client gọi hàm `stub.addStudent(s)`, Stub sẽ đứng ra đóng gói tên hàm và các tham số thành luồng byte (gọi là quá trình **Marshaling**) và gửi qua kết nối mạng Socket tới Server.
  3. **Remote Object (Server Implementation)**: Là đối tượng thực tế nằm trên máy chủ (`StudentSyncServiceImpl`). Khi nhận được luồng dữ liệu từ Stub, tầng RMI của Server sẽ giải mã (gọi là **Unmarshaling**), thực thi phương thức thực tế trên CPU của Server, sau đó đóng gói kết quả trả về cho Client.

---

### ❓ CÂU HỎI 2: TẠI SAO ĐỐI TƯỢNG `Student` BẮT BUỘC PHẢI IMPLEMENT `java.io.Serializable`? NẾU QUÊN SẼ XẢY RA LỖI GÌ?
**Vị trí code:** [`lab5/src/common/Student.java`](file:///c:/Users/ADMIN/Python/LTM/lab5/src/common/Student.java)

**Trả lời:**
- **Nguyên lý truyền đối tượng qua mạng**:
  - Trong bộ nhớ RAM của một JVM, đối tượng `Student` là một vùng nhớ nhị phân cục bộ. Khi truyền đối tượng này sang một máy khác qua RMI (chuyển giao theo hình thức Pass-by-value), Java phải biến đổi toàn bộ cấu trúc trạng thái của đối tượng (các thuộc tính `id`, `name`, `info`, `lastUpdated`) thành một chuỗi byte tuần tự để có thể đẩy qua luồng Socket TCP. Quá trình này gọi là **Tuần tự hóa (Serialization)**.
  - Phía máy nhận sẽ thực hiện quá trình ngược lại là **Giải tuần tự hóa (Deserialization)** để tái tạo lại đối tượng nguyên vẹn trong bộ nhớ RAM của JVM đích.
- **Hệ quả nếu không implement `Serializable`**:
  - Ngay khi Client gọi hàm truyền đối tượng `Student` qua RMI, JVM sẽ lập tức ném ra ngoại lệ:
    `java.rmi.MarshalException: error marshalling arguments; nested exception is: java.io.NotSerializableException: common.Student`
- **Ý nghĩa của `serialVersionUID = 1L`**:
  - Là mã định danh phiên bản của lớp. Nếu không khai báo tường minh, JVM sẽ tự tính toán mã này dựa trên cấu trúc các trường. Khi chạy trên các máy khác nhau hoặc các phiên bản JDK khác nhau (ví dụ Java 17 và Java 21), mã tự sinh có thể bị lệch nhau, dẫn tới lỗi `InvalidClassException`. Việc khai báo tường minh `private static final long serialVersionUID = 1L;` đảm bảo tính tương thích tuyệt đối khi trao đổi qua mạng.

---

### ❓ CÂU HỎI 3: CƠ CHẾ ĐỒNG BỘ HÓA 2 CHIỀU (ACTIVE - ACTIVE REPLICATION) HOẠT ĐỘNG NHƯ THẾ NÀO?
**Vị trí code:** [`lab5/src/server/StudentSyncServiceImpl.java`](file:///c:/Users/ADMIN/Python/LTM/lab5/src/server/StudentSyncServiceImpl.java)

**Trả lời:**
- **Mô hình Active - Active**:
  - Khác với mô hình Active - Passive (Master - Slave) nơi chỉ có duy nhất 1 Server nhận lệnh ghi, trong hệ thống của dự án, **cả 2 Server (Server 1 và Server 2) đều là Active**. Client có thể kết nối đến bất kỳ Server nào để thực hiện Thêm, Sửa hoặc Xóa.
- **Quy trình hoạt động khi Client ghi dữ liệu vào Server X**:
  1. Client kết nối tới Server X (ví dụ Server 1) và gọi `addStudent(student)`.
  2. Server X cập nhật đối tượng vào cơ sở dữ liệu/bộ nhớ nội bộ của chính nó: `studentStore.put(student.getId(), student)`.
  3. Ngay lập tức, Server X đóng vai trò là một **RMI Client đối với Server Y**, tự động tìm kiếm Remote Reference của Server đối tác qua `LocateRegistry.getRegistry(partnerHost, partnerPort).lookup(...)`.
  4. Server X gọi phương thức `syncAdd(student)` sang Server Y.
  5. Server Y tiếp nhận và lưu đối tượng vào bộ nhớ nội bộ của nó.
  - Kết quả: Dữ liệu trên cả 2 máy chủ luôn ở trạng thái đồng nhất (Consistent). Người dùng kết nối vào Server 1 hay Server 2 đều thấy dữ liệu mới nhất.

---

### ❓ CÂU HỎI 4: CƠ CHẾ CHỐNG LẶP VÔ HẠN (LOOPBACK PREVENTION) ĐƯỢC GIẢI QUYẾT BẰNG THUẬT TOÁN NÀO?
**Vị trí code:** [`lab5/src/common/StudentSyncService.java`](file:///c:/Users/ADMIN/Python/LTM/lab5/src/common/StudentSyncService.java) và [`lab5/src/server/StudentSyncServiceImpl.java`](file:///c:/Users/ADMIN/Python/LTM/lab5/src/server/StudentSyncServiceImpl.java)

**Trả lời:**
- **Nguy cơ lặp vô hạn (Infinite Loop)**:
  - Nếu Server sử dụng chung một phương thức `addStudent`:
    - Khi Client thêm trên Server 1: Server 1 gọi `Server 2.addStudent()`.
    - Khi Server 2 nhận lệnh, nếu Server 2 lại gọi ngược lại `Server 1.addStudent()`.
    - Hai Server sẽ liên tục gọi nhau qua lại không ngừng, dẫn đến cạn kiệt ngăn xếp luồng (`StackOverflowError`), tràn hàng đợi mạng và làm treo hoàn toàn cả hai máy chủ.
- **Giải pháp Phân tách Phương thức (Split Interface Pattern) trong dự án**:
  Dự án tách giao thức RMI thành 2 nhóm hàm độc lập:
  1. **Nhóm phương thức do Client gọi**: `addStudent`, `updateStudent`, `deleteStudent`.
     - Nhiệm vụ: Ghi nhận dữ liệu cục bộ + **Kích hoạt việc gọi sang đối tác** (`replicateAdd`, `replicateUpdate`, `replicateDelete`).
  2. **Nhóm phương thức đồng bộ nội bộ giữa 2 Server**: `syncAdd`, `syncUpdate`, `syncDelete`.
     - Nhiệm vụ: Khi Server đối tác nhận được các hàm này, nó **CHỈ thực hiện lưu vào bộ nhớ cục bộ** và **TUYỆT ĐỐI DỪNG LAN TRUYỀN (TERMINATE PROPAGATION)**, không gọi ngược lại máy gửi.
  - Nhờ đó, chu trình đồng bộ luôn dừng lại chính xác sau đúng 1 chặng truyền (1-hop replication), triệt tiêu hoàn toàn nguy cơ lặp vô hạn.

---

### ❓ CÂU HỎI 5: TẠI SAO BẮT BUỘC PHẢI THIẾT LẬP THUỘC TÍNH `java.rmi.server.hostname` KHI CHẠY TRÊN MẠNG LAN HOẶC MÁY ẢO?
**Vị trí code:** [`lab5/src/server/ServerMain.java`](file:///c:/Users/ADMIN/Python/LTM/lab5/src/server/ServerMain.java)

**Trả lời:**
- **Bản chất vấn đề**:
  - Khi một Server đăng ký dịch vụ RMI lên Registry, RMI Registry sẽ ghi nhận địa chỉ của Remote Object vào đối tượng **Stub**.
  - Nếu không thiết lập cấu hình cụ thể, Java RMI sẽ tự động dùng hàm `InetAddress.getLocalHost()` của hệ điều hành. Trên hầu hết máy tính và máy ảo (VMware, VirtualBox), hàm này thường trả về địa chỉ loopback `127.0.0.1` hoặc IP của card mạng ảo nội bộ (Host-Only).
  - Khi Client từ xa (chạy trên máy vật lý khác hoặc trong máy ảo qua Bridged Adapter) kết nối tới RMI Registry của Server, nó lấy được Stub về. Nhưng bên trong Stub lại chứa địa chỉ `127.0.0.1`! Khi Client thực hiện gọi hàm trên Stub, Client sẽ cố gắng kết nối vào `127.0.0.1` của **chính máy Client** thay vì kết nối tới máy chủ thật. Hậu quả là Client lập tức bị lỗi `java.rmi.ConnectException: Connection refused to host: 127.0.0.1`.
- **Cách khắc phục trong dự án**:
  - Trước khi khởi tạo Registry và export Remote Object, Server thực hiện:
    ```java
    System.setProperty("java.rmi.server.hostname", localHost);
    ```
  - Trong đó `localHost` là địa chỉ IP mạng LAN thật của máy chủ (ví dụ `192.168.1.100`). Khi đó, Stub trả về cho Client sẽ chứa chính xác IP này, giúp mọi máy tính trong mạng LAN gọi hàm thành công 100%.

---

### ❓ CÂU HỎI 6: CƠ CHẾ CHỊU LỖI (FAULT-TOLERANCE) KHI MỘT TRONG HAI SERVER BỊ TẮT ĐƯỢC XỬ LÝ NHƯ THẾ NÀO?
**Vị trí code:** [`lab5/src/server/StudentSyncServiceImpl.java`](file:///c:/Users/ADMIN/Python/LTM/lab5/src/server/StudentSyncServiceImpl.java) (Các hàm `replicateAdd`, `replicateUpdate`, `replicateDelete`)

**Trả lời:**
- **Yêu cầu thực tế**: Trong hệ thống phân tán, sự cố mất mạng hoặc một node bị sập là điều khó tránh khỏi. Không thể vì Server 2 bị tắt mà Server 1 từ chối phục vụ người dùng.
- **Giải pháp lập trình trong dự án**:
  1. Thao tác đồng bộ sang Server đối tác được thực hiện trong một **luồng bất đồng bộ riêng biệt (Background Thread)**: `new Thread(() -> { ... }).start();`.
  2. Bọc toàn bộ quá trình lookup và gọi hàm đối tác trong khối `try-catch (Exception e)`.
  3. Nếu đối tác không phản hồi (ném ra `ConnectException`, `NotBoundException` hoặc Timeout):
     - Server hiện tại chỉ in cảnh báo ra log console:
       `[WARNING] Đối tác đang OFFLINE. Thao tác cục bộ vẫn hoàn tất an toàn.`
     - **Không ném ngoại lệ lên Client**, phương thức `addStudent` vẫn trả về `true` cho Client.
  4. Khi Server bị sập khởi động lại, nó có thể gọi hàm `pullSyncFromPartner()` (hoặc gọi qua lệnh console `sync`) để kéo toàn bộ dữ liệu mới nhất về, đưa hệ thống trở lại trạng thái đồng nhất.

---

### ❓ CÂU HỎI 7: AN TOÀN ĐA LUỒNG (THREAD-SAFETY) TRÊN SERVER ĐƯỢC ĐẢM BẢO NHƯ THẾ NÀO?
**Vị trí code:** [`lab5/src/server/StudentSyncServiceImpl.java`](file:///c:/Users/ADMIN/Python/LTM/lab5/src/server/StudentSyncServiceImpl.java)

**Trả lời:**
- **Đặc điểm của RMI Server**:
  - Cơ chế RMI Runtime của Java tự động duy trì một Thread Pool ngầm. Mỗi khi có Client hoặc Server đối tác gọi đến một Remote Method, RMI sẽ cấp phát một luồng Worker riêng để thực thi.
  - Do đó, nếu 10 Client cùng lúc gửi yêu cầu Thêm, Sửa hoặc Xóa, mã nguồn của Server sẽ chạy đồng thời trên 10 luồng khác nhau.
- **Giải pháp bảo vệ**:
  - Không sử dụng `java.util.HashMap` thông thường (vốn không an toàn và dễ gây lỗi `ConcurrentModificationException` hoặc mất dữ liệu).
  - Sử dụng cấu trúc `java.util.concurrent.ConcurrentHashMap<String, Student>`:
    - Cơ chế khóa phân đoạn (Lock Striping) và thao tác nguyên tử (Atomic Operations) của `ConcurrentHashMap` cho phép nhiều luồng đọc và ghi đồng thời mà không bị tranh chấp dữ liệu (Race Condition) và đạt hiệu năng tối đa.

---

### ❓ CÂU HỎI 8: SO SÁNH SỰ KHÁC NHAU GIỮA LẬP TRÌNH SOCKET TCP THUẦN VÀ LẬP TRÌNH JAVA RMI?
**Trả lời:**

| Tiêu chí so sánh | Socket TCP thuần (Low-level Socket) | Java RMI (High-level Distributed Object) |
| :--- | :--- | :--- |
| **Mức độ trừu tượng** | Tầng giao vận (Transport Layer) - Cấp thấp. | Tầng ứng dụng (Application Layer) - Trừu tượng hóa cấp cao. |
| **Dữ liệu truyền nhận** | Luồng byte thô (`InputStream`, `OutputStream`). | Đối tượng Java hoàn chỉnh (`Student`, `List<Student>`). |
| **Đóng gói / Phân giải** | Lập trình viên phải tự quy định cú pháp, tự viết bộ phân tích chuỗi (Parser) hoặc ký tự phân cách (`\r\n\r\n`). | Java RMI Runtime tự động xử lý toàn bộ quá trình Marshaling / Unmarshaling qua Java Serialization. |
| **Cú pháp lập trình** | Phải mở kết nối, gửi lệnh văn bản, nhận mã phản hồi và đóng kênh thủ công. | Gọi hàm từ xa giống hệt như gọi một hàm cục bộ bình thường trong Java: `boolean ok = service.addStudent(s);`. |
| **Tính đa ngôn ngữ** | Rất cao: Client viết bằng C#, Python, C++ có thể giao tiếp dễ dàng với Server viết bằng Java. | Hạn chế: Thường chỉ hỗ trợ giữa các JVM với nhau (thuần Java). |

---

### ❓ CÂU HỎI 9: TRÌNH BÀY CÁC THÀNH PHẦN CHÍNH TRÊN GIAO DIỆN CLIENT SWING VÀ CƠ CHẾ SỰ KIỆN?
**Vị trí code:** [`lab5/src/client/ClientForm.java`](file:///c:/Users/ADMIN/Python/LTM/lab5/src/client/ClientForm.java)

**Trả lời:**
- **Thanh cấu hình Server IP & Port**: Cho phép Client nhập IP và cổng của Server 1 (`1099`) hoặc Server 2 (`1100`) và bấm "Kết nối". Nhờ đó, người dùng có thể linh hoạt kiểm tra tính đồng bộ Active-Active của cả 2 máy chủ.
- **Form nhập liệu Sinh viên**: Gồm `ID` (Mã SV - Khóa chính), `Tên` (Họ và tên), `Thông tin` (Chuyên ngành / Ghi chú).
- **Bộ nút tác vụ CRUD**:
  - `Thêm mới`: Tạo đối tượng `Student` và gọi `service.addStudent(student)`.
  - `Cập nhật`: Gọi `service.updateStudent(student)`.
  - `Xóa`: Bật hộp thoại xác nhận `JOptionPane` và gọi `service.deleteStudent(id)`.
  - `Làm mới`: Gọi `service.getAllStudents()` và nạp lại vào bảng.
- **Bảng hiển thị `JTable`**:
  - Hiển thị danh sách sinh viên hiện có.
  - Đăng ký `ListSelectionListener`: Khi người dùng nhấp chuột vào bất kỳ dòng nào trên bảng, thông tin dòng đó sẽ tự động được điền lên các ô nhập liệu phía trên.
- **Xử lý bất đồng bộ (Non-blocking UI)**:
  - Tất cả các lệnh gọi RMI qua mạng (`addStudent`, `getAllStudents`, `connect`) đều được đưa vào một `Thread` chạy ngầm, sau đó dùng `SwingUtilities.invokeLater()` để cập nhật lại giao diện. Nhờ đó, giao diện Client luôn mượt mà, không bao giờ bị đơ (Not Responding) kể cả khi mạng bị lag.

---

### ❓ CÂU HỎI 10: QUY TRÌNH TRÌNH DIỄN THỰC HÀNH TẠI BUỔI VẤN ĐÁP ĐỂ ĐẠT ĐIỂM TỐI ĐA?
**Quy trình 6 bước chuẩn bị trước Giảng viên:**
1. **Bước 1**: Nhấp đúp `run_server1.bat` (Server 1 chạy trên port 1099, in ra các IP mạng LAN của máy).
2. **Bước 2**: Nhấp đúp `run_server2.bat` (Server 2 chạy trên port 1100).
3. **Bước 3**: Nhấp đúp `run_client.bat` để mở giao diện Client:
   - Bấm nút **"Nối Server 1 (1099)"** -> Đã kết nối Server 1.
   - Nhập thông tin: ID=`SV100`, Tên=`Nguyễn Hoàng Long`, Thông tin=`An toàn thông tin K21` -> Bấm **➕ Thêm mới**.
4. **Bước 4 (Chứng minh đồng bộ chiều đi)**:
   - Trên Client bấm nút **"Nối Server 2 (1100)"** -> Bảng hiển thị ngay lập tức sinh viên `SV100` (Chứng minh Server 1 đã replicate sang Server 2).
5. **Bước 5 (Chứng minh đồng bộ chiều về & Chống lặp)**:
   - Trên Server 2, chọn sinh viên `SV100`, sửa Tên thành `Nguyễn Hoàng Long (Đã Cập Nhật)` -> Bấm **✏️ Cập nhật**.
   - Bấm nút **"Nối Server 1 (1099)"** -> Thông tin trên Server 1 đã được cập nhật chính xác. Nhìn console log của cả 2 Server thấy xuất hiện dòng `DỪNG LAN TRUYỀN ĐỂ CHỐNG LẶP` -> Chứng minh cơ chế Loopback Prevention hoạt động đúng.
6. **Bước 6 (Chứng minh chịu lỗi & Chạy Test tự động)**:
   - Tắt cửa sổ Server 2. Trên Client (đang nối Server 1), bấm Thêm sinh viên mới -> Server 1 vẫn thêm thành công và ghi log `Đối tác offline. Thao tác cục bộ an toàn`.
   - Chạy `test_simulation.bat` trên Terminal -> Toàn bộ 7 bài kiểm thử tự động báo **PASS 100%**.

---
*Tài liệu được biên soạn phục vụ học phần Lập Trình Mạng - Giảng dạy và kiểm tra mã nguồn tại Đại học.*

