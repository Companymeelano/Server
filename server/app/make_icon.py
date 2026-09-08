"""Procedural launcher icon + splash theme for every generated app.

No designer needed: a gradient rounded-square with the app's initial,
in all Android densities (legacy + round + adaptive foreground).
Requires Pillow (in requirements.txt).
"""
import io

BG = "#241423"

LEGACY = {"mipmap-mdpi": 48, "mipmap-hdpi": 72, "mipmap-xhdpi": 96,
          "mipmap-xxhdpi": 144, "mipmap-xxxhdpi": 192}
ADAPTIVE = {"mipmap-mdpi": 108, "mipmap-hdpi": 162, "mipmap-xhdpi": 216,
            "mipmap-xxhdpi": 324, "mipmap-xxxhdpi": 432}

ADAPTIVE_XML = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_bg" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
"""

COLORS_XML = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_bg">%s</color>
</resources>
""" % BG

THEMES_XML = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.GeneratedApp" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@color/ic_bg</item>
        <item name="android:statusBarColor">@color/ic_bg</item>
        <item name="android:navigationBarColor">@color/ic_bg</item>
        <item name="android:windowSplashScreenBackground">@color/ic_bg</item>
        <item name="android:windowSplashScreenAnimatedIcon">@mipmap/ic_launcher</item>
    </style>
</resources>
"""


def _rgb(h: str) -> tuple:
    h = h.strip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def _initial(app_name: str) -> str:
    for ch in app_name.strip():
        if ch.isascii() and ch.isalnum():
            return ch.upper()
    return "M"  # non-latin names get the MeeLano M


def _gradient(size: int, top: tuple, bottom: tuple):
    from PIL import Image
    img = Image.new("RGB", (size, size))
    px = img.load()
    for y in range(size):
        k = y / max(size - 1, 1)
        px[0, y] = 0  # touch
        c = tuple(int(top[i] + (bottom[i] - top[i]) * k) for i in range(3))
        for x in range(size):
            px[x, y] = c
    return img


def _rounded(img, radius: int):
    from PIL import Image, ImageDraw
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0) + img.size, radius=radius, fill=255)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img.convert("RGBA"), mask=mask)
    return out


def _letter(size: int, ch: str, color=(255, 255, 255), shadow=True):
    from PIL import Image, ImageDraw, ImageFont
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    try:
        font = ImageFont.load_default(size=int(size * 0.56))
    except TypeError:  # very old Pillow
        font = ImageFont.load_default()
    bbox = d.textbbox((0, 0), ch, font=font)
    w, h = bbox[2] - bbox[0], bbox[3] - bbox[1]
    x, y = (size - w) / 2 - bbox[0], (size - h) / 2 - bbox[1]
    if shadow:
        d.text((x + size * 0.02, y + size * 0.03), ch, font=font,
               fill=(0, 0, 0, 160))
    d.text((x, y), ch, font=font, fill=color + (255,))
    return img


def _png(img) -> bytes:
    buf = io.BytesIO()
    img.save(buf, "PNG", optimize=True)
    return buf.getvalue()


def make_icon(app_name: str, accent: str, size: int) -> bytes:
    """Full legacy launcher icon (gradient + initial)."""
    from PIL import Image
    top, bottom = _rgb(accent), tuple(max(0, c - 90) for c in _rgb(accent))
    img = _rounded(_gradient(size, top, bottom), int(size * 0.22))
    out = Image.alpha_composite(
        img, _letter(size, _initial(app_name)))
    return _png(out.convert("RGB"))


def make_round(app_name: str, accent: str, size: int) -> bytes:
    """Circular variant."""
    from PIL import Image, ImageDraw
    top, bottom = _rgb(accent), tuple(max(0, c - 90) for c in _rgb(accent))
    img = _gradient(size, top, bottom)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size, size), fill=255)
    base = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    base.paste(img.convert("RGBA"), mask=mask)
    out = Image.alpha_composite(base, _letter(size, _initial(app_name)))
    flat = Image.new("RGBA", out.size, _rgb(BG) + (255,))
    flat.paste(out, mask=out)
    return _png(flat.convert("RGB"))


def make_foreground(app_name: str, size: int) -> bytes:
    """Adaptive-icon foreground: glyph on transparency (safe zone)."""
    from PIL import Image
    inner = int(size * 0.62)
    glyph = _letter(inner, _initial(app_name)).resize((inner, inner))
    fg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    fg.alpha_composite(glyph, ((size - inner) // 2, (size - inner) // 2))
    return _png(fg)


def android_branding_files(app_name: str, accent: str) -> dict:
    """All icon/theme files for the generated Android project."""
    base = "android/app/src/main/res"
    files = {
        f"{base}/values/themes.xml": THEMES_XML,
        f"{base}/values/colors.xml": COLORS_XML,
        f"{base}/mipmap-anydpi-v26/ic_launcher.xml": ADAPTIVE_XML,
        f"{base}/mipmap-anydpi-v26/ic_launcher_round.xml": ADAPTIVE_XML,
    }
    for folder, size in LEGACY.items():
        files[f"{base}/{folder}/ic_launcher.png"] = make_icon(
            app_name, accent, size)
        files[f"{base}/{folder}/ic_launcher_round.png"] = make_round(
            app_name, accent, size)
    for folder, size in ADAPTIVE.items():
        files[f"{base}/{folder}/ic_launcher_foreground.png"] = make_foreground(
            app_name, size)
    return files
