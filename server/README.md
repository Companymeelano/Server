# MeeLano Builder — server

FastAPI build server + web UI. One short idea in → installable apps out.

## Run

```bash
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
# Web UI: http://localhost:8000
```

Docker:

```bash
docker build -t mbuilder .
docker run -p 8000:8000 -e GITHUB_REPO=... -e GITHUB_TOKEN=... mbuilder
```

Tests: `python -m pytest tests/ -q`

## Environment

| Var | Purpose |
|---|---|
| `BUILDER_DATA` | job workspace dir (default `server/data`) |
| `OPENAI_API_KEY` / `OPENAI_BASE_URL` / `OPENAI_MODEL` | optional AI customisation |
| `GITHUB_TOKEN` / `GITHUB_REPO` | cloud builds via Actions |
| `CLOUD_BUILD_WORKFLOW` | workflow file (default `build-generated.yml`) |
| `CLOUD_BUILD_TIMEOUT_MIN` | cloud wait (default 40) |
| `PUBLIC_BASE_URL` | absolute artifact links (optional) |

## How a job flows

1. `POST /api/v1/jobs {idea, platforms}` → id
2. classify (EN/FA keywords) → template → render `web/` + `windows/` + `android/` + `installer/`
3. optional LLM rewrite of the web app (server key or per-job `ai_key`)
4. local PyInstaller/NSIS/Gradle if present → else GitHub Actions cloud build → else source bundle + scripts
5. poll `GET /api/v1/jobs/{id}` / SSE `/logs/stream`; fetch preview + artifacts

Headless build (used by CI): `python local_build.py --template blog --name "My Blog" --platform all --out dist`
