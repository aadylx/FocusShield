package com.focusshield.app.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.focusshield.app.manager.FocusSessionManager
import com.focusshield.app.ui.OverlayWindowManager

class FocusAccessibilityService : AccessibilityService() {

    private lateinit var overlayManager: OverlayWindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastInterceptTime = 0L

    override fun onCreate() {
        super.onCreate()
        overlayManager = OverlayWindowManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val state = FocusSessionManager.sessionState.value
        if (!state.isRunning) return

        val packageName = event?.packageName?.toString() ?: return

        // 1. Anti-Uninstall & Settings Tamper Protection (Strict Mode)
        if (FocusSessionManager.isStrictModeActive()) {
            if (handleSettingsAndUninstallProtection(packageName)) {
                return
            }
        }

        // 2. YouTube Specific Inspection (Allow Lectures, Block Shorts & Reels)
        if (packageName == "com.google.android.youtube") {
            if (FocusSessionManager.isShortsBlockEnabled()) {
                handleYouTubeShortsFilter()
            }
            return // YouTube itself is allowed in whitelist
        }

        // 3. Whitelist Enforcement (Only Allowed Apps Open)
        if (!FocusSessionManager.isAppAllowed(packageName)) {
            handleUnauthorizedAppLaunch(packageName)
        }
    }

    /**
     * Prevents the user from entering Settings, deactivating Device Admin,
     * or uninstalling/force-stopping the app during an active session.
     */
    private fun handleSettingsAndUninstallProtection(packageName: String): Boolean {
        val dangerousPackages = listOf(
            "com.android.settings",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller"
        )

        if (dangerousPackages.any { packageName.contains(it) }) {
            val root = rootInActiveWindow ?: return false
            val sensitiveKeywords = listOf(
                "Uninstall",
                "Force stop",
                "Deactivate",
                "GoalYourDream",
                "FocusShield",
                "Device admin",
                "Storage & cache",
                "Clear storage"
            )

            val matchedKeyword = sensitiveKeywords.firstOrNull { keyword ->
                root.findAccessibilityNodeInfosByText(keyword).isNotEmpty()
            }

            if (matchedKeyword != null) {
                throttleAction {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    showToast("⛔ Strict Mode Active: Cannot deactivate or uninstall until session ends!")
                }
                return true
            }
        }
        return false
    }

    /**
     * Inspects YouTube UI hierarchy in real-time. If the user navigates into
     * Shorts or Reels, immediately kicks them back to normal video search/home.
     */
    private fun handleYouTubeShortsFilter() {
        val root = rootInActiveWindow ?: return

        // Known Shorts View IDs and labels in YouTube Android app
        val shortsViewIds = listOf(
            "com.google.android.youtube:id/shorts_container",
            "com.google.android.youtube:id/reel_recycler",
            "com.google.android.youtube:id/pivot_shorts"
        )

        val hasShortsView = shortsViewIds.any { id ->
            root.findAccessibilityNodeInfosByViewId(id).isNotEmpty()
        }

        val hasShortsText = root.findAccessibilityNodeInfosByText("Shorts").any { node ->
            // Check if node is selected/focused tab
            node.isSelected || node.isFocused
        }

        if (hasShortsView || hasShortsText) {
            throttleAction {
                performGlobalAction(GLOBAL_ACTION_BACK)
                overlayManager.showBlockOverlay(
                    "📵 YouTube Shorts is blocked during study hours!\nOnly educational lectures and standard videos are permitted."
                ) {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            }
        }
    }

    /**
     * Blocks any app not on the allowed whitelist (e.g., Instagram, WhatsApp, Games).
     */
    private fun handleUnauthorizedAppLaunch(packageName: String) {
        throttleAction {
            performGlobalAction(GLOBAL_ACTION_HOME)
            overlayManager.showBlockOverlay(
                "📚 Study Session in Progress!\n\nThis app is locked. Only YouTube & Google Search are whitelisted right now."
            ) {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }

    private fun throttleAction(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastInterceptTime > 800) {
            lastInterceptTime = now
            action()
        }
    }

    private fun showToast(msg: String) {
        mainHandler.post {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInterrupt() {
        overlayManager.hideOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager.hideOverlay()
    }
}
