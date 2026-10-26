package com.focusshield.app.ui

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class OverlayWindowManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun showBlockOverlay(reason: String, onDismiss: () -> Unit) {
        mainHandler.post {
            if (overlayView != null) return@post

            val layoutParams = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                format = PixelFormat.TRANSLUCENT
                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                gravity = Gravity.CENTER
            }

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor("#E60F172A")) // Slate 900 with 90% opacity
                setPadding(60, 60, 60, 60)
            }

            // Card Container
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                val shape = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E293B"))
                    cornerRadius = 48f
                    setStroke(2, Color.parseColor("#334155"))
                }
                background = shape
                setPadding(70, 80, 70, 80)
            }

            // Title
            val titleText = TextView(context).apply {
                text = "🛡️ Focus Shield Active"
                setTextColor(Color.WHITE)
                textSize = 22f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }

            // Reason / Message
            val messageText = TextView(context).apply {
                text = reason
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 15f
                gravity = Gravity.CENTER
                setPadding(0, 30, 0, 50)
            }

            // Action Button
            val returnButton = Button(context).apply {
                text = "Return to Study"
                setTextColor(Color.WHITE)
                val btnShape = GradientDrawable().apply {
                    setColor(Color.parseColor("#6366F1")) // Indigo
                    cornerRadius = 24f
                }
                background = btnShape
                setPadding(40, 20, 40, 20)
                setOnClickListener {
                    hideOverlay()
                    onDismiss()
                }
            }

            card.addView(titleText)
            card.addView(messageText)
            card.addView(returnButton)
            container.addView(card)

            overlayView = container
            try {
                windowManager.addView(container, layoutParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun hideOverlay() {
        mainHandler.post {
            overlayView?.let {
                try {
                    windowManager.removeView(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                overlayView = null
            }
        }
    }
}
