"""
Macro recorder — captures mouse clicks and key presses with inter-event delays.
Usage: python recorder.py <output_file.json>
Stop : send SIGTERM or KeyboardInterrupt (Ctrl+C).
"""
import json
import sys
import time
import threading
from pynput import mouse, keyboard

output_file = sys.argv[1] if len(sys.argv) > 1 else "macro.json"
events = []
last_time = [time.time()]
_lock = threading.Lock()
_stop = threading.Event()


def _elapsed_ms():
    now = time.time()
    ms = int((now - last_time[0]) * 1000)
    last_time[0] = now
    return ms


def _save():
    try:
        with open(output_file, "w") as f:
            json.dump(events, f, indent=2)
    except Exception:
        pass


def on_click(x, y, button, pressed):
    if not pressed:
        return
    with _lock:
        events.append({
            "type": "click",
            "x": int(x),
            "y": int(y),
            "button": str(button).replace("Button.", ""),
            "delay_ms": _elapsed_ms(),
        })
        _save()


def on_press(key):
    with _lock:
        try:
            k = key.char
        except AttributeError:
            k = str(key).replace("Key.", "")
        events.append({
            "type": "key",
            "key": k,
            "delay_ms": _elapsed_ms(),
        })
        _save()


ml = mouse.Listener(on_click=on_click)
kl = keyboard.Listener(on_press=on_press)
ml.start()
kl.start()

try:
    _stop.wait()
except (KeyboardInterrupt, SystemExit):
    pass
finally:
    ml.stop()
    kl.stop()
    _save()
