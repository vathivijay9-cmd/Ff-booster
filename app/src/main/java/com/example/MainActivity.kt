package com.example

// Force rebuild 1
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.persistence.PingLog
import com.example.persistence.TweakConfigs
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Instantiate ViewModel safely using Factory default
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val config by viewModel.configState.collectAsStateWithLifecycle()
    val pingLogs by viewModel.pingLogsState.collectAsStateWithLifecycle()
    val livePingVal by viewModel.livePing
    
    var activeTab by remember { mutableStateOf("Turbo") }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // System Alert Window Permission Checker logic
    fun checkAndStartOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                showPermissionDialog = true
            } else {
                context.startService(Intent(context, FloatingWindowService::class.java))
                Toast.makeText(context, "Floating overlay active!", Toast.LENGTH_SHORT).show()
            }
        } else {
            context.startService(Intent(context, FloatingWindowService::class.java))
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        containerColor = HighDensityDarkBg,
        bottomBar = {
            BottomNavigationBar(activeTab = activeTab, onTabSelected = { activeTab = it })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header: Device Status
            HeaderSection()

            if (showPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = false },
                    title = { Text("Overlay Permission Required", color = Color.White) },
                    text = {
                        Text(
                            "This app requires Floating Window permission as customized in the requested theme. This allows rendering gamer stats inside Free Fire specifically on your Vivo T2x 5G.",
                            color = Slate400,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showPermissionDialog = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HighDensityAccentBlue)
                        ) {
                            Text("Grant Permission", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPermissionDialog = false }) {
                            Text("Cancel", color = Slate500)
                        }
                    },
                    containerColor = HighDensityCard,
                    shape = RoundedCornerShape(24.dp)
                )
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { tab ->
                when (tab) {
                    "Turbo" -> {
                        TurboPanel(
                            viewModel = viewModel,
                            config = config,
                            livePing = livePingVal,
                            onToggleFloatingWindow = { enabled ->
                                if (enabled) {
                                    checkAndStartOverlay()
                                } else {
                                    context.stopService(Intent(context, FloatingWindowService::class.java))
                                }
                            }
                        )
                    }
                    "Tools" -> {
                        SensPresetsPanel(viewModel = viewModel, config = config)
                    }
                    "Profile" -> {
                        DnsAndLagPanel(viewModel = viewModel, config = config, pingLogs = pingLogs)
                    }
                    else -> {
                        SetupPanel(viewModel = viewModel, config = config)
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HighDensityCard)
            .border(width = 1.dp, color = HighDensityBorder, shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "V-TURBO T2X 5G",
                color = HighDensityAccentBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                text = "System Optimized",
                color = Slate100,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp
            )
        }
        
        Row(
            modifier = Modifier
                .background(HighDensityActiveCard, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(HighDensityAccentGreen, CircleShape)
            )
            Text(
                text = "5G STABLE",
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TurboPanel(
    viewModel: MainViewModel,
    config: TweakConfigs,
    livePing: Int,
    onToggleFloatingWindow: (Boolean) -> Unit
) {
    var logs by remember {
        mutableStateOf(
            listOf(
                "Searching game process... Found (com.dts.freefire)",
                "Injecting low-latency hardware protocol... OK",
                "Calibrating touch matrix sampling... SUCCESS"
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SVG Gauge Container
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HighDensityCard, RoundedCornerShape(32.dp))
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(32.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background grid decorative layout
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            val patternSize = 25.dp.toPx()
                            val numLinesX = (size.width / patternSize).toInt()
                            val numLinesY = (size.height / patternSize).toInt()
                            for (i in 0..numLinesX) {
                                drawLine(
                                    color = HighDensityBorder.copy(alpha = 0.3f),
                                    start = Offset(i * patternSize, 0f),
                                    end = Offset(i * patternSize, size.height),
                                    strokeWidth = 1f
                                )
                            }
                            for (j in 0..numLinesY) {
                                drawLine(
                                    color = HighDensityBorder.copy(alpha = 0.3f),
                                    start = Offset(0f, j * patternSize),
                                    end = Offset(size.width, j * patternSize),
                                    strokeWidth = 1f
                                )
                            }
                        }
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Gauge Dial
                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Track circle
                            drawCircle(
                                color = HighDensityActiveCard,
                                radius = size.minDimension / 2f - 6.dp.toPx(),
                                style = Stroke(width = 8.dp.toPx())
                            )
                            // Dynamic gauge active arc
                            val sweepAngle = ((livePing.coerceIn(15, 120) / 120f) * 360f)
                            drawArc(
                                color = HighDensityAccentBlue,
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                                size = Size(size.width - 12.dp.toPx() * 2, size.height - 12.dp.toPx() * 2),
                                topLeft = Offset(12.dp.toPx(), 12.dp.toPx())
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$livePing",
                                color = Slate100,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "PING (MS)",
                                color = Slate400,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3-way telemetry metrics grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TelemetryItem(value = "41°C", label = "TEMP", color = HighDensityAccentBlue)
                        TelemetryItem(value = "${config.currentFpsTarget}", label = "FPS", color = HighDensityAccentGreen)
                        TelemetryItem(value = "${config.smoothnessLevel}%", label = "SMOOTH", color = HighDensityAccentPurple)
                    }
                }
            }
        }

        // Tweak Controls Quick Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Zero Recoil Control
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(115.dp)
                        .background(HighDensityCard, RoundedCornerShape(24.dp))
                        .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp))
                        .clickable {
                            viewModel.updateConfig { it.copy(zeroRecoilAssistEnabled = !it.zeroRecoilAssistEnabled) }
                            logs = logs + "[${System.currentTimeMillis() % 100000}] Zero Recoil assistance toggled: ${!config.zeroRecoilAssistEnabled}"
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(HighDensityAccentBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Recoil Assist",
                                tint = HighDensityAccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "ZERO RECOIL",
                            color = Slate100,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (config.zeroRecoilAssistEnabled) "Sensitivity Preset-Locked" else "Calibration Off",
                            color = if (config.zeroRecoilAssistEnabled) HighDensityAccentBlue else Slate500,
                            fontSize = 10.sp
                        )
                    }
                }

                // Ultra Smooth Control
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(115.dp)
                        .background(
                            if (config.touchResponseMode == "Extreme") HighDensityActiveCard else HighDensityCard,
                            RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (config.touchResponseMode == "Extreme") HighDensityAccentGreen.copy(alpha = 0.5f) else HighDensityBorder,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable {
                            val nextMode = if (config.touchResponseMode == "Extreme") "Normal" else "Extreme"
                            viewModel.updateConfig { it.copy(touchResponseMode = nextMode) }
                            logs = logs + "[${System.currentTimeMillis() % 100000}] Touch Response Mode updated limit: $nextMode"
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(HighDensityAccentGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Smoothness Assist",
                                tint = HighDensityAccentGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "ULTRA SMOOTH",
                            color = Slate100,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (config.touchResponseMode == "Extreme") "Extreme Bypass ON" else "Standard Touch Flow",
                            color = if (config.touchResponseMode == "Extreme") HighDensityAccentGreen else Slate500,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Float Window Toggle Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HighDensityActiveCard, RoundedCornerShape(24.dp))
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color.Transparent, CircleShape)
                            .border(2.dp, Slate500, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Widget Active",
                            tint = HighDensityAccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Floating Widget",
                            color = Slate100,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "In-game overlay for Vivo T2x",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = config.gpuTurboEnabled, // Connected state represent overlay
                    onCheckedChange = { active ->
                        viewModel.updateConfig { it.copy(gpuTurboEnabled = active) }
                        onToggleFloatingWindow(active)
                        logs = logs + "[${System.currentTimeMillis() % 100000}] Floating overlay widget toggled: $active"
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = HighDensityAccentBlue,
                        uncheckedBorderColor = Slate500
                    )
                )
            }
        }

        // Live Gaming Booster Action
        item {
            var boostSuccessMsg by remember { mutableStateOf("") }
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        logs = logs + "[SYSTEM] Starting manual Free Fire Engine boost sequence..."
                        viewModel.runGameTurboBoost {
                            logs = logs + "[SUCCESS] RAM optimization completed. Flushed cache."
                            logs = logs + "[SUCCESS] Recoil Presets applied & touch rates calibrated at 120Hz."
                            boostSuccessMsg = "Lag solved! Free Fire boosted successfully."
                        }
                    },
                    enabled = !viewModel.isTweakingActive.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HighDensityAccentPurple
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (viewModel.isTweakingActive.value) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ENGAGING GAME BOOSTER... ${(viewModel.tweakProgress.value * 100).toInt()}%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Boost now")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "INSTANT 1-CLICK MEMORY BOOSTER",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
                
                if (boostSuccessMsg.isNotEmpty()) {
                    Text(
                        text = boostSuccessMsg,
                        color = HighDensityAccentGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        }

        // Log section matching high density aesthetic
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0C14), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .border(
                        1.dp,
                        HighDensityBorder,
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OPTIMIZATION LOGS",
                        color = Slate500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "v2.5.0-STABLE",
                        color = HighDensityAccentBlue,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    logs.takeLast(4).forEach { log ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "[LOG]",
                                color = Slate500,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = log,
                                color = if (log.contains("SUCCESS") || log.contains("completed")) HighDensityAccentGreen else Slate400,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryItem(value: String, label: String, color: Color) {
    Column(
        modifier = Modifier
            .background(HighDensityActiveCard, RoundedCornerShape(16.dp))
            .border(1.dp, HighDensityBorder, RoundedCornerShape(16.dp))
            .width(80.dp)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            color = Slate500,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun SensPresetsPanel(viewModel: MainViewModel, config: TweakConfigs) {
    val selectedCategory by viewModel.selectedGunCategory
    val currentPreset = viewModel.getSensitivityValues(selectedCategory, config)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explanatory note
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = HighDensityCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, HighDensityBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "VIVO T2X 5G ZERO RECOIL MOTOR",
                        color = HighDensityAccentBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Adjust configurations below to synchronize dynamic touchscreen sampling filters. Preset parameters adapt specifically to the Dimensity 6020 CPU thread pipeline to stabilize coordinate shifts.",
                        color = Slate400,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Weapon Categories Selector Slider Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select Weapon Class",
                    color = Slate100,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.weaponCategories.forEach { category ->
                        val isSelected = category == selectedCategory
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) HighDensityAccentBlue else HighDensityCard,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else HighDensityBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.selectedGunCategory.value = category }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) Color.White else Slate400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Presets Values Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "$selectedCategory Calibration Preset",
                    color = Slate100,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HighDensityCard, RoundedCornerShape(24.dp))
                        .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SensValueRow(name = "General Sensitivity", value = currentPreset.general)
                        SensValueRow(name = "Red Dot Sight", value = currentPreset.redDot)
                        SensValueRow(name = "2x Scope", value = currentPreset.scope2x)
                        SensValueRow(name = "4x Scope", value = currentPreset.scope4x)
                        SensValueRow(name = "AWM Sniper Scope", value = currentPreset.awm)
                        SensValueRow(name = "Free Look Angle", value = currentPreset.freeLook)
                        SensValueRow(name = "Drag-Up Assist Level", value = currentPreset.dragUp)
                        
                        Divider(color = HighDensityBorder, modifier = Modifier.padding(vertical = 4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "DPI (Recommended)", color = Slate400, fontSize = 11.sp)
                            Text(
                                text = "${currentPreset.dpiValue} DPI",
                                color = HighDensityAccentGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Coach tip card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HighDensityActiveCard, RoundedCornerShape(16.dp))
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Tips",
                        tint = HighDensityAccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Vivo T2x Drag Coaching Tip:",
                            color = Slate100,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentPreset.dragSpeedTip,
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // High Drag-Up Custom Tuning Options
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CUSTOM DRAG-UP BOOSTER",
                    color = Slate100,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HighDensityCard, RoundedCornerShape(24.dp))
                        .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Vertical Drag-Up Sensitivity", color = Slate100, fontSize = 12.sp)
                            Text(
                                text = "${config.dragUpSensitivity}%",
                                color = HighDensityAccentBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Slider(
                            value = config.dragUpSensitivity.toFloat(),
                            onValueChange = { newValue ->
                                viewModel.updateConfig { it.copy(dragUpSensitivity = newValue.toInt()) }
                            },
                            valueRange = 80f..120f,
                            colors = SliderDefaults.colors(
                                thumbColor = HighDensityAccentBlue,
                                activeTrackColor = HighDensityAccentBlue,
                                inactiveTrackColor = HighDensityActiveCard
                            )
                        )

                        Divider(color = HighDensityBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto Drag-Up Calibration",
                                    color = Slate100,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Enhances shot registration during vertical finger flick transitions.",
                                    color = Slate400,
                                    fontSize = 10.sp
                                )
                            }
                            Switch(
                                checked = config.dragUpBoosterEnabled,
                                onCheckedChange = { isChecked ->
                                    viewModel.updateConfig { it.copy(dragUpBoosterEnabled = isChecked) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = HighDensityAccentBlue,
                                    uncheckedThumbColor = Slate400,
                                    uncheckedTrackColor = HighDensityActiveCard
                                )
                            )
                        }
                    }
                }
            }
        }

        // Drag Gesture calibration trainer
        item {
            DragUpCoachingTrainerCard()
        }
    }
}

@Composable
fun DragUpCoachingTrainerCard() {
    val points = remember { mutableStateListOf<Offset>() }
    var coachVerdict by remember { mutableStateOf("Swipe UP rapidly inside the area above to calibrate drag gestures.") }
    var scoreStatus by remember { mutableStateOf("") }
    var scoreColor by remember { mutableStateOf(HighDensityAccentBlue) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "DRAG-UP PHYSICAL CALIBRATION",
            color = Slate100,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HighDensityCard, RoundedCornerShape(24.dp))
                .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Aesthetic Swipe Calibration Engine. Measures speed, deviation and snap latency of headshot pulls.",
                    color = Slate400,
                    fontSize = 11.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(HighDensityActiveCard)
                        .border(1.dp, HighDensityBorder, RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    points.clear()
                                    points.add(offset)
                                    coachVerdict = "Tracking touch vector coordinate maps..."
                                    scoreStatus = ""
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    points.add(change.position)
                                },
                                onDragEnd = {
                                    if (points.size < 2) {
                                        coachVerdict = "Awaiting coordinate stream. Draw an upward swipe."
                                        return@detectDragGestures
                                    }
                                    val first = points.first()
                                    val last = points.last()
                                    val dy = first.y - last.y
                                    val dx = kotlin.math.abs(first.x - last.x)

                                    if (dy > 50f) {
                                        if (dx < 35f) {
                                            val speed = points.size
                                            if (speed < 7) {
                                                coachVerdict = "PERFECT SNAP! Virtual multiplier active."
                                                scoreStatus = "PHYSICAL SPEED: 112% (EXTREME HEADSHOT TARGET LOCK)"
                                                scoreColor = HighDensityAccentGreen
                                            } else {
                                                coachVerdict = "GOOD LINEAR VECTOR. Recoil balance stable."
                                                scoreStatus = "PHYSICAL SPEED: 84% (ACCURATE DRAG RANGE)"
                                                scoreColor = HighDensityAccentBlue
                                            }
                                        } else {
                                            coachVerdict = "HORIZONTAL DRIFT DETECTED. Align finger straight!"
                                            scoreStatus = "DECELERATION DETECTED due to crosshair slide."
                                            scoreColor = HighDensityAccentPurple
                                        }
                                    } else {
                                        coachVerdict = "INSUFFICIENT LIFT. Pull up faster to lock headshots!"
                                        scoreStatus = "CALIBRATION INCOMPLETE: Speed too low."
                                        scoreColor = Color(0xFFFF3E3E)
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(
                            color = HighDensityBorder,
                            start = Offset(size.width / 2f, size.height - 15f),
                            end = Offset(size.width / 2f, 15f),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (points.size > 1) {
                            val path = Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (i in 1 until points.size) {
                                    lineTo(points[i].x, points[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = scoreColor,
                                style = Stroke(width = 5f, cap = StrokeCap.Round)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .align(Alignment.BottomCenter)
                            .offset(y = (-8).dp)
                            .background(scoreColor.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, scoreColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(scoreColor, CircleShape))
                    }
                }

                Column {
                    Text(
                        text = coachVerdict,
                        color = scoreColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (scoreStatus.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = scoreStatus,
                            color = Slate100,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SensValueRow(name: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, color = Slate100, fontSize = 12.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinearProgressIndicator(
                progress = value / 100f,
                modifier = Modifier
                    .width(80.dp)
                    .height(6.dp)
                    .clip(CircleShape),
                color = HighDensityAccentBlue,
                trackColor = HighDensityActiveCard
            )
            Text(
                text = "$value",
                color = HighDensityAccentBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun DnsAndLagPanel(viewModel: MainViewModel, config: TweakConfigs, pingLogs: List<PingLog>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // DNS selections card
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "High Precision DNS Router",
                    color = Slate100,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HighDensityCard, RoundedCornerShape(24.dp))
                        .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Enable DNS Optimization", color = Slate100, fontSize = 12.sp)
                            Switch(
                                checked = config.dnsFixerEnabled,
                                onCheckedChange = { active ->
                                    viewModel.updateConfig { it.copy(dnsFixerEnabled = active) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = HighDensityAccentBlue
                                )
                            )
                        }

                        if (config.dnsFixerEnabled) {
                            Text(
                                text = "Select Safe Gaming DNS Route:",
                                color = Slate400,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            
                            viewModel.dnsServers.forEach { server ->
                                val isSelected = config.selectedDnsServer == server
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isSelected) HighDensityActiveCard else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            viewModel.updateConfig { it.copy(selectedDnsServer = server) }
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = server,
                                        color = if (isSelected) HighDensityAccentBlue else Slate400,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = HighDensityAccentBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Lag status log & ping graph mock representations
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Real-Time Latency History",
                        color = Slate100,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { viewModel.clearLogHistory() }) {
                        Text("Clear logs", color = HighDensityCoral, fontSize = 11.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HighDensityCard, RoundedCornerShape(24.dp))
                        .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    if (pingLogs.isEmpty()) {
                        Text(
                            text = "No ping telemetry logged yet. Keep applet open or start shooting in-game to update feed history.",
                            color = Slate500,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pingLogs.take(5).forEach { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Ping pinged cellular tower...",
                                        color = Slate400,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${log.pingMs} ms",
                                        color = if (log.pingMs < 35) HighDensityAccentGreen else HighDensityAccentBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetupPanel(viewModel: MainViewModel, config: TweakConfigs) {
    var expandedFps by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Vivo T2x target profiles configurations
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Core Display Optimization",
                    color = Slate100,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HighDensityCard, RoundedCornerShape(24.dp))
                        .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Target Frame-rate Profile", color = Slate100, fontSize = 12.sp)
                            Box {
                                Button(
                                    onClick = { expandedFps = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityActiveCard),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("${config.currentFpsTarget} FPS Peak", color = HighDensityAccentBlue, fontSize = 11.sp)
                                }
                                DropdownMenu(
                                    expanded = expandedFps,
                                    onDismissRequest = { expandedFps = false },
                                    modifier = Modifier.background(HighDensityCard).border(1.dp, HighDensityBorder)
                                ) {
                                    listOf(60, 90, 120).forEach { limit ->
                                        DropdownMenuItem(
                                            text = { Text("$limit Hz Ultra Screen Rate", color = Slate100) },
                                            onClick = {
                                                viewModel.updateConfig { it.copy(currentFpsTarget = limit) }
                                                expandedFps = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Divider(color = HighDensityBorder)

                        // Smoothness level slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Calibration Strength (Smoothness)", color = Slate100, fontSize = 12.sp)
                                Text(text = "${config.smoothnessLevel}%", color = HighDensityAccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = config.smoothnessLevel.toFloat(),
                                onValueChange = {
                                    viewModel.updateConfig { current ->
                                        current.copy(smoothnessLevel = it.toInt())
                                    }
                                },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = HighDensityAccentGreen,
                                    activeTrackColor = HighDensityAccentGreen,
                                    inactiveTrackColor = HighDensityActiveCard
                                )
                            )
                        }

                        Divider(color = HighDensityBorder)

                        // Touch Response mode select dropdown alternative
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Touch Response Mode", color = Slate100, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Normal", "Ultra", "Extreme").forEach { mode ->
                                    val isSelected = config.touchResponseMode == mode
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) HighDensityAccentBlue.copy(alpha = 0.2f) else Color.Transparent,
                                                CircleShape
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) HighDensityAccentBlue else HighDensityBorder,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                viewModel.updateConfig { it.copy(touchResponseMode = mode) }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = mode,
                                            color = if (isSelected) HighDensityAccentBlue else Slate400,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // About Device hardware details
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HighDensityCard, RoundedCornerShape(24.dp))
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Optimized Hardware Details",
                        color = HighDensityAccentPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    
                    HardwareValueRow(label = "SoC", valStr = "MediaTek Dimensity 6020")
                    HardwareValueRow(label = "GPU Cores", valStr = "Mali-G57 MC2")
                    HardwareValueRow(label = "Touch Sampling", valStr = "180 Hz Multi-touch")
                    HardwareValueRow(label = "Engine Pipeline", valStr = "V-Turbo Gaming Bypass v2.4")
                }
            }
        }
    }
}

@Composable
fun HardwareValueRow(label: String, valStr: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Slate400, fontSize = 11.sp)
        Text(text = valStr, color = Slate100, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BottomNavigationBar(activeTab: String, onTabSelected: (String) -> Unit) {
    NavigationBar(
        containerColor = HighDensityCard,
        tonalElevation = 8.dp,
        modifier = Modifier
            .border(width = 1.dp, color = HighDensityBorder, shape = RoundedCornerShape(0.dp))
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        val menuItems = listOf(
            Triple("Turbo", Icons.Default.PlayArrow, "Optimize"),
            Triple("Tools", Icons.Default.Build, "Sens Presets"),
            Triple("Profile", Icons.Default.Person, "Network DNS"),
            Triple("Setup", Icons.Default.Settings, "Device Specs")
        )

        menuItems.forEach { (tabName, icon, label) ->
            val isSelected = activeTab == tabName
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tabName) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = tabName.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = HighDensityAccentBlue,
                    selectedTextColor = HighDensityAccentBlue,
                    unselectedIconColor = Slate500,
                    unselectedTextColor = Slate500,
                    indicatorColor = HighDensityActiveCard
                )
            )
        }
    }
}
