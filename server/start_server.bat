@echo off
title MeeLano Builder Server
cd /d %~dp0
echo [1/3] Checking Python...
where python >nul 2>nul
if errorlevel 1 (
  echo.
  echo  [ERROR] Python is not installed or not in PATH.
  echo  Install Python 3.10+ from https://www.python.org/downloads/
  echo  IMPORTANT: tick "Add python.exe to PATH" during setup.
  echo.
  pause
  exit /b 1
)
echo [2/3] Installing dependencies (first run takes a minute)...
python -m pip install -q -r requirements.txt
if errorlevel 1 (
  echo [ERROR] pip install failed. Check your internet connection.
  pause
  exit /b 1
)
for /f "delims=" %%i in ('python -c "import socket;s=socket.socket(socket.AF_INET,socket.SOCK_DGRAM);s.settimeout(2);s.connect(('8.8.8.8',80));print(s.getsockname()[0])"') do set LAN=%%i
echo.
echo  ============================================================
echo   MeeLano Builder Server is running!
echo.
echo   On THIS computer open :  http://localhost:8000
echo   On YOUR PHONE enter   :  http://%LAN%:8000
echo   (phone and PC must be on the same Wi-Fi)
echo  ============================================================
echo.
echo [3/3] Server log (keep this window open, Ctrl+C to stop):
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
pause
