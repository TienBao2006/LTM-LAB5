package server;

import common.Student;
import common.StudentSyncService;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;

/**
 * Điểm khởi chạy chính của RMI Server (Hỗ trợ cấu hình IP cho mạng LAN thực tế và máy ảo).
 * Hướng dẫn chạy:
 *   Server 1: java server.ServerMain 1099 1100 127.0.0.1 127.0.0.1 "Server 1"
 *   Server 2: java server.ServerMain 1100 1099 127.0.0.1 127.0.0.1 "Server 2"
 */
public class ServerMain {

    public static void main(String[] args) {
        // Cấu hình mặc định (Server 1 trên localhost)
        int localPort = 1099;
        int partnerPort = 1100;
        String localHost = "127.0.0.1";
        String partnerHost = "127.0.0.1";
        String serverId = "Server 1";
        String serviceName = "StudentSyncService";

        // Tự động phân tích tham số dòng lệnh
        if (args.length >= 1) {
            try { localPort = Integer.parseInt(args[0]); } catch (Exception ignored) {}
        }
        if (args.length >= 2) {
            try { partnerPort = Integer.parseInt(args[1]); } catch (Exception ignored) {}
        }
        if (args.length >= 3) {
            localHost = args[2];
        }
        if (args.length >= 4) {
            partnerHost = args[3];
        }
        if (args.length >= 5) {
            serverId = args[4];
        }

        // Hỗ trợ cả cờ named flags (ví dụ: --port 1099 --partner-host 192.168.1.5)
        for (int i = 0; i < args.length; i++) {
            if ("--port".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                localPort = Integer.parseInt(args[++i]);
            } else if ("--partner-port".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                partnerPort = Integer.parseInt(args[++i]);
            } else if ("--host".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                localHost = args[++i];
            } else if ("--partner-host".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                partnerHost = args[++i];
            } else if ("--id".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                serverId = args[++i];
            }
        }

        // =========================================================================
        // QUAN TRỌNG KHI VẤN ĐÁP: THIẾT LẬP java.rmi.server.hostname
        // Khi chạy trên 2 máy vật lý và máy ảo kết nối qua mạng LAN (Bridged Adapter),
        // Java RMI mặc định rất dễ bind nhầm vào 127.0.0.1 khiến máy khác không gọi được.
        // Cài đặt thuộc tính này TRƯỚC KHI tạo Registry và export Remote Object.
        // =========================================================================
        System.setProperty("java.rmi.server.hostname", localHost);

        printBanner(serverId, localHost, localPort, partnerHost, partnerPort, serviceName);

        try {
            // 1. Tạo hoặc lấy RMI Registry tại cổng localPort
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(localPort);
                System.out.println("✅ [RMI Registry] Đã khởi tạo mới RMI Registry trên cổng " + localPort);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(localPort);
                System.out.println("ℹ️ [RMI Registry] Đã kết nối tới RMI Registry sẵn có trên cổng " + localPort);
            }

            // 2. Khởi tạo đối tượng triển khai dịch vụ (Service Implementation)
            StudentSyncServiceImpl serviceImpl = new StudentSyncServiceImpl(
                    serverId, localHost, localPort, partnerHost, partnerPort
            );

            // 3. Đăng ký dịch vụ vào RMI Registry
            registry.rebind(serviceName, serviceImpl);
            System.out.println("✅ [RMI Registry] Đã rebind dịch vụ \"" + serviceName + "\" thành công!");
            System.out.println("🟢 [" + serverId + "] ĐANG HOẠT ĐỘNG. Sẵn sàng tiếp nhận kết nối từ Client và Partner.\n");

            // Tự động thử Full Sync với đối tác sau 2 giây (nếu đối tác đã bật trước đó)
            final StudentSyncServiceImpl finalService = serviceImpl;
            final String finalPartnerHost = partnerHost;
            final int finalPartnerPort = partnerPort;
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    System.out.println("🔍 Đang thử kết nối kiểm tra đối tác (" + finalPartnerHost + ":" + finalPartnerPort + ")...");
                    int synced = finalService.pullSyncFromPartner();
                    if (synced > 0) {
                        System.out.println("🎉 [AUTO-SYNC] Đã đồng bộ " + synced + " sinh viên ban đầu từ đối tác!");
                    } else {
                        System.out.println("ℹ️ [AUTO-SYNC] Chưa phát hiện dữ liệu mới hoặc đối tác chưa bật. Server hoạt động độc lập.");
                    }
                } catch (Exception ignored) {}
            }).start();

            // Khởi động bảng điều khiển dòng lệnh tương tác (Console Commands)
            runConsoleLoop(serviceImpl, serverId);

        } catch (Exception e) {
            System.err.println("❌ Lỗi nghiêm trọng khi khởi động Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printBanner(String id, String lHost, int lPort, String pHost, int pPort, String sName) {
        System.out.println("==========================================================================");
        System.out.println("   HỆ THỐNG JAVA RMI ĐỒNG BỘ DỮ LIỆU 2 CHIỀU (LAB 5 - LẬP TRÌNH MẠNG)   ");
        System.out.println("==========================================================================");
        System.out.println("  • Định danh máy chủ:    " + id);
        System.out.println("  • Địa chỉ RMI Hostname: " + System.getProperty("java.rmi.server.hostname"));
        System.out.println("  • Cổng lắng nghe (Port):" + lPort);
        System.out.println("  • Tên RMI Service:      " + sName);
        System.out.println("  • Server đối tác:       " + pHost + ":" + pPort);
        System.out.println("  • Mô hình đồng bộ:      Active - Active (Two-Way Replication)");
        System.out.println("  • Chống lặp vô hạn:     Loopback Prevention (addStudent vs syncAdd)");
        System.out.println("  • Chịu lỗi mạng:        Fault-Tolerance (Tiếp tục khi đối tác offline)");
        System.out.println("--------------------------------------------------------------------------");
        printLocalIpAddresses();
        System.out.println("==========================================================================");
    }

    private static void printLocalIpAddresses() {
        try {
            System.out.println("  📌 Các IP khả dụng trên máy này (Dùng cấu hình LAN / Máy ảo):");
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.getHostAddress().contains(".")) { // IPv4
                        System.out.println("     -> " + iface.getDisplayName() + ": " + addr.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static void runConsoleLoop(StudentSyncServiceImpl service, String serverId) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("💡 Lệnh console: [list] Xem DS sinh viên | [sync] Đồng bộ ngay | [ping] Kiểm tra đối tác | [exit] Thoát\n");

        while (true) {
            System.out.print("[" + serverId + "]> ");
            if (!scanner.hasNextLine()) break;
            String cmd = scanner.nextLine().trim().toLowerCase();

            if ("exit".equals(cmd) || "quit".equals(cmd)) {
                System.out.println("Đang tắt Server...");
                System.exit(0);
            } else if ("list".equals(cmd)) {
                try {
                    List<Student> list = service.getAllStudents();
                    System.out.println("----- DANH SÁCH SINH VIÊN HIỆN CÓ (" + list.size() + ") -----");
                    for (Student s : list) {
                        System.out.printf("  [%s] %-20s | %-30s | %s\n", s.getId(), s.getName(), s.getInfo(), s.getLastUpdated());
                    }
                    System.out.println("--------------------------------------------------");
                } catch (Exception e) {
                    System.err.println("Lỗi: " + e.getMessage());
                }
            } else if ("sync".equals(cmd)) {
                int count = service.pullSyncFromPartner();
                System.out.println("Kết quả: Đã nhận " + count + " bản ghi từ đối tác.");
            } else if ("ping".equals(cmd)) {
                try {
                    int count = service.getLocalCount();
                    System.out.println("Trạng thái cục bộ: " + count + " sinh viên.");
                } catch (Exception e) {
                    System.err.println("Lỗi ping: " + e.getMessage());
                }
            } else if (!cmd.isEmpty()) {
                System.out.println("Lệnh không nhận diện. Hãy nhập: list, sync, ping, exit");
            }
        }
    }
}
