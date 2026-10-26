package com.focusshield.app.manager

import com.focusshield.app.model.SessionConfig
import com.focusshield.app.model.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FocusSessionManager {

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    fun startSession(config: SessionConfig) {
        val totalSecs = config.durationMinutes * 60L
        _sessionState.value = SessionState(
            isRunning = true,
            remainingSeconds = totalSecs,
            totalSeconds = totalSecs,
            config = config
        )
    }

    fun updateRemainingSeconds(seconds: Long) {
        if (_sessionState.value.isRunning) {
            _sessionState.value = _sessionState.value.copy(remainingSeconds = seconds)
        }
    }

    fun stopSession() {
        _sessionState.value = _sessionState.value.copy(
            isRunning = false,
            remainingSeconds = 0
        )
    }

    fun isAppAllowed(packageName: String): Boolean {
        val state = _sessionState.value
        if (!state.isRunning) return true

        // Our own app is always allowed
        if (packageName == "com.focusshield.app") return true

        // Core system UI (Launcher / System UI / Keyguard) must not be locked
        val systemWhitelisted = setOf(
            "com.android.systemui",
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher",
            "com.android.launcher3",
            "com.sec.android.app.launcher",
            "com.miui.home",
            "com.oppo.launcher"
        )
        if (packageName in systemWhitelisted) return true

        return packageName in state.config.allowedPackages
    }

    fun isStrictModeActive(): Boolean {
        val state = _sessionState.value
        return state.isRunning && state.config.isStrictMode
    }

    fun isShortsBlockEnabled(): Boolean {
        val state = _sessionState.value
        return state.isRunning && state.config.blockYouTubeShorts
    }
}
