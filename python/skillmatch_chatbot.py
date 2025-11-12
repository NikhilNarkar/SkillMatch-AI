import os
import google.generativeai as genai
import tkinter as tk
from tkinter import scrolledtext
from datetime import datetime

# Configuration
API_KEY = os.getenv("GEMINI_API_KEY", "").strip()
if not API_KEY:
    # Fallback to env var used by Google libs if provided
    API_KEY = os.getenv("GOOGLE_API_KEY", "").strip()

# Default to a broadly available model alias. You can change via env MODEL_NAME
MODEL_NAME = os.getenv("MODEL_NAME", "gemini-1.5-flash-latest")

# Modern Color Scheme (Dark Theme)
BG_PRIMARY = "#1a1d29"
BG_SECONDARY = "#252836"
BG_CHAT = "#2d3142"
ACCENT_PRIMARY = "#6366f1"
ACCENT_HOVER = "#4f46e5"
TEXT_PRIMARY = "#e2e8f0"
TEXT_SECONDARY = "#94a3b8"
USER_MSG_BG = "#6366f1"
BOT_MSG_BG = "#374151"
BORDER_COLOR = "#374151"
SUCCESS_COLOR = "#0d9614"
ERROR_COLOR = "#ef4444"


def chat_with_gemini(prompt: str) -> str:
    try:
        if not API_KEY:
            return "Error: GEMINI_API_KEY/GOOGLE_API_KEY environment variable is not set."
        genai.configure(api_key=API_KEY)
        model = genai.GenerativeModel(MODEL_NAME)
        response = model.generate_content(prompt)
        if hasattr(response, "text") and response.text:
            return response.text.strip()
        if hasattr(response, "candidates") and response.candidates:
            return response.candidates[0].content.parts[0].text.strip()
        return "No response from model."
    except Exception as e:
        err = str(e)
        if "404" in err or "not found" in err:
            return f"Error: Model {MODEL_NAME} not found or not supported."
        return f"Error: {err}"


def format_time() -> str:
    return datetime.now().strftime("%I:%M %p")


def send_message(event=None):
    user_input = user_entry.get().strip()
    if not user_input:
        return

    user_entry.delete(0, tk.END)

    chat_history.config(state=tk.NORMAL)
    time_str = format_time()
    chat_history.insert(tk.END, "\n")
    chat_history.insert(tk.END, f"You  •  {time_str}\n", "timestamp")
    chat_history.insert(tk.END, f"{user_input}\n", "user_msg")
    chat_history.config(state=tk.DISABLED)
    chat_history.yview(tk.END)

    root.update_idletasks()

    response = chat_with_gemini(user_input)

    chat_history.config(state=tk.NORMAL)
    time_str = format_time()
    chat_history.insert(tk.END, "\n")
    chat_history.insert(tk.END, f"AI Assistant  •  {time_str}\n", "timestamp")
    chat_history.insert(tk.END, f"{response}\n", "bot_msg")
    chat_history.config(state=tk.DISABLED)
    chat_history.yview(tk.END)


def clear_chat():
    chat_history.config(state=tk.NORMAL)
    chat_history.delete(1.0, tk.END)
    chat_history.insert(tk.END, "Welcome to SkillMatch AI ChatBot\n", "welcome")
    chat_history.insert(tk.END, "Personal AI assistant\n\n", "subtitle")
    chat_history.insert(tk.END, "How can I help you today?\n", "bot_msg")
    chat_history.config(state=tk.DISABLED)


def exit_chat():
    root.destroy()


def on_entry_focus_in(event):
    user_entry.config(relief=tk.FLAT, highlightbackground=ACCENT_PRIMARY, highlightcolor=ACCENT_PRIMARY)


def on_entry_focus_out(event):
    user_entry.config(highlightbackground=BORDER_COLOR)


# Create main window
root = tk.Tk()
root.title("SkillMatch AI Assistant")
root.geometry("900x700")
root.configure(bg=BG_PRIMARY)
root.resizable(True, True)

root.grid_rowconfigure(1, weight=1)
root.grid_columnconfigure(0, weight=1)

header_frame = tk.Frame(root, bg=BG_SECONDARY, height=80)
header_frame.grid(row=0, column=0, sticky="ew", padx=0, pady=0)
header_frame.grid_propagate(False)

title_label = tk.Label(header_frame, text="SkillMatch AI Assistant", font=("Segoe UI", 24, "bold"), bg=BG_SECONDARY, fg=TEXT_PRIMARY)
title_label.pack(side=tk.LEFT, padx=30, pady=10)

subtitle_label = tk.Label(header_frame, text="Hint - Ask for free interview question", font=("Segoe UI", 10), bg=BG_SECONDARY, fg=TEXT_SECONDARY)
subtitle_label.pack(side=tk.LEFT, padx=0, pady=10)

status_frame = tk.Frame(header_frame, bg=BG_SECONDARY)
status_frame.pack(side=tk.RIGHT, padx=30, pady=10)

status_dot = tk.Label(status_frame, text="●", font=("Segoe UI", 16), bg=BG_SECONDARY, fg=SUCCESS_COLOR)
status_dot.pack(side=tk.LEFT, padx=(0, 5))

status_label = tk.Label(status_frame, text="Online", font=("Segoe UI", 10), bg=BG_SECONDARY, fg=TEXT_SECONDARY)
status_label.pack(side=tk.LEFT)

content_frame = tk.Frame(root, bg=BG_PRIMARY)
content_frame.grid(row=1, column=0, sticky="nsew", padx=20, pady=20)
content_frame.grid_rowconfigure(0, weight=1)
content_frame.grid_columnconfigure(0, weight=1)

chat_container = tk.Frame(content_frame, bg=BORDER_COLOR, bd=0)
chat_container.grid(row=0, column=0, sticky="nsew", padx=2, pady=2)
chat_container.grid_rowconfigure(0, weight=1)
chat_container.grid_columnconfigure(0, weight=1)

chat_frame = tk.Frame(chat_container, bg=BG_CHAT)
chat_frame.grid(row=0, column=0, sticky="nsew")
chat_frame.grid_rowconfigure(0, weight=1)
chat_frame.grid_columnconfigure(0, weight=1)

chat_history = tk.Text(
    chat_frame,
    wrap=tk.WORD,
    state=tk.DISABLED,
    bg=BG_CHAT,
    fg=TEXT_PRIMARY,
    font=("Segoe UI", 11),
    padx=20,
    pady=20,
    relief=tk.FLAT,
    borderwidth=0,
    highlightthickness=0,
    spacing1=5,
    spacing3=5
)
chat_history.grid(row=0, column=0, sticky="nsew")

scrollbar = tk.Scrollbar(chat_frame, command=chat_history.yview, bg=BG_CHAT)
scrollbar.grid(row=0, column=1, sticky="ns")
chat_history.config(yscrollcommand=scrollbar.set)

chat_history.tag_config("welcome", font=("Segoe UI", 18, "bold"), foreground=TEXT_PRIMARY, spacing1=10)
chat_history.tag_config("subtitle", font=("Segoe UI", 10), foreground=TEXT_SECONDARY, spacing3=15)
chat_history.tag_config("timestamp", font=("Segoe UI", 9), foreground=TEXT_SECONDARY, spacing1=10, spacing3=3)
chat_history.tag_config("user_msg", font=("Segoe UI", 11), foreground=TEXT_PRIMARY, spacing3=10, lmargin1=20, lmargin2=20)
chat_history.tag_config("bot_msg", font=("Segoe UI", 11), foreground=TEXT_PRIMARY, spacing3=10, lmargin1=20, lmargin2=20)

clear_chat()

input_frame = tk.Frame(root, bg=BG_PRIMARY, height=100)
input_frame.grid(row=2, column=0, sticky="ew", padx=20, pady=(0, 20))

input_container = tk.Frame(input_frame, bg=BG_SECONDARY, bd=0)
input_container.pack(fill=tk.BOTH, expand=True, pady=(10, 0))

entry_frame = tk.Frame(input_container, bg=BG_SECONDARY)
entry_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, padx=20, pady=15)

user_entry = tk.Entry(
    entry_frame,
    font=("Segoe UI", 12),
    bg=BG_CHAT,
    fg=TEXT_PRIMARY,
    relief=tk.FLAT,
    insertbackground=TEXT_PRIMARY,
    highlightthickness=2,
    highlightbackground=BORDER_COLOR,
    highlightcolor=ACCENT_PRIMARY
)
user_entry.pack(fill=tk.BOTH, ipady=10, ipadx=10)
user_entry.bind("<Return>", send_message)
user_entry.bind("<FocusIn>", on_entry_focus_in)
user_entry.bind("<FocusOut>", on_entry_focus_out)
user_entry.focus()

button_frame = tk.Frame(input_container, bg=BG_SECONDARY)
button_frame.pack(side=tk.RIGHT, padx=(0, 20), pady=15)

send_button = tk.Button(
    button_frame,
    text="Send",
    command=send_message,
    font=("Segoe UI", 11, "bold"),
    bg=ACCENT_PRIMARY,
    fg="white",
    relief=tk.FLAT,
    padx=30,
    pady=10,
    cursor="hand2",
    activebackground=ACCENT_HOVER,
    activeforeground="white",
    borderwidth=0
)
send_button.pack(side=tk.LEFT, padx=5)

clear_button = tk.Button(
    button_frame,
    text="Clear",
    command=clear_chat,
    font=("Segoe UI", 11),
    bg=BG_CHAT,
    fg=TEXT_SECONDARY,
    relief=tk.FLAT,
    padx=20,
    pady=10,
    cursor="hand2",
    activebackground=BORDER_COLOR,
    activeforeground=TEXT_PRIMARY,
    borderwidth=0
)
clear_button.pack(side=tk.LEFT, padx=5)

exit_button = tk.Button(
    button_frame,
    text="Exit",
    command=exit_chat,
    font=("Segoe UI", 11),
    bg=BG_CHAT,
    fg=TEXT_SECONDARY,
    relief=tk.FLAT,
    padx=20,
    pady=10,
    cursor="hand2",
    activebackground=ERROR_COLOR,
    activeforeground="white",
    borderwidth=0
)
exit_button.pack(side=tk.LEFT, padx=5)

root.mainloop()



