"""Modern dark desktop apps (CustomTkinter) for every template.

Same placeholders as the web templates: {{APP_NAME}}, {{ACCENT}}, {{IDEA}}.
Bundled into a one-file .exe with: pyinstaller --onefile --windowed
--collect-all customtkinter
"""

_HEAD = '''"""{name} - desktop edition."""
import customtkinter as ctk

ctk.set_appearance_mode("dark")
ctk.set_default_color_theme("dark-blue")
BG = "#170B16"
CARD = "#241423"
ACCENT = "{accent}"
'''

BLOG_WIN = _HEAD + '''
POSTS = [
    ("Hello World", "Welcome to {{APP_NAME}}! This starter post shows how the reader works."),
    ("Getting Started", "Select a post on the left to read it here. Search filters the list."),
    ("Tips & Tricks", "Rename the app, change the theme, and publish your own version."),
    ("Offline First", "Everything runs on-device. No account, no tracking."),
]

app = ctk.CTk()
app.title("{{APP_NAME}}")
app.geometry("800x520")
app.configure(fg_color=BG)

left = ctk.CTkFrame(app, width=240, fg_color=CARD, corner_radius=14)
left.pack(side="left", fill="y", padx=10, pady=10)
ctk.CTkLabel(left, text="{{APP_NAME}}", font=ctk.CTkFont(size=16, weight="bold"),
             text_color=ACCENT).pack(pady=12)
search = ctk.CTkEntry(left, placeholder_text="🔍 Search...")
search.pack(fill="x", padx=12)
lst = ctk.CTkScrollableFrame(left, fg_color="transparent")
lst.pack(fill="both", expand=True, padx=6, pady=10)

right = ctk.CTkFrame(app, fg_color="transparent")
right.pack(side="right", fill="both", expand=True, padx=10, pady=10)
title = ctk.CTkLabel(right, text="", font=ctk.CTkFont(size=20, weight="bold"),
                     text_color=ACCENT)
title.pack(anchor="w", pady=(10, 4))
body = ctk.CTkTextbox(right, wrap="word", font=ctk.CTkFont(size=14),
                      fg_color=CARD, corner_radius=14)
body.pack(fill="both", expand=True)


def show(t, b):
    title.configure(text=t)
    body.delete("1.0", "end")
    body.insert("1.0", b)


def refresh(*_):
    q = search.get().lower()
    for w in lst.winfo_children():
        w.destroy()
    first = None
    for t, b in POSTS:
        if q in t.lower():
            if first is None:
                first = (t, b)
            ctk.CTkButton(lst, text=t, anchor="w", fg_color="transparent",
                          text_color="white", hover_color=CARD,
                          command=lambda t=t, b=b: show(t, b)).pack(
                              fill="x", pady=2)
    if first:
        show(*first)


search.bind("<KeyRelease>", refresh)
refresh()
app.mainloop()
'''

CALC_WIN = _HEAD + '''
app = ctk.CTk()
app.title("{{APP_NAME}}")
app.geometry("330x520")
app.configure(fg_color=BG)
app.resizable(False, False)
expr = ctk.StringVar(value="0")

ctk.CTkLabel(app, text="{{APP_NAME}}", font=ctk.CTkFont(size=15, weight="bold"),
             text_color=ACCENT).pack(pady=(14, 4))
disp = ctk.CTkEntry(app, textvariable=expr, font=ctk.CTkFont(size=30),
                    justify="right", height=64, fg_color=CARD,
                    border_width=0, corner_radius=14)
disp.pack(fill="x", padx=14, pady=8)
grid = ctk.CTkFrame(app, fg_color="transparent")
grid.pack(padx=14, pady=6)


def on(b):
    v = expr.get()
    if b == "C":
        expr.set("0")
    elif b == "=":
        try:
            expr.set(str(eval(v, {"__builtins__": {}}, {})))
        except Exception:
            expr.set("Error")
    elif b == "\\u232b":
        expr.set(v[:-1] or "0")
    else:
        expr.set(b if v in ("0", "Error") else v + b)


btns = ["C", "(", ")", "/",
        "7", "8", "9", "*",
        "4", "5", "6", "-",
        "1", "2", "3", "+",
        "0", ".", "\\u232b", "="]
for i, b in enumerate(btns):
    ctk.CTkButton(grid, text=b, width=64, height=52,
                  font=ctk.CTkFont(size=17, weight="bold"),
                  fg_color=ACCENT if b in "/*-+()" else (
                      "#4AA3FF" if b == "=" else CARD),
                  text_color="#170B16" if b in "/*-+()=" else "white",
                  hover_color="#B985C4" if b in "/*-+()=" else "#453055",
                  corner_radius=12,
                  command=lambda b=b: on(b)).grid(
                      row=i // 4, column=i % 4, padx=4, pady=4)
app.mainloop()
'''

NOTES_WIN = _HEAD + '''
import json
from pathlib import Path

DB = Path.home() / ".{{APP_NAME}}_notes.json".replace(" ", "_").lower()


def load():
    try:
        return json.loads(DB.read_text(encoding="utf-8"))
    except Exception:
        return []


def save():
    DB.write_text(json.dumps(notes, ensure_ascii=False), encoding="utf-8")


notes = load()
sel = {"i": None}

app = ctk.CTk()
app.title("{{APP_NAME}}")
app.geometry("560x540")
app.configure(fg_color=BG)

ctk.CTkLabel(app, text="📝 {{APP_NAME}}",
             font=ctk.CTkFont(size=17, weight="bold"),
             text_color=ACCENT).pack(pady=(14, 6))
entry = ctk.CTkTextbox(app, height=90, fg_color=CARD, corner_radius=14,
                       font=ctk.CTkFont(size=13))
entry.pack(fill="x", padx=14)

bar = ctk.CTkFrame(app, fg_color="transparent")
bar.pack(fill="x", padx=14, pady=8)
lst = ctk.CTkScrollableFrame(app, fg_color=CARD, corner_radius=14)
lst.pack(fill="both", expand=True, padx=14, pady=(0, 14))


def refresh():
    for w in lst.winfo_children():
        w.destroy()
    for i, n in enumerate(notes):
        ctk.CTkButton(lst, text=n[:80], anchor="w", fg_color=(
            ACCENT if sel["i"] == i else "transparent"),
            text_color="#170B16" if sel["i"] == i else "white",
            hover_color="#453055",
            command=lambda i=i: (sel.update(i=i), refresh())).pack(
                fill="x", pady=2)


def add():
    v = entry.get("1.0", "end").strip()
    if v:
        notes.insert(0, v)
        save()
        entry.delete("1.0", "end")
        sel["i"] = None
        refresh()


def delete():
    if sel["i"] is not None and 0 <= sel["i"] < len(notes):
        notes.pop(sel["i"])
        save()
        sel["i"] = None
        refresh()


ctk.CTkButton(bar, text="+ Add", fg_color=ACCENT, text_color="#170B16",
              font=ctk.CTkFont(weight="bold"),
              command=add).pack(side="left", expand=True, fill="x", padx=3)
ctk.CTkButton(bar, text="Delete", fg_color="#5A1E2E",
              command=delete).pack(side="left", expand=True, fill="x", padx=3)
refresh()
app.mainloop()
'''

DICE_WIN = _HEAD + '''
import random

FACES = ["\\u2680", "\\u2681", "\\u2682", "\\u2683", "\\u2684", "\\u2685"]

app = ctk.CTk()
app.title("{{APP_NAME}}")
app.geometry("360x440")
app.configure(fg_color=BG)

ctk.CTkLabel(app, text="🎲 {{APP_NAME}}",
             font=ctk.CTkFont(size=18, weight="bold"),
             text_color=ACCENT).pack(pady=(16, 4))
face = ctk.CTkLabel(app, text="\\U0001f3b2", font=ctk.CTkFont(size=130))
face.pack()
hist = ctk.CTkLabel(app, text="History: —", text_color="#9C6BA8",
                    font=ctk.CTkFont(size=13))
hist.pack(pady=4)
rolls = []


def roll(n=0):
    face.configure(text=random.choice(FACES))
    if n < 8:
        app.after(70, lambda: roll(n + 1))
    else:
        v = random.randint(1, 6)
        face.configure(text=FACES[v - 1])
        rolls.insert(0, v)
        del rolls[12:]
        hist.configure(text="History: " + " · ".join(map(str, rolls)))


ctk.CTkButton(app, text="ROLL", width=180, height=48,
              font=ctk.CTkFont(size=16, weight="bold"),
              fg_color=ACCENT, text_color="#170B16",
              hover_color="#B985C4", corner_radius=14,
              command=roll).pack(pady=10)
app.mainloop()
'''

WEATHER_WIN = _HEAD + '''
import json
import urllib.request

CITIES = {"Amsterdam": (52.37, 4.89), "Tehran": (35.69, 51.38),
          "London": (51.5, -0.12), "New York": (40.71, -74.0)}

app = ctk.CTk()
app.title("{{APP_NAME}}")
app.geometry("380x470")
app.configure(fg_color=BG)

ctk.CTkLabel(app, text="⛅ {{APP_NAME}}",
             font=ctk.CTkFont(size=18, weight="bold"),
             text_color=ACCENT).pack(pady=(16, 6))
city = ctk.StringVar(value="Amsterdam")
ctk.CTkOptionMenu(app, variable=city, values=list(CITIES),
                  fg_color=CARD, button_color=ACCENT,
                  button_hover_color="#B985C4").pack(pady=4)
icon = ctk.CTkLabel(app, text="⛅", font=ctk.CTkFont(size=80))
icon.pack()
temp = ctk.CTkLabel(app, text="--°", font=ctk.CTkFont(size=54, weight="bold"))
temp.pack()
desc = ctk.CTkLabel(app, text="", text_color="#9C6BA8")
desc.pack()


def refresh():
    la, lo = CITIES[city.get()]
    try:
        url = ("https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s"
               "&current=temperature_2m,weather_code" % (la, lo))
        j = json.load(urllib.request.urlopen(url, timeout=8))
        t = round(j["current"]["temperature_2m"])
        c = j["current"]["weather_code"]
        temp.configure(text="%d°C" % t)
        icon.configure(text="☀️" if c == 0 else (
            "⛅" if c < 4 else ("🌧️" if c < 50 else "🌨️")))
        desc.configure(text="Live · Open-Meteo")
    except Exception:
        temp.configure(text="21°C")
        desc.configure(text="Demo mode (offline)")
    app.after(60000, refresh)


ctk.CTkButton(app, text="Refresh", fg_color=ACCENT, text_color="#170B16",
              font=ctk.CTkFont(weight="bold"), command=refresh).pack(pady=10)
refresh()
app.mainloop()
'''

SHOP_WIN = _HEAD + '''
PRODUCTS = [("T-Shirt", 19), ("Sneakers", 59), ("Headphones", 39),
            ("Watch", 99), ("Sunglasses", 25), ("Backpack", 45)]

app = ctk.CTk()
app.title("{{APP_NAME}}")
app.geometry("620x540")
app.configure(fg_color=BG)

ctk.CTkLabel(app, text="🛍️ {{APP_NAME}}",
             font=ctk.CTkFont(size=18, weight="bold"),
             text_color=ACCENT).pack(pady=(14, 6))
body = ctk.CTkFrame(app, fg_color="transparent")
body.pack(fill="both", expand=True, padx=14)
pl = ctk.CTkScrollableFrame(body, fg_color=CARD, corner_radius=14)
pl.pack(side="left", fill="both", expand=True)
cl = ctk.CTkScrollableFrame(body, fg_color=CARD, corner_radius=14)
cl.pack(side="right", fill="both", expand=True, padx=(10, 0))
total = ctk.CTkLabel(app, text="Total: $0",
                     font=ctk.CTkFont(size=16, weight="bold"),
                     text_color=ACCENT)
total.pack(pady=8)
cart = []


def refresh():
    for w in cl.winfo_children():
        w.destroy()
    for n, p in cart:
        ctk.CTkLabel(cl, text="%s — $%d" % (n, p), anchor="w").pack(
            fill="x", padx=8, pady=1)
    total.configure(text="Total: $%d" % sum(p for _, p in cart))


for n, p in PRODUCTS:
    row = ctk.CTkFrame(pl, fg_color="transparent")
    row.pack(fill="x", pady=3)
    ctk.CTkLabel(row, text="%s — $%d" % (n, p), anchor="w").pack(
        side="left", padx=8)
    ctk.CTkButton(row, text="Add", width=70, fg_color=ACCENT,
                  text_color="#170B16",
                  command=lambda n=n, p=p: (cart.append((n, p)),
                                            refresh())).pack(side="right",
                                                             padx=8)

bar = ctk.CTkFrame(app, fg_color="transparent")
bar.pack(fill="x", padx=14, pady=(0, 14))
ctk.CTkButton(bar, text="Clear cart", fg_color="#5A1E2E",
              command=lambda: (cart.clear(), refresh())).pack(
                  fill="x")
refresh()
app.mainloop()
'''

STARTER_WIN = _HEAD + '''
app = ctk.CTk()
app.title("{{APP_NAME}}")
app.geometry("400x340")
app.configure(fg_color=BG)

ctk.CTkLabel(app, text="✨ {{APP_NAME}}",
             font=ctk.CTkFont(size=19, weight="bold"),
             text_color=ACCENT).pack(pady=(18, 6))
ctk.CTkLabel(app, text="💡 {{IDEA}}", text_color="#C79BD4",
             wraplength=340).pack(padx=16, pady=6)
n = ctk.IntVar(value=0)
ctk.CTkLabel(app, textvariable=n,
             font=ctk.CTkFont(size=56, weight="bold")).pack()
bar = ctk.CTkFrame(app, fg_color="transparent")
bar.pack(pady=10)
ctk.CTkButton(bar, text="+ Tap", width=110, fg_color=ACCENT,
              text_color="#170B16", font=ctk.CTkFont(weight="bold"),
              command=lambda: n.set(n.get() + 1)).pack(side="left", padx=5)
ctk.CTkButton(bar, text="Reset", width=110, fg_color=CARD,
              command=lambda: n.set(0)).pack(side="left", padx=5)
app.mainloop()
'''

NEW_WIN = {
    "blog": BLOG_WIN,
    "calculator": CALC_WIN,
    "notes": NOTES_WIN,
    "dice": DICE_WIN,
    "weather": WEATHER_WIN,
    "shop": SHOP_WIN,
    "starter": STARTER_WIN,
}
