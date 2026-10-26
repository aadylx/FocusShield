package com.focusshield.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.focusshield.app.MainActivity
import com.focusshield.app.R
import com.focusshield.app.manager.FocusSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusTimerService : Service() {

    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var timerJob: Job? = null

    companion object {
        const val CHANNEL_ID = "focus_timer_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCountdown()
            ACTION_STOP -> stopCountdown()
        }
        return START_STICKY
    }

    private fun startCountdown() {
        val notification = buildNotification("Starting focus session...")
        startForeground(NOTIFICATION_ID, notification)

        timerJob?.cancel()
        timerJob = scope.launch {
            val totalSeconds = FocusSessionManager.sessionState.value.totalSeconds
            var remaining = totalSeconds

            while (isActive && remaining > 0) {
                FocusSessionManager.updateRemainingSeconds(remaining)
                updateNotification(remaining)
                delay(1000)
                remaining--
            }

            // Session completed naturally
            FocusSessionManager.stopSession()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopCountdown() {
        timerJob?.cancel()
        FocusSessionManager.stopSession()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(contentText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎯 GoalYourDream Active (Strict Mode)")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun updateNotification(remainingSeconds: Long) {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)
        val notification = buildNotification("Time remaining: $timeString | YouTube & Google allowed")

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Shield Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active study session and remaining timer"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
