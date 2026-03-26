package com.flowpay.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.flowpay.app.data.SettingsRepository
import com.flowpay.app.utils.LauncherIconManager
import com.meta.wearable.dat.core.Wearables

class FlowPayApplication : Application() {

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(applicationContext) }

    companion object {
        private const val TAG = "FlowPayApplication"

        const val GLASSES_SESSION_CHANNEL_ID = "glasses_session_channel"
        const val PAYMENT_READY_CHANNEL_ID = "payment_ready_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        LauncherIconManager.applyForAccentTheme(
            context = this,
            accentTheme = settingsRepository.settingsFlow.value.accentTheme
        )
        try {
            Wearables.initialize(this)
            Log.d(TAG, "Meta Wearables DAT SDK initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Wearables SDK: ${e.message}", e)
        }
        Log.d(TAG, "FlowPayApplication initialized")
        Log.d(TAG, "Debug logs ON. View with: adb logcat -s FlowPayApplication RealMetaGlassesManager GlassesSessionService WakeWordListener")
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sessionChannel = NotificationChannel(
                GLASSES_SESSION_CHANNEL_ID,
                "Glasses Connection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification while glasses are connected"
                setShowBadge(false)
                setSound(null, null)
            }

            val paymentChannel = NotificationChannel(
                PAYMENT_READY_CHANNEL_ID,
                "Payment Ready",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a QR code is detected via glasses"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(sessionChannel)
            manager.createNotificationChannel(paymentChannel)

            Log.d(TAG, "Notification channels created")
        }
    }
}
