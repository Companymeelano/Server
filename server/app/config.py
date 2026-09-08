"""Central configuration for the MeeLano Builder server (env-driven)."""
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent          # server/
DATA = Path(os.environ.get("BUILDER_DATA", ROOT / "data"))
JOBS = DATA / "jobs"
DATA.mkdir(parents=True, exist_ok=True)
JOBS.mkdir(parents=True, exist_ok=True)

# Optional OpenAI-compatible LLM (for free-form ideas). Empty = template engine only.
OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY", "")
OPENAI_BASE_URL = os.environ.get("OPENAI_BASE_URL", "https://api.openai.com/v1")
OPENAI_MODEL = os.environ.get("OPENAI_MODEL", "gpt-4o-mini")

# Optional cloud build via GitHub Actions (build-generated.yml). Used when the
# local machine has no toolchains (PyInstaller/NSIS/Android SDK).
GITHUB_TOKEN = os.environ.get("GITHUB_TOKEN", "")
GITHUB_REPO = os.environ.get("GITHUB_REPO", "")  # e.g. "Companymeelano/Server"
CLOUD_BUILD_WORKFLOW = os.environ.get("CLOUD_BUILD_WORKFLOW", "build-generated.yml")
CLOUD_BUILD_TIMEOUT_MIN = int(os.environ.get("CLOUD_BUILD_TIMEOUT_MIN", "40"))

# Public base URL used to build absolute artifact links (optional).
PUBLIC_BASE_URL = os.environ.get("PUBLIC_BASE_URL", "").rstrip("/")

APP_VERSION = "1.0.0"
JOB_LOG_LIMIT = 2000
