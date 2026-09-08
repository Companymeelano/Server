import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app import generator


def test_classify_english():
    assert generator.classify("Create a Blog reader app") == "blog"
    assert generator.classify("build me a calculator") == "calculator"
    assert generator.classify("notes app with dark mode") == "notes"
    assert generator.classify("dice roller game") == "dice"
    assert generator.classify("weather forecast for cities") == "weather"
    assert generator.classify("mini shop with cart") == "shop"


def test_classify_persian():
    assert generator.classify("یک برنامه وبلاگ‌خوان بساز") == "blog"
    assert generator.classify("ماشین حساب ساده") == "calculator"
    assert generator.classify("برنامه یادداشت بساز") == "notes"
    assert generator.classify("یک تاس شانسی") == "dice"
    assert generator.classify("هواشناسی شهرها") == "weather"
    assert generator.classify("فروشگاه کوچک") == "shop"


def test_classify_fallback():
    assert generator.classify("xyz qwerty hello") == "starter"


def test_extract_name():
    n = generator.extract_name("Create a Blog reader app", "blog", "en")
    assert "blog" in n.lower()
    n = generator.extract_name("یک ماشین‌حساب بساز", "calculator", "fa")
    assert n


def test_render_all_templates():
    for tid in ["blog", "calculator", "notes", "dice", "weather", "shop", "starter"]:
        files = generator.render(tid, "Demo App", "demo idea")
        assert "web/index.html" in files
        assert "windows/app.py" in files
        assert "android/app/src/main/AndroidManifest.xml" in files
        assert "android/app/src/main/java/com/meelano/generated/MainActivity.kt" in files
        assert "installer/installer.nsi" in files
        assert "{{APP_NAME}}" not in files["web/index.html"]
        compile(files["windows/app.py"], "app.py", "exec")  # valid python
