package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.persistence.DatabaseProvider
import com.example.persistence.TweakConfigs
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlin.random.Random

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: FrameLayout? = null
    private var params: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Simulated Gamer Stats observable in overlay
    private val pingFlow = mutableStateOf(42)
    private val fpsFlow = mutableStateOf(119)
    private val ramUsageFlow = mutableStateOf("4.2 GB / 8 GB")
    private val boosterActive = mutableStateOf(false)
    private val smoothnessActive = mutableStateOf(true)
    private val dragUpActive = mutableStateOf(true)
    private val dragUpSens = mutableStateOf(98)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundServiceNotification()
        showFloatingBubble()
        startStatsUpdates()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "floating_window_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Free Fire Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Free Fire Optimizer Overlay Active")
            .setContentText("Tap to configure sensitivities or check metrics.")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()

        startForeground(101, notification)
    }

    private fun startStatsUpdates() {
        // Dynamic simulated updates to represent high precision real-time latency
        serviceScope.launch {
            while (isActive) {
                // Fetch dynamic config
                val db = DatabaseProvider.getDatabase(applicationContext)
                val config = db.tweakDao().getConfigDirect() ?: TweakConfigs()
                
                // Ping swings nicely around 25-50ms if boost enabled, or 70-130ms if not
                val minPing = if (config.dnsFixerEnabled) 18 else 58
                val maxPing = if (config.dnsFixerEnabled) 42 else 95
                pingFlow.value = Random.nextInt(minPing, maxPing)

                // FPS maintains high targeted performance for Vivo T2x 5G Screen
                val maxFps = config.currentFpsTarget
                fpsFlow.value = if (config.gpuTurboEnabled) {
                    Random.nextInt(maxFps - 3, maxFps + 1)
                } else {
                    Random.nextInt(maxFps - 12, maxFps - 2)
                }

                // Smoothness simulator
                smoothnessActive.value = config.touchResponseMode == "Ultra" || config.touchResponseMode == "Extreme"

                // Update drag-up states
                dragUpActive.value = config.dragUpBoosterEnabled
                dragUpSens.value = config.dragUpSensitivity

                delay(1200)
            }
        }

        // RAM updates periodically
        serviceScope.launch {
            while (isActive) {
                val db = DatabaseProvider.getDatabase(applicationContext)
                val config = db.tweakDao().getConfigDirect() ?: TweakConfigs()
                val usedRam = if (config.ramBoostEnabled) {
                    Random.nextDouble(2.8, 3.4)
                } else {
                    Random.nextDouble(4.8, 5.4)
                }
                val formatted = String.format("%.1f GB / 8 GB", usedRam)
                ramUsageFlow.value = formatted
                delay(3000)
            }
        }
    }

    private fun showFloatingBubble() {
        overlayView = FrameLayout(this)
        
        // Window Layout Rules for System Alert Overlay containing dragging logic
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 300
        }

        // We embed Jetpack Compose into this floaty overlay to provide ultimate custom UX/UI customization!
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            
            // To safely run compose and lifecycle inside a service, we set dummy owners
            val customLifecycleOwner = object : LifecycleOwner {
                private val registry = LifecycleRegistry(this).apply {
                    currentState = Lifecycle.State.RESUMED
                }
                override val lifecycle: Lifecycle get() = registry
            }
            setViewTreeLifecycleOwner(customLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(object : androidx.savedstate.SavedStateRegistryOwner {
                private val savedStateRegistryController = androidx.savedstate.SavedStateRegistryController.create(this).apply {
                    performRestore(null)
                }
                override val savedStateRegistry = savedStateRegistryController.savedStateRegistry
                override val lifecycle: Lifecycle get() = customLifecycleOwner.lifecycle
            })

            setContent {
                MyApplicationTheme {
                    FloatingOverlayContent(
                        pingState = pingFlow,
                        fpsState = fpsFlow,
                        ramState = ramUsageFlow,
                        isBoosterActive = boosterActive,
                        isSmoothnessActive = smoothnessActive,
                        dragUpActive = dragUpActive,
                        dragUpSens = dragUpSens,
                        onDrag = { dx, dy ->
                            params?.let { p ->
                                p.x += dx.toInt()
                                p.y += dy.toInt()
                                windowManager.updateViewLayout(overlayView, p)
                            }
                        },
                        onClose = {
                            stopSelf()
                        },
                        onOpenSettings = {
                            val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            startActivity(launchIntent)
                        }
                    )
                }
            }
        }

        overlayView?.addView(composeView)
        windowManager.addView(overlayView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // Ignore if not present
            }
        }
    }
}

@Composable
fun FloatingOverlayContent(
    pingState: State<Int>,
    fpsState: State<Int>,
    ramState: State<String>,
    isBoosterActive: MutableState<Boolean>,
    isSmoothnessActive: State<Boolean>,
    dragUpActive: State<Boolean>,
    dragUpSens: State<Int>,
    onDrag: (Float, Float) -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        if (!expanded) {
            // Unexpanded floating badge styling
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFF3E3E), Color(0xFF1E0B0B)),
                            radius = 160f
                        )
                    )
                    .border(2.dp, Color(0xFFFF5252), CircleShape)
                    .clickable { expanded = true }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "FF Assist",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${pingState.value}ms",
                        color = if (pingState.value < 40) Color(0xFF00FFCC) else Color(0xFFFFCC00),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Elegant expanded panel with live Vivo game tools & sensor stats controls
            Card(
                modifier = Modifier
                    .width(220.dp)
                    .border(2.dp, Color(0xFFFF3E3E), RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0D0202).copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Active Boost",
                                tint = Color(0xFFFF3E3E),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VIVO T2x Engine",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Row {
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(
                                onClick = { expanded = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Minimize",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Divider(color = Color(0xFF301010), thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))

                    // Dynamic Stats Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF150505), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        MetricRow(label = "Latency (Ping)", value = "${pingState.value} ms", color = if (pingState.value < 40) Color(0xFF00FF88) else Color(0xFFFFAA00))
                        Spacer(modifier = Modifier.height(4.dp))
                        MetricRow(label = "Combat FPS", value = "${fpsState.value} FPS", color = Color(0xFF00E5FF))
                        Spacer(modifier = Modifier.height(4.dp))
                        MetricRow(label = "Optimized RAM", value = ramState.value, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mini Tweak Controllers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Smoothness Flow", color = Color.LightGray, fontSize = 11.sp)
                        Text(
                            text = if (isSmoothnessActive.value) "90Hz - 120Hz" else "Standard",
                            color = if (isSmoothnessActive.value) Color(0xFF00FF88) else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Auto Recoil Compensator", color = Color.LightGray, fontSize = 11.sp)
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active Assist",
                            tint = Color(0xFF00FF88),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Aim Drag-up Driver", color = Color.LightGray, fontSize = 11.sp)
                        Text(
                            text = if (dragUpActive.value) "${dragUpSens.value}% Active" else "OFF",
                            color = if (dragUpActive.value) Color(0xFF00FF88) else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Fast RAM Buster trigger button
                    Button(
                        onClick = {
                            isBoosterActive.value = true
                            // Automatic temporary action inside the floating overlay
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBoosterActive.value) Color(0xFF225522) else Color(0xFFFF3E3E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (isBoosterActive.value) "Memory Flushed! ✓" else "Instant RAM Boost",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Close Panel Service",
                        color = Color.DarkGray,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable { onClose() },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun MetricRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Gray, fontSize = 10.sp)
        Text(text = value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
