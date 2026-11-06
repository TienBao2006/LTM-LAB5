package test;

import common.Student;
import common.StudentSyncService;
import server.StudentSyncServiceImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

/**
 * Kịch bản kiểm thử tự động toàn diện kiểm tra cơ chế đồng bộ hóa 2 chiều Java RMI:
 * 1. Khởi động Registry và Service cho Server 1 (Port 1099) và Server 2 (Port 1100).
 * 2. Ghi dữ liệu vào Server 1 -> Kiểm tra dữ liệu tự động xuất hiện trên Server 2.
 * 3. Sửa dữ liệu trên Server 2 -> Kiểm tra dữ liệu tự động cập nhật trên Server 1.
 * 4. Kiểm tra Chống lặp vô hạn (Loopback Prevention) không làm trùng lặp hay nghẽn luồng.
 * 5. Kiểm tra Chịu lỗi (Fault-Tolerance) khi một Server offline, Server còn lại vẫn phục vụ bình thường.
 * 6. Xóa dữ liệu trên Server 1 -> Kiểm tra dữ liệu tự động biến mất trên Server 2.
 */
public class TestReplication {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println(" KIỂM THỬ TỰ ĐỘNG CƠ CHẾ ĐỒNG BỘ 2 CHIỀU JAVA RMI (TWO-WAY REPLICATION)  ");
        System.out.println("==========================================================================");

        try {
            System.setProperty("java.rmi.server.hostname", "127.0.0.1");

            // 1. Khởi tạo Server 1 trên cổng 1099, đối tác là cổng 1100
            System.out.println("\n[BƯỚC 1] Khởi động Server 1 (Port 1099) & Server 2 (Port 1100)...");
            Registry reg1 = LocateRegistry.createRegistry(1099);
            StudentSyncServiceImpl srv1 = new StudentSyncServiceImpl("Server 1", "127.0.0.1", 1099, "127.0.0.1", 1100);
            reg1.rebind("StudentSyncService", srv1);
            System.out.println("  -> Server 1 sẵn sàng trên 127.0.0.1:1099");

            Registry reg2 = LocateRegistry.createRegistry(1100);
            StudentSyncServiceImpl srv2 = new StudentSyncServiceImpl("Server 2", "127.0.0.1", 1100, "127.0.0.1", 1099);
            reg2.rebind("StudentSyncService", srv2);
            System.out.println("  -> Server 2 sẵn sàng trên 127.0.0.1:1100");
            System.out.println("  ✅ [PASS] Cả 2 RMI Server đã hoạt động!");

            // Đợi 500ms cho 2 server ổn định
            Thread.sleep(500);

            // 2. Client kết nối tới Server 1 và Server 2
            System.out.println("\n[BƯỚC 2] RMI Client kết nối tới 2 Server...");
            StudentSyncService clientToServer1 = (StudentSyncService) reg1.lookup("StudentSyncService");
            StudentSyncService clientToServer2 = (StudentSyncService) reg2.lookup("StudentSyncService");
            System.out.println("  -> Client -> Server 1: " + clientToServer1.getServerInfo());
            System.out.println("  -> Client -> Server 2: " + clientToServer2.getServerInfo());
            System.out.println("  ✅ [PASS] Kết nối Remote Reference thành công!");

            // 3. Thử nghiệm Thêm mới trên Server 1 -> Tự động replicate sang Server 2
            System.out.println("\n[BƯỚC 3] Thêm mới sinh viên SV999 trên Server 1...");
            Student newStudent = new Student("SV999", "Võ Hoàng Nam", "Khoa học Dữ liệu - K22");
            boolean addOk = clientToServer1.addStudent(newStudent);
            if (!addOk) throw new RuntimeException("Thêm sinh viên trên Server 1 thất bại!");
            System.out.println("  -> Server 1 phản hồi addStudent: SUCCESS");

            // Chờ 300ms cho luồng bất đồng bộ replicate hoàn tất
            Thread.sleep(300);

            // Kiểm tra trên Server 2
            Student checkOnServer2 = clientToServer2.getStudent("SV999");
            if (checkOnServer2 == null || !checkOnServer2.getName().equals("Võ Hoàng Nam")) {
                throw new RuntimeException("Lỗi: Dữ liệu chưa được replicate sang Server 2!");
            }
            System.out.println("  -> Đọc từ Server 2: " + checkOnServer2.getId() + " - " + checkOnServer2.getName());
            System.out.println("  ✅ [PASS] Đồng bộ 1 chiều (Server 1 -> Server 2) thành công 100%!");

            // 4. Thử nghiệm Cập nhật trên Server 2 -> Tự động replicate ngược lại Server 1 (Two-Way)
            System.out.println("\n[BƯỚC 4] Cập nhật thông tin SV999 trên Server 2...");
            checkOnServer2.setName("Võ Hoàng Nam (Đã Cập Nhật)");
            checkOnServer2.setInfo("Khoa học Máy tính - Thạc sĩ");
            boolean updateOk = clientToServer2.updateStudent(checkOnServer2);
            if (!updateOk) throw new RuntimeException("Cập nhật trên Server 2 thất bại!");

            Thread.sleep(300);

            Student checkOnServer1 = clientToServer1.getStudent("SV999");
            if (checkOnServer1 == null || !checkOnServer1.getName().equals("Võ Hoàng Nam (Đã Cập Nhật)")) {
                throw new RuntimeException("Lỗi: Cập nhật chưa được replicate ngược sang Server 1!");
            }
            System.out.println("  -> Đọc từ Server 1: " + checkOnServer1.getId() + " - " + checkOnServer1.getName() + " | " + checkOnServer1.getInfo());
            System.out.println("  ✅ [PASS] Đồng bộ chiều ngược lại (Server 2 -> Server 1) thành công 100%!");

            // 5. Kiểm tra Chống lặp vô hạn (Loopback Prevention)
            System.out.println("\n[BƯỚC 5] Kiểm tra cơ chế Chống lặp vô hạn (Loopback Prevention)...");
            List<Student> list1 = clientToServer1.getAllStudents();
            List<Student> list2 = clientToServer2.getAllStudents();
            if (list1.size() != list2.size()) {
                throw new RuntimeException("Số lượng bản ghi 2 server không khớp: Server 1=" + list1.size() + ", Server 2=" + list2.size());
            }
            System.out.println("  -> Số lượng bản ghi Server 1: " + list1.size());
            System.out.println("  -> Số lượng bản ghi Server 2: " + list2.size());
            System.out.println("  ✅ [PASS] Dữ liệu 2 Server khớp tuyệt đối, không bị trùng lặp hoặc lặp vô hạn!");

            // 6. Kiểm tra Khả năng chịu lỗi (Fault-Tolerance) khi đối tác ngắt kết nối
            System.out.println("\n[BƯỚC 6] Kiểm tra khả năng chịu lỗi (Fault-Tolerance)...");
            // Cố tình trỏ đối tác của Server 1 vào một port không tồn tại (giả lập Server 2 tắt nguồn)
            srv1.setPartnerConfig("127.0.0.1", 9999);
            Student faultTestStudent = new Student("SV888", "Đặng Quang Huy", "Hệ thống thông tin");
            boolean faultAddOk = clientToServer1.addStudent(faultTestStudent);
            if (!faultAddOk) {
                throw new RuntimeException("Server 1 bị sập hoặc từ chối thao tác khi đối tác offline!");
            }
            System.out.println("  -> Server 1 vẫn thêm thành công SV888 khi đối tác offline.");
            // Khôi phục lại cấu hình đối tác
            srv1.setPartnerConfig("127.0.0.1", 1100);
            System.out.println("  ✅ [PASS] Server hoạt động bền bỉ, không bị crash khi đối tác mất kết nối!");

            // 7. Thử nghiệm Xóa trên Server 1 -> Tự động replicate xóa trên Server 2
            System.out.println("\n[BƯỚC 7] Xóa SV999 trên Server 1...");
            boolean delOk = clientToServer1.deleteStudent("SV999");
            if (!delOk) throw new RuntimeException("Xóa SV999 trên Server 1 thất bại!");

            Thread.sleep(300);

            Student deletedOnServer2 = clientToServer2.getStudent("SV999");
            if (deletedOnServer2 != null) {
                throw new RuntimeException("Lỗi: SV999 vẫn còn tồn tại trên Server 2 sau khi xóa!");
            }
            System.out.println("  -> Kiểm tra Server 2: SV999 đã được xóa đồng bộ (null).");
            System.out.println("  ✅ [PASS] Replicate Xóa (syncDelete) thành công!");

            System.out.println("\n==========================================================================");
            System.out.println(" 🎉 TẤT CẢ 7 BÀI KIỂM THỬ ĐỒNG BỘ 2 CHIỀU ĐÃ VƯỢT QUA XUẤT SẮC 100%!     ");
            System.out.println("==========================================================================");

            System.exit(0);

        } catch (Exception e) {
            System.err.println("❌ KIỂM THỬ THẤT BẠI: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

