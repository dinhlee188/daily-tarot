# Daily Tarot

Offline Android tarot app.

## V1
- Full 78-card deck
- Fresh shuffle per reading
- Upright/reversed orientation locked at shuffle time
- Daily / Situation / Custom spreads
- Pick order 1 → 2 → 3 preserved
- Copy result for ChatGPT reading
- Local history
- No account, no backend, no internet required for app use

## Build
GitHub Actions builds a debug APK on every push to main.

Open Actions → Build Android APK → latest run → Artifacts → daily-tarot-apk.

Install app-debug.apk on Android. You may need to allow installing unknown apps for the browser/files app you use to open the APK.
