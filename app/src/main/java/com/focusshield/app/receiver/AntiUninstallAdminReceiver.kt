package com.focusshield.app.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * DeviceAdminReceiver registers FocusShield as an active Device Administrator.
 * When enabled, the Android OS natively blocks direct uninstallation attempts
 * from the app launcher or home screen.
 */
class AntiUninstallAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(
            context,
            "GoalYourDream: Anti-Uninstall protection is now ACTIVE",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(
            context,
            "GoalYourDream: Anti-Uninstall protection disabled",
            Toast.LENGTH_SHORT
        ).show()
    }
}
