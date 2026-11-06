package server;

import common.Student;
import common.StudentSyncService;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lớp cài đặt (Implementation) của StudentSyncService.
 * Xử lý các thao tác CRUD và điều phối cơ chế đồng bộ hóa 2 chiều (Two-Way Active-Active Replication).
 * Tích hợp cơ chế:
 * 1. Chống lặp vô hạn (Loopback Prevention) bằng cách phân tách addStudent vs syncAdd.
 * 2. Khả năng chịu lỗi (Fault-Tolerance) khi Server đối tác bị tắt/ngắt mạng.
 */
public class StudentSyncServiceImpl extends UnicastRemoteObject implements StudentSyncService {
    private static final long serialVersionUID = 1L;

    // Kho lưu trữ dữ liệu trong bộ nhớ (Thread-safe)
    private final Map<String, Student> studentStore = new ConcurrentHashMap<>();

    // Thông tin nhận diện của Server hiện tại
    private final String serverId;
    private final String localHost;
    private final int localPort;

    // Thông tin kết nối của Server đối tác (Partner Server)
    private String partnerHost;
    private int partnerPort;
    private String serviceName = "StudentSyncService";

    public StudentSyncServiceImpl(String serverId, String localHost, int localPort, String partnerHost, int partnerPort) throws RemoteException {
        super(localPort); // Export UnicastRemoteObject trên đúng port mong muốn
        this.serverId = serverId;
        this.localHost = localHost;
        this.localPort = localPort;
        this.partnerHost = partnerHost;
        this.partnerPort = partnerPort;

        // Khởi tạo một số dữ liệu sinh viên mẫu để test ngay
        seedSampleData();
        log("Khởi tạo dịch vụ thành công [" + serverId + "]. Sẵn sàng phục vụ.");
    }

    private void seedSampleData() {
        Student s1 = new Student("SV001", "Nguyễn Văn An", "Công nghệ thông tin - K21");
        Student s2 = new Student("SV002", "Trần Thị Bích", "An toàn thông tin - K21");
        Student s3 = new Student("SV003", "Lê Hoàng Cường", "Kỹ thuật phần mềm - K22");
        studentStore.put(s1.getId(), s1);
        studentStore.put(s2.getId(), s2);
        studentStore.put(s3.getId(), s3);
    }

    private void log(String message) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        System.out.println("[" + time + "] [" + serverId + "] " + message);
    }

    /**
     * Lấy Remote Reference của Server đối tác thông qua RMI Registry.
     * Trả về null nếu không thể kết nối hoặc đối tác đang offline.
     */
    private StudentSyncService getPartnerService() {
        try {
            Registry registry = LocateRegistry.getRegistry(partnerHost, partnerPort);
            return (StudentSyncService) registry.lookup(serviceName);
        } catch (Exception e) {
            // Không ném lỗi ra ngoài để đảm bảo tính chịu lỗi (Fault-Tolerance)
            return null;
        }
    }

    // =========================================================================
    // PHẦN 1: CÁC PHƯƠNG THỨC DO CLIENT GỌI (TRIGGER ĐỒNG BỘ SANG ĐỐI TÁC)
    // =========================================================================

    @Override
    public boolean addStudent(Student student) throws RemoteException {
        if (student == null || student.getId() == null || student.getId().trim().isEmpty()) {
            log("[CLIENT-ADD ERROR] Dữ liệu sinh viên hoặc ID không hợp lệ.");
            return false;
        }

        student.touch(); // Cập nhật thời gian
        studentStore.put(student.getId(), student);
        log("✅ [CLIENT-ADD] Đã thêm SV cục bộ: " + student.getId() + " - " + student.getName());

        // ĐỒNG BỘ TỰ ĐỘNG SANG SERVER ĐỐI TÁC
        replicateAdd(student);

        return true;
    }

    @Override
    public boolean updateStudent(Student student) throws RemoteException {
        if (student == null || student.getId() == null || !studentStore.containsKey(student.getId())) {
            log("⚠️ [CLIENT-UPDATE] Không tìm thấy sinh viên ID=" + (student != null ? student.getId() : "null"));
            return false;
        }

        student.touch();
        studentStore.put(student.getId(), student);
        log("✏️ [CLIENT-UPDATE] Đã cập nhật SV cục bộ: " + student.getId() + " - " + student.getName());

        // ĐỒNG BỘ TỰ ĐỘNG SANG SERVER ĐỐI TÁC
        replicateUpdate(student);

        return true;
    }

    @Override
    public boolean deleteStudent(String id) throws RemoteException {
        if (id == null || !studentStore.containsKey(id)) {
            log("⚠️ [CLIENT-DELETE] Không tìm thấy sinh viên ID=" + id + " để xóa.");
            return false;
        }

        Student removed = studentStore.remove(id);
        log("🗑️ [CLIENT-DELETE] Đã xóa SV cục bộ: " + id + " (" + removed.getName() + ")");

        // ĐỒNG BỘ TỰ ĐỘNG SANG SERVER ĐỐI TÁC
        replicateDelete(id);

        return true;
    }

    @Override
    public Student getStudent(String id) throws RemoteException {
        return studentStore.get(id);
    }

    @Override
    public List<Student> getAllStudents() throws RemoteException {
        List<Student> list = new ArrayList<>(studentStore.values());
        // Sắp xếp theo ID tăng dần
        list.sort(Comparator.comparing(Student::getId));
        return list;
    }

    @Override
    public String getServerInfo() throws RemoteException {
        return serverId + " (" + localHost + ":" + localPort + ")";
    }

    // =========================================================================
    // HÀM HỖ TRỢ REPLICATION (CÓ XỬ LÝ FAULT-TOLERANCE)
    // =========================================================================

    private void replicateAdd(Student student) {
        new Thread(() -> {
            try {
                StudentSyncService partner = getPartnerService();
                if (partner != null) {
                    partner.syncAdd(student);
                    log("🔄 [REPLICATION] Đã gửi syncAdd SV " + student.getId() + " sang đối tác " + partnerHost + ":" + partnerPort + " thành công!");
                } else {
                    log("⚠️ [FAULT-TOLERANCE] Đối tác (" + partnerHost + ":" + partnerPort + ") đang OFFLINE. Thao tác cục bộ vẫn hoàn tất an toàn.");
                }
            } catch (Exception e) {
                log("⚠️ [FAULT-TOLERANCE] Không thể đồng bộ sang đối tác (" + e.getClass().getSimpleName() + ": " + e.getMessage() + "). Server vẫn tiếp tục hoạt động.");
            }
        }).start();
    }

    private void replicateUpdate(Student student) {
        new Thread(() -> {
            try {
                StudentSyncService partner = getPartnerService();
                if (partner != null) {
                    partner.syncUpdate(student);
                    log("🔄 [REPLICATION] Đã gửi syncUpdate SV " + student.getId() + " sang đối tác " + partnerHost + ":" + partnerPort + " thành công!");
                } else {
                    log("⚠️ [FAULT-TOLERANCE] Đối tác (" + partnerHost + ":" + partnerPort + ") đang OFFLINE. Thao tác cục bộ vẫn hoàn tất an toàn.");
                }
            } catch (Exception e) {
                log("⚠️ [FAULT-TOLERANCE] Lỗi khi gửi syncUpdate sang đối tác: " + e.getMessage());
            }
        }).start();
    }

    private void replicateDelete(String id) {
        new Thread(() -> {
            try {
                StudentSyncService partner = getPartnerService();
                if (partner != null) {
                    partner.syncDelete(id);
                    log("🔄 [REPLICATION] Đã gửi syncDelete SV ID=" + id + " sang đối tác " + partnerHost + ":" + partnerPort + " thành công!");
                } else {
                    log("⚠️ [FAULT-TOLERANCE] Đối tác (" + partnerHost + ":" + partnerPort + ") đang OFFLINE. Thao tác cục bộ vẫn hoàn tất an toàn.");
                }
            } catch (Exception e) {
                log("⚠️ [FAULT-TOLERANCE] Lỗi khi gửi syncDelete sang đối tác: " + e.getMessage());
            }
        }).start();
    }


    // =========================================================================
    // PHẦN 2: CÁC PHƯƠNG THỨC ĐỒNG BỘ NỘI BỘ (CHỐNG LẶP VÔ HẠN - LOOPBACK PREVENTION)
    // Khi nhận được lệnh sync..., Server CHỈ cập nhật nội bộ, KHÔNG ĐƯỢC gọi ngược lại!
    // =========================================================================

    @Override
    public boolean syncAdd(Student student) throws RemoteException {
        if (student == null || student.getId() == null) return false;
        studentStore.put(student.getId(), student);
        log("📥 [SYNC-RECEIVED: ADD] Đã nhận SV từ Server đối tác: " + student.getId() + " - " + student.getName() + " -> Lưu nội bộ thành công. (DỪNG LAN TRUYỀN ĐỂ CHỐNG LẶP)");
        return true;
    }

    @Override
    public boolean syncUpdate(Student student) throws RemoteException {
        if (student == null || student.getId() == null) return false;
        studentStore.put(student.getId(), student);
        log("📥 [SYNC-RECEIVED: UPDATE] Đã nhận cập nhật SV từ Server đối tác: " + student.getId() + " - " + student.getName() + " -> Lưu nội bộ thành công. (DỪNG LAN TRUYỀN)");
        return true;
    }

    @Override
    public boolean syncDelete(String id) throws RemoteException {
        if (id == null) return false;
        studentStore.remove(id);
        log("📥 [SYNC-RECEIVED: DELETE] Đã nhận lệnh xóa SV ID=" + id + " từ Server đối tác -> Xóa nội bộ thành công. (DỪNG LAN TRUYỀN)");
        return true;
    }

    @Override
    public List<Student> requestFullSync() throws RemoteException {
        log("📦 [FULL-SYNC-REQUEST] Đang xuất toàn bộ dữ liệu (" + studentStore.size() + " sinh viên) cho Server đối tác.");
        return new ArrayList<>(studentStore.values());
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }

    /**
     * Kéo toàn bộ dữ liệu từ Server đối tác về đồng bộ cục bộ (dùng khi mới khởi động hoặc sau khi phục hồi mạng).
     */
    public int pullSyncFromPartner() {
        try {
            StudentSyncService partner = getPartnerService();
            if (partner == null) {
                log("⚠️ Không thể kết nối tới đối tác để thực hiện Full Sync.");
                return 0;
            }
            List<Student> remoteList = partner.requestFullSync();
            int count = 0;
            for (Student s : remoteList) {
                studentStore.put(s.getId(), s);
                count++;
            }
            log("🎉 [FULL-SYNC SUCCESS] Đã đồng bộ thành công " + count + " sinh viên từ Server đối tác (" + partnerHost + ":" + partnerPort + ").");
            return count;
        } catch (Exception e) {
            log("⚠️ Lỗi trong quá trình Full Sync: " + e.getMessage());
            return 0;
        }
    }

    public void setPartnerConfig(String host, int port) {
        this.partnerHost = host;
        this.partnerPort = port;
        log("Đã cập nhật cấu hình đối tác mới: " + host + ":" + port);
    }

    public int getLocalCount() {
        return studentStore.size();
    }
}

