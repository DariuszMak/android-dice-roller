package com.example.soundenhancement

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.audiofx.Equalizer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat


data class EqConfig(
    val baseLevel: Int = 700,
    val multipliers: DoubleArray = doubleArrayOf(1.40, 1.20, 0.60, 0.90, 1.10)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EqConfig) return false
        return baseLevel == other.baseLevel && multipliers.contentEquals(other.multipliers)
    }

    override fun hashCode(): Int = 31 * baseLevel + multipliers.contentHashCode()
}


fun calculateBandLevel(
    freqHz: Double,
    baseLevel: Int,
    minLevel: Int,
    maxLevel: Int,
    config: EqConfig = EqConfig()
): Short {
    val m = config.multipliers

    val boost = when {
        freqHz <= 100 -> baseLevel * m[0]
        freqHz <= 500 -> baseLevel * m[1]
        freqHz <= 2000 -> baseLevel * m[2]
        freqHz <= 8000 -> baseLevel * m[3]
        else -> baseLevel * m[4]
    }

    val range = maxLevel - minLevel
    val scaled = (Math.pow(boost / 1000.0, 1.2) * range).toInt() + minLevel
    return scaled.coerceIn(minLevel, maxLevel).toShort()
}

class AudioEffectService : Service() {

    private val binder = LocalBinder()


    internal var equalizer: Equalizer? = null

    var isEqEnabled: Boolean = false
        private set

    inner class LocalBinder : android.os.Binder() {
        fun getService(): AudioEffectService = this@AudioEffectService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        applyConfig(EqConfig())
        Log.d("AudioBoostService", "Dynamic Bass Enabled")
    }


    fun applyConfig(config: EqConfig) {
        enableProfessionalDynamicBass(config)
    }


    fun disableEq() {
        equalizer?.enabled = false
        isEqEnabled = false
        Log.d("AudioBoostService", "Equalizer disabled")
    }


    fun enableEq() {
        equalizer?.enabled = true
        isEqEnabled = true
        Log.d("AudioBoostService", "Equalizer re-enabled")
    }


    private fun enableProfessionalDynamicBass(config: EqConfig) {
        try {
            equalizer?.release()
            equalizer = Equalizer(0, 0)
            equalizer?.enabled = true
            isEqEnabled = true

            equalizer?.let { eq ->
                val numberOfBands = eq.numberOfBands
                val (minLevel, maxLevel) = eq.bandLevelRange

                for (i in 0 until numberOfBands) {
                    val freqHz = eq.getCenterFreq(i.toShort()) / 1000.0
                    val level = calculateBandLevel(
                        freqHz,
                        config.baseLevel,
                        minLevel.toInt(),
                        maxLevel.toInt(),
                        config
                    )
                    eq.setBandLevel(i.toShort(), level)
                }
            }

            Log.d("AudioBoostService", "EQ applied: baseLevel=${config.baseLevel}")
        } catch (e: Exception) {
            Log.e("AudioBoostService", "Error applying EQ", e)
        }
    }

    override fun onDestroy() {
        try {
            equalizer?.release()
            equalizer = null
            isEqEnabled = false
        } catch (e: Exception) {
            Log.e("AudioBoostService", "Error releasing Equalizer", e)
        }
        super.onDestroy()
    }

    private fun startForegroundService() {
        val channelId = "boost_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Bass Booster",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bass Boost Active")
            .setContentText("Boosting system bass")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(1, notification)
        }
    }
}