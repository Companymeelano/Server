"""Cloud build via GitHub Actions (no local toolchains needed).

Dispatches `build-generated.yml` with (template, app_name, slug, platforms),
waits for the run, then downloads the artifacts (exe / setup / apk / source).
"""
import time
import zipfile
from pathlib import Path

import httpx

from . import config

API = "https://api.github.com"


def enabled() -> bool:
    return bool(config.GITHUB_TOKEN and config.GITHUB_REPO)


def _h() -> dict:
    return {"Authorization": "Bearer " + config.GITHUB_TOKEN,
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28"}


def dispatch(template: str, app_name: str, slug: str,
             platforms: list) -> tuple[str, str]:
    """Returns (run_id_hint, created_marker). We locate the run by polling."""
    with httpx.Client(timeout=30) as c:
        r = c.post(
            f"{API}/repos/{config.GITHUB_REPO}/actions/workflows/"
            f"{config.CLOUD_BUILD_WORKFLOW}/dispatches",
            headers=_h(),
            json={"ref": config.GITHUB_BRANCH,
                  "inputs": {"template": template, "app_name": app_name,
                             "slug": slug,
                             "platforms": ",".join(platforms)}})
        r.raise_for_status()
    marker = str(time.time())
    return marker, marker


def find_run(marker_time: float, timeout_s: int = 300) -> dict | None:
    """Find the newest queued/in-progress run started after marker_time."""
    deadline = time.time() + timeout_s
    with httpx.Client(timeout=30) as c:
        while time.time() < deadline:
            r = c.get(
                f"{API}/repos/{config.GITHUB_REPO}/actions/workflows/"
                f"{config.CLOUD_BUILD_WORKFLOW}/runs?per_page=5",
                headers=_h())
            r.raise_for_status()
            for run in r.json().get("workflow_runs", []):
                created = run.get("created_at", "")
                try:
                    import datetime
                    ts = datetime.datetime.fromisoformat(
                        created.replace("Z", "+00:00")).timestamp()
                except Exception:
                    ts = 0
                if ts >= marker_time - 120:
                    return run
            time.sleep(10)
    return None


def wait_run(run_id: int, timeout_min: int) -> dict | None:
    deadline = time.time() + timeout_min * 60
    with httpx.Client(timeout=30) as c:
        while time.time() < deadline:
            r = c.get(f"{API}/repos/{config.GITHUB_REPO}/actions/runs/{run_id}",
                      headers=_h())
            r.raise_for_status()
            run = r.json()
            if run.get("status") == "completed":
                return run
            time.sleep(20)
    return None


def download_artifacts(run_id: int, dest: Path) -> list:
    """Download all artifacts of a run into dest. Returns file paths."""
    out = []
    with httpx.Client(timeout=120, follow_redirects=False) as c:
        r = c.get(f"{API}/repos/{config.GITHUB_REPO}/actions/runs/{run_id}/artifacts",
                  headers=_h())
        r.raise_for_status()
        for art in r.json().get("artifacts", []):
            if art.get("expired"):
                continue
            dl = c.get(
                f"{API}/repos/{config.GITHUB_REPO}/actions/artifacts/"
                f"{art['id']}/zip", headers=_h())
            if dl.status_code in (301, 302, 303, 307, 308) and "location" in dl.headers:
                dl = c.get(dl.headers["location"])
            dl.raise_for_status()
            zf = dest / (art["name"] + ".zip")
            zf.write_bytes(dl.content)
            with zipfile.ZipFile(zf) as z:
                for member in z.namelist():
                    target = dest / Path(member).name
                    if target.suffix == ".zip":
                        continue
                    target.write_bytes(z.read(member))
                    out.append(target)
            zf.unlink(missing_ok=True)
    return out
