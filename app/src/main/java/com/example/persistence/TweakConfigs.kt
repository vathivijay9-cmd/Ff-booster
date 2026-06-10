package com.example.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tweak_configs")
data class TweakConfigs(
    @PrimaryKey val id: String = "vivot2x_default",
    val ramBoostEnabled: Boolean = true,
    val dnsFixerEnabled: Boolean = true,
    val gpuTurboEnabled: Boolean = true,
    val touchResponseMode: String = "Ultra", // Extreme, Ultra, Normal
    val smoothnessLevel: Int = 100, // 0 to 100
    val sensitivityRedDot: Int = 98,
    val sensitivityGeneral: Int = 99,
    val sensitivity2x: Int = 95,
    val sensitivity4x: Int = 90,
    val sensitivityAwm: Int = 75,
    val dragUpSensitivity: Int = 98, // High Drag Up Sensitivity
    val dragUpBoosterEnabled: Boolean = true, // Drag Up Auto Compensation
    val zeroRecoilAssistEnabled: Boolean = true,
    val autoActivePingFix: Boolean = true,
    val currentFpsTarget: Int = 120, // 60, 90, 120
    val selectedDnsServer: String = "Cloudflare Gaming (1.1.1.1)"
)

@Entity(tableName = "ping_logs")
data class PingLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val pingMs: Int
)
