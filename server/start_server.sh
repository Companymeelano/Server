#!/bin/sh
# One-click launcher for macOS / Linux.
cd "$(dirname "$0")" || exit 1
if ! command -v python3 >/dev/null 2>&1; then
  echo "[ERROR] python3 not found. Install Python 3.10+."
  exit 1
fi
python3 -m pip install -q -r requirements.txt || exit 1
LAN=$(python3 -c "import socket;s=socket.socket(socket.AF_INET,socket.SOCK_DGRAM);s.settimeout(2);s.connect(('8.8.8.8',80));print(s.getsockname()[0])")
echo "============================================================"
echo " MeeLano Builder Server is running!"
echo " On this computer : http://localhost:8000"
echo " On your phone    : http://${LAN}:8000  (same Wi-Fi)"
echo "============================================================"
exec python3 -m uvicorn app.main:app --host 0.0.0.0 --port 8000
