# Nidra Sleep

A simple, private sleep tracker for Android. Dark UI with bold bars so you can see your sleep per **day**, **week** and **month** (with previous-period navigation).

## Features
- Log bedtime, wake time, quality (1–5 stars) and an optional note
- Day view: hours vs goal plus a bedtime-to-wake timeline
- Week and month bar charts coloured by goal (met / close / short), tap a bar for details
- Sleep score, goal-met count and a quality mix bar
- 7-day sleep debt, bedtime consistency, average bed/wake times, goal streak
- Personalised tips, history list, adjustable sleep goal
- JSON export; demo data for trying it out
- 100% on-device storage, nothing is uploaded

## Get the APK
Every push to `main` or `sleep-tracker` builds the app on GitHub Actions. Open the **Releases** page of this repo on your phone and download `nidra-sleep.apk`, then allow "install unknown apps" for your browser when asked.

## Run in a browser
```
npm start
```

## Build locally
Requires Node 20, JDK 17 and the Android SDK:
```
npm install
npx cap add android
npx cap sync android
cd android && ./gradlew assembleDebug
```
