import socket


class ClientLogic:
    """
    Lớp xử lý logic nghiệp vụ và giao tiếp mạng cho Mail Client.
    Tách biệt hoàn toàn với giao diện người dùng (không phụ thuộc tkinter).
    """

    def __init__(self, host="127.0.0.1", port=2345):
        self.host = host
        self.port = port

    def set_server(self, host: str, port: int = 2345):
        """Cập nhật địa chỉ IP và Port của máy chủ."""
        self.host = host.strip()
        self.port = int(port)

    def send_request(self, request: str):
        """
        Gửi yêu cầu chuỗi dạng văn bản đến server qua TCP Socket và nhận phản hồi.
        Trả về: (thành công: bool, phản hồi server hoặc thông báo lỗi: str)
        """
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(5.0)  # Timeout 5 giây tránh treo ứng dụng
            sock.connect((self.host, self.port))
            sock.sendall(request.encode("utf-8"))
            data = sock.recv(8192)
            sock.close()
            return True, data.decode("utf-8")
        except socket.timeout:
            return False, f"Hết thời gian chờ phản hồi từ server ({self.host}:{self.port})"
        except Exception as e:
            return False, f"Không thể kết nối đến server ({self.host}:{self.port}):\n{e}"

    def register(self, username: str, password: str):
        """
        Gửi yêu cầu đăng ký tài khoản mới tới server.
        Giao thức: CREATE_ACCOUNT|<username>|<password>
        Trả về: (thành công: bool, thông điệp: str)
        """
        username = username.strip()
        password = password.strip()

        if not username or not password:
            return False, "Vui lòng nhập đầy đủ username và password."

        request = f"CREATE_ACCOUNT|{username}|{password}"
        success, response = self.send_request(request)

        if not success:
            return False, response

        parts = response.split("|", 1)
        if parts[0] == "SUCCESS":
            msg = parts[1] if len(parts) > 1 else "Đăng ký tài khoản thành công!"
            return True, msg
        else:
            msg = parts[1] if len(parts) > 1 else "Đăng ký thất bại."
            return False, msg

    def login(self, username: str, password: str):
        """
        Gửi yêu cầu đăng nhập tài khoản tới server.
        Giao thức: LOGIN|<username>|<password>
        Trả về: (thành công: bool, thông báo: str, danh sách file email: list[str])
        """
        username = username.strip()
        password = password.strip()

        if not username or not password:
            return False, "Vui lòng nhập đầy đủ username và password.", []

        request = f"LOGIN|{username}|{password}"
        success, response = self.send_request(request)

        if not success:
            return False, response, []

        parts = response.split("|")
        if parts[0] == "SUCCESS":
            # Lọc bỏ các chuỗi rỗng nếu tài khoản chưa có email
            email_files = [f for f in parts[1:] if f]
            return True, "Đăng nhập thành công!", email_files
        else:
            error_msg = parts[1] if len(parts) > 1 else "Đăng nhập thất bại."
            return False, error_msg, []

    def get_email_list(self, username: str, password: str):
        """
        Lấy lại danh sách email mới nhất từ server thông qua thông tin đăng nhập.
        Trả về: (thành công: bool, thông báo: str, danh sách file email: list[str])
        """
        return self.login(username, password)

    def get_email_content(self, username: str, filename: str):
        """
        Gửi yêu cầu lấy nội dung chi tiết của một email.
        Giao thức: GET_EMAIL|<username>|<filename>
        Trả về: (thành công: bool, nội dung email hoặc thông báo lỗi: str)
        """
        filename = filename.strip()
        if not filename:
            return False, "Vui lòng chọn một email để xem."

        request = f"GET_EMAIL|{username}|{filename}"
        success, response = self.send_request(request)

        if not success:
            return False, response

        parts = response.split("|", 1)
        if parts[0] == "SUCCESS":
            content = parts[1] if len(parts) > 1 else "(Email không có nội dung)"
            return True, content
        else:
            error_msg = parts[1] if len(parts) > 1 else "Không thể mở email."
            return False, error_msg

    def send_email(self, sender: str, receiver: str, content: str):
        """
        Gửi email từ người gửi đến người nhận.
        Giao thức: SEND_EMAIL|<sender>|<receiver>|<content>
        Trả về: (thành công: bool, thông báo: str)
        """
        sender = sender.strip()
        receiver = receiver.strip()
        content = content.strip()

        if not receiver:
            return False, "Vui lòng nhập người nhận."
        if not content:
            return False, "Vui lòng nhập nội dung email."

        request = f"SEND_EMAIL|{sender}|{receiver}|{content}"
        success, response = self.send_request(request)

        if not success:
            return False, response

        parts = response.split("|")
        if parts[0] == "SUCCESS":
            msg = parts[1] if len(parts) > 1 else "Gửi email thành công!"
            return True, msg
        else:
            error_msg = parts[1] if len(parts) > 1 else "Gửi email thất bại."
            return False, error_msg
