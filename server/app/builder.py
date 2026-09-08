"""The build pipeline: idea -> sources -> exe / setup / apk.

Order of attempts for real binaries:
  1. local toolchains (PyInstaller / NSIS / Gradle+SDK) if installed
  2. GitHub Actions cloud build (if GITHUB_TOKEN + GITHUB_REPO are set)
  3. graceful fallback: source bundle + one-click build scripts + live preview
"""
import os
import shutil
import subprocess
import time
import zipfile
from pathlib import Path

from . import config, generator, jobs
from .templates_data import TEMPLATES


def run_job(job_id: str):
    job = jobs.get(job_id)
    if not job:
        return
    idea, platforms = job["idea"], job["platforms"]
    lang = job.get("lang") or generator.detect_lang(idea)
    base = config.JOBS / job_id
    src, out = base / "src", base / "out"

    def say(m, p=None, step=None):
        jobs.log(job_id, m, progress=p, step=step)

    try:
        jobs.update(job_id, status="building", lang=lang)
        # 1. understand the idea
        say("🔍 Understanding your idea...", 5, step="analyze")
        template_id = generator.classify(idea)
        name = job.get("name") or generator.extract_name(idea, template_id, lang)
        slug = generator.slugify(name)
        t = TEMPLATES[template_id]
        jobs.update(job_id, template=template_id, name=name, slug=slug)
        say(f"✨ Matched template: {t['emoji']} {t['name_en']}  |  App name: {name}", 12)
        jobs.finish_step(job_id)

        # 2. generate sources (optional LLM customisation)
        say("🧠 Generating app sources...", 18, step="generate")
        files = generator.render(template_id, name, idea)
        custom = generator.llm_customise_web(
            idea, files["web/index.html"],
            api_key=job.get("ai_key", ""), base_url=job.get("ai_base", ""))
        if custom:
            files["web/index.html"] = custom
            files["android/app/src/main/assets/web/index.html"] = custom
            say("🤖 AI customised the app design for your idea", 26)
        for rel, content in files.items():
            p = src / rel
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text(content, encoding="utf-8")
        say(f"📝 {len(files)} source files generated", 32)
        jobs.finish_step(job_id)

        # 3. source bundle (always available)
        say("📦 Packing source bundle...", 36, step="package")
        bundle = out / f"{slug}-source.zip"
        with zipfile.ZipFile(bundle, "w", zipfile.ZIP_DEFLATED) as z:
            for f in sorted(src.rglob("*")):
                if f.is_file():
                    z.write(f, f.relative_to(src))
        jobs.add_artifact(job_id, "source", "Source code (.zip)",
                          bundle.name, bundle.stat().st_size)
        say("📦 Source bundle ready", 40)
        jobs.finish_step(job_id)

        built = {"windows": [], "android": []}

        # 4. Windows build (local PyInstaller/NSIS)
        if "windows" in platforms:
            say("🪟 Building Windows .exe ...", 46, step="windows")
            exe = _build_exe_local(job_id, src / "windows", out, slug, say)
            if exe:
                built["windows"].append(exe)
                nsi = _build_setup_local(job_id, src, out, slug, exe, say)
                if nsi:
                    built["windows"].append(nsi)
            else:
                say("⚠️ No local PyInstaller — will try cloud build later", 55)
            jobs.finish_step(job_id, ok=bool(exe))

        # 5. Android build (local Gradle + SDK)
        if "android" in platforms:
            say("🤖 Building Android APK...", 62, step="android")
            apk = _build_apk_local(job_id, src / "android", out, slug, say)
            if apk:
                built["android"].append(apk)
            else:
                say("⚠️ No local Android SDK — will try cloud build later", 70)
            jobs.finish_step(job_id, ok=bool(apk))

        # 6. Cloud build fallback (GitHub Actions)
        missing = [p for p in platforms if not built.get(p)]
        if missing:
            from . import github_cloud
            if github_cloud.enabled():
                say("☁️ Starting cloud build (GitHub Actions)...", 74, step="cloud")
                try:
                    t0 = time.time()
                    github_cloud.dispatch(template_id, name, slug, missing)
                    say("☁️ Build dispatched, waiting for runner...", 76)
                    run = github_cloud.find_run(t0)
                    if not run:
                        say("❌ Cloud build not found", 80)
                    else:
                        say(f"☁️ Run #{run.get('run_number')} "
                            f"({run.get('display_title', '')[:40]})", 78)
                        done = github_cloud.wait_run(
                            run["id"], config.CLOUD_BUILD_TIMEOUT_MIN)
                        if done and done.get("conclusion") == "success":
                            say("☁️ Downloading artifacts...", 90)
                            for f in github_cloud.download_artifacts(
                                    run["id"], out):
                                kind = ("apk" if f.suffix == ".apk"
                                        else "exe" if f.suffix == ".exe"
                                        else "file")
                                label = {"apk": "Android app (.apk)",
                                         "exe": ("Windows setup (.exe)"
                                                 if "setup" in f.name.lower()
                                                 else "Windows app (.exe)")}.get(
                                    kind, f.name)
                                jobs.add_artifact(job_id, kind, label, f.name,
                                                  f.stat().st_size)
                                if f.suffix == ".apk":
                                    built["android"].append(f)
                                elif f.suffix == ".exe":
                                    built["windows"].append(f)
                            say("✅ Cloud build finished", 96)
                        else:
                            say("❌ Cloud build failed or timed out", 90)
                except Exception as e:
                    say(f"❌ Cloud build error: {e}", 90)
                jobs.finish_step(job_id, ok=any(built.values()))
            else:
                say("ℹ️ Tip: set GITHUB_TOKEN/GITHUB_REPO for automatic cloud "
                    "builds, or run the one-click scripts in the source bundle.",
                    92, step="cloud")
                jobs.finish_step(job_id)

        # 7. done
        n_bin = len(built["windows"]) + len(built["android"])
        if n_bin:
            status = "done"
            say(f"🎉 Done! {n_bin} installable file(s) ready + live preview", 100)
        else:
            status = "partial"
            say("✅ Sources + preview ready (binaries need a build machine — "
                "see README in the bundle)", 100)
        jobs.update(job_id, status=status, progress=100)
    except Exception as e:
        jobs.log(job_id, f"❌ Build failed: {e}", progress=100)
        jobs.update(job_id, status="failed", error=str(e)[:500])


def _run(cmd, cwd: Path, timeout: int = 900) -> tuple[int, str]:
    try:
        p = subprocess.run(cmd, cwd=cwd, capture_output=True, text=True,
                           timeout=timeout)
        return p.returncode, (p.stdout + p.stderr)[-4000:]
    except FileNotFoundError:
        return 127, "command not found"
    except subprocess.TimeoutExpired:
        return 124, "timeout"


def _build_exe_local(job_id, wdir: Path, out: Path, slug: str, say) -> Path | None:
    if not shutil.which("pyinstaller") and _run(
            ["python", "-m", "PyInstaller", "--version"], wdir, 60)[0] != 0:
        return None
    say("   ... compiling with PyInstaller (this takes a while)", 50)
    code, log = _run(["pyinstaller", "--noconfirm", "--onefile", "--windowed",
                      "--name", slug, "app.py"], wdir, timeout=1200)
    if code != 0:
        say("   PyInstaller output: " + log[-600:], 54)
        return None
    exe = wdir / "dist" / f"{slug}.exe"
    if not exe.exists():  # non-windows host produces extensionless binary
        exe = wdir / "dist" / slug
    if not exe.exists():
        return None
    dest = out / f"{slug}.exe"
    shutil.copy(exe, dest)
    jobs.add_artifact(job_id, "exe", "Windows app (.exe)", dest.name,
                      dest.stat().st_size)
    say(f"✅ EXE ready ({dest.stat().st_size // 1024} KB)", 58)
    return dest


def _build_setup_local(job_id, src: Path, out: Path, slug: str,
                       exe: Path, say) -> Path | None:
    if not shutil.which("makensis"):
        (out / "BUILD_SETUP.txt").write_text(
            "To create the installer on Windows:\n  makensis installer\\installer.nsi\n",
            encoding="utf-8")
        return None
    nsi = src / "installer" / "installer.nsi"
    code, log = _run(["makensis", str(nsi)], src / "installer", 600)
    setup = src / f"{slug}-Setup.exe"
    if code == 0 and setup.exists():
        dest = out / setup.name
        shutil.move(str(setup), dest)
        jobs.add_artifact(job_id, "exe", "Windows setup (.exe)", dest.name,
                          dest.stat().st_size)
        say("✅ Setup installer ready", 60)
        return dest
    say("   NSIS output: " + log[-400:], 60)
    return None


def _build_apk_local(job_id, adir: Path, out: Path, slug: str, say) -> Path | None:
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    gradle = shutil.which("gradle")
    if not (sdk and gradle):
        return None
    say("   ... compiling with Gradle (first run downloads a lot)", 66)
    code, log = _run([gradle, ":app:assembleDebug", "--no-daemon"], adir,
                     timeout=1800)
    apk = adir / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
    if code == 0 and apk.exists():
        dest = out / f"{slug}.apk"
        shutil.copy(apk, dest)
        jobs.add_artifact(job_id, "apk", "Android app (.apk)", dest.name,
                          dest.stat().st_size)
        say(f"✅ APK ready ({dest.stat().st_size // 1024} KB) — install it "
            "directly on any phone", 70)
        return dest
    say("   Gradle output: " + log[-600:], 70)
    return None
