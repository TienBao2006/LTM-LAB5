"""
Chương trình Mail Client - Điểm khởi chạy chính (Main Entry Point)
Đã được tách biệt rõ ràng thành:
- client_logic.py: Xử lý kết nối mạng Socket và nghiệp vụ (Code riêng)
- client_ui.py: Xử lý giao diện đồ họa Tkinter (Giao diện riêng)
"""

import tkinter as tk
from client_logic import ClientLogic
from client_ui import MailClientUI

# Bí danh tương thích ngược nếu có file khác import MailClient
MailClient = MailClientUI


def main():
    # Khởi tạo cửa sổ Tkinter chính
    root = tk.Tk()

    # Khởi tạo tầng logic xử lý mạng
    logic = ClientLogic(host="127.0.0.1", port=2345)

    # Khởi tạo tầng giao diện và liên kết với logic
    app = MailClientUI(root, logic=logic)

    # Chạy vòng lặp sự kiện chính của ứng dụng
    root.mainloop()


if __name__ == "__main__":
    main()