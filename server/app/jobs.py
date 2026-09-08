"""Disk-backed job store + background worker pool.

Thread-safe: atomic file replacement on write + tolerant reads, so polling
a job while it builds can never observe a half-written file.
"""
import json
import os
import tempfile
import threading
import time
import uuid
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from pathlib import Path

from . import config

_lock = threading.Lock()
_pool = ThreadPoolExecutor(max_workers=4, thread_name_prefix="job")

TERMINAL = {"done", "partial", "failed"}


def _path(job_id: str) -> Path:
    return config.JOBS / job_id / "job.json"


def _read(path: Path, retries: int = 8):
    """Read JSON defensively: never die on a torn read during a concurrent write."""
    for i in range(retries):
        try:
            return json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            if i == retries - 1:
                raise
            time.sleep(0.02)
    return None


def create(idea: str, platforms: list, lang: str, name: str = "") -> dict:
    job_id = uuid.uuid4().hex[:12]
    job = {
        "id": job_id, "idea": idea, "platforms": platforms, "lang": lang,
        "name": name, "template": "", "slug": "",
        "status": "queued", "progress": 0,
        "steps": [], "logs": [], "artifacts": [],
        "created": datetime.now(timezone.utc).isoformat(),
        "error": "",
    }
    d = config.JOBS / job_id
    (d / "src").mkdir(parents=True, exist_ok=True)
    (d / "out").mkdir(parents=True, exist_ok=True)
    _save(job)
    return job


def get(job_id: str) -> dict | None:
    p = _path(job_id)
    if not p.exists():
        return None
    return _read(p)


def list_all(limit: int = 50) -> list:
    jobs = []
    for p in sorted(config.JOBS.glob("*/job.json"),
                    key=lambda x: x.stat().st_mtime, reverse=True)[:limit]:
        try:
            jobs.append(_read(p))
        except Exception:
            continue
    return jobs


def _save(job: dict):
    """Atomic write: temp file + os.replace, so readers never see partial JSON."""
    p = _path(job["id"])
    with _lock:
        fd, tmp = tempfile.mkstemp(dir=p.parent, prefix="job",
                                   suffix=".tmp")
        try:
            with os.fdopen(fd, "w", encoding="utf-8") as f:
                f.write(json.dumps(job, ensure_ascii=False))
            os.replace(tmp, p)
        except BaseException:
            try:
                os.unlink(tmp)
            except OSError:
                pass
            raise


def update(job_id: str, **fields):
    job = get(job_id)
    if not job:
        return
    job.update(fields)
    _save(job)


def log(job_id: str, msg: str, progress: int | None = None,
        step: str | None = None):
    job = get(job_id)
    if not job:
        return
    ts = datetime.now(timezone.utc).strftime("%H:%M:%S")
    job["logs"].append(f"[{ts}] {msg}")
    job["logs"] = job["logs"][-config.JOB_LOG_LIMIT:]
    if progress is not None:
        job["progress"] = progress
    if step:
        if not job["steps"] or job["steps"][-1]["name"] != step:
            job["steps"].append({"name": step, "state": "run",
                                 "at": time.time()})
        for s in job["steps"][:-1]:
            if s["state"] == "run":
                s["state"] = "ok"
    _save(job)


def finish_step(job_id: str, ok: bool = True):
    job = get(job_id)
    if not job or not job["steps"]:
        return
    job["steps"][-1]["state"] = "ok" if ok else "fail"
    _save(job)


def add_artifact(job_id: str, kind: str, label: str, filename: str, size: int = 0):
    job = get(job_id)
    if not job:
        return
    job["artifacts"] = [a for a in job["artifacts"] if a["filename"] != filename]
    job["artifacts"].append({"kind": kind, "label": label,
                             "filename": filename, "size": size,
                             "url": f"/api/v1/artifacts/{job_id}/{filename}"})
    _save(job)


def submit(fn, job_id: str):
    _pool.submit(fn, job_id)


def cleanup(retention_days: int = 7, max_jobs: int = 200):
    """Delete job workspaces older than `retention_days`, keep newest `max_jobs`."""
    import shutil
    items = []
    for p in config.JOBS.glob("*/job.json"):
        try:
            items.append((p.stat().st_mtime, p.parent))
        except OSError:
            continue
    items.sort(reverse=True)  # newest first
    now = time.time()
    for i, (mtime, d) in enumerate(items):
        age_days = (now - mtime) / 86400
        if age_days > retention_days or i >= max_jobs:
            shutil.rmtree(d, ignore_errors=True)
