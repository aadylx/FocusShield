package com.focusshield.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.focusshield.app.manager.FocusSessionManager
import com.focusshield.app.model.SessionConfig
import com.focusshield.app.receiver.AntiUninstallAdminReceiver
import com.focusshield.app.service.FocusAccessibilityService
import com.focusshield.app.service.FocusTimerService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FocusShieldTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A) // Slate 900
                ) {
                    FocusDashboardScreen(
                        onStartSession = { config -> startFocusSession(config) },
                        onStopSession = { stopFocusSession() }
                    )
                }
            }
        }
    }

    private fun startFocusSession(config: SessionConfig) {
        if (!hasAllRequiredPermissions()) {
            Toast.makeText(this, "Please grant all 3 permissions first!", Toast.LENGTH_LONG).show()
            return
        }

        FocusSessionManager.startSession(config)

        val intent = Intent(this, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopFocusSession() {
        val state = FocusSessionManager.sessionState.value
        if (state.isRunning && state.config.isStrictMode) {
            Toast.makeText(
                this,
                "Strict Mode is ACTIVE. You cannot cancel until the timer expires!",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val intent = Intent(this, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_STOP
        }
        startService(intent)
    }

    private fun hasAllRequiredPermissions(): Boolean {
        return isAccessibilityServiceEnabled() && isDeviceAdminActive() && isOverlayPermissionGranted()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceName = "$packageName/${FocusAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        return enabledServices.contains(serviceName)
    }

    private fun isDeviceAdminActive(): Boolean {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, AntiUninstallAdminReceiver::class.java)
        return dpm.isAdminActive(admin)
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(this)
    }
}

@Composable
fun FocusDashboardScreen(
    onStartSession: (SessionConfig) -> Unit,
    onStopSession: () -> Unit
) {
    val context = LocalContext.current
    val sessionState by FocusSessionManager.sessionState.collectAsState()

    var selectedDuration by remember { mutableStateOf(25) }
    var strictMode by remember { mutableStateOf(true) }
    var blockShorts by remember { mutableStateOf(true) }
    var allowYouTube by remember { mutableStateOf(true) }
    var allowChrome by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "🎯", fontSize = 32.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "GoalYourDream",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Text(
            text = "Study Mode & Anti-Uninstall Blocker",
            color = Color(0xFF94A3B8),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (sessionState.isRunning) {
            // Active Session Screen
            ActiveSessionCard(
                remainingSeconds = sessionState.remainingSeconds,
                totalSeconds = sessionState.totalSeconds,
                isStrict = sessionState.config.isStrictMode,
                onCancelClick = onStopSession
            )
        } else {
            // 1. Permission Status Section
            PermissionCheckSection(context = context)

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Duration Selector
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⏱️ Select Focus Duration",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(15, 25, 45, 60).forEach { mins ->
                            val isSelected = selectedDuration == mins
                            Button(
                                onClick = { selectedDuration = mins },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF6366F1) else Color(0xFF334155)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(text = "${mins}m", color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Study Whitelist & Toggles
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📚 Study Whitelist & Rules",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ToggleItem(
                        icon = Icons.Default.PlayCircle,
                        title = "Allow YouTube for Lectures",
                        subtitle = "Watch classes and educational videos",
                        checked = allowYouTube,
                        onCheckedChange = { allowYouTube = it }
                    )

                    ToggleItem(
                        icon = Icons.Default.Block,
                        title = "Block YouTube Shorts",
                        subtitle = "Kicks out of Shorts and Reels instantly",
                        checked = blockShorts,
                        onCheckedChange = { blockShorts = it }
                    )

                    ToggleItem(
                        icon = Icons.Default.Search,
                        title = "Allow Google & Chrome",
                        subtitle = "Browse articles & study resources",
                        checked = allowChrome,
                        onCheckedChange = { allowChrome = it }
                    )

                    HorizontalDivider(
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    ToggleItem(
                        icon = Icons.Default.Lock,
                        title = "Strict Mode (Anti-Uninstall)",
                        subtitle = "Blocks phone settings & prevents uninstallation until timer finishes",
                        checked = strictMode,
                        onCheckedChange = { strictMode = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Start Button
            Button(
                onClick = {
                    val allowed = mutableSetOf<String>()
                    if (allowYouTube) allowed.add("com.google.android.youtube")
                    if (allowChrome) {
                        allowed.add("com.android.chrome")
                        allowed.add("com.google.android.googlequicksearchbox")
                    }

                    onStartSession(
                        SessionConfig(
                            durationMinutes = selectedDuration,
                            isStrictMode = strictMode,
                            blockYouTubeShorts = blockShorts,
                            allowedPackages = allowed
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Study Session ($selectedDuration mins)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ActiveSessionCard(
    remainingSeconds: Long,
    totalSeconds: Long,
    isStrict: Boolean,
    onCancelClick: () -> Unit
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔥 STUDY SESSION IN PROGRESS",
                color = Color(0xFF10B981), // Emerald
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Timer Display
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF6366F1),
                    strokeWidth = 10.dp,
                    trackColor = Color(0xFF334155)
                )
                Text(
                    text = timeFormatted,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Allowed: YouTube Lectures & Google Search",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )

            if (isStrict) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Anti-Uninstall & Settings Locked",
                        color = Color(0xFFF59E0B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onCancelClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isStrict) Color(0xFF64748B) else Color(0xFFEF4444)
                )
            ) {
                Text(text = if (isStrict) "Strict Mode (Locked)" else "Cancel Session")
            }
        }
    }
}

@Composable
fun PermissionCheckSection(context: Context) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚡ Required Permissions",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            PermissionRow(
                title = "1. Accessibility Service",
                desc = "Required to block apps & YouTube Shorts",
                onGrantClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                }
            )

            PermissionRow(
                title = "2. Device Administrator",
                desc = "Required for Anti-Uninstall protection",
                onGrantClick = {
                    val admin = ComponentName(context, AntiUninstallAdminReceiver::class.java)
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                        putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "GoalYourDream uses Device Admin to prevent uninstalling during active focus hours."
                        )
                    }
                    context.startActivity(intent)
                }
            )

            PermissionRow(
                title = "3. Overlay Permission",
                desc = "Displays blocking screens over other apps",
                onGrantClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun PermissionRow(title: String, desc: String, onGrantClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = desc, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
        TextButton(onClick = onGrantClick) {
            Text(text = "Grant", color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF6366F1),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF6366F1)
            )
        )
    }
}

@Composable
fun FocusShieldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF6366F1),
            background = Color(0xFF0F172A),
            surface = Color(0xFF1E293B)
        ),
        content = content
    )
}
