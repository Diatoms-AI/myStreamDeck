import os
import socket
import subprocess
import threading
import time

import pystray
from PIL import Image, ImageDraw

PORT = 8765
NODE_SCRIPT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "server.js")

_proc = None
_lock = threading.Lock()


# ── Icon drawing ────────────────────────────────────────────────────────────

def _make_icon(running: bool) -> Image.Image:
    sz = 64
    img = Image.new("RGBA", (sz, sz), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Dark background tile
    d.rounded_rectangle([0, 0, sz - 1, sz - 1], radius=10, fill=(13, 17, 23))

    # 3×2 grid of small Stream Deck "buttons"
    cols, rows, pad, gap = 3, 2, 9, 5
    bw = (sz - 2 * pad - (cols - 1) * gap) // cols
    bh = (sz - 2 * pad - (rows - 1) * gap) // rows
    for r in range(rows):
        for c in range(cols):
            x = pad + c * (bw + gap)
            y = pad + r * (bh + gap)
            d.rounded_rectangle([x, y, x + bw, y + bh], radius=2, fill=(42, 58, 78))

    # Status dot — bottom-right corner
    dot = 14
    ox, oy = sz - dot - 2, sz - dot - 2
    color = (46, 204, 113) if running else (231, 76, 60)   # green / red
    d.ellipse([ox, oy, ox + dot, oy + dot], fill=color)
    # White centre of dot
    inner = 5
    cx, cy = ox + dot // 2, oy + dot // 2
    d.ellipse([cx - inner, cy - inner, cx + inner, cy + inner], fill=(255, 255, 255, 180))

    return img


# ── Server control ──────────────────────────────────────────────────────────

def _port_in_use() -> bool:
    with socket.socket() as s:
        s.settimeout(0.3)
        return s.connect_ex(("127.0.0.1", PORT)) == 0


def _is_running() -> bool:
    with _lock:
        if _proc is not None and _proc.poll() is None:
            return True
    return _port_in_use()


def _start(icon, item=None):
    global _proc
    if _is_running():
        _refresh(icon)
        return
    with _lock:
        _proc = subprocess.Popen(
            ["node", NODE_SCRIPT],
            creationflags=subprocess.CREATE_NO_WINDOW,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
    time.sleep(0.5)
    _refresh(icon)


def _stop(icon, item=None):
    global _proc
    with _lock:
        if _proc:
            _proc.terminate()
            _proc = None
    # Also kill any external process holding the port
    if _port_in_use():
        _kill_port()
    _refresh(icon)


def _kill_port():
    try:
        r = subprocess.run(
            ["netstat", "-ano"],
            capture_output=True, text=True
        )
        for line in r.stdout.splitlines():
            if f":{PORT}" in line and "LISTENING" in line:
                pid = line.strip().split()[-1]
                subprocess.run(
                    ["taskkill", "/F", "/PID", pid],
                    capture_output=True
                )
    except Exception:
        pass


def _refresh(icon):
    running = _is_running()
    icon.icon  = _make_icon(running)
    status     = f"● Running on :{PORT}" if running else "○ Stopped"
    icon.title = f"myStreamDeck  {status}"


def _toggle(icon, item):
    if _is_running():
        _stop(icon)
    else:
        _start(icon)


def _exit(icon, item):
    _stop(icon)
    icon.stop()


# ── Setup & run ─────────────────────────────────────────────────────────────

def _setup(icon):
    icon.visible = True
    _start(icon)                          # auto-start server on launch

    def _watch():                         # refresh icon if server dies
        while icon.visible:
            time.sleep(3)
            _refresh(icon)

    threading.Thread(target=_watch, daemon=True).start()


menu = pystray.Menu(
    pystray.MenuItem(
        lambda text, item: "Stop Server" if _is_running() else "Start Server",
        _toggle,
        default=True,
    ),
    pystray.Menu.SEPARATOR,
    pystray.MenuItem("Exit", _exit),
)

tray = pystray.Icon(
    "myStreamDeck",
    _make_icon(False),
    "myStreamDeck  ○ Stopped",
    menu,
)
tray.run(_setup)
