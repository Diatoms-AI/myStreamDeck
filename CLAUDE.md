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
| `app/src/main/java/.../ui/MacroConfigScreen.kt` | Settings dialog with Cancel/Reset/Record/Save buttons; recording mode shows blinking indicator + Done |
| `app/src/main/java/.../model/MacroButton.kt` | Data model + defaultButtons (button 1 pre-wired) |
| `app/src/main/java/.../ui/theme/Theme.kt` | Dark Material3 theme |

### Windows Server
| File | Purpose |
|------|---------|
| `server/server.js` | Node.js HTTP server, dynamic routing for all 15 buttons — spawns `pwsh` (PS7) for PS1 scripts |
| `server/tray.pyw` | Python system tray app (green/red dot), auto-starts server + ADB reverse; must have `# -*- coding: utf-8 -*-` header |
| `server/recorder.py` | pynput listener — records mouse clicks + keystrokes to JSON |
| `server/player.py` | pyautogui — replays a recorded macro JSON |
| `server/switch_desktop.py` | pyautogui helper — switches virtual desktop by exact delta (right/left N presses as chord) |
| `server/actions/button1.ps1` | Switches to "YouTube" virtual desktop by name, opens YouTube_Reference_Tool + YouTube Transcripts side-by-side on 4K display |
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
Requires `local.properties` in project root:
```
sdk.dir=C:\\Users\\Diatom\\AppData\\Local\\Android\\Sdk
```

### Windows Server
```
Double-click: server\Start Tray.bat
```
- Tray icon appears in system tray (may need to enable in Taskbar Settings → Other system tray icons)
- Green dot = server running, Red dot = stopped
- Right-click menu: **Start/Stop Server** | **Record** (status only) | **Exit**

## Settings Dialog (MacroConfigScreen)
Four buttons at the bottom of every button's edit dialog:
- **Cancel** — discard changes and close
- **Reset** — clear label to `#N`, clear sub-label and API URL
- **Record** — start recording on PC; dialog switches to blinking indicator + **Done** button
- **Save** — save label/sub-label/URL changes

When **Done** is tapped after recording: recording stops, button URL is auto-set to
`http://localhost:8765/button/<id>`, button is saved, dialog closes. No separate Save needed.

## Macro Recorder Flow
1. Android: Settings gear → tap a button → tap **Record**
2. PC: recorder.py starts capturing all mouse clicks + keystrokes
3. Android: Live blinking indicator + event count (polls /record/status every 1s)
4. Android: tap **Done** → stops recorder, auto-saves button with URL set to server endpoint
5. Next tap replays via player.py with exact timing (delays capped at 3s)

**Important:** Recorded macros CANNOT reliably switch virtual desktops. pynput captures
Win/Ctrl/← as individual key presses; pyautogui replays them individually — the chord never fires.
Any button that switches virtual desktops must be a PS1 script, not a recorded macro.

## System Tray — Record Status
The tray's **Record** menu item is a status indicator only — the phone controls recording:
- **Greyed out** = not recording
- **Active "Recording  Button N"** = phone is in record mode for button N

Tray polls `/record/status` every 3 seconds and calls `icon.update_menu()` to rebuild the
native Win32 menu. Without `update_menu()` the menu stays frozen regardless of state changes.

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

## Virtual Desktop Switching Pattern (PS1 scripts)
All buttons that switch desktops must use this pattern (see button1.ps1):
```powershell
Import-Module VirtualDesktop -WarningAction SilentlyContinue
$targetIndex = -1
$count = Get-DesktopCount
for ($i = 0; $i -lt $count; $i++) {
    if ((Get-DesktopName (Get-Desktop $i)) -eq "DesktopLabel") { $targetIndex = $i; break }
}
if ($targetIndex -lt 0) { exit 1 }
$delta = $targetIndex - (Get-DesktopIndex (Get-CurrentDesktop))
$python = "$env:LOCALAPPDATA\Microsoft\WindowsApps\python.exe"
& $python "$PSScriptRoot\..\switch_desktop.py" $delta
Start-Sleep -Milliseconds 800
```
**Find by label, not index.** Index changes when desktops are reordered.

## Known Virtual Desktops
| Desktop Label | Button | Status |
|---------------|--------|--------|
| YouTube | 1 | Fully implemented (button1.ps1) |
| Kuzweil | 15 | Label confirmed; button15.ps1 not yet written — need to know what apps to open |

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
| Virtual desktop switching | PowerShell VirtualDesktop module (PS7) + pyautogui |

## Python Environment
Microsoft Store Python — `pythonw.exe` is at:
`%LOCALAPPDATA%\Microsoft\WindowsApps\pythonw.exe`
(NOT in PATH — use full path in scripts)

Installed packages: `pystray`, `Pillow`, `pynput`, `pyautogui`

## Known Issues / Next Steps
- [ ] Button 15: write button15.ps1 — switch to "Kuzweil" desktop; confirm apps to open
- [ ] Buttons 2–14: need actions (recorded macros or PS1 scripts)
- [ ] Button state (red/green) resets on every app launch — no persistence yet
- [ ] No Windows startup shortcut created yet (copy Start Tray.bat to shell:startup)
- [ ] Macro recorder captures ALL PC input including unintended keystrokes during recording pauses
- [x] YouTube Virtual Desktop auto-switching — resolves "YouTube" desktop by label, survives reorder
- [x] System tray Record status item syncs with phone recording state
- [x] Settings dialog: Cancel / Reset / Record / Done (auto-save) / Save buttons
