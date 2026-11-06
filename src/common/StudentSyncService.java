package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Remote Interface định nghĩa các dịch vụ CRUD và cơ chế đồng bộ hóa dữ liệu 2 chiều giữa 2 Server RMI.
 * Tất cả các phương thức trong interface bắt buộc phải khai báo throws RemoteException.
 */
public interface StudentSyncService extends Remote {

    // =========================================================================
    // PHẦN 1: CÁC PHƯƠNG THỨC DO CLIENT GỌI (TỰ ĐỘNG TRIGGER ĐỒNG BỘ SANG ĐỐI TÁC)
    // =========================================================================

    /**
     * Thêm mới sinh viên từ Client.
     * Server nhận yêu cầu sẽ lưu vào bộ nhớ cục bộ, sau đó tự động gọi syncAdd sang Server đối tác.
     */
    boolean addStudent(Student student) throws RemoteException;

    /**
     * Cập nhật thông tin sinh viên từ Client.
     * Server nhận yêu cầu sẽ cập nhật cục bộ, sau đó tự động gọi syncUpdate sang Server đối tác.
     */
    boolean updateStudent(Student student) throws RemoteException;

    /**
     * Xóa sinh viên theo mã ID từ Client.
     * Server nhận yêu cầu sẽ xóa cục bộ, sau đó tự động gọi syncDelete sang Server đối tác.
     */
    boolean deleteStudent(String id) throws RemoteException;

    /**
     * Lấy thông tin chi tiết một sinh viên theo mã ID.
     */
    Student getStudent(String id) throws RemoteException;

    /**
     * Lấy toàn bộ danh sách sinh viên hiện có trên Server đang kết nối.
     */
    List<Student> getAllStudents() throws RemoteException;

    /**
     * Lấy tên nhận diện và địa chỉ IP/Port của Server hiện tại để hiển thị trên giao diện Client.
     */
    String getServerInfo() throws RemoteException;


    // =========================================================================
    // PHẦN 2: CÁC PHƯƠNG THỨC ĐỒNG BỘ NỘI BỘ GIỮA 2 SERVER (CHỐNG LẶP VÔ HẠN)
    // Server nhận được các lệnh sync... CHỈ cập nhật nội bộ, TUYỆT ĐỐI KHÔNG gọi ngược lại!
    // =========================================================================

    /**
     * Phương thức nhận đồng bộ Thêm mới từ Server đối tác.
     */
    boolean syncAdd(Student student) throws RemoteException;

    /**
     * Phương thức nhận đồng bộ Cập nhật từ Server đối tác.
     */
    boolean syncUpdate(Student student) throws RemoteException;

    /**
     * Phương thức nhận đồng bộ Xóa từ Server đối tác.
     */
    boolean syncDelete(String id) throws RemoteException;

    /**
     * Yêu cầu đồng bộ toàn bộ dữ liệu (Full Sync) khi một Server vừa khởi động lại hoặc kết nối lại.
     */
    List<Student> requestFullSync() throws RemoteException;

    /**
     * Kiểm tra trạng thái hoạt động (Heartbeat / Ping) của Server đối tác.
     */
    boolean ping() throws RemoteException;
}

