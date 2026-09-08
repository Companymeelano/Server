"""MeeLano Builder API — type one short sentence, get installable apps."""
import json
import time
from collections import defaultdict
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import FileResponse, JSONResponse, StreamingResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field

from . import builder, config, jobs
from .templates_data import SUGGESTIONS, TEMPLATES


@asynccontextmanager
async def lifespan(app: FastAPI):
    jobs.cleanup(config.JOB_RETENTION_DAYS, config.MAX_JOBS)
    yield


app = FastAPI(title="MeeLano Builder", version=config.APP_VERSION,
              lifespan=lifespan)

# Simple in-memory rate limiter: ip -> [hit timestamps]
_hits: dict[str, list[float]] = defaultdict(list)


@app.middleware("http")
async def _guard(request: Request, call_next):
    if request.url.path.startswith("/api/"):
        if (config.API_TOKEN and request.headers.get("X-Builder-Token", "")
                != config.API_TOKEN):
            return JSONResponse(
                {"detail": "unauthorized (bad or missing token)"},
                status_code=401)
        if request.method == "POST" and request.url.path == "/api/v1/jobs":
            ip = request.client.host if request.client else "?"
            now = time.time()
            _hits[ip] = [t for t in _hits[ip] if now - t < 3600]
            if len(_hits[ip]) >= config.RATE_PER_HOUR:
                return JSONResponse(
                    {"detail": "rate limit exceeded, try again later"},
                    status_code=429)
            _hits[ip].append(now)
    return await call_next(request)


class CreateJob(BaseModel):
    idea: str = Field(min_length=3, max_length=500)
    platforms: list[str] = Field(default=["android", "windows"])
    lang: str = "auto"
    name: str = ""
    ai_key: str = ""   # optional per-job OpenAI-compatible key (from the app)
    ai_base: str = ""  # optional per-job base URL


@app.get("/api/v1/health")
def health():
    from . import github_cloud
    return {"ok": True, "version": config.APP_VERSION,
            "ai": bool(config.OPENAI_API_KEY),
            "cloud_build": github_cloud.enabled(),
            "auth": bool(config.API_TOKEN)}


@app.get("/api/v1/templates")
def templates():
    return {"templates": [
        {"id": tid, "emoji": t["emoji"], "name_en": t["name_en"],
         "name_fa": t["name_fa"], "desc_en": t["desc_en"],
         "desc_fa": t["desc_fa"], "accent": t["accent"]}
        for tid, t in TEMPLATES.items()],
        "suggestions": SUGGESTIONS}


@app.post("/api/v1/jobs", status_code=201)
def create_job(body: CreateJob):
    platforms = [p for p in body.platforms if p in ("android", "windows")]
    if not platforms:
        raise HTTPException(400, "platforms must contain android and/or windows")
    job = jobs.create(body.idea.strip(), platforms, body.lang,
                      body.name.strip()[:40])
    if body.ai_key:
        jobs.update(job["id"], ai_key=body.ai_key[:200],
                    ai_base=body.ai_base[:200])
    jobs.submit(builder.run_job, job["id"])
    return job


@app.get("/api/v1/jobs")
def list_jobs(limit: int = 50):
    out = []
    for j in jobs.list_all(min(limit, 100)):
        j.pop("ai_key", None)
        j.pop("ai_base", None)
        out.append(j)
    return {"jobs": out}


@app.get("/api/v1/jobs/{job_id}")
def get_job(job_id: str):
    job = jobs.get(job_id)
    if not job:
        raise HTTPException(404, "job not found")
    job.pop("ai_key", None)
    job.pop("ai_base", None)
    return job


@app.get("/api/v1/jobs/{job_id}/logs/stream")
def stream_logs(job_id: str):
    if not jobs.get(job_id):
        raise HTTPException(404, "job not found")

    def gen():
        seen = 0
        while True:
            job = jobs.get(job_id)
            if not job:
                break
            for line in job["logs"][seen:]:
                yield f"data: {json.dumps({'log': line}, ensure_ascii=False)}\n\n"
            seen = len(job["logs"])
            snap = {"progress": job["progress"], "status": job["status"]}
            yield f"data: {json.dumps(snap)}\n\n"
            if job["status"] in jobs.TERMINAL:
                break
            time.sleep(1)
    return StreamingResponse(gen(), media_type="text/event-stream")


@app.get("/api/v1/jobs/{job_id}/preview")
def preview(job_id: str):
    f = config.JOBS / job_id / "src" / "web" / "index.html"
    if not f.exists():
        raise HTTPException(404, "preview not ready yet")
    return FileResponse(f, media_type="text/html")


@app.get("/api/v1/artifacts/{job_id}/{filename}")
def artifact(job_id: str, filename: str):
    if "/" in filename or "\\" in filename or filename.startswith("."):
        raise HTTPException(400, "bad filename")
    f = config.JOBS / job_id / "out" / filename
    if not f.exists() or not f.is_file():
        raise HTTPException(404, "artifact not found")
    return FileResponse(f, filename=filename)


@app.post("/api/v1/admin/cleanup")
def admin_cleanup():
    jobs.cleanup(config.JOB_RETENTION_DAYS, config.MAX_JOBS)
    return {"ok": True, "jobs": len(jobs.list_all(100000))}


static_dir = Path(__file__).parent / "static"
if static_dir.exists():
    app.mount("/", StaticFiles(directory=static_dir, html=True), name="web")
