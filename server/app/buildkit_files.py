"""Static build-kit file templates: Android wrapper project + NSIS installer."""

ANDROID_SETTINGS = """pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "{{SLUG}}"
include(":app")
"""

ANDROID_ROOT_BUILD = """buildscript {
    repositories { google(); mavenCentral() }
    dependencies { classpath("com.android.tools.build:gradle:8.5.2") }
}
allprojects { repositories { google(); mavenCentral() } }
tasks.register("clean", Delete) { delete(rootProject.buildDir) }
"""

ANDROID_APP_BUILD = """plugins { id("com.android.application") }
android {
    namespace = "com.meelano.generated"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.meelano.generated.{{SLUG_DOT}}"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release { minifyEnabled = false }
        debug { applicationIdSuffix = ".debug" }
    }
    compileOptions { sourceCompatibility = "17"; targetCompatibility = "17" }
}
"""

ANDROID_MANIFEST = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <application
        android:label="{{APP_NAME}}"
        android:theme="@android:style/Theme.Material.NoActionBar"
        android:usesCleartextTraffic="true"
        android:allowBackup="true">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|keyboardHidden">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
"""

ANDROID_MAIN_ACTIVITY = """package com.meelano.generated

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {
    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)
        val s: WebSettings = web.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.allowFileAccess = true
        s.mediaPlaybackRequiresUserGesture = false
        web.webViewClient = WebViewClient()
        web.loadUrl("file:///android_asset/web/index.html")
    }

    @Deprecated("back")
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
"""

ANDROID_README = """# {{APP_NAME}} — Android project

Build the APK (needs JDK 17 + Android SDK):

    export ANDROID_HOME=/path/to/Android/Sdk
    gradle :app:assembleDebug

APK: `app/build/outputs/apk/debug/app-debug.apk` — install it directly on any phone.
"""

NSIS_SCRIPT = """; {{APP_NAME}} — Windows installer (NSIS)
Unicode true
Name "{{APP_NAME}}"
OutFile "..\\{{SLUG}}-Setup.exe"
InstallDir "$PROGRAMFILES\\{{APP_NAME}}"
RequestExecutionLevel admin

Page directory
Page instfiles

Section "Install"
  SetOutPath "$INSTDIR"
  File "..\\windows\\dist\\{{EXE_NAME}}"
  CreateShortcut "$DESKTOP\\{{APP_NAME}}.lnk" "$INSTDIR\\{{EXE_NAME}}"
  CreateDirectory "$SMPROGRAMS\\{{APP_NAME}}"
  CreateShortcut "$SMPROGRAMS\\{{APP_NAME}}\\{{APP_NAME}}.lnk" "$INSTDIR\\{{EXE_NAME}}"
  WriteUninstaller "$INSTDIR\\Uninstall.exe"
SectionEnd

Section "Uninstall"
  Delete "$INSTDIR\\{{EXE_NAME}}"
  Delete "$INSTDIR\\Uninstall.exe"
  Delete "$DESKTOP\\{{APP_NAME}}.lnk"
  RMDir "$SMPROGRAMS\\{{APP_NAME}}"
  RMDir "$INSTDIR"
SectionEnd
"""

WINDOWS_BUILD_BAT = """@echo off
REM Build {{APP_NAME}} -> single-file .exe (run on Windows with Python 3.10+)
pip install pyinstaller
pyinstaller --noconfirm --onefile --windowed --name {{SLUG}} app.py
echo EXE: dist\\{{SLUG}}.exe
"""

WINDOWS_BUILD_SH = """#!/bin/sh
# Build {{APP_NAME}} on macOS/Linux (for test) or Windows-GitBash
pip install pyinstaller
pyinstaller --noconfirm --onefile --windowed --name {{SLUG}} app.py
echo "EXE: dist/{{SLUG}}.exe"
"""


def android_project_files(slug: str, app_name: str, web_html: str) -> dict:
    slug_dot = slug.replace("-", "")
    if not slug_dot or slug_dot[0].isdigit():  # package segments can't start with a digit
        slug_dot = "app" + slug_dot
    java_pkg_path = "app/src/main/java/com/meelano/generated/MainActivity.kt"
    return {
        "android/settings.gradle": ANDROID_SETTINGS.replace("{{SLUG}}", slug),
        "android/build.gradle": ANDROID_ROOT_BUILD,
        "android/app/build.gradle": ANDROID_APP_BUILD.replace("{{SLUG_DOT}}", slug_dot),
        "android/app/src/main/AndroidManifest.xml":
            ANDROID_MANIFEST.replace("{{APP_NAME}}", app_name),
        "android/" + java_pkg_path: ANDROID_MAIN_ACTIVITY,
        "android/app/src/main/assets/web/index.html": web_html,
        "android/README.md": ANDROID_README.replace("{{APP_NAME}}", app_name),
    }


def installer_files(app_name: str, slug: str) -> dict:
    exe = slug + ".exe"
    return {
        "installer/installer.nsi": (NSIS_SCRIPT.replace("{{APP_NAME}}", app_name)
                                    .replace("{{SLUG}}", slug).replace("{{EXE_NAME}}", exe)),
        "windows/build_exe.bat": WINDOWS_BUILD_BAT.replace("{{APP_NAME}}", app_name)
                                                  .replace("{{SLUG}}", slug),
        "windows/build_exe.sh": WINDOWS_BUILD_SH.replace("{{APP_NAME}}", app_name)
                                                .replace("{{SLUG}}", slug),
        "installer/README.md": ("# Installer\n\n1. Build the exe: run `windows/build_exe.bat` "
                                "on Windows.\n2. `cd installer` then `makensis installer.nsi` "
                                "-> `%s-Setup.exe`.\n" % slug),
    }
