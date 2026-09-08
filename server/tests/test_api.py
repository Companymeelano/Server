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


def test_auth_token():
    from app import config

    old = config.API_TOKEN
    try:
        config.API_TOKEN = "s3cret"
        assert client.get("/api/v1/health").status_code == 401
        r = client.get("/api/v1/health",
                       headers={"X-Builder-Token": "s3cret"})
        assert r.status_code == 200
        assert r.json()["auth"] is True
    finally:
        config.API_TOKEN = old


def test_rate_limit_and_cleanup():
    from app import config
    from app import jobs as J
    from app.main import _hits

    _hits.clear()
    old_rate = config.RATE_PER_HOUR
    try:
        config.RATE_PER_HOUR = 1000  # no interference while seeding
        for i in range(3):
            r = client.post("/api/v1/jobs", json={
                "idea": f"cleanup probe number {i}",
                "platforms": ["android"],
            })
            assert r.status_code == 201
        config.RATE_PER_HOUR = 1
        _hits.clear()
        r = client.post("/api/v1/jobs", json={
            "idea": "rate probe one", "platforms": ["android"]})
        assert r.status_code == 201
        r = client.post("/api/v1/jobs", json={
            "idea": "rate probe two", "platforms": ["android"]})
        assert r.status_code == 429
        time.sleep(4)  # let background jobs finish
        J.cleanup(retention_days=365, max_jobs=2)
        assert len(J.list_all(100)) <= 2
        r = client.post("/api/v1/admin/cleanup")
        assert r.status_code == 200
    finally:
        config.RATE_PER_HOUR = old_rate
        _hits.clear()
