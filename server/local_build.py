"""Headless builder used by GitHub Actions (and any build machine).

Example (Windows runner):
    python local_build.py --template blog --name "My Blog" --platform windows --out dist

Example (Linux runner with Android SDK):
    python local_build.py --template blog --name "My Blog" --platform android --out dist
"""
import argparse
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

try:
    import PIL  # noqa: F401  (needed for generated-app icons)
except ImportError:
    subprocess.run([sys.executable, "-m", "pip", "install", "pillow"],
                   check=True)

sys.path.insert(0, str(Path(__file__).resolve().parent))

from app import generator  # noqa: E402


def run(cmd, cwd):
    print("+", " ".join(cmd), flush=True)
    p = subprocess.run(cmd, cwd=cwd)
    if p.returncode != 0:
        raise SystemExit(f"command failed ({p.returncode}): {' '.join(cmd)}")


def _write_files(files: dict, src: Path):
    for rel, content in files.items():
        p = src / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        if isinstance(content, bytes):
            p.write_bytes(content)
        else:
            p.write_text(content, encoding="utf-8")


def _validate_apk(apk: Path):
    with zipfile.ZipFile(apk) as z:
        names = z.namelist()
    assert any(n.endswith(".dex") for n in names), "APK has no classes.dex!"
    assert "AndroidManifest.xml" in names, "APK has no manifest!"
    assert apk.stat().st_size > 100_000, "APK suspiciously small!"
    print(f"APK valid: {len(names)} entries, "
          f"{apk.stat().st_size // 1024} KB", flush=True)


def _validate_exe(exe: Path):
    assert exe.stat().st_size > 1_000_000, "EXE suspiciously small!"
    print(f"EXE valid: {exe.stat().st_size // 1024} KB", flush=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--template", default="blog")
    ap.add_argument("--name", default="My App")
    ap.add_argument("--slug", default="")
    ap.add_argument("--idea", default="")
    ap.add_argument("--platform", default="all",
                    choices=["all", "windows", "android"])
    ap.add_argument("--out", default="dist")
    args = ap.parse_args()

    from app.templates_data import TEMPLATES
    if args.template not in TEMPLATES:
        raise SystemExit(f"unknown template: {args.template}")

    idea = args.idea or f"Create a {args.name} app"
    slug = args.slug or generator.slugify(args.name)
    out = Path(args.out)
    work = Path("build-work")
    if work.exists():
        shutil.rmtree(work)
    src = work / "src"
    out.mkdir(parents=True, exist_ok=True)

    files = generator.render(args.template, args.name, idea)
    _write_files(files, src)
    print(f"generated {len(files)} files for '{args.name}' [{args.template}]",
          flush=True)

    with zipfile.ZipFile(out / f"{slug}-source.zip", "w",
                         zipfile.ZIP_DEFLATED) as z:
        for f in sorted(src.rglob("*")):
            if f.is_file():
                z.write(f, f.relative_to(src))

    if args.platform in ("all", "windows"):
        run([sys.executable, "-m", "pip", "install", "pyinstaller",
             "customtkinter"], src / "windows")
        run([sys.executable, "-m", "PyInstaller", "--noconfirm", "--onefile",
             "--windowed", "--collect-all", "customtkinter",
             "--name", slug, "app.py"], src / "windows")
        exe = src / "windows" / "dist" / (slug + ".exe")
        _validate_exe(exe)
        shutil.copy(exe, out / exe.name)
        print("EXE ->", out / exe.name, flush=True)
        if shutil.which("makensis"):
            run(["makensis", "installer.nsi"], src / "installer")
            setup = src / f"{slug}-Setup.exe"
            if setup.exists():
                _validate_exe(setup)
                shutil.move(str(setup), out / setup.name)
                print("SETUP ->", out / setup.name, flush=True)

    if args.platform in ("all", "android"):
        run(["gradle", ":app:assembleDebug", "--no-daemon"], src / "android")
        apk = (src / "android" / "app" / "build" / "outputs" / "apk" /
               "debug" / "app-debug.apk")
        _validate_apk(apk)
        shutil.copy(apk, out / f"{slug}.apk")
        print("APK ->", out / f"{slug}.apk", flush=True)

    print("DONE:", sorted(p.name for p in out.iterdir()), flush=True)


if __name__ == "__main__":
    main()
