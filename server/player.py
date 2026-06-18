"""
Macro player — replays a recorded macro JSON file.
Usage: python player.py <macro_file.json>
"""
import json
import sys
import time
import pyautogui

pyautogui.FAILSAFE = False  # don't abort on corner-move during playback
MAX_DELAY_S = 3.0           # cap inter-event delays at 3 seconds

macro_file = sys.argv[1]

with open(macro_file) as f:
    events = json.load(f)

for i, event in enumerate(events):
    delay_s = min(event.get("delay_ms", 0) / 1000.0, MAX_DELAY_S)
    if i > 0 and delay_s > 0:
        time.sleep(delay_s)

    t = event.get("type")
    if t == "click":
        btn = event.get("button", "left")
        pyautogui.click(event["x"], event["y"], button=btn if btn in ("left", "right", "middle") else "left")
    elif t == "key":
        key = event.get("key", "")
        if key:
            try:
                pyautogui.press(key)
            except Exception:
                pass
