import socket
import threading
import os
import re
from datetime import datetime


HOST = "0.0.0.0"
PORT = 2345

BASE_DIR = "accounts"


# ==============================
# TẠO THƯ MỤC ACCOUNTS
# ==============================

os.makedirs(BASE_DIR, exist_ok=True)


# ==============================
# KIỂM TRA USERNAME
# ==============================

def valid_username(username):
    """
    Username chỉ được chứa:
    - chữ cái
    - số
    - dấu _
    """

    return re.fullmatch(r"[A-Za-z0-9_]+", username) is not None


# ==============================
# LẤY ĐƯỜNG DẪN TÀI KHOẢN
# ==============================

def account_path(username):
    return os.path.join(BASE_DIR, username)


# ==============================
# ĐĂNG KÝ TÀI KHOẢN
# ==============================

def create_account(username, password):

    if not valid_username(username):
        return "ERROR|Username chỉ được chứa chữ, số và _"

    if not username:
        return "ERROR|Username không được để trống"

    if not password:
        return "ERROR|Password không được để trống"

    path = account_path(username)

    # Tài khoản đã tồn tại
    if os.path.exists(path):
        return "ERROR|Tài khoản đã tồn tại"

    # Tạo thư mục tài khoản
    os.makedirs(path)

    # Lưu password
    with open(
        os.path.join(path, "password.txt"),
        "w",
        encoding="utf-8"
    ) as f:
        f.write(password)

    # Tạo file email chào mừng
    with open(
        os.path.join(path, "new_email.txt"),
        "w",
        encoding="utf-8"
    ) as f:

        f.write(
            "===== MAIL SERVER =====\n"
            "Chào mừng bạn đến với Mail Server!\n\n"
            f"Tài khoản: {username}\n"
            f"Thời gian tạo: {datetime.now()}\n"
        )

    print(f"[REGISTER] Tạo tài khoản: {username}")

    return "SUCCESS|Đăng ký tài khoản thành công"


# ==============================
# ĐĂNG NHẬP
# ==============================

def login(username, password):

    path = account_path(username)

    # Không tồn tại tài khoản
    if not os.path.exists(path):
        return "ERROR|Tài khoản không tồn tại"

    password_file = os.path.join(path, "password.txt")

    # Đọc password
    with open(
        password_file,
        "r",
        encoding="utf-8"
    ) as f:

        saved_password = f.read()

    # Sai password
    if password != saved_password:
        return "ERROR|Sai mật khẩu"

    # Lấy danh sách email
    files = []

    for filename in os.listdir(path):

        if filename.endswith(".txt") and filename != "password.txt":
            files.append(filename)

    print(f"[LOGIN] {username} đăng nhập")

    return "SUCCESS|" + "|".join(files)


# ==============================
# GỬI EMAIL
# ==============================

def send_email(sender, receiver, content):

    sender_path = account_path(sender)
    receiver_path = account_path(receiver)

    # Kiểm tra người gửi
    if not os.path.exists(sender_path):
        return "ERROR|Tài khoản người gửi không tồn tại"

    # Kiểm tra người nhận
    if not os.path.exists(receiver_path):
        return "ERROR|Tài khoản người nhận không tồn tại"

    # Tìm số email tiếp theo
    number = 1

    while True:

        filename = f"email_{number:03d}.txt"

        filepath = os.path.join(
            receiver_path,
            filename
        )

        if not os.path.exists(filepath):
            break

        number += 1

    # Nội dung email
    email_content = (
        f"From: {sender}\n"
        f"To: {receiver}\n"
        f"Time: {datetime.now()}\n"
        f"\n"
        f"{content}"
    )

    # Lưu email
    with open(
        filepath,
        "w",
        encoding="utf-8"
    ) as f:

        f.write(email_content)

    print(
        f"[SEND] {sender} -> {receiver}: {filename}"
    )

    return f"SUCCESS|Gửi email thành công|{filename}"


# ==============================
# LẤY NỘI DUNG EMAIL
# ==============================

def get_email(username, filename):

    path = account_path(username)

    if not os.path.exists(path):
        return "ERROR|Tài khoản không tồn tại"

    # Không cho truy cập file ngoài thư mục
    filename = os.path.basename(filename)

    filepath = os.path.join(
        path,
        filename
    )

    if not os.path.exists(filepath):
        return "ERROR|Email không tồn tại"

    with open(
        filepath,
        "r",
        encoding="utf-8"
    ) as f:

        content = f.read()

    return "SUCCESS|" + content


# ==============================
# XỬ LÝ REQUEST
# ==============================

def handle_request(request):

    try:

        parts = request.split("|")

        command = parts[0]

        # ----------------------
        # CREATE_ACCOUNT
        # ----------------------

        if command == "CREATE_ACCOUNT":

            if len(parts) != 3:
                return "ERROR|Dữ liệu đăng ký không hợp lệ"

            username = parts[1]
            password = parts[2]

            return create_account(
                username,
                password
            )

        # ----------------------
        # LOGIN
        # ----------------------

        elif command == "LOGIN":

            if len(parts) != 3:
                return "ERROR|Dữ liệu đăng nhập không hợp lệ"

            username = parts[1]
            password = parts[2]

            return login(
                username,
                password
            )

        # ----------------------
        # SEND_EMAIL
        # ----------------------

        elif command == "SEND_EMAIL":

            if len(parts) != 4:
                return "ERROR|Dữ liệu email không hợp lệ"

            sender = parts[1]
            receiver = parts[2]
            content = parts[3]

            return send_email(
                sender,
                receiver,
                content
            )

        # ----------------------
        # GET_EMAIL
        # ----------------------

        elif command == "GET_EMAIL":

            if len(parts) != 3:
                return "ERROR|Dữ liệu không hợp lệ"

            username = parts[1]
            filename = parts[2]

            return get_email(
                username,
                filename
            )

        # ----------------------
        # COMMAND KHÔNG TỒN TẠI
        # ----------------------

        else:

            return "ERROR|Không biết yêu cầu này"

    except Exception as e:

        print("Server error:", e)

        return "ERROR|Lỗi server"


# ==============================
# XỬ LÝ CLIENT
# ==============================

def handle_client(conn, address):

    print(f"[CONNECT] Client: {address}")

    try:

        while True:

            data = conn.recv(8192)

            if not data:
                break

            request = data.decode("utf-8")

            print(
                f"[REQUEST] {address}: {request}"
            )

            response = handle_request(request)

            conn.send(
                response.encode("utf-8")
            )

    except Exception as e:

        print(
            f"[ERROR] Client {address}: {e}"
        )

    finally:

        conn.close()

        print(
            f"[DISCONNECT] Client: {address}"
        )


# ==============================
# CHẠY SERVER
# ==============================

def start_server():

    server = socket.socket(
        socket.AF_INET,
        socket.SOCK_STREAM
    )

    server.setsockopt(
        socket.SOL_SOCKET,
        socket.SO_REUSEADDR,
        1
    )

    server.bind(
        (HOST, PORT)
    )

    server.listen(10)

    print("==============================")
    print("       MAIL SERVER")
    print("==============================")
    print(f"Server đang chạy tại port {PORT}")
    print("Đang chờ client...")
    print()

    while True:

        conn, address = server.accept()

        thread = threading.Thread(
            target=handle_client,
            args=(conn, address),
            daemon=True
        )

        thread.start()


if __name__ == "__main__":
    start_server()