import tkinter as tk
from tkinter import messagebox
from client_logic import ClientLogic


class MailClientUI:
    """
    Lớp giao diện người dùng (GUI) cho Mail Client bằng Tkinter.
    Chỉ tập trung vào hiển thị, bố cục widgets và bắt sự kiện người dùng.
    Mọi thao tác nghiệp vụ và mạng đều ủy thác qua đối tượng ClientLogic.
    """

    def __init__(self, root: tk.Tk, logic: ClientLogic = None):
        self.root = root
        self.logic = logic if logic is not None else ClientLogic()

        self.root.title("Mail Client - Ứng dụng Quản lý Email")
        self.root.geometry("700x520")
        self.root.minsize(600, 450)

        # Lưu trạng thái phiên làm việc của người dùng hiện tại
        self.username = ""
        self.password = ""

        # Khởi tạo màn hình đầu tiên: Đăng nhập
        self.build_login()

    def clear_window(self):
        """Xóa toàn bộ widget con trên cửa sổ chính để chuyển màn hình."""
        for widget in self.root.winfo_children():
            widget.destroy()

    # ==========================================================
    # MÀN HÌNH ĐĂNG NHẬP / ĐĂNG KÝ
    # ==========================================================

    def _apply_server_config(self):
        """Lấy cấu hình IP và Port máy chủ từ giao diện và cập nhật vào logic."""
        host = self.server_ip_entry.get().strip()
        port_str = self.server_port_entry.get().strip()

        if not host:
            messagebox.showwarning("Thông báo", "Vui lòng nhập địa chỉ IP máy chủ.")
            self.server_ip_entry.focus()
            return False

        if not port_str:
            messagebox.showwarning("Thông báo", "Vui lòng nhập cổng (Port) máy chủ.")
            self.server_port_entry.focus()
            return False

        try:
            port = int(port_str)
            if port < 1 or port > 65535:
                raise ValueError()
        except ValueError:
            messagebox.showerror("Lỗi", "Cổng (Port) phải là một số nguyên từ 1 đến 65535.")
            self.server_port_entry.focus()
            return False

        self.logic.set_server(host, port)
        return True

    def build_login(self):
        """Xây dựng giao diện đăng nhập và đăng ký."""
        self.clear_window()

        container = tk.Frame(self.root, padx=30, pady=20)
        container.pack(expand=True)

        # Tiêu đề
        title_label = tk.Label(
            container,
            text="MAIL CLIENT",
            font=("Arial", 22, "bold"),
            fg="#1a73e8"
        )
        title_label.pack(pady=(0, 15))

        # Khung chứa form nhập liệu
        form_frame = tk.Frame(container)
        form_frame.pack(fill="x", pady=5)

        # Cấu hình Server (IP và Port)
        server_frame = tk.Frame(form_frame)
        server_frame.pack(fill="x", pady=(0, 10))

        # IP máy chủ
        ip_frame = tk.Frame(server_frame)
        ip_frame.pack(side="left", fill="x", expand=True, padx=(0, 6))

        tk.Label(
            ip_frame,
            text="Địa chỉ IP Máy chủ:",
            font=("Arial", 10, "bold")
        ).pack(anchor="w", pady=(0, 3))

        self.server_ip_entry = tk.Entry(
            ip_frame,
            width=24,
            font=("Arial", 11)
        )
        self.server_ip_entry.insert(0, getattr(self.logic, "host", "127.0.0.1"))
        self.server_ip_entry.pack(fill="x", ipady=3)

        # Cổng Port
        port_frame = tk.Frame(server_frame)
        port_frame.pack(side="right")

        tk.Label(
            port_frame,
            text="Cổng (Port):",
            font=("Arial", 10, "bold")
        ).pack(anchor="w", pady=(0, 3))

        self.server_port_entry = tk.Entry(
            port_frame,
            width=8,
            font=("Arial", 11)
        )
        self.server_port_entry.insert(0, str(getattr(self.logic, "port", 2345)))
        self.server_port_entry.pack(fill="x", ipady=3)

        # Tên đăng nhập
        tk.Label(
            form_frame,
            text="Tên đăng nhập (Username):",
            font=("Arial", 10, "bold")
        ).pack(anchor="w", pady=(0, 3))

        self.username_entry = tk.Entry(
            form_frame,
            width=36,
            font=("Arial", 11)
        )
        self.username_entry.pack(pady=(0, 10), ipady=3)
        self.username_entry.focus()

        # Mật khẩu
        tk.Label(
            form_frame,
            text="Mật khẩu (Password):",
            font=("Arial", 10, "bold")
        ).pack(anchor="w", pady=(0, 3))

        self.password_entry = tk.Entry(
            form_frame,
            width=36,
            font=("Arial", 11),
            show="*"
        )
        self.password_entry.pack(pady=(0, 15), ipady=3)

        # Điều hướng bằng phím Enter
        self.server_ip_entry.bind("<Return>", lambda event: self.server_port_entry.focus())
        self.server_port_entry.bind("<Return>", lambda event: self.username_entry.focus())
        self.username_entry.bind("<Return>", lambda event: self.password_entry.focus())
        self.password_entry.bind("<Return>", lambda event: self.on_login())

        # Các nút bấm hành động
        btn_frame = tk.Frame(container)
        btn_frame.pack(pady=10)

        login_btn = tk.Button(
            btn_frame,
            text="Đăng nhập",
            width=14,
            bg="#1a73e8",
            fg="white",
            font=("Arial", 10, "bold"),
            relief="groove",
            command=self.on_login
        )
        login_btn.pack(side="left", padx=8)

        register_btn = tk.Button(
            btn_frame,
            text="Đăng ký",
            width=14,
            bg="#f1f3f4",
            fg="#202124",
            font=("Arial", 10),
            relief="groove",
            command=self.on_register
        )
        register_btn.pack(side="left", padx=8)

    def on_register(self):
        """Xử lý sự kiện khi người dùng bấm nút Đăng ký."""
        if not self._apply_server_config():
            return

        username = self.username_entry.get().strip()
        password = self.password_entry.get().strip()

        success, msg = self.logic.register(username, password)
        if success:
            messagebox.showinfo("Thành công", msg)
        else:
            messagebox.showerror("Đăng ký thất bại", msg)

    def on_login(self):
        """Xử lý sự kiện khi người dùng bấm nút Đăng nhập."""
        if not self._apply_server_config():
            return

        username = self.username_entry.get().strip()
        password = self.password_entry.get().strip()

        success, msg, email_files = self.logic.login(username, password)
        if success:
            self.username = username
            self.password = password
            messagebox.showinfo("Đăng nhập", "Đăng nhập thành công!")
            self.build_mailbox(email_files)
        else:
            messagebox.showerror("Đăng nhập thất bại", msg)

    # ==========================================================
    # MÀN HÌNH HỘP THƯ (MAILBOX)
    # ==========================================================

    def build_mailbox(self, email_files):
        """Xây dựng giao diện danh sách email hộp thư đến."""
        self.clear_window()

        # Thanh tiêu đề trên cùng (Header)
        header_frame = tk.Frame(self.root, bg="#f8f9fa", padx=15, pady=10)
        header_frame.pack(fill="x")

        welcome_lbl = tk.Label(
            header_frame,
            text=f"Xin chào: {self.username}  |  Server: {self.logic.host}:{self.logic.port}",
            font=("Arial", 11, "bold"),
            bg="#f8f9fa",
            fg="#202124"
        )
        welcome_lbl.pack(side="left")

        logout_btn = tk.Button(
            header_frame,
            text="Đăng xuất",
            font=("Arial", 9),
            command=self.on_logout
        )
        logout_btn.pack(side="right")

        # Khung hiển thị danh sách email
        content_frame = tk.Frame(self.root, padx=15, pady=10)
        content_frame.pack(fill="both", expand=True)

        list_title = tk.Label(
            content_frame,
            text="Danh sách hộp thư:",
            font=("Arial", 11, "bold")
        )
        list_title.pack(anchor="w", pady=(0, 5))

        listbox_container = tk.Frame(content_frame)
        listbox_container.pack(fill="both", expand=True)

        scrollbar = tk.Scrollbar(listbox_container, orient="vertical")
        self.email_list = tk.Listbox(
            listbox_container,
            yscrollcommand=scrollbar.set,
            font=("Consolas", 10),
            selectbackground="#c2dbff",
            selectforeground="black",
            activestyle="none"
        )
        scrollbar.config(command=self.email_list.yview)
        scrollbar.pack(side="right", fill="y")
        self.email_list.pack(side="left", fill="both", expand=True)

        # Gán sự kiện double-click vào email để mở trực tiếp
        self.email_list.bind("<Double-Button-1>", lambda event: self.on_open_email())

        # Điền danh sách file email vào Listbox
        self._populate_email_list(email_files)

        # Khung chứa các nút thao tác phía dưới
        btn_bar = tk.Frame(self.root, pady=12)
        btn_bar.pack(fill="x")

        tk.Button(
            btn_bar,
            text="Mở email",
            width=14,
            bg="#1a73e8",
            fg="white",
            font=("Arial", 10, "bold"),
            command=self.on_open_email
        ).pack(side="left", padx=10)

        tk.Button(
            btn_bar,
            text="Soạn email",
            width=14,
            font=("Arial", 10),
            command=self.on_compose_email
        ).pack(side="left", padx=5)

        tk.Button(
            btn_bar,
            text="Làm mới",
            width=14,
            font=("Arial", 10),
            command=self.on_refresh
        ).pack(side="left", padx=5)

    def _populate_email_list(self, email_files):
        """Điền danh sách tên file email vào Listbox."""
        self.email_list.delete(0, tk.END)
        if not email_files:
            return

        for filename in email_files:
            self.email_list.insert(tk.END, filename)

    # ==========================================================
    # CỬA SỔ XEM NỘI DUNG EMAIL
    # ==========================================================

    def on_open_email(self):
        """Mở cửa sổ hiển thị nội dung email được chọn."""
        selection = self.email_list.curselection()
        if not selection:
            messagebox.showwarning("Thông báo", "Vui lòng chọn một email để mở.")
            return

        filename = self.email_list.get(selection[0])
        success, content = self.logic.get_email_content(self.username, filename)

        if not success:
            messagebox.showerror("Lỗi", content)
            return

        # Cửa sổ chi tiết email
        detail_win = tk.Toplevel(self.root)
        detail_win.title(f"Email: {filename}")
        detail_win.geometry("620x450")
        detail_win.minsize(450, 300)

        # Header thông tin file
        header = tk.Frame(detail_win, bg="#e8f0fe", padx=10, pady=8)
        header.pack(fill="x")
        tk.Label(
            header,
            text=f"Tập tin: {filename}",
            font=("Arial", 10, "bold"),
            bg="#e8f0fe",
            fg="#1a73e8"
        ).pack(anchor="w")

        text_frame = tk.Frame(detail_win, padx=10, pady=10)
        text_frame.pack(fill="both", expand=True)

        scrollbar = tk.Scrollbar(text_frame)
        scrollbar.pack(side="right", fill="y")

        text_area = tk.Text(
            text_frame,
            wrap="word",
            font=("Consolas", 10),
            yscrollcommand=scrollbar.set
        )
        scrollbar.config(command=text_area.yview)
        text_area.pack(side="left", fill="both", expand=True)

        text_area.insert("1.0", content)
        text_area.config(state="disabled")

        # Nút đóng
        tk.Button(
            detail_win,
            text="Đóng",
            width=12,
            command=detail_win.destroy
        ).pack(pady=(0, 10))

    # ==========================================================
    # CỬA SỔ SOẠN EMAIL
    # ==========================================================

    def on_compose_email(self):
        """Mở cửa sổ soạn và gửi email mới."""
        compose_win = tk.Toplevel(self.root)
        compose_win.title("Soạn thư mới")
        compose_win.geometry("540x450")
        compose_win.minsize(450, 350)

        # Người nhận
        tk.Label(
            compose_win,
            text="Người nhận (Receiver username):",
            font=("Arial", 10, "bold")
        ).pack(anchor="w", padx=12, pady=(12, 4))

        receiver_entry = tk.Entry(compose_win, font=("Arial", 10))
        receiver_entry.pack(fill="x", padx=12, ipady=3)
        receiver_entry.focus()

        # Nội dung
        tk.Label(
            compose_win,
            text="Nội dung thư:",
            font=("Arial", 10, "bold")
        ).pack(anchor="w", padx=12, pady=(12, 4))

        content_text = tk.Text(
            compose_win,
            font=("Arial", 10),
            wrap="word"
        )
        content_text.pack(fill="both", expand=True, padx=12, pady=5)

        def handle_send():
            receiver = receiver_entry.get().strip()
            content = content_text.get("1.0", tk.END).strip()

            success, msg = self.logic.send_email(self.username, receiver, content)
            if success:
                messagebox.showinfo("Thành công", msg)
                compose_win.destroy()
                # Tự động làm mới lại danh sách thư phòng trường hợp gửi cho chính mình
                if receiver == self.username:
                    self.on_refresh(show_alert=False)
            else:
                messagebox.showerror("Gửi thất bại", msg)

        # Thanh nút bấm
        btn_frame = tk.Frame(compose_win, pady=10)
        btn_frame.pack(fill="x")

        send_btn = tk.Button(
            btn_frame,
            text="Gửi thư",
            width=14,
            bg="#1a73e8",
            fg="white",
            font=("Arial", 10, "bold"),
            command=handle_send
        )
        send_btn.pack(side="right", padx=12)

        cancel_btn = tk.Button(
            btn_frame,
            text="Hủy",
            width=10,
            command=compose_win.destroy
        )
        cancel_btn.pack(side="right", padx=5)

    # ==========================================================
    # CÁC THAO TÁC KHÁC: LÀM MỚI, ĐĂNG XUẤT
    # ==========================================================

    def on_refresh(self, show_alert=True):
        """Làm mới danh sách hộp thư từ server."""
        if not self.username or not self.password:
            messagebox.showinfo("Thông báo", "Vui lòng đăng nhập lại để làm mới.")
            return

        success, msg, files = self.logic.get_email_list(self.username, self.password)
        if success:
            self._populate_email_list(files)
            if show_alert:
                messagebox.showinfo("Làm mới", "Đã cập nhật danh sách email mới nhất!")
        else:
            if show_alert:
                messagebox.showerror("Lỗi", f"Không thể làm mới:\n{msg}")

    def on_logout(self):
        """Đăng xuất người dùng hiện tại và trở về màn hình đăng nhập."""
        self.username = ""
        self.password = ""
        self.build_login()


if __name__ == "__main__":
    root = tk.Tk()
    app = MailClientUI(root)
    root.mainloop()
