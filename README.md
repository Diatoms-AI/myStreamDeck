# My Stream Deck Android App

A native Android Stream Deck — 15 configurable macro buttons in a 3×5 grid, with live camera preview and global settings panel.

## Features

- **3×5 macro button grid** — 15 individually configurable buttons
- **Flexible actions** — HTTP/API calls, app launch, multi-step macros, keystrokes, system toggles
- **Camera panel** — live CameraX preview strip on the right side
- **Macro builder** — chain multiple actions with custom delays
- **JSON import/export** — back up and share button configurations

## Tech Stack

| Layer | Library |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Repository |
| Networking | Retrofit + OkHttp |
| Database | Room (SQLite) |
| Camera | CameraX |
| DI | Hilt |
| Min SDK | API 26 (Android 8.0) |

## Roadmap

- [ ] Phase 1 — Static grid UI + Room schema
- [ ] Phase 2 — Action engine (API calls, app launch)
- [ ] Phase 3 — Button editor screen
- [ ] Phase 4 — Macro support
- [ ] Phase 5 — Camera panel + settings
- [ ] Phase 6 — Polish, haptics, animations, release
