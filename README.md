# Ethereal Launcher

A clean, customizable Android launcher built with Jetpack Compose.

Ethereal is a lightweight home screen replacement focused on simplicity and deep customization. Tile-based layout, frosted glass aesthetics, no ads, no tracking.

---

## Features

- **Tile-based home screen** — arrange apps, widgets, and nested spaces however you want
- **Nested spaces** — folders reimagined as full screens with their own layouts
- **System widget support** — real Android widgets (AppWidgetHost) on your home screen
- **Built-in widgets** — clock, weather, and search
- **29 color palettes** — from AMOLED black to Catppuccin, Dracula, and Nord
- **16 font families** — system default plus bundled custom fonts
- **Frosted glass UI** — adjustable blur and opacity for panels, Dock, and E-Search
- **E-Search** — swipe up to search apps with a frosted overlay
- **Custom icon shapes** — circle, square, squircle, cut corner
- **Themed icons** — tint app icons in your theme colors (Material You style)
- **Wallpaper manager** — pick from gallery, apply to home, lock screen, or everywhere
- **Multi-language** — English and Russian
- **No ads, no tracking** — analytics is anonymous and opt-in

---

## Screenshots

> Screenshots will be added soon.

---

## Tech Stack

- **Kotlin** — 100% Kotlin codebase
- **Jetpack Compose** — declarative UI, Material 3
- **WorkManager** — background update checks
- **Coil** — image loading (with GIF and adaptive icons support)
- **Firebase Analytics** — optional, anonymous usage stats
- **GitHub Releases API** — in-app update notifications

---

## Requirements

- Android 8.1 (API 27) or higher
- No Google Play Services required (except for optional analytics)

---

## Building

### From source

```bash
git clone https://github.com/Neytron951/Ethereal.git
cd Ethereal
./gradlew assembleDebug