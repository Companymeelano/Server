import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import tempfile

tmp = tempfile.mkdtemp()
import os

os.environ["BUILDER_DATA"] = tmp

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health():
    r = client.get("/api/v1/health")
    assert r.status_code == 200
    assert r.json()["ok"] is True


def test_templates():
    r = client.get("/api/v1/templates")
    assert r.status_code == 200
    body = r.json()
    assert len(body["templates"]) >= 7
    assert len(body["suggestions"]) >= 4


def test_full_job_flow():
    r = client.post("/api/v1/jobs", json={
        "idea": "Create a Blog reader app",
        "platforms": ["android", "windows"],
        "lang": "en",
    })
    assert r.status_code == 201
    job_id = r.json()["id"]

    for _ in range(120):  # wait up to 2 min
        j = client.get(f"/api/v1/jobs/{job_id}").json()
        if j["status"] in ("done", "partial", "failed"):
            break
        time.sleep(1)
    assert j["status"] in ("done", "partial"), j
    assert j["template"] == "blog"
    kinds = [a["kind"] for a in j["artifacts"]]
    assert "source" in kinds

    # preview works
    p = client.get(f"/api/v1/jobs/{job_id}/preview")
    assert p.status_code == 200
    assert "html" in p.text.lower()

    # source bundle downloads
    src = [a for a in j["artifacts"] if a["kind"] == "source"][0]
    d = client.get(src["url"])
    assert d.status_code == 200
    assert len(d.content) > 1000
