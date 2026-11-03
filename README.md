# DukaanSaathi — Smart Ledger for Small Shopkeepers

An Android app (Kotlin) that solves a real everyday problem in small towns like
Chalisgaon, Maharashtra: **kirana shopkeepers still track stock and customer
credit (udhaar) in paper notebooks**. DukaanSaathi replaces the paper bahi-khata
with a fully offline app.

## Features
- **Dashboard** — today's sales total, low-stock item count, total outstanding
  udhaar, item count, and one-tap **share full ledger** (WhatsApp etc.).
- **Inventory** — add/edit/delete stock items, sell items (auto-decrements stock
  and records the sale), low-stock alerts with a red LOW badge.
- **Credit Book (udhaar)** — record "udhar diya" / "payment received" per
  customer with running outstanding balance per customer.
- **Voice input** — mic button next to item name, customer name and note fields
  (Hindi/Marathi-friendly speech recognition, uses the device's recognizer).
- **100% offline** — all data stored locally (SharedPreferences + JSON), no
  internet needed.

## Tech
- Kotlin, Android SDK 33, minSdk 24
- Jetpack: AppCompat, Fragment, RecyclerView, ViewBinding, Material Components
- No third-party libraries beyond AndroidX — JSON persistence via `org.json`
- Gradle 7.6.4 + Android Gradle Plugin 7.4.2

## Build
```bash
./gradlew assembleDebug      # debug APK (signed, installable)
./gradlew assembleRelease    # release APK (add signing config for Play Store)
```
Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`
A prebuilt copy is at the repo root: `DukaanSaathi.apk`

## Install on a phone
```bash
adb install DukaanSaathi.apk
# or copy DukaanSaathi.apk to the phone and tap it (allow "install from unknown sources")
```

## Deploy to Google Play Store
1. In Android Studio: Build > Generate Signed Bundle / APK, create a keystore,
   build `app-release.aab`.
2. Create a Google Play Developer account (one-time $25 fee) at
   https://play.google.com/console
3. Create an app, fill store listing (name, screenshots, privacy policy — the app
   needs no special permissions beyond RECORD_AUDIO for voice input; declare it
   and add a privacy policy URL), upload the `.aab`, complete content rating &
   data safety forms, then submit for review.

## Project structure
```
app/src/main/java/com/dukaansaathi/app/
  MainActivity.kt            # bottom-nav host
  data/Models.kt             # Item, CreditEntry, Sale
  data/StoreRepository.kt    # JSON persistence + business logic
  ui/DashboardFragment.kt
  ui/InventoryFragment.kt
  ui/CreditFragment.kt
  util/VoiceHelper.kt        # speech-recognition intent
  util/VoiceCapableFragment.kt
```
