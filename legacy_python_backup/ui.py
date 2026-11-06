"""
File bí danh giao diện (UI) - ánh xạ trực tiếp sang client_ui.py
"""

from client_ui import MailClientUI

__all__ = ["MailClientUI"]

if __name__ == "__main__":
    import tkinter as tk
    root = tk.Tk()
    app = MailClientUI(root)
    root.mainloop()
