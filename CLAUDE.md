# myStreamDeck — Project Guide

## What This Is
An Android app that acts as a physical Stream Deck, paired with a Windows companion server. The phone sends HTTP commands over USB (ADB reverse tunnel) to the PC, which executes macros, opens apps, and replays recorded mouse/keyboard sequences.

## Repo
https://github.com/Diatoms-AI/myStreamDeck

## Architecture

```
Android App (Pixel phone)
  └── taps button → POST http://localhost:8765/button/<id>  (USB tunnel)
         │
         ▼
Windows Server  (server/server.js  — Node.js on port 8765)
  ├── macros/button<id>.json exists?  → python player.py   (pyautogui replay)
  └── actions/button<id>.ps1 exists?  → PowerShell script
```

### USB Tunnel (Critical)
The Makercude WiFi has **client isolation** — phone and PC are on different subnets (10.67.x vs 10.158.x) and cannot talk over WiFi.
All traffic goes through the USB-C cable via ADB reverse:
```
adb reverse tcp:8765 tcp:8765
```
The tray app (`server/tray.pyw`) runs this automatically on every server start.

## Key Files

### Android App
| File | Purpose |
|------|---------|
| `app/src/main/java/.../MainActivity.kt` | Navigation state (Deck ↔ Config), shared button list + activeIds |
| `app/src/main/java/.../ui/MainScreen.kt` | 3×5 grid (nested Row/Column weight-based, not LazyGrid), red/green borders |
| `app/src/main/java/.../ui/MacroConfigScreen.kt` | Settings: edit label/URL, macro recorder UI |
| `app/src/main/java/.../model/MacroButton.kt` | Data model + defaultButtons (button 1 pre-wired) |
| `app/src/main/java/.../ui/theme/Theme.kt` | Dark Material3 theme |

### Windows Server
| File | Purpose |
|------|---------|
| `server/server.js` | Node.js HTTP server, dynamic routing for all 15 buttons |
| `server/tray.pyw` | Python system tray app (green/red dot), auto-starts server + ADB reverse |
| `server/recorder.py` | pynput listener — records mouse clicks + keystrokes to JSON |
| `server/player.py` | pyautogui — replays a recorded macro JSON |
| `server/actions/button1.ps1` | Opens YouTube_Reference_Tool + YouTube Transcripts on Display 2 |
| `server/macros/button<id>.json` | Saved recorded macros (created at runtime) |
| `server/Start Tray.bat` | Double-click launcher (uses %LOCALAPPDATA%\Microsoft\WindowsApps\pythonw.exe) |
| `server/Start Tray.vbs` | Silent VBScript launcher alternative |

## Build & Run

### Android
```powershell
# Build
.\gradlew.bat assembleDebug

# Deploy to Pixel (USB connected)
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n "com.diatoms.mystreamdeck/.MainActivity"

# Re-establish USB tunnel after phone reconnect
adb reverse tcp:8765 tcp:8765
```

### Windows Server
```
Double-click: server\Start Tray.bat
```
- Tray icon appears in system tray (may need to enable in Taskbar Settings → Other system tray icons)
- Green dot = server running, Red dot = stopped
- Right-click: Start/Stop Server | Exit

## Button Border Logic
- **Red border** = button has never had a successful HTTP response this session
- **Green border** = last HTTP call to this button's URL returned 200–299
- State resets on every app launch (not persisted)

## Display Layout (PC)
```
\\.\DISPLAY2  — 1920×1080  X=0     (Primary — where you work day-to-day)
\\.\DISPLAY1  — 3840×2160  X=1920  (4K panel — "Display 2" in button1.ps1)
```
Button 1 opens VS Code side-by-side on the 4K display:
- Left half:  YouTube_Reference_Tool  → X=1920, Y=0, W=1920, H=2160
- Right half: YouTube Transcripts      → X=3840, Y=0, W=1920, H=2160

## Macro Recorder Flow
1. Android: Settings gear → tap a button → "Record Macro"
2. PC: recorder.py starts capturing all mouse clicks + keystrokes
3. Android: Live event counter shown (polls /record/status every 1s)
4. Android: "Stop & Save" → stops recorder, saves to server/macros/button<id>.json
5. Button's apiUrl is auto-set to http://localhost:8765/button/<id>
6. Next tap replays via player.py with exact timing (delays capped at 3s)

## Tech Stack
| Layer | Choice |
|-------|--------|
| Android language | Kotlin 2.1.0 |
| Android UI | Jetpack Compose + Material3 |
| Min SDK | 26 (Android 8.0) |
| Build tools | AGP 9.2.1, Gradle 9.4.1 |
| Windows server | Node.js v24 |
| Tray app | Python 3.13 + pystray + Pillow |
| Macro record | Python pynput |
| Macro replay | Python pyautogui |

## Python Environment
Microsoft Store Python — `pythonw.exe` is at:
`%LOCALAPPDATA%\Microsoft\WindowsApps\pythonw.exe`
(NOT in PATH — use full path in scripts)

Installed packages: `pystray`, `Pillow`, `pynput`, `pyautogui`

## Known Issues / Next Steps
- [ ] Button state (red/green) resets on every app launch — no persistence yet
- [ ] No Windows startup shortcut created yet (copy Start Tray.bat to shell:startup)
- [ ] YouTube Virtual Desktop auto-switching not implemented (switch manually before pressing button)
- [ ] Only button 1 has a configured action (PS1 script) — buttons 2–15 need macros recorded
- [ ] Macro recorder captures ALL PC input including unintended keystrokes during recording pauses
