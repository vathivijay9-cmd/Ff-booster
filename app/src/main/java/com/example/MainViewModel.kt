package com.example

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.persistence.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import kotlin.random.Random

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val tweakDao = DatabaseProvider.getDatabase(application).tweakDao()

    // Configuration Flow from DB
    val configState: StateFlow<TweakConfigs> = tweakDao.getConfigFlow()
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TweakConfigs()
        )

    // Ping Logs Flow from DB
    val pingLogsState: StateFlow<List<PingLog>> = tweakDao.getRecentPingLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI Interactive States
    val livePing = mutableStateOf(44)
    val isTweakingActive = mutableStateOf(false)
    val tweakProgress = mutableStateOf(0f)
    val selectedGunCategory = mutableStateOf("Assault Rifles") // Custom Sensitivity Engine State

    // DNS server options
    val dnsServers = listOf(
        "Cloudflare Gaming (1.1.1.1)",
        "Google Super DNS (8.8.8.8)",
        "AdGuard Game Shield (94.140.14.14)",
        "Quad9 Secure Route (9.9.9.9)",
        "Default Cellular Operator"
    )

    // Weapon category list
    val weaponCategories = listOf("Assault Rifles", "Submachine Guns (SMG)", "Snipers", "Shotguns")

    init {
        // Prepare initial DB entry if missing
        viewModelScope.launch {
            val current = tweakDao.getConfigDirect()
            if (current == null) {
                tweakDao.saveConfig(TweakConfigs())
            }
        }
        
        // Start continuous background ping simulation
        startLivePingLoop()
    }

    private fun startLivePingLoop() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val config = tweakDao.getConfigDirect() ?: TweakConfigs()
                // If DNS optimizer is on, ping is much lower
                val basePing = if (config.dnsFixerEnabled) {
                    when (config.selectedDnsServer) {
                        "Cloudflare Gaming (1.1.1.1)" -> 22
                        "Google Super DNS (8.8.8.8)" -> 28
                        else -> 35
                    }
                } else {
                    75
                }
                
                val currentTest = basePing + Random.nextInt(-4, 6)
                withContext(Dispatchers.Main) {
                    livePing.value = currentTest.coerceAtLeast(14)
                }

                // Periodically save log
                if (Random.nextInt(0, 10) == 5) {
                    tweakDao.insertPingLog(PingLog(pingMs = livePing.value))
                }

                delay(2500)
            }
        }
    }

    // Save and update configurations in DB reactively
    fun updateConfig(update: (TweakConfigs) -> TweakConfigs) {
        viewModelScope.launch {
            val current = tweakDao.getConfigDirect() ?: TweakConfigs()
            val next = update(current)
            tweakDao.saveConfig(next)
        }
    }

    // Comprehensive Booster Process
    fun runGameTurboBoost(onComplete: () -> Unit) {
        if (isTweakingActive.value) return
        viewModelScope.launch {
            isTweakingActive.value = true
            tweakProgress.value = 0.0f
            
            // Speed up simulated performance calibration
            while (tweakProgress.value < 1.0f) {
                delay(70)
                tweakProgress.value += 0.05f
            }
            
            // Apply config changes in Database upon boost completion
            updateConfig { current ->
                current.copy(
                    smoothnessLevel = 100,
                    ramBoostEnabled = true,
                    gpuTurboEnabled = true,
                    touchResponseMode = "Extreme"
                )
            }
            
            // Randomly insert low ping log
            tweakDao.insertPingLog(PingLog(pingMs = Random.nextInt(16, 24)))
            
            isTweakingActive.value = false
            onComplete()
        }
    }

    // Recoil Calibration Values specific for Vivo T2x 5G MediaTek Dimensity 6020 touch screen
    fun getSensitivityValues(category: String, config: TweakConfigs): SensitivityPreset {
        val multiplier = 0.8f + (config.smoothnessLevel / 500f) // Scale values dynamically based on optimization level
        val dragTweakRatio = config.dragUpSensitivity / 100f
        val boosterMultiplier = if (config.dragUpBoosterEnabled) 1.15f else 1.00f
        val dragMultiplier = multiplier * dragTweakRatio * boosterMultiplier
        
        return when (category) {
            "Assault Rifles" -> SensitivityPreset(
                general = (98 * multiplier).coerceIn(80f, 100f).toInt(),
                redDot = (95 * multiplier).coerceIn(80f, 100f).toInt(),
                scope2x = (92 * multiplier).coerceIn(75f, 100f).toInt(),
                scope4x = (88 * multiplier).coerceIn(70f, 100f).toInt(),
                awm = (50 * multiplier).coerceIn(30f, 90f).toInt(),
                freeLook = (75 * multiplier).coerceIn(50f, 95f).toInt(),
                dragUp = (112 * dragMultiplier).coerceIn(95f, 140f).toInt(), // Hyper drag sensitive
                dpiValue = 460,
                dragSpeedTip = "Medium Drag Speed. Pull down custom crosshair moderately immediately after firing the 2nd bullet."
            )
            "Submachine Guns (SMG)" -> SensitivityPreset(
                general = (100 * multiplier).coerceIn(90f, 100f).toInt(),
                redDot = (99 * multiplier).coerceIn(85f, 100f).toInt(),
                scope2x = (96 * multiplier).coerceIn(80f, 100f).toInt(),
                scope4x = (92 * multiplier).coerceIn(75f, 100f).toInt(),
                awm = (45 * multiplier).coerceIn(25f, 80f).toInt(),
                freeLook = (80 * multiplier).coerceIn(55f, 100f).toInt(),
                dragUp = (118 * dragMultiplier).coerceIn(100f, 145f).toInt(), // Maximum drag sensitivity
                dpiValue = 485,
                dragSpeedTip = "Fast Drag Speed. Swipe your visual fire button upward rapidly for quick headshots on SMGs (MP40/UMP)."
            )
            "Snipers" -> SensitivityPreset(
                general = (90 * multiplier).coerceIn(70f, 98f).toInt(),
                redDot = (85 * multiplier).coerceIn(65f, 95f).toInt(),
                scope2x = (80 * multiplier).coerceIn(60f, 95f).toInt(),
                scope4x = (75 * multiplier).coerceIn(55f, 95f).toInt(),
                awm = (38 * multiplier).coerceIn(15f, 70f).toInt(),
                freeLook = (60 * multiplier).coerceIn(40f, 90f).toInt(),
                dragUp = (95 * dragMultiplier).coerceIn(80f, 125f).toInt(),
                dpiValue = 420,
                dragSpeedTip = "Micro Drag. Zero horizontal shifts. Let crosshair align first before quick scopes."
            )
            else -> SensitivityPreset( // Shotguns
                general = (99 * multiplier).coerceIn(85f, 100f).toInt(),
                redDot = (98 * multiplier).coerceIn(80f, 100f).toInt(),
                scope2x = (94 * multiplier).coerceIn(75f, 100f).toInt(),
                scope4x = (90 * multiplier).coerceIn(70f, 100f).toInt(),
                awm = (40 * multiplier).coerceIn(20f, 80f).toInt(),
                freeLook = (85 * multiplier).coerceIn(60f, 100f).toInt(),
                dragUp = (115 * dragMultiplier).coerceIn(100f, 140f).toInt(), // Extreme drag sensitivity
                dpiValue = 500,
                dragSpeedTip = "Heavy Rotation Drag. Draw a circular motion with fire trigger for extreme close-quarters headshots."
            )
        }
    }

    fun clearLogHistory() {
        viewModelScope.launch {
            tweakDao.clearPingLogs()
        }
    }
}

// Sensitivity parameters response model
data class SensitivityPreset(
    val general: Int,
    val redDot: Int,
    val scope2x: Int,
    val scope4x: Int,
    val awm: Int,
    val freeLook: Int,
    val dragUp: Int,
    val dpiValue: Int,
    val dragSpeedTip: String
)
