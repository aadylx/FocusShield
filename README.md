# 🛡️ FocusShield (Regain Study Mode Clone)

FocusShield is a digital wellbeing and strict focus application built for Android in **Kotlin** and **Jetpack Compose**. It is designed specifically for students to eliminate phone distractions while keeping essential study tools available.

---

## ✨ Features Included

1. **Anti-Uninstall & Anti-Bypass Protection (Strict Mode):**
   - Registers as an Android **Device Administrator** (`AntiUninstallAdminReceiver`).
   - Android natively rejects any direct uninstallation attempts while the admin is active.
   - During an active focus session, the `AccessibilityService` detects and locks attempts to enter phone Settings (`com.android.settings`), preventing users from force-stopping the app or deactivating Device Admin.

2. **YouTube Study Mode (Shorts & Feed Blocker):**
   - Allows YouTube to open so you can watch classes, tutorials, and recorded lectures.
   - Real-time `AccessibilityNodeInfo` tree inspection automatically detects the **Shorts container** or the "Shorts" tab and instantly kicks the user back.

3. **Study Whitelist (Google & YouTube Only):**
   - During focus sessions, any non-whitelisted app (Instagram, WhatsApp, games, TikTok) is blocked with an immediate home redirection and a floating lock overlay.
   - Allowed by default:
     - `com.google.android.youtube` (YouTube Lectures)
     - `com.android.chrome` (Chrome Browser)
     - `com.google.android.googlequicksearchbox` (Google Search)

4. **Persistent Countdown Timer:**
   - Runs inside a `ForegroundService` with ongoing notification so Android's battery saver doesn't terminate the timer.

---

## 📁 Project Structure

```
FocusShield/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/focusshield/app/
│       │   ├── MainActivity.kt               # Jetpack Compose Dashboard UI & Permissions
│       │   ├── manager/
│       │   │   └── FocusSessionManager.kt   # Singleton state manager & whitelist logic
│       │   ├── model/
│       │   │   └── SessionState.kt          # Session models & configurations
│       │   ├── receiver/
│       │   │   └── AntiUninstallAdminReceiver.kt # Device Administrator protection
│       │   ├── service/
│       │   │   ├── FocusAccessibilityService.kt  # Intercepts blocked apps & Shorts
│       │   │   └── FocusTimerService.kt          # Background countdown timer
│       │   └── ui/
│       │       └── OverlayWindowManager.kt       # Floating block screen
│       └── res/
│           ├── values/
│           └── xml/
│               ├── accessibility_service_config.xml
│               └── device_admin_policies.xml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 🚀 How to Run the App

1. **Open in Android Studio:**
   - Launch **Android Studio**.
   - Select **Open** and choose the folder:
     `C:\Users\Owner\.gemini\antigravity\scratch\FocusShield`
   - Let Gradle sync the dependencies.

2. **Connect Device or Launch Emulator:**
   - Connect your Android phone via USB with **USB Debugging enabled**, or launch an Android Virtual Device (API 30+ recommended).
   - Click the green **Run (▶)** button.

3. **Grant Required Permissions (First Time):**
   - **Accessibility Service:** Toggle `FocusShield` to ON.
   - **Device Administrator:** Tap "Activate this device admin app".
   - **Overlay Permission:** Enable "Allow display over other apps".

4. **Start a Session:**
   - Select your duration (e.g., 25 mins).
   - Ensure "Strict Mode" and "Block YouTube Shorts" are toggled ON.
   - Tap **Start Study Session**.
