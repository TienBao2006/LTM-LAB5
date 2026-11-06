package client;

import common.Student;
import common.StudentSyncService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Giao diện người dùng Java Swing mô phỏng RMI Client kết nối đa Server (Active-Active).
 * Cho phép Client chuyển đổi kết nối linh hoạt giữa Server 1 và Server 2,
 * thực hiện các thao tác CRUD và quan sát cơ chế đồng bộ hóa 2 chiều tự động.
 */
public class ClientForm extends JFrame {

    // Service RMI đang kết nối
    private StudentSyncService syncService;
    private String currentServerAddress = "";

    // Giao diện: Cấu hình kết nối
    private JTextField txtServerIp;
    private JTextField txtServerPort;
    private JTextField txtServiceName;
    private JButton btnConnect;
    private JButton btnQuickServer1;
    private JButton btnQuickServer2;
    private JLabel lblStatus;

    // Giao diện: Nhập liệu
    private JTextField txtId;
    private JTextField txtName;
    private JTextField txtInfo;

    // Giao diện: Nút thao tác
    private JButton btnAdd;
    private JButton btnUpdate;
    private JButton btnDelete;
    private JButton btnRefresh;
    private JButton btnClearForm;

    // Giao diện: Bảng dữ liệu
    private JTable studentTable;
    private DefaultTableModel tableModel;

    // Giao diện: Console Log
    private JTextArea txtLog;

    public ClientForm() {
        super("Java RMI Client - Quản lý Sinh viên & Đồng bộ Dữ liệu 2 Server (Lab 5)");
        initUI();
        // Mặc định kết nối tới Server 1 ban đầu
        SwingUtilities.invokeLater(() -> connectToServer(txtServerIp.getText().trim(), Integer.parseInt(txtServerPort.getText().trim())));
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 720);
        setMinimumSize(new Dimension(880, 600));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // 1. TOP PANEL: KẾT NỐI SERVER
        JPanel topPanel = createConnectionPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 2. CENTER PANEL: FORM NHẬP LIỆU & BẢNG DỮ LIỆU
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        JPanel formPanel = createFormPanel();
        centerPanel.add(formPanel, BorderLayout.NORTH);

        JPanel tablePanel = createTablePanel();
        centerPanel.add(tablePanel, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // 3. BOTTOM PANEL: NHẬT KÝ SỰ KIỆN (LOG)
        JPanel logPanel = createLogPanel();
        mainPanel.add(logPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder(" 🌐 Cấu hình Kết nối RMI Server (Chuyển đổi Active - Active) "),
                new EmptyBorder(6, 8, 6, 8)
        ));

        JPanel inputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        inputs.add(new JLabel("Server IP:"));
        txtServerIp = new JTextField("127.0.0.1", 10);
        inputs.add(txtServerIp);

        inputs.add(new JLabel("Port:"));
        txtServerPort = new JTextField("1099", 5);
        inputs.add(txtServerPort);

        inputs.add(new JLabel("Service:"));
        txtServiceName = new JTextField("StudentSyncService", 12);
        inputs.add(txtServiceName);

        btnConnect = new JButton("🔗 Kết nối");
        btnConnect.setBackground(new Color(24, 119, 242));
        btnConnect.setForeground(Color.WHITE);
        btnConnect.setFocusPainted(false);
        btnConnect.addActionListener(e -> {
            try {
                String ip = txtServerIp.getText().trim();
                int port = Integer.parseInt(txtServerPort.getText().trim());
                connectToServer(ip, port);
            } catch (NumberFormatException ex) {
                showError("Cổng (Port) phải là một số nguyên hợp lệ!");
            }
        });
        inputs.add(btnConnect);

        // Nút chuyển nhanh giữa 2 Server để test đồng bộ
        btnQuickServer1 = new JButton("Nối Server 1 (1099)");
        btnQuickServer1.addActionListener(e -> {
            txtServerPort.setText("1099");
            connectToServer(txtServerIp.getText().trim(), 1099);
        });
        inputs.add(btnQuickServer1);

        btnQuickServer2 = new JButton("Nối Server 2 (1100)");
        btnQuickServer2.addActionListener(e -> {
            txtServerPort.setText("1100");
            connectToServer(txtServerIp.getText().trim(), 1100);
        });
        inputs.add(btnQuickServer2);

        panel.add(inputs, BorderLayout.WEST);

        lblStatus = new JLabel("⚪ Chưa kết nối");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setForeground(Color.GRAY);
        panel.add(lblStatus, BorderLayout.EAST);

        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder(" 📝 Thông tin Sinh viên (Thêm, Sửa, Xóa) "),
                new EmptyBorder(8, 10, 8, 10)
        ));

        JPanel fieldGrid = new JPanel(new GridLayout(3, 2, 10, 8));

        fieldGrid.add(new JLabel("Mã Sinh viên (ID - Khóa chính):"));
        txtId = new JTextField();
        fieldGrid.add(txtId);

        fieldGrid.add(new JLabel("Họ và Tên:"));
        txtName = new JTextField();
        fieldGrid.add(txtName);

        fieldGrid.add(new JLabel("Thông tin / Chuyên ngành / Lớp:"));
        txtInfo = new JTextField();
        fieldGrid.add(txtInfo);

        panel.add(fieldGrid, BorderLayout.CENTER);

        // Nút bấm tác vụ CRUD
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));

        btnAdd = new JButton("➕ Thêm mới");
        btnAdd.setBackground(new Color(40, 167, 69));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.addActionListener(e -> handleAdd());
        actionPanel.add(btnAdd);

        btnUpdate = new JButton("✏️ Cập nhật");
        btnUpdate.setBackground(new Color(255, 193, 7));
        btnUpdate.setForeground(Color.BLACK);
        btnUpdate.addActionListener(e -> handleUpdate());
        actionPanel.add(btnUpdate);

        btnDelete = new JButton("🗑️ Xóa");
        btnDelete.setBackground(new Color(220, 53, 69));
        btnDelete.setForeground(Color.WHITE);
        btnDelete.addActionListener(e -> handleDelete());
        actionPanel.add(btnDelete);

        btnRefresh = new JButton("🔄 Làm mới (Xem)");
        btnRefresh.setBackground(new Color(23, 162, 184));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.addActionListener(e -> refreshData());
        actionPanel.add(btnRefresh);

        btnClearForm = new JButton("🧹 Xóa trắng form");
        btnClearForm.addActionListener(e -> clearInputs());
        actionPanel.add(btnClearForm);

        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder(" 📊 Danh sách Dữ liệu trên Server đang kết nối "),
                new EmptyBorder(6, 6, 6, 6)
        ));

        String[] columns = {"Mã SV (ID)", "Họ và Tên", "Thông tin chi tiết", "Thời gian cập nhật"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Chỉ cho phép xem và chọn
            }
        };

        studentTable = new JTable(tableModel);
        studentTable.setRowHeight(24);
        studentTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        studentTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Canh giữa cột ID và Thời gian
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        studentTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        studentTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        studentTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        studentTable.getColumnModel().getColumn(2).setPreferredWidth(280);
        studentTable.getColumnModel().getColumn(3).setPreferredWidth(160);
        studentTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        // Sự kiện: Bấm vào dòng trên bảng tự động fill thông tin lên các ô nhập liệu
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = studentTable.getSelectedRow();
                if (selectedRow >= 0) {
                    txtId.setText(tableModel.getValueAt(selectedRow, 0).toString());
                    txtName.setText(tableModel.getValueAt(selectedRow, 1).toString());
                    txtInfo.setText(tableModel.getValueAt(selectedRow, 2).toString());
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(studentTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder(" 📜 Nhật ký Sự kiện RMI & Trạng thái Đồng bộ "),
                new EmptyBorder(4, 6, 6, 6)
        ));

        txtLog = new JTextArea(5, 50);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtLog.setEditable(false);
        txtLog.setBackground(new Color(245, 247, 250));
        txtLog.setForeground(new Color(33, 37, 41));

        JScrollPane scrollPane = new JScrollPane(txtLog);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void log(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        txtLog.append("[" + timestamp + "] " + message + "\n");
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }

    // =========================================================================
    // XỬ LÝ KẾT NỐI RMI SERVER
    // =========================================================================

    public void connectToServer(String host, int port) {
        String serviceName = txtServiceName.getText().trim();
        log("Đang kết nối tới RMI Registry tại " + host + ":" + port + "...");

        new Thread(() -> {
            try {
                Registry registry = LocateRegistry.getRegistry(host, port);
                StudentSyncService service = (StudentSyncService) registry.lookup(serviceName);

                String serverInfo = service.getServerInfo();

                SwingUtilities.invokeLater(() -> {
                    this.syncService = service;
                    this.currentServerAddress = host + ":" + port;
                    lblStatus.setText("🟢 Đã kết nối: " + serverInfo);
                    lblStatus.setForeground(new Color(40, 167, 69));
                    log("✅ Kết nối thành công tới [" + serverInfo + "]!");
                    refreshData();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    this.syncService = null;
                    lblStatus.setText("🔴 Mất kết nối (" + host + ":" + port + ")");
                    lblStatus.setForeground(Color.RED);
                    log("❌ Lỗi kết nối tới " + host + ":" + port + ": " + e.getMessage());
                    showError("Không thể kết nối tới Server RMI tại " + host + ":" + port + "!\nChi tiết: " + e.getMessage());
                });
            }
        }).start();
    }

    // =========================================================================
    // XỬ LÝ CÁC THAO TÁC CRUD
    // =========================================================================

    private void handleAdd() {
        if (!checkConnection()) return;

        String id = txtId.getText().trim();
        String name = txtName.getText().trim();
        String info = txtInfo.getText().trim();

        if (id.isEmpty() || name.isEmpty()) {
            showError("Vui lòng nhập đầy đủ Mã sinh viên và Họ tên!");
            return;
        }

        Student student = new Student(id, name, info);

        new Thread(() -> {
            try {
                boolean ok = syncService.addStudent(student);
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        log("✅ [ADD THÀNH CÔNG] Đã thêm SV " + id + " trên " + currentServerAddress + " (Đã tự động replicate sang Server đối tác).");
                        clearInputs();
                        refreshData();
                    } else {
                        showError("Thêm sinh viên thất bại!");
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    log("❌ Lỗi RMI Add: " + e.getMessage());
                    showError("Lỗi gọi RMI: " + e.getMessage());
                });
            }
        }).start();
    }

    private void handleUpdate() {
        if (!checkConnection()) return;

        String id = txtId.getText().trim();
        String name = txtName.getText().trim();
        String info = txtInfo.getText().trim();

        if (id.isEmpty() || name.isEmpty()) {
            showError("Vui lòng chọn hoặc nhập Mã sinh viên cần cập nhật!");
            return;
        }

        Student student = new Student(id, name, info);

        new Thread(() -> {
            try {
                boolean ok = syncService.updateStudent(student);
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        log("✏️ [UPDATE THÀNH CÔNG] Đã cập nhật SV " + id + " trên " + currentServerAddress + " (Đã replicate sang đối tác).");
                        refreshData();
                    } else {
                        showError("Không tìm thấy sinh viên có mã " + id + " để cập nhật!");
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    log("❌ Lỗi RMI Update: " + e.getMessage());
                    showError("Lỗi gọi RMI: " + e.getMessage());
                });
            }
        }).start();
    }

    private void handleDelete() {
        if (!checkConnection()) return;

        String id = txtId.getText().trim();
        if (id.isEmpty()) {
            showError("Vui lòng nhập hoặc chọn Mã sinh viên cần xóa!");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc chắn muốn xóa sinh viên mã [" + id + "]?\nThao tác này cũng sẽ đồng bộ xóa trên Server đối tác!",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            try {
                boolean ok = syncService.deleteStudent(id);
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        log("🗑️ [DELETE THÀNH CÔNG] Đã xóa SV " + id + " trên " + currentServerAddress + " (Đã replicate sang đối tác).");
                        clearInputs();
                        refreshData();
                    } else {
                        showError("Không tìm thấy sinh viên mã " + id + " để xóa!");
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    log("❌ Lỗi RMI Delete: " + e.getMessage());
                    showError("Lỗi gọi RMI: " + e.getMessage());
                });
            }
        }).start();
    }

    private void refreshData() {
        if (syncService == null) return;

        new Thread(() -> {
            try {
                List<Student> list = syncService.getAllStudents();
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (Student s : list) {
                        tableModel.addRow(new Object[]{
                                s.getId(),
                                s.getName(),
                                s.getInfo(),
                                s.getLastUpdated()
                        });
                    }
                    log("🔄 Đã tải " + list.size() + " sinh viên từ Server (" + currentServerAddress + ").");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    log("⚠️ Không thể tải dữ liệu: " + e.getMessage());
                });
            }
        }).start();
    }

    private void clearInputs() {
        txtId.setText("");
        txtName.setText("");
        txtInfo.setText("");
        studentTable.clearSelection();
    }

    private boolean checkConnection() {
        if (syncService == null) {
            showError("Chưa kết nối tới Server RMI! Vui lòng kiểm tra IP/Port và bấm nút 'Kết nối'.");
            return false;
        }
        return true;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thông báo Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    // =========================================================================
    // HÀM MAIN KHỞI CHẠY CLIENT
    // =========================================================================

    public static void main(String[] args) {
        // Cố gắng thiết lập Look and Feel hiện đại (Nimbus hoặc System)
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            ClientForm form = new ClientForm();
            form.setVisible(true);
        });
    }
}
