package com.focusshield.app.model

data class SessionConfig(
    val durationMinutes: Int = 25,
    val isStrictMode: Boolean = true,
    val blockYouTubeShorts: Boolean = true,
    val allowedPackages: Set<String> = setOf(
        "com.google.android.youtube",             // YouTube (for lectures)
        "com.android.chrome",                     // Chrome browser
        "com.google.android.googlequicksearchbox" // Google Search
    )
)

data class SessionState(
    val isRunning: Boolean = false,
    val remainingSeconds: Long = 0,
    val totalSeconds: Long = 0,
    val config: SessionConfig = SessionConfig()
)
