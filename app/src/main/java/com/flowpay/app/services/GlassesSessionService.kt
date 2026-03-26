package com.flowpay.app.services

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.flowpay.app.FlowPayApplication
import com.flowpay.app.helpers.SetupHelper
import com.flowpay.app.MainActivity
import com.flowpay.app.constants.PermissionConstants
import com.flowpay.app.glasses.CheckCameraResult
import com.flowpay.app.glasses.GlassesSessionManager
import com.flowpay.app.glasses.GlassesSessionState
import com.flowpay.app.glasses.HeadlessQRScanner
import com.flowpay.app.glasses.MetaGlassesManager
import com.flowpay.app.glasses.PocketWakeUpManager
import com.flowpay.app.glasses.RealMetaGlassesManager
import com.flowpay.app.glasses.WakeWordListener
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.types.RegistrationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.lang.ref.WeakReference

class GlassesSessionService : LifecycleService() {

    companion object {
        private const val TAG = "GlassesSessionService"
        private const val NOTIFICATION_ID = 9001
        /** If stream stops within this time while Scanning, show stream-failed error. */
        private const val SCANNING_STREAM_FAIL_MS = 5000L
        /** Minimum time stream must run without a frame before we show "stream stopped" error (avoids false error on early/transient STOPPED). */
        private const val MIN_ELAPSED_BEFORE_STREAM_ERROR_MS = 1500L
        /** Delay before retrying DAT camera permission check (sync delay after grant). */
        private const val PERMISSION_RETRY_DELAY_MS = 600L

        private val _state = MutableStateFlow<GlassesSessionState>(GlassesSessionState.Idle)
        val state: StateFlow<GlassesSessionState> = _state.asStateFlow()

        var instance: GlassesSessionService? = null
            private set
    }

    private var glassesManager: MetaGlassesManager? = null
    private var wakeWordListener: WakeWordListener? = null
    private var headlessScanner: HeadlessQRScanner? = null
    private var pocketWakeUp: PocketWakeUpManager? = null

    /** When we entered Scanning (stream start); used to show error if stream stops quickly. */
    private var scanningStartedAtMs: Long = 0L

    /** True once we have received at least one video frame in this Scanning session. Used to avoid showing the "camera stream couldn't start" error when the stream was actually working. */
    @Volatile
    private var scanningReceivedFirstFrame: Boolean = false

    /** Current Activity (e.g. MainActivity) so we can pass it to startVideoStream for Meta DAT. Cleared in onPause. */
    private var currentActivityRef: WeakReference<Activity>? = null

    fun setCurrentActivity(activity: Activity?) {
        currentActivityRef = activity?.let { WeakReference(it) }
        Log.d(TAG, "Current activity ${if (activity != null) "set" else "cleared"}")
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "GlassesSessionService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIFICATION_ID, buildPersistentNotification())
        if (_state.value == GlassesSessionState.Idle || _state.value is GlassesSessionState.Error) {
            connectAndListen()
        }
        return START_STICKY
    }

    private fun connectAndListen() {
        lifecycleScope.launch {
            try {
                _state.value = GlassesSessionState.Connecting

                // Clean up any previous session when retrying after Error
                wakeWordListener?.destroy()
                wakeWordListener = null
                glassesManager?.destroy()
                glassesManager = null
                headlessScanner?.destroy()
                headlessScanner = null
                pocketWakeUp?.cancelVibration()
                pocketWakeUp = null

                pocketWakeUp = PocketWakeUpManager(this@GlassesSessionService)

                headlessScanner = HeadlessQRScanner { rawQR ->
                    onQRCodeDetected(rawQR)
                }

                // Create wake word listener BEFORE glasses manager so audio callbacks always have
                // a valid reference when frames start flowing. Post to main thread to avoid
                // calling stopAudioStream() from the audio thread (deadlock) and to start camera instantly.
                wakeWordListener = WakeWordListener(
                    context = this@GlassesSessionService,
                    onWakeWordDetected = {
                        Handler(Looper.getMainLooper()).post { onWakeWordDetected() }
                    }
                )

                val useRealGlasses = isMetaGlassesRegistered()
                if (!useRealGlasses) {
                    Log.e(TAG, "Meta glasses not registered — will NOT use phone mic. User must pair glasses first.")
                    _state.value = GlassesSessionState.Error(
                        "Pair Ray-Ban Meta glasses first. Tap to pair, then connect."
                    )
                    return@launch
                }

                Log.d(TAG, "Meta glasses registered -- using RealMetaGlassesManager (glasses mic and glasses camera only)")
                glassesManager = RealMetaGlassesManager(
                    context = this@GlassesSessionService,
                    scope = lifecycleScope,
                    onAudioFrame = { pcm -> wakeWordListener?.processAudioFrame(pcm) },
                    onVideoFrame = { /* unused when onVideoFrameNv21 is set */ },
                    onVideoStreamStopped = {
                        Log.d(TAG, "Video stream stopped — resuming wake word listening")
                        lifecycleScope.launch {
                            if (_state.value is GlassesSessionState.Scanning) {
                                val elapsed = System.currentTimeMillis() - scanningStartedAtMs
                                if (elapsed < SCANNING_STREAM_FAIL_MS) {
                                    if (!scanningReceivedFirstFrame) {
                                        // Only show error if stream ran long enough without a frame (avoids false error on early/transient STOPPED)
                                        if (elapsed >= MIN_ELAPSED_BEFORE_STREAM_ERROR_MS) {
                                            val message = if (GlassesSessionManager.hasCameraPermission()) {
                                                PermissionConstants.GLASSES_STREAM_STOPPED_UNEXPECTEDLY
                                            } else {
                                                "Glasses camera: ${PermissionConstants.GLASSES_CAMERA_GRANT_STEPS}"
                                            }
                                            _state.value = GlassesSessionState.Error(message)
                                            Log.w(TAG, "Stream stopped within ${elapsed}ms and no frame received — $message; DAT camera permission: ${GlassesSessionManager.hasCameraPermission()}")
                                        } else {
                                            _state.value = GlassesSessionState.Listening
                                            startListening()
                                            startForeground(NOTIFICATION_ID, buildPersistentNotification())
                                            Log.d(TAG, "Stream stopped early (${elapsed}ms, no frame yet) — resuming Listening (no error)")
                                        }
                                    } else {
                                        _state.value = GlassesSessionState.Listening
                                        startListening()
                                        startForeground(NOTIFICATION_ID, buildPersistentNotification())
                                        Log.d(TAG, "Stream stopped within ${elapsed}ms but had received frames — resuming Listening (no error)")
                                    }
                                } else {
                                    _state.value = GlassesSessionState.Listening
                                    startListening()
                                    startForeground(NOTIFICATION_ID, buildPersistentNotification())
                                }
                            }
                        }
                    },
                    onFirstVideoFrameReceived = {
                        scanningReceivedFirstFrame = true
                        lifecycleScope.launch(Dispatchers.IO) {
                            glassesManager?.stopAudioStream()
                        }
                    },
                    onVideoFrameNv21 = { data, w, h, rot -> headlessScanner?.processFrame(data, w, h, rot) }
                )

                _state.value = GlassesSessionState.Connected
                Log.d(TAG, "Glasses manager connected (glasses mic and glasses camera only)")

                startListening()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect: ${e.message}", e)
                _state.value = GlassesSessionState.Error(e.message ?: "Connection failed")
            }
        }
    }

    private suspend fun isMetaGlassesRegistered(): Boolean {
        return try {
            val regState = Wearables.registrationState.first()
            regState is RegistrationState.Registered
        } catch (e: Exception) {
            Log.w(TAG, "Could not check DAT registration: ${e.message}")
            false
        }
    }

    private fun startListening() {
        lifecycleScope.launch {
            try {
                wakeWordListener?.start()
                val audioStarted = glassesManager?.connectAndStartAudioStream() ?: false
                if (audioStarted) {
                    _state.value = GlassesSessionState.Listening
                    Log.d(TAG, "Listening for wake word (glasses mic)")
                } else {
                    val errorMessage = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        "Glasses wake word requires Android 10 or later."
                    } else {
                        "Glasses mic not found. Ensure Ray-Ban Meta glasses are paired and connected as audio device."
                    }
                    _state.value = GlassesSessionState.Error(errorMessage)
                    Log.e(TAG, "Glasses mic not available — not using phone mic")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening: ${e.message}", e)
                _state.value = GlassesSessionState.Error(e.message ?: "Listening failed")
            }
        }
    }

    private fun onWakeWordDetected() {
        Log.d(TAG, "Wake word detected — showing Scanning state, then checking camera permission")
        _state.value = GlassesSessionState.Scanning
        scanningStartedAtMs = System.currentTimeMillis()
        scanningReceivedFirstFrame = false
        startForeground(NOTIFICATION_ID, buildPersistentNotification())

        lifecycleScope.launch {
            var result = GlassesSessionManager.checkCameraPermissionResult()
            if (result is CheckCameraResult.Denied || result is CheckCameraResult.Error) {
                Log.d(TAG, "Camera permission not granted on first check — retrying after ${PERMISSION_RETRY_DELAY_MS}ms")
                delay(PERMISSION_RETRY_DELAY_MS)
                result = GlassesSessionManager.checkCameraPermissionResult()
            }
            when (result) {
                is CheckCameraResult.Granted -> {
                    Log.d(TAG, "Camera permission granted — starting camera stream for QR scanning")
                    val activityContext = currentActivityRef?.get()
                    if (activityContext != null) {
                        Log.d(TAG, "Using Activity context for video stream (Meta AI permission/stream flow)")
                    } else {
                        Log.d(TAG, "No Activity context — using Service context for video stream")
                    }
                    glassesManager?.startVideoStream(activityContext)
                }
                is CheckCameraResult.Denied -> {
                    Log.d(TAG, "Camera permission denied — showing grant steps")
                    _state.value = GlassesSessionState.Error(PermissionConstants.GLASSES_CAMERA_GRANT_STEPS)
                    startForeground(NOTIFICATION_ID, buildPersistentNotification())
                }
                is CheckCameraResult.Error -> {
                    Log.d(TAG, "Camera permission check failed: ${result.reason}")
                    _state.value = GlassesSessionState.Error(result.reason)
                    startForeground(NOTIFICATION_ID, buildPersistentNotification())
                }
            }
        }
    }

    private fun onQRCodeDetected(rawQR: String) {
        Log.d(TAG, "QR code detected from glasses: ${rawQR.take(80)}...")
        SetupHelper.getScanToPayBlockedMessage(this)?.let { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            _state.value = GlassesSessionState.Error(msg)
            startForeground(NOTIFICATION_ID, buildPersistentNotification())
            return
        }

        _state.value = GlassesSessionState.PaymentReady(rawQR)

        glassesManager?.stopVideoStream()

        pocketWakeUp?.wakePhoneAndLaunchPayment(rawQR)

        lifecycleScope.launch {
            kotlinx.coroutines.delay(5000)
            if (_state.value is GlassesSessionState.PaymentReady) {
                startListening()
            }
        }
    }

    private fun buildPersistentNotification(): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingTap = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = when (val s = _state.value) {
            is GlassesSessionState.Scanning -> "Scanning for QR..."
            is GlassesSessionState.Listening -> "Say \"flowpay\" to glasses or tap to scan in app"
            is GlassesSessionState.Connecting -> "Connecting..."
            is GlassesSessionState.PaymentReady -> "QR detected!"
            is GlassesSessionState.Error -> s.message
            else -> "Say \"flowpay\" to glasses or tap to scan in app"
        }

        return NotificationCompat.Builder(this, FlowPayApplication.GLASSES_SESSION_CHANNEL_ID)
            .setContentTitle("Flowpay: Glasses Connected")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingTap)
            .build()
    }

    /**
     * Manually trigger QR scan (same as wake word detected).
     * Uses the same permission gate: only starts stream when camera permission is Granted.
     */
    fun triggerScan() {
        if (_state.value is GlassesSessionState.Listening) {
            Log.d(TAG, "Manual scan triggered")
            onWakeWordDetected()
        }
    }

    /**
     * Start the glasses video stream after the user has granted camera permission (e.g. from Meta AI).
     * Call from MainActivity when [Wearables.RequestPermissionContract] returns Granted.
     * Uses the given Activity context for DAT if non-null.
     */
    fun startStreamAfterPermissionGranted(activity: Activity?) {
        val manager = glassesManager ?: return
        Log.d(TAG, "Starting stream after permission granted (Activity context: ${activity != null})")
        _state.value = GlassesSessionState.Scanning
        scanningStartedAtMs = System.currentTimeMillis()
        scanningReceivedFirstFrame = false
        manager.startVideoStream(activity)
    }

    fun stopSession() {
        Log.d(TAG, "Stopping glasses session")
        wakeWordListener?.stop()
        glassesManager?.stopAudioStream()
        glassesManager?.stopVideoStream()
        pocketWakeUp?.cancelVibration()
        _state.value = GlassesSessionState.Idle
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        wakeWordListener?.destroy()
        glassesManager?.destroy()
        headlessScanner?.destroy()
        pocketWakeUp?.cancelVibration()
        _state.value = GlassesSessionState.Idle
        instance = null
        Log.d(TAG, "GlassesSessionService destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
