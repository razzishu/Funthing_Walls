# 🎨 Funthing Walls

An advanced, open-source Android Wallpaper Engine built with Kotlin and Material Design 3.

Funthing Walls goes beyond static images by featuring a smart-cropping algorithm for dual-screen pairings, a Live Video wallpaper engine that dynamically extracts Material You colors, and a background Ghost Worker for automated aesthetic shifts.

## ✨ Flagship Features
* **Live Video Engine:** Stream high-definition MP4s from Pexels or apply local gallery videos as your wallpaper.
* **True Material You Integration:** The app dynamically extracts primary, secondary, and tertiary color formulas directly from active video wallpapers to tint the Android System UI (Android 12+).
* **Smart Center-Crop Algorithm:** Automatically slices ultra-wide images into mathematically perfect Lock Screen and Home Screen pairings.
* **Ghost Worker Auto-Changer:** A memory-safe background `WorkManager` that updates your wallpaper automatically every 1, 6, or 24 hours.
* **No-Compromise Local Storage:** Bypass Android's standard memory limits to apply uncompressed, massive images from your own phone gallery.

## 🛠️ Built With
* **Kotlin:** 100% Kotlin architecture.
* **Coroutines & Flow:** For asynchronous API calls and safe background threading.
* **Coil:** High-performance image loading and caching.
* **Retrofit2 & Gson:** Seamless connection to the Pexels REST API.
* **Material Components:** Adaptive layout featuring a custom floating navigation pill and dynamic color theming.

## 🚀 How to Build
1. Clone this repository: `git clone https://github.com/razzishu/FunthingWalls.git`
2. Open the project in **Android Studio**.
3. Build and run!
   *Note: You will need to input your own free Pexels API key inside the app's Settings menu to fetch online wallpapers.*

## 📬 Contact & Support
* **Developer:** [@razzishu](https://github.com/razzishu)
* **Telegram:** [@Razz_ishu](https://t.me/Razz_ishu)