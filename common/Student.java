package common;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * Entity đại diện cho Sinh viên (Student) trong hệ thống Java RMI.
 * Bắt buộc implement java.io.Serializable để có thể truyền qua mạng giữa các JVM.
 */
public class Student implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;          // Mã sinh viên (Khóa chính - Primary Key)
    private String name;        // Họ và tên
    private String info;        // Thông tin chuyên ngành / Lớp / Ghi chú
    private String lastUpdated; // Dấu thời gian cập nhật gần nhất

    public Student() {
        this.lastUpdated = getCurrentTimestamp();
    }

    public Student(String id, String name, String info) {
        this.id = id;
        this.name = name;
        this.info = info;
        this.lastUpdated = getCurrentTimestamp();
    }

    public Student(String id, String name, String info, String lastUpdated) {
        this.id = id;
        this.name = name;
        this.info = info;
        this.lastUpdated = lastUpdated != null ? lastUpdated : getCurrentTimestamp();
    }

    private static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

    public void touch() {
        this.lastUpdated = getCurrentTimestamp();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return Objects.equals(id, student.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Student{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", info='" + info + '\'' +
                ", lastUpdated='" + lastUpdated + '\'' +
                '}';
    }
}
