"""Built-in app templates (works with zero API keys).

Every template ships:
  - a single-file functional web app  -> preview + Android WebView asset
  - a single-file tkinter desktop app -> PyInstaller .exe + NSIS installer
Placeholders: {{APP_NAME}}, {{ACCENT}}, {{IDEA}} (replaced with str.replace).
"""

BLOG_WEB = """<!DOCTYPE html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{{APP_NAME}}</title><style>
*{box-sizing:border-box}body{margin:0;font-family:system-ui;background:#170B16;color:#F2D9F7}
header{padding:20px;text-align:center;background:linear-gradient(135deg,#2A1230,#170B16)}
h1{margin:0;color:{{ACCENT}}}#q{width:90%;max-width:480px;margin:14px auto;display:block;padding:12px 16px;
border-radius:12px;border:1px solid #5A2A66;background:#241423;color:#fff}
#list{max-width:640px;margin:0 auto;padding:0 16px 40px}
.card{background:#241423;border:1px solid #3A1E44;border-radius:14px;padding:16px;margin:12px 0;cursor:pointer}
.card h3{margin:0 0 6px;color:{{ACCENT}}}.card p{margin:0;color:#C79BD4;font-size:14px}
#reader{display:none;max-width:640px;margin:0 auto;padding:16px}#reader button{margin-bottom:12px}
button{background:{{ACCENT}};border:0;border-radius:10px;padding:10px 18px;font-weight:700;cursor:pointer}
#body{line-height:1.8;color:#EADCF0}</style></head><body>
<header><h1>{{APP_NAME}}</h1><div style="color:#9C6BA8">Blog reader</div></header>
<input id="q" placeholder="Search posts..." oninput="render()">
<div id="list"></div>
<div id="reader"><button onclick="back()">← Back</button><h2 id="t"></h2><div id="body"></div></div>
<script>
const POSTS=[
{t:"Hello World",s:"Your first post",b:"Welcome to {{APP_NAME}}! This starter post shows how the reader works. Add your own posts by editing the POSTS array."},
{t:"Getting Started",s:"How to use this app",b:"Use the search box to filter posts. Tap any card to read it. This app also ships as a Windows program and an Android app."},
{t:"Tips & Tricks",s:"Make it yours",b:"Rename the app, change the accent color, and publish. Your idea becomes installable software in one click."},
{t:"Offline First",s:"Works anywhere",b:"Everything runs on-device. No account, no tracking, no internet needed for reading."}];
function render(){const q=document.getElementById('q').value.toLowerCase();
document.getElementById('list').innerHTML=POSTS.filter(p=>(p.t+p.s+p.b).toLowerCase().includes(q))
.map((p,i)=>'<div class=card onclick="open('+POSTS.indexOf(p)+')"><h3>'+p.t+'</h3><p>'+p.s+'</p></div>').join('')||'<p>No posts found.</p>'}
function open(i){document.getElementById('list').style.display='none';document.getElementById('q').style.display='none';
document.getElementById('reader').style.display='block';document.getElementById('t').innerText=POSTS[i].t;
document.getElementById('body').innerText=POSTS[i].b}
function back(){document.getElementById('reader').style.display='none';
document.getElementById('list').style.display='block';document.getElementById('q').style.display='block'}
render();</script></body></html>"""

BLOG_PY = '''"""{{APP_NAME}} - desktop edition (tkinter)."""
import tkinter as tk
from tkinter import ttk

POSTS = [
    ("Hello World", "Welcome to {{APP_NAME}}! This starter post shows how the reader works."),
    ("Getting Started", "Select a post on the left to read it here. Search filters the list."),
    ("Tips & Tricks", "Rename the app, change the theme, and publish your own version."),
    ("Offline First", "Everything runs on-device. No account, no tracking."),
]

root = tk.Tk()
root.title("{{APP_NAME}}")
root.geometry("760x480")
root.configure(bg="#170B16")
style = ttk.Style(root)
style.theme_use("clam")

left = tk.Frame(root, bg="#241423", width=220)
left.pack(side="left", fill="y")
tk.Label(left, text="{{APP_NAME}}", fg="{{ACCENT}}", bg="#241423",
         font=("Segoe UI", 13, "bold")).pack(pady=10)
search = tk.Entry(left, bg="#170B16", fg="white", insertbackground="white")
search.pack(fill="x", padx=10)
lst = tk.Listbox(left, bg="#170B16", fg="#F2D9F7", selectbackground="{{ACCENT}}",
                 highlightthickness=0, relief="flat")
lst.pack(fill="both", expand=True, padx=10, pady=10)

right = tk.Frame(root, bg="#170B16")
right.pack(side="right", fill="both", expand=True)
title = tk.Label(right, text="", fg="{{ACCENT}}", bg="#170B16", font=("Segoe UI", 15, "bold"))
title.pack(anchor="w", padx=16, pady=(14, 4))
body = tk.Text(right, bg="#170B16", fg="#EADCF0", relief="flat", wrap="word",
               font=("Segoe UI", 11))
body.pack(fill="both", expand=True, padx=16, pady=(0, 14))

def refresh(*_):
    q = search.get().lower()
    lst.delete(0, "end")
    for t, _ in POSTS:
        if q in t.lower():
            lst.insert("end", t)
    if lst.size():
        lst.selection_set(0)
        show()

def show(*_):
    if not lst.curselection():
        return
    t = lst.get(lst.curselection()[0])
    b = dict(POSTS)[t]
    title.config(text=t)
    body.delete("1.0", "end")
    body.insert("1.0", b)

search.bind("<KeyRelease>", refresh)
lst.bind("<<ListboxSelect>>", show)
refresh()
root.mainloop()
'''

CALC_WEB = """<!DOCTYPE html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{{APP_NAME}}</title><style>
*{box-sizing:border-box}body{margin:0;font-family:system-ui;background:#170B16;color:#fff;
display:flex;justify-content:center;align-items:center;min-height:100vh}
.calc{background:#241423;border:1px solid #3A1E44;border-radius:20px;padding:20px;width:min(92vw,340px)}
h2{text-align:center;color:{{ACCENT}};margin:0 0 10px}#d{width:100%;font-size:28px;text-align:right;
padding:12px;border-radius:12px;border:1px solid #5A2A66;background:#170B16;color:#fff;margin-bottom:12px}
.grid{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}
button{padding:16px;font-size:18px;border-radius:12px;border:0;background:#33203C;color:#fff;cursor:pointer}
button.op{background:{{ACCENT}};color:#170B16;font-weight:700}button.eq{background:#4AA3FF;color:#0B1520;font-weight:700}</style></head><body>
<div class="calc"><h2>{{APP_NAME}}</h2><input id="d" readonly value="0">
<div class="grid">
<button onclick="c()">C</button><button onclick="a('(')">(</button><button onclick="a(')')">)</button><button class="op" onclick="a('/')">÷</button>
<button onclick="a('7')">7</button><button onclick="a('8')">8</button><button onclick="a('9')">9</button><button class="op" onclick="a('*')">×</button>
<button onclick="a('4')">4</button><button onclick="a('5')">5</button><button onclick="a('6')">6</button><button class="op" onclick="a('-')">−</button>
<button onclick="a('1')">1</button><button onclick="a('2')">2</button><button onclick="a('3')">3</button><button class="op" onclick="a('+')">+</button>
<button onclick="a('0')">0</button><button onclick="a('.')">.</button><button onclick="b()">⌫</button><button class="eq" onclick="e()">=</button>
</div></div><script>
let s='';const d=document.getElementById('d');
function a(x){s+=x;d.value=s}function c(){s='';d.value='0'}function b(){s=s.slice(0,-1);d.value=s||'0'}
function e(){try{s=String(Function('return ('+s+')')());d.value=s}catch(_){d.value='Error';s=''}}</script></body></html>"""

CALC_PY = '''"""{{APP_NAME}} - desktop calculator (tkinter)."""
import tkinter as tk

root = tk.Tk()
root.title("{{APP_NAME}}")
root.configure(bg="#170B16")
root.resizable(False, False)
expr = tk.StringVar(value="0")

tk.Entry(root, textvariable=expr, font=("Segoe UI", 22), justify="right",
         bg="#241423", fg="white", relief="flat", width=18).grid(
             row=0, column=0, columnspan=4, padx=12, pady=12, ipady=8)

def press(x):
    v = expr.get()
    expr.set(x if v in ("0", "Error") else v + x)

def calc():
    try:
        expr.set(str(eval(expr.get(), {"__builtins__": {}}, {})))
    except Exception:
        expr.set("Error")

btns = ["C", "(", ")", "/",
        "7", "8", "9", "*",
        "4", "5", "6", "-",
        "1", "2", "3", "+",
        "0", ".", "⌫", "="]
def on(b):
    if b == "C":
        expr.set("0")
    elif b == "=":
        calc()
    elif b == "⌫":
        expr.set(expr.get()[:-1] or "0")
    else:
        press(b)

for i, b in enumerate(btns):
    tk.Button(root, text=b, width=5, height=2, font=("Segoe UI", 13, "bold"),
              bg="{{ACCENT}}" if b in "/*-+()" else ("#4AA3FF" if b == "=" else "#33203C"),
              fg="#170B16" if b in "/*-+()=" else "white", relief="flat",
              command=lambda b=b: on(b)).grid(row=1 + i // 4, column=i % 4,
                                              padx=4, pady=4)
root.mainloop()
'''

NOTES_WEB = """<!DOCTYPE html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{{APP_NAME}}</title><style>
*{box-sizing:border-box}body{margin:0;font-family:system-ui;background:#170B16;color:#F2D9F7}
header{padding:18px;text-align:center}h1{color:{{ACCENT}};margin:0}
main{max-width:560px;margin:0 auto;padding:0 16px 40px}
#t{width:100%;padding:12px;border-radius:12px;border:1px solid #5A2A66;background:#241423;color:#fff}
#row{display:flex;gap:8px;margin:10px 0}button{flex:1;padding:12px;border:0;border-radius:12px;
background:{{ACCENT}};font-weight:700;cursor:pointer}
.note{background:#241423;border:1px solid #3A1E44;border-radius:12px;padding:12px;margin:8px 0;
display:flex;justify-content:space-between;gap:8px}.note small{color:#9C6BA8;display:block}
.del{background:#5A1E2E;color:#fff;flex:0 0 auto}</style></head><body>
<header><h1>{{APP_NAME}}</h1><div style="color:#9C6BA8">My notes</div></header>
<main><textarea id="t" rows="3" placeholder="Write a note..."></textarea>
<div id="row"><button onclick="add()">+ Add note</button></div><div id="list"></div></main>
<script>
let notes=JSON.parse(localStorage.getItem('notes')||'[]');
function save(){localStorage.setItem('notes',JSON.stringify(notes));render()}
function add(){const v=document.getElementById('t').value.trim();if(!v)return;
notes.unshift({t:v,d:new Date().toLocaleString()});document.getElementById('t').value='';save()}
function del(i){notes.splice(i,1);save()}
function render(){document.getElementById('list').innerHTML=notes.map((n,i)=>
'<div class=note><div>'+n.t.replace(/</g,'&lt;')+'<small>'+n.d+'</small></div><button class=del onclick="del('+i+')">✕</button></div>').join('')||'<p>No notes yet.</p>'}
render();</script></body></html>"""

NOTES_PY = '''"""{{APP_NAME}} - desktop notes (tkinter)."""
import json
import tkinter as tk
from pathlib import Path

DB = Path.home() / ".{{APP_NAME}}_notes.json".replace(" ", "_").lower()

def load():
    try:
        return json.loads(DB.read_text(encoding="utf-8"))
    except Exception:
        return []

def save(notes):
    DB.write_text(json.dumps(notes, ensure_ascii=False), encoding="utf-8")

notes = load()
root = tk.Tk()
root.title("{{APP_NAME}}")
root.geometry("560x480")
root.configure(bg="#170B16")

entry = tk.Text(root, height=4, bg="#241423", fg="white", relief="flat",
                insertbackground="white", font=("Segoe UI", 11))
entry.pack(fill="x", padx=12, pady=(12, 6))

bar = tk.Frame(root, bg="#170B16")
bar.pack(fill="x", padx=12, pady=4)
lst = tk.Listbox(root, bg="#241423", fg="#F2D9F7", relief="flat",
                 highlightthickness=0, selectbackground="{{ACCENT}}",
                 font=("Segoe UI", 11))
lst.pack(fill="both", expand=True, padx=12, pady=6)

def refresh():
    lst.delete(0, "end")
    for n in notes:
        lst.insert("end", n)

def add():
    v = entry.get("1.0", "end").strip()
    if v:
        notes.insert(0, v)
        save(notes)
        entry.delete("1.0", "end")
        refresh()

def delete():
    if lst.curselection():
        notes.pop(lst.curselection()[0])
        save(notes)
        refresh()

tk.Button(bar, text="+ Add", bg="{{ACCENT}}", fg="#170B16", relief="flat",
          font=("Segoe UI", 10, "bold"), command=add).pack(side="left", expand=True, fill="x", padx=2)
tk.Button(bar, text="Delete", bg="#5A1E2E", fg="white", relief="flat",
          font=("Segoe UI", 10, "bold"), command=delete).pack(side="left", expand=True, fill="x", padx=2)
refresh()
root.mainloop()
'''

DICE_WEB = """<!DOCTYPE html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{{APP_NAME}}</title><style>
body{margin:0;font-family:system-ui;background:#170B16;color:#fff;text-align:center}
h1{color:{{ACCENT}};margin-top:28px}#dice{font-size:110px;margin:10px;transition:transform .15s}
button{padding:14px 42px;font-size:18px;border:0;border-radius:14px;background:{{ACCENT}};
font-weight:800;cursor:pointer}#hist{color:#9C6BA8;margin-top:14px;font-size:15px}</style></head><body>
<h1>{{APP_NAME}}</h1><div id="dice">🎲</div>
<button onclick="roll()">ROLL</button><div id="hist"></div>
<script>
const F=['⚀','⚁','⚂','⚃','⚄','⚅'];let h=[];
function roll(){const d=document.getElementById('dice');let n=0;
const t=setInterval(()=>{d.innerText=F[Math.floor(Math.random()*6)];
d.style.transform='rotate('+(Math.random()*60-30)+'deg)';
if(++n>8){clearInterval(t);d.style.transform='';const v=Math.floor(Math.random()*6)+1;
d.innerText=F[v-1];h.unshift(v);h=h.slice(0,12);
document.getElementById('hist').innerText='History: '+h.join(' · ')}}},70)}</script></body></html>"""

DICE_PY = '''"""{{APP_NAME}} - dice roller (tkinter)."""
import random
import tkinter as tk

FACES = ["⚀", "⚁", "⚂", "⚃", "⚄", "⚅"]

root = tk.Tk()
root.title("{{APP_NAME}}")
root.geometry("340x380")
root.configure(bg="#170B16")
tk.Label(root, text="{{APP_NAME}}", fg="{{ACCENT}}", bg="#170B16",
         font=("Segoe UI", 16, "bold")).pack(pady=10)
face = tk.Label(root, text="🎲", bg="#170B16", font=("Segoe UI", 110))
face.pack()
hist = tk.Label(root, text="History: —", fg="#9C6BA8", bg="#170B16",
                font=("Segoe UI", 11))
hist.pack(pady=6)
rolls = []

def roll(n=0):
    face.config(text=random.choice(FACES))
    if n < 8:
        root.after(70, lambda: roll(n + 1))
    else:
        v = random.randint(1, 6)
        face.config(text=FACES[v - 1])
        rolls.insert(0, v)
        del rolls[12:]
        hist.config(text="History: " + " · ".join(map(str, rolls)))

tk.Button(root, text="ROLL", bg="{{ACCENT}}", fg="#170B16", relief="flat",
          font=("Segoe UI", 13, "bold"), width=14, command=roll).pack(pady=8)
root.mainloop()
'''

WEATHER_WEB = """<!DOCTYPE html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{{APP_NAME}}</title><style>
*{box-sizing:border-box}body{margin:0;font-family:system-ui;background:#170B16;color:#fff;text-align:center}
h1{color:{{ACCENT}};margin:26px 0 6px}select{padding:10px 16px;border-radius:12px;background:#241423;
color:#fff;border:1px solid #5A2A66;font-size:15px;margin:8px}
#w{font-size:64px;margin:6px}#t{font-size:52px;font-weight:800}#d{color:#9C6BA8;margin-bottom:30px}</style></head><body>
<h1>{{APP_NAME}}</h1>
<select id="c" onchange="go()">
<option value="52.37,4.89">Amsterdam</option><option value="35.69,51.38">Tehran</option>
<option value="51.5,-0.12">London</option><option value="40.71,-74">New York</option>
<option value="34.05,-118.24">Los Angeles</option><option value="55.75,37.61">Moscow</option>
</select><div id="w">⛅</div><div id="t">--°</div><div id="d">loading...</div>
<script>
async function go(){const[la,lo]=document.getElementById('c').value.split(',');
try{const r=await fetch('https://api.open-meteo.com/v1/forecast?latitude='+la+'&longitude='+lo+'&current=temperature_2m,weather_code');
const j=await r.json();const t=Math.round(j.current.temperature_2m);const c=j.current.weather_code;
document.getElementById('t').innerText=t+'°C';
const e=c==0?'☀️':c<4?'⛅':c<50?'🌧️':c<70?'🌨️':'⛈️';
document.getElementById('w').innerText=e;document.getElementById('d').innerText='Live · Open-Meteo';
}catch(_){document.getElementById('t').innerText='21°C';document.getElementById('w').innerText='⛅';
document.getElementById('d').innerText='Demo mode (offline)'}}
go();</script></body></html>"""

WEATHER_PY = '''"""{{APP_NAME}} - weather (tkinter)."""
import json
import tkinter as tk
import urllib.request

CITIES = {"Amsterdam": (52.37, 4.89), "Tehran": (35.69, 51.38),
          "London": (51.5, -0.12), "New York": (40.71, -74.0)}

root = tk.Tk()
root.title("{{APP_NAME}}")
root.geometry("360x420")
root.configure(bg="#170B16")
tk.Label(root, text="{{APP_NAME}}", fg="{{ACCENT}}", bg="#170B16",
         font=("Segoe UI", 16, "bold")).pack(pady=10)

city = tk.StringVar(value="Amsterdam")
tk.OptionMenu(root, city, *CITIES.keys()).pack()
icon = tk.Label(root, text="⛅", bg="#170B16", font=("Segoe UI", 72))
icon.pack()
temp = tk.Label(root, text="--°", fg="white", bg="#170B16",
                font=("Segoe UI", 44, "bold"))
temp.pack()
desc = tk.Label(root, text="", fg="#9C6BA8", bg="#170B16",
                font=("Segoe UI", 11))
desc.pack()

def refresh():
    la, lo = CITIES[city.get()]
    try:
        url = ("https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s"
               "&current=temperature_2m,weather_code" % (la, lo))
        j = json.load(urllib.request.urlopen(url, timeout=8))
        t = round(j["current"]["temperature_2m"])
        c = j["current"]["weather_code"]
        temp.config(text="%d°C" % t)
        icon.config(text="☀️" if c == 0 else ("⛅" if c < 4 else ("🌧️" if c < 50 else "🌨️")))
        desc.config(text="Live · Open-Meteo")
    except Exception:
        temp.config(text="21°C")
        desc.config(text="Demo mode (offline)")
    root.after(60000, refresh)

tk.Button(root, text="Refresh", bg="{{ACCENT}}", fg="#170B16", relief="flat",
          font=("Segoe UI", 10, "bold"), command=refresh).pack(pady=8)
refresh()
root.mainloop()
'''

SHOP_WEB = """<!DOCTYPE html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{{APP_NAME}}</title><style>
*{box-sizing:border-box}body{margin:0;font-family:system-ui;background:#170B16;color:#fff}
header{padding:18px;text-align:center}h1{color:{{ACCENT}};margin:0}
#grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:12px;
max-width:640px;margin:0 auto;padding:0 16px}
.p{background:#241423;border:1px solid #3A1E44;border-radius:14px;padding:14px;text-align:center}
.p .e{font-size:44px}.p b{color:{{ACCENT}}}button{width:100%;margin-top:8px;padding:10px;border:0;
border-radius:10px;background:{{ACCENT}};font-weight:700;cursor:pointer}
#cart{max-width:640px;margin:16px auto 40px;padding:0 16px}
#c{background:#241423;border:1px solid #3A1E44;border-radius:14px;padding:14px}#c small{color:#9C6BA8}</style></head><body>
<header><h1>{{APP_NAME}}</h1><div style="color:#9C6BA8">Mini shop</div></header>
<div id="grid"></div><div id="cart"><h3>🧺 Cart (<span id="n">0</span>)</h3><div id="c"></div></div>
<script>
const P=[{e:'👕',n:'T-Shirt',p:19},{e:'👟',n:'Sneakers',p:59},{e:'🎧',n:'Headphones',p:39},
{e:'⌚',n:'Watch',p:99},{e:'🕶️',n:'Sunglasses',p:25},{e:'🎒',n:'Backpack',p:45}];
let cart=JSON.parse(localStorage.getItem('cart')||'[]');
function save(){localStorage.setItem('cart',JSON.stringify(cart));render()}
function add(i){cart.push(i);save()}function clear(){cart=[];save()}
function render(){document.getElementById('grid').innerHTML=P.map((p,i)=>
'<div class=p><div class=e>'+p.e+'</div><div>'+p.n+'</div><b>$'+p.p+'</b><button onclick="add('+i+')">Add</button></div>').join('');
document.getElementById('n').innerText=cart.length;
const t=cart.reduce((s,i)=>s+P[i].p,0);
document.getElementById('c').innerHTML=(cart.map(i=>P[i].n+' — $'+P[i].p).join('<br>')||'<small>Empty</small>')+
'<br><b>Total: $'+t+'</b> <button onclick="clear()" style="width:auto;margin-top:8px">Clear</button>'}
render();</script></body></html>"""

SHOP_PY = '''"""{{APP_NAME}} - mini shop (tkinter)."""
import tkinter as tk

PRODUCTS = [("👕 T-Shirt", 19), ("👟 Sneakers", 59), ("🎧 Headphones", 39),
            ("⌚ Watch", 99), ("🕶️ Sunglasses", 25), ("🎒 Backpack", 45)]

root = tk.Tk()
root.title("{{APP_NAME}}")
root.geometry("560x480")
root.configure(bg="#170B16")
tk.Label(root, text="{{APP_NAME}}", fg="{{ACCENT}}", bg="#170B16",
         font=("Segoe UI", 15, "bold")).pack(pady=10)

body = tk.Frame(root, bg="#170B16")
body.pack(fill="both", expand=True, padx=12)
pl = tk.Listbox(body, bg="#241423", fg="white", relief="flat",
                highlightthickness=0, selectbackground="{{ACCENT}}",
                font=("Segoe UI", 11))
pl.pack(side="left", fill="both", expand=True)
cl = tk.Listbox(body, bg="#241423", fg="#F2D9F7", relief="flat",
                highlightthickness=0, font=("Segoe UI", 11))
cl.pack(side="right", fill="both", expand=True, padx=(8, 0))
for n, p in PRODUCTS:
    pl.insert("end", "%s — $%d" % (n, p))

total = tk.Label(root, text="Total: $0", fg="{{ACCENT}}", bg="#170B16",
                 font=("Segoe UI", 13, "bold"))
total.pack(pady=6)
cart = []

def refresh():
    cl.delete(0, "end")
    for n, p in cart:
        cl.insert("end", "%s — $%d" % (n, p))
    total.config(text="Total: $%d" % sum(p for _, p in cart))

def add():
    if pl.curselection():
        cart.append(PRODUCTS[pl.curselection()[0]])
        refresh()

tk.Button(root, text="Add to cart →", bg="{{ACCENT}}", fg="#170B16",
          relief="flat", font=("Segoe UI", 10, "bold"), command=add).pack(pady=4)
tk.Button(root, text="Clear cart", bg="#5A1E2E", fg="white", relief="flat",
          font=("Segoe UI", 10, "bold"),
          command=lambda: (cart.clear(), refresh())).pack(pady=4)
refresh()
root.mainloop()
'''

STARTER_WEB = """<!DOCTYPE html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{{APP_NAME}}</title><style>
body{margin:0;font-family:system-ui;background:#170B16;color:#fff;text-align:center}
h1{color:{{ACCENT}};margin-top:30px}#idea{max-width:480px;margin:8px auto;color:#C79BD4;
background:#241423;border:1px solid #3A1E44;border-radius:12px;padding:14px}
#c{font-size:72px;margin:14px}button{padding:12px 34px;font-size:16px;border:0;border-radius:12px;
background:{{ACCENT}};font-weight:800;cursor:pointer;margin:4px}</style></head><body>
<h1>✨ {{APP_NAME}}</h1><div id="idea">💡 {{IDEA}}</div>
<div id="c">0</div>
<button onclick="n++;u()">+ Tap</button> <button onclick="n=0;u()">Reset</button>
<p style="color:#9C6BA8">Built with MeeLano Builder</p>
<script>let n=0;function u(){document.getElementById('c').innerText=n}</script></body></html>"""

STARTER_PY = '''"""{{APP_NAME}} - starter app (tkinter)."""
import tkinter as tk

root = tk.Tk()
root.title("{{APP_NAME}}")
root.geometry("380x300")
root.configure(bg="#170B16")
tk.Label(root, text="✨ {{APP_NAME}}", fg="{{ACCENT}}", bg="#170B16",
         font=("Segoe UI", 16, "bold")).pack(pady=10)
tk.Label(root, text="💡 {{IDEA}}", fg="#C79BD4", bg="#241423",
         font=("Segoe UI", 10), wraplength=320, justify="center").pack(padx=16, pady=6)
n = tk.IntVar(value=0)
tk.Label(root, textvariable=n, fg="white", bg="#170B16",
         font=("Segoe UI", 48, "bold")).pack()
bar = tk.Frame(root, bg="#170B16")
bar.pack(pady=8)
tk.Button(bar, text="+ Tap", bg="{{ACCENT}}", fg="#170B16", relief="flat",
          font=("Segoe UI", 11, "bold"), width=10,
          command=lambda: n.set(n.get() + 1)).pack(side="left", padx=4)
tk.Button(bar, text="Reset", bg="#33203C", fg="white", relief="flat",
          font=("Segoe UI", 11, "bold"), width=10,
          command=lambda: n.set(0)).pack(side="left", padx=4)
root.mainloop()
'''

TEMPLATES = {
    "blog": {
        "emoji": "📰", "name_en": "Blog Reader", "name_fa": "وبلاگ‌خوان",
        "desc_en": "Read posts, search and enjoy offline",
        "desc_fa": "خواندن مطالب، جستجو و حالت آفلاین",
        "accent": "#D9A7E6", "web": BLOG_WEB, "py": BLOG_PY,
        "kw_en": ["blog", "post", "article", "news", "reader", "magazine", "rss"],
        "kw_fa": ["وبلاگ", "بلاگ", "خبر", "مقاله", "مطلب", "خواندن", "مجله"],
    },
    "calculator": {
        "emoji": "🧮", "name_en": "Calculator", "name_fa": "ماشین‌حساب",
        "desc_en": "Fast and simple calculator",
        "desc_fa": "ماشین‌حساب سریع و ساده",
        "accent": "#7BD88F", "web": CALC_WEB, "py": CALC_PY,
        "kw_en": ["calcul", "math", "numbers", "arithmetic"],
        "kw_fa": ["حساب", "ماشین حساب", "ماشین‌حساب", "محاسبه", "ریاضی", "اعداد"],
    },
    "notes": {
        "emoji": "📝", "name_en": "Notes", "name_fa": "یادداشت‌ها",
        "desc_en": "Take notes, saved on your device",
        "desc_fa": "یادداشت‌برداری با ذخیره روی دستگاه",
        "accent": "#F2D060", "web": NOTES_WEB, "py": NOTES_PY,
        "kw_en": ["note", "memo", "todo", "task", "list", "reminder", "diary"],
        "kw_fa": ["یادداشت", "نوت", "دفترچه", "کارها", "لیست", "یادآور", "خاطرات"],
    },
    "dice": {
        "emoji": "🎲", "name_en": "Dice Roller", "name_fa": "تاس",
        "desc_en": "Roll the dice with history",
        "desc_fa": "تاس انداختن با تاریخچه",
        "accent": "#FF9D6F", "web": DICE_WEB, "py": DICE_PY,
        "kw_en": ["dice", "roll", "random", "lottery", "chance", "game"],
        "kw_fa": ["تاس", "شانس", "قرعه", "تصادفی", "بازی"],
    },
    "weather": {
        "emoji": "⛅", "name_en": "Weather", "name_fa": "هواشناسی",
        "desc_en": "Live weather for world cities",
        "desc_fa": "هوای زنده شهرهای جهان",
        "accent": "#6FC3FF", "web": WEATHER_WEB, "py": WEATHER_PY,
        "kw_en": ["weather", "forecast", "temperature", "climate", "rain"],
        "kw_fa": ["آب و هوا", "هواشناسی", "هوا", "دما", "باران", "اقلیم"],
    },
    "shop": {
        "emoji": "🛍️", "name_en": "Mini Shop", "name_fa": "فروشگاه",
        "desc_en": "Products with a shopping cart",
        "desc_fa": "محصولات با سبد خرید",
        "accent": "#FF7DAD", "web": SHOP_WEB, "py": SHOP_PY,
        "kw_en": ["shop", "store", "cart", "product", "market", "sell", "buy"],
        "kw_fa": ["فروشگاه", "خرید", "مغازه", "محصول", "سبد", "بازار", "فروش"],
    },
    "starter": {
        "emoji": "✨", "name_en": "Starter App", "name_fa": "برنامه پایه",
        "desc_en": "A simple starter for any idea",
        "desc_fa": "یک شروع ساده برای هر ایده‌ای",
        "accent": "#D9A7E6", "web": STARTER_WEB, "py": STARTER_PY,
        "kw_en": [], "kw_fa": [],
    },
}

# Suggestion chips shown on the home screen (like the YBee screenshot).
SUGGESTIONS = [
    {"emoji": "📰", "en": "Create a Blog reader app", "fa": "یک برنامه وبلاگ‌خوان بساز"},
    {"emoji": "🎲", "en": "Create a Dice roller app", "fa": "یک برنامه تاس بساز"},
    {"emoji": "📝", "en": "Create a Notes app", "fa": "یک برنامه یادداشت بساز"},
    {"emoji": "🧮", "en": "Create a Calculator app", "fa": "یک ماشین‌حساب بساز"},
    {"emoji": "⛅", "en": "Create a Weather app", "fa": "یک برنامه هواشناسی بساز"},
    {"emoji": "🛍️", "en": "Create a Mini Shop app", "fa": "یک فروشگاه کوچک بساز"},
]
