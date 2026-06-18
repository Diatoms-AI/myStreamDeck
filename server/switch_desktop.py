import pyautogui
import time
import sys

delta = int(sys.argv[1]) if len(sys.argv) > 1 else 0
if delta == 0:
    sys.exit(0)

direction = 'right' if delta > 0 else 'left'
steps = abs(delta)

for _ in range(steps):
    pyautogui.hotkey('ctrl', 'win', direction)
    time.sleep(0.4)

time.sleep(0.5)
