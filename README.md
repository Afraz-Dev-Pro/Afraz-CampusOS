# Afraz CampusOS

CampusOS is an Android student super-app with application ID `com.afraz.campusos`. It uses Kotlin, Jetpack Compose, Room, Hilt, DataStore, Google Play services Document Scanner, native PDF generation, Retrofit/OkHttp, and Firebase's client SDK.

## Build

Open the project in Android Studio with JDK 17 or newer and Android SDK 35. On Windows, use `gradlew.bat`; on macOS/Linux, use `./gradlew`.

```shell
./gradlew assembleDebug
```

The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Cloud setup

Offline academic and toolkit features work without Firebase or sign-in. To enable teammate posts and authenticated backup, replace the `CONFIGURE_...` values in `app/src/main/res/values/strings.xml` with the Firebase Android app values and Google web OAuth client ID. Enable Google as a Firebase Authentication provider, create the Firestore database, and deploy `firestore.rules`. Google One Tap is requested only after the user chooses **Post Teammate Request** or **Cloud Backup / Export**.

The app reads hackathon and PYQ JSON from the `BASE_URL` in `CommunityFeedApi.kt`; if the endpoint is unavailable or returns empty content, the bundled JSON files keep both feeds populated. Change that URL to your published GitHub Pages JSON endpoint before release.

## Included modules

- Dashboard: next class, aggregate and lowest-subject attendance, three customizable pinned tools, 48-hour deadlines, and OEM battery guidance.
- Academics: weekly timetable, compressed URL-safe share codes and QR import/export, attendance with undo and prediction, tasks, syllabus progress, and SGPA target prediction.
- Toolkit: ML Kit scanning, local image/note PDFs, expense budgets, equal and custom UPI splits, offline document wallet, and Pomodoro service controls.
- HackHub: paginated teammate requests, skill filters, cached JSON feeds with bundled seed data, PYQ links, lazy Google sign-in, and SAF backup/restore.
- Android integrations: reboot-safe class/deadline alarms, Glance widget, and Quick Settings scan tile.

The app does not declare camera or broad external-storage permissions. It uses Android's photo picker, SAF, and the Play services scanner flow.
