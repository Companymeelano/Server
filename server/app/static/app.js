let LANG = localStorage.getItem("m_lang") || "en";
let TOKEN = localStorage.getItem("m_token") || "";
let TPL = { suggestions: [] };
let currentJob = null, pollTimer = null;

const STR = {
  en: { headline: "What do you want to make today?", ph: "Type your idea...",
        myapps: "📱 My apps", building: "🔨 Building...", back: "← Back" },
  fa: { headline: "امروز می‌خوای چی بسازی؟", ph: "ایده‌ات رو بنویس...",
        myapps: "📱 برنامه‌های من", building: "🔨 در حال ساخت...", back: "← بازگشت" },
};

function t(k) { return STR[LANG][k]; }

async function api(path, opts) {
  opts = opts || {};
  opts.headers = Object.assign({}, opts.headers || {},
    TOKEN ? { "X-Builder-Token": TOKEN } : {});
  const r = await fetch(path, opts);
  if (r.status === 401) {
    askToken();
    throw new Error("unauthorized");
  }
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

function askToken() {
  const v = prompt(LANG === "fa"
    ? "توکن امنیتی سرور (BUILDER_TOKEN) را وارد کن — خالی = بدون توکن:"
    : "Enter the server API token (BUILDER_TOKEN) — empty = no token:", TOKEN);
  if (v === null) return;
  TOKEN = v.trim();
  if (TOKEN) localStorage.setItem("m_token", TOKEN);
  else localStorage.removeItem("m_token");
}

async function init() {
  document.getElementById("langBtn").onclick = () => {
    LANG = LANG === "en" ? "fa" : "en";
    localStorage.setItem("m_lang", LANG);
    applyLang();
  };
  document.getElementById("keyBtn").onclick = askToken;
  document.getElementById("go").onclick = createJob;
  document.getElementById("idea").addEventListener("keydown", e => {
    if (e.key === "Enter") createJob();
  });
  try { TPL = await api("/api/v1/templates"); } catch (e) { TPL = { suggestions: [] }; }
  applyLang();
  refreshJobs();
  setInterval(refreshJobs, 8000);
}

function applyLang() {
  document.getElementById("headline").innerText = t("headline");
  document.getElementById("idea").placeholder = t("ph");
  document.getElementById("myApps").innerText = t("myapps");
  document.documentElement.dir = LANG === "fa" ? "rtl" : "ltr";
  document.documentElement.lang = LANG;
  const box = document.getElementById("chips");
  box.innerHTML = "";
  for (const s of TPL.suggestions) {
    const b = document.createElement("button");
    b.className = "chip";
    b.innerText = s.emoji + " " + (LANG === "fa" ? s.fa : s.en);
    b.onclick = () => {
      document.getElementById("idea").value = LANG === "fa" ? s.fa : s.en;
      createJob();
    };
    box.appendChild(b);
  }
}

async function createJob() {
  const idea = document.getElementById("idea").value.trim();
  if (idea.length < 3) { alert(LANG === "fa" ? "ایده‌ات رو بنویس!" : "Type your idea first!"); return; }
  const platforms = [];
  if (document.getElementById("p_android").checked) platforms.push("android");
  if (document.getElementById("p_windows").checked) platforms.push("windows");
  if (!platforms.length) { alert("Pick at least one platform"); return; }
  try {
    const job = await api("/api/v1/jobs", {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ idea, platforms, lang: LANG }),
    });
    openJob(job.id);
  } catch (e) {
    alert(e.message === "unauthorized"
      ? (LANG === "fa" ? "توکن اشتباه است (دکمه 🔑)" : "Bad token (🔑 button)")
      : e.message);
  }
}

function showHome() {
  clearInterval(pollTimer);
  document.getElementById("jobview").classList.add("hidden");
  document.getElementById("home").classList.remove("hidden");
  refreshJobs();
}

async function openJob(id) {
  currentJob = id;
  document.getElementById("home").classList.add("hidden");
  document.getElementById("jobview").classList.remove("hidden");
  document.getElementById("j_prev").classList.add("hidden");
  document.getElementById("j_dl").innerHTML = "";
  await pollJob();
  clearInterval(pollTimer);
  pollTimer = setInterval(pollJob, 1500);
}

function togglePreview() {
  const f = document.getElementById("j_prev");
  if (f.classList.contains("hidden")) {
    f.src = "/api/v1/jobs/" + currentJob + "/preview";
    f.classList.remove("hidden");
  } else f.classList.add("hidden");
}

async function pollJob() {
  try {
    const j = await api("/api/v1/jobs/" + currentJob);
    document.getElementById("j_title").innerText = (j.name ? "📦 " + j.name : t("building"));
    document.getElementById("j_idea").innerText = "💡 " + j.idea;
    const st = document.getElementById("j_status");
    st.innerText = j.status;
    st.className = "badge " + (j.status === "done" ? "done" : j.status === "failed" ? "failed" : "building");
    document.getElementById("j_bar").style.width = j.progress + "%";
    const logs = document.getElementById("j_logs");
    logs.innerText = j.logs.join("\n");
    logs.scrollTop = logs.scrollHeight;
    const dl = document.getElementById("j_dl");
    dl.innerHTML = "";
    for (const a of j.artifacts) {
      const link = document.createElement("a");
      link.href = a.url;
      link.innerText = "⬇ " + a.label + (a.size ? " (" + Math.round(a.size / 1024) + " KB)" : "");
      link.className = a.kind === "source" ? "ghost" : "";
      dl.appendChild(link);
    }
    const pv = document.createElement("a");
    pv.href = "/api/v1/jobs/" + currentJob + "/preview";
    pv.target = "_blank";
    pv.className = "ghost";
    pv.innerText = "👁 Open preview";
    dl.appendChild(pv);
    if (["done", "partial", "failed"].includes(j.status)) clearInterval(pollTimer);
  } catch (e) { /* keep polling */ }
}

async function refreshJobs() {
  try {
    const { jobs } = await api("/api/v1/jobs?limit=20");
    const box = document.getElementById("jobs");
    box.innerHTML = "";
    if (!jobs.length) {
      box.innerHTML = "<div style='color:var(--dim)'>" +
        (LANG === "fa" ? "هنوز چیزی نساختی — بالا ایده‌ات رو بنویس 👆" : "Nothing yet — type an idea above 👆") + "</div>";
      return;
    }
    for (const j of jobs) {
      const d = document.createElement("div");
      d.className = "job";
      d.innerHTML = "<div style='font-size:26px'>📦</div><div style='flex:1'><b>" +
        (j.name || j.idea).replace(/</g, "&lt;") +
        "</b><div style='color:var(--dim);font-size:12px'>" + j.idea.replace(/</g, "&lt;") +
        "</div></div><span class='badge " +
        (j.status === "done" ? "done" : j.status === "failed" ? "failed" : "building") + "'>" +
        j.status + "</span>";
      d.onclick = () => openJob(j.id);
      box.appendChild(d);
    }
  } catch (e) { /* server warming up */ }
}

init();
