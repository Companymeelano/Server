"""Turns a one-line idea into a real multi-platform project.

Pipeline: classify idea -> template -> render web + windows + android sources.
Optional: an OpenAI-compatible LLM customises the web app for free-form ideas.
"""
import re
import unicodedata

from . import config
from .templates_data import TEMPLATES

STOPWORDS = {
    "create", "make", "build", "please", "a", "an", "the", "app", "application",
    "program", "software", "for", "me", "my", "with", "that", "and", "to", "simple",
    "بساز", "بسازید", "بسازم", "لطفا", "لطفاً", "یک", "یه", "یکعدد", "برنامه",
    "اپ", "اپلیکیشن", "نرم", "افزار", "نرم‌افزار", "برای", "من", "با", "که",
    "ساده", "را", "رو",
}


def slugify(text: str) -> str:
    text = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode()
    text = re.sub(r"[^a-zA-Z0-9]+", "-", text).strip("-").lower()
    return text or "my-app"


def detect_lang(idea: str) -> str:
    fa = sum(1 for ch in idea if "\u0600" <= ch <= "\u06FF")
    return "fa" if fa >= 3 else "en"


def classify(idea: str) -> str:
    """Pick the best template id by keyword scoring (EN + FA)."""
    low = idea.lower()
    best, best_score = "starter", 0
    for tid, t in TEMPLATES.items():
        if tid == "starter":
            continue
        score = sum(2 for k in t["kw_fa"] if k in idea)
        score += sum(1 for k in t["kw_en"] if k in low)
        if score > best_score:
            best, best_score = tid, score
    return best


def extract_name(idea: str, template_id: str, lang: str) -> str:
    words = [w.strip(".,!؟?\"'") for w in idea.split()]
    words = [w for w in words if w and w.lower() not in STOPWORDS and len(w) > 1]
    if words:
        name = " ".join(words[:4])
        if lang == "en" or not any("\u0600" <= c <= "\u06FF" for c in name):
            name = name.title()
        return name[:32]
    t = TEMPLATES[template_id]
    return t["name_fa"] if lang == "fa" else t["name_en"]


def render(template_id: str, app_name: str, idea: str) -> dict:
    """Render all bundle files: {relative_path: content}."""
    t = TEMPLATES[template_id]

    def fill(s: str) -> str:
        return (s.replace("{{APP_NAME}}", app_name)
                 .replace("{{ACCENT}}", t["accent"])
                 .replace("{{IDEA}}", idea.strip()[:200]))

    from .buildkit_files import android_project_files, installer_files
    files = {
        "web/index.html": fill(t["web"]),
        "windows/app.py": fill(t["py"]),
        "windows/requirements.txt": "# no third-party deps (tkinter is in stdlib)\n",
        "README.md": ("# %s\n\nBuilt with MeeLano Builder from the idea:\n> %s\n\n"
                       "Folders: `web/` (preview + Android asset), `windows/` (PyInstaller exe),\n"
                       "`android/` (APK project), `installer/` (NSIS setup script).\n"
                       % (app_name, idea.strip())),
    }
    slug = slugify(app_name)
    files.update(android_project_files(slug, app_name, fill(t["web"])))
    files.update(installer_files(app_name, slug))
    return files


def llm_customise_web(idea: str, base_html: str, api_key: str = "",
                      base_url: str = "") -> str | None:
    """Ask the LLM to rewrite the single-file web app. Returns None on any failure."""
    key = api_key or config.OPENAI_API_KEY
    if not key:
        return None
    try:
        import httpx
        prompt = (
            "Rewrite the single-file HTML app below to match this idea: %s\n"
            "Rules: output ONLY raw HTML (no markdown fences). Keep it one file, "
            "offline-friendly, dark purple theme (#170B16 bg), mobile friendly, "
            "working JavaScript, no external assets except optional public APIs.\n\n%s"
            % (idea.strip()[:500], base_html[:12000]))
        r = httpx.post(
            (base_url or config.OPENAI_BASE_URL).rstrip("/") + "/chat/completions",
            headers={"Authorization": "Bearer " + key},
            json={"model": config.OPENAI_MODEL,
                  "messages": [{"role": "user", "content": prompt}],
                  "temperature": 0.6, "max_tokens": 6000},
            timeout=90)
        if r.status_code != 200:
            return None
        out = r.json()["choices"][0]["message"]["content"].strip()
        out = re.sub(r"^```html\s*|```$", "", out).strip()
        return out if "<html" in out.lower() and len(out) > 500 else None
    except Exception:
        return None
