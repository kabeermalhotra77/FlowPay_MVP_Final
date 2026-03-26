package com.flowpay.app.glasses

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.flowpay.app.FlowPayApplication
import com.flowpay.app.features.qr_scanner.presentation.QRScannerActivity

/**
 * Wakes the phone from the pocket and launches directly into the existing
 * QR scan-to-pay flow via [QRScannerActivity] with the raw QR string.
 */
class PocketWakeUpManager(private val context: Context) {

    companion object {
        private const val TAG = "PocketWakeUpManager"
        private const val NOTIFICATION_ID = 9002
        private const val WAKE_LOCK_TAG = "FlowPay:GlassesWakeUp"
        const val ACTION_CANCEL_VIBRATION = "com.flowpay.app.CANCEL_VIBRATION"
        private const val MAX_VIBRATION_MS = 30_000L
    }

    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var vibrationTimeoutRunnable: Runnable? = null

    private val cancelReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == ACTION_CANCEL_VIBRATION) {
                cancelVibration()
            }
        }
    }

    init {
        val filter = IntentFilter(ACTION_CANCEL_VIBRATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(cancelReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(cancelReceiver, filter)
        }
    }

    /**
     * Entry point: wake the phone and launch QRScannerActivity with the raw QR string.
     */
    fun wakePhoneAndLaunchPayment(rawQR: String) {
        Log.d(TAG, "Waking phone for QR: ${rawQR.take(60)}...")

        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                WAKE_LOCK_TAG
            )
        wakeLock.acquire(10_000L)

        try {
            val launchIntent = Intent(context, QRScannerActivity::class.java).apply {
                putExtra(QRScannerActivity.EXTRA_GLASSES_QR_DATA, rawQR)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            context.startActivity(launchIntent)
            Log.d(TAG, "Launched QRScannerActivity with glasses QR data")

            val pendingIntent = PendingIntent.getActivity(
                context, NOTIFICATION_ID, launchIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val deleteIntent = PendingIntent.getBroadcast(
                context, 0,
                Intent(ACTION_CANCEL_VIBRATION),
                PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(
                context, FlowPayApplication.PAYMENT_READY_CHANNEL_ID
            )
                .setContentTitle("QR Code Detected")
                .setContentText("Tap to pay via FlowPay")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(deleteIntent)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Full-screen notification fired")

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val canFSI = if (Build.VERSION.SDK_INT >= 34) {
                nm.canUseFullScreenIntent()
            } else {
                true
            }

            if (!canFSI) {
                Log.w(TAG, "FSI not granted — starting persistent vibration fallback")
                startPersistentVibration()
            }
        } finally {
            wakeLock.release()
        }
    }

    private fun startPersistentVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 800, 400, 800, 400, 800)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }

        vibrationTimeoutRunnable = Runnable {
            Log.d(TAG, "Vibration safety timeout reached ($MAX_VIBRATION_MS ms)")
            cancelVibration()
        }
        handler.postDelayed(vibrationTimeoutRunnable!!, MAX_VIBRATION_MS)
    }

    fun cancelVibration() {
        vibrator?.cancel()
        vibrator = null
        vibrationTimeoutRunnable?.let { handler.removeCallbacks(it) }
        vibrationTimeoutRunnable = null
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        Log.d(TAG, "Vibration and notification cancelled")
    }
}
