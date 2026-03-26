package com.flowpay.app.glasses

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioDeviceInfo
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.meta.wearable.dat.camera.StreamSession
import com.meta.wearable.dat.camera.startStreamSession
import com.meta.wearable.dat.camera.types.StreamConfiguration
import com.meta.wearable.dat.camera.types.StreamSessionState
import com.meta.wearable.dat.camera.types.VideoFrame
import com.meta.wearable.dat.camera.types.VideoQuality
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Connects to Ray-Ban Meta glasses via the official Meta Wearables
 * Device Access Toolkit (DAT) v0.4.0.
 *
 * - **Audio**: Glasses mic only. Uses Bluetooth HFP to capture the glasses microphone at 8 kHz mono,
 *   locks to glasses via [setPreferredDevice] (no phone mic), upsamples to 16 kHz via [AudioResampler],
 *   and passes to [onAudioFrame] for Porcupine.
 * - **Video**: Glasses camera only. Uses [StreamSession.videoStream] from the DAT SDK to receive
 *   frames from the glasses camera (no phone camera). When [onVideoFrameNv21] is set, passes raw
 *   NV21 bytes for ML Kit (no JPEG re-encode); otherwise converts to [Bitmap] and [onVideoFrame].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RealMetaGlassesManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onAudioFrame: (ByteArray) -> Unit,
    private val onVideoFrame: (Bitmap) -> Unit,
    private val onVideoStreamStopped: (() -> Unit)? = null,
    private val onFirstVideoFrameReceived: (() -> Unit)? = null,
    private val onVideoFrameNv21: ((ByteArray, Int, Int, Int) -> Unit)? = null
) : MetaGlassesManager {

    companion object {
        private const val TAG = "RealMetaGlassesManager"
        private const val HFP_SAMPLE_RATE = 8000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val SCO_CONNECT_TIMEOUT_MS = 8000L
        private const val GLASSES_INPUT_RETRY_ATTEMPTS = 3
        private const val GLASSES_INPUT_RETRY_DELAY_MS = 450L

        /** Device name substrings that identify Ray-Ban Meta / Oakley Meta glasses.
         * RBM = Ray-Ban Meta prefix when paired via Meta AI app. */
        private val GLASSES_DEVICE_NAME_PATTERNS = listOf(
            "RBM", "Ray-Ban", "Ray Ban", "Meta", "Oakley", "Stories"
        )

        private fun isGlassesDevice(device: BluetoothDevice): Boolean {
            val name = device.name?.takeIf { it.isNotBlank() } ?: return false
            return GLASSES_DEVICE_NAME_PATTERNS.any { pattern ->
                name.contains(pattern, ignoreCase = true)
            }
        }
    }

    private var streamSession: StreamSession? = null
    private var videoCollectorJob: Job? = null
    private var stateCollectorJob: Job? = null

    private var audioRecord: AudioRecord? = null
    private var audioThread: Thread? = null
    private var scoReceiver: BroadcastReceiver? = null
    @Volatile private var isRecordingAudio = false

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // --- Audio (Bluetooth HFP from glasses mic) ---

    /**
     * Returns the connected Ray-Ban Meta / Oakley Meta glasses HFP device, or null if none.
     * Only glasses mic is used — never falls back to phone or other headsets.
     */
    private suspend fun getConnectedGlassesHfpDevice(): BluetoothDevice? {
        return withTimeoutOrNull(2000L) {
            suspendCancellableCoroutine { cont ->
                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter == null || !adapter.isEnabled) {
                    cont.resume(null) {}
                    return@suspendCancellableCoroutine
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        cont.resume(null) {}
                        return@suspendCancellableCoroutine
                    }
                }
                val resumed = AtomicBoolean(false)
                adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        if (!resumed.compareAndSet(false, true)) return
                        if (profile == BluetoothProfile.HEADSET && proxy is BluetoothHeadset) {
                            val connected = proxy.connectedDevices
                            if (connected.isEmpty()) {
                                Log.w(TAG, "No HFP devices connected. Pair glasses in Meta AI app first.")
                            } else {
                                connected.forEach { d ->
                                    Log.d(TAG, "HFP device: name='${d.name}' address=${d.address} matchesGlasses=${isGlassesDevice(d)}")
                                }
                            }
                            var glassesDevice = connected.firstOrNull { isGlassesDevice(it) }
                            // Fallback: when DAT is registered, single HFP device is likely the glasses
                            if (glassesDevice == null && connected.size == 1) {
                                glassesDevice = connected.first()
                                Log.d(TAG, "Using single HFP device as glasses (name='${glassesDevice.name}')")
                            }
                            adapter.closeProfileProxy(BluetoothProfile.HEADSET, proxy)
                            cont.resume(glassesDevice) {}
                        } else {
                            adapter.closeProfileProxy(profile, proxy)
                            cont.resume(null) {}
                        }
                    }
                    override fun onServiceDisconnected(profile: Int) {
                        if (!resumed.compareAndSet(false, true)) return
                        cont.resume(null) {}
                    }
                }, BluetoothProfile.HEADSET)
                cont.invokeOnCancellation { /* no cleanup needed */ }
            }
        } ?: null
    }

    /**
     * Waits for Bluetooth SCO to connect, then starts AudioRecord.
     * Must be called from a coroutine. Ensures glasses mic is used, not phone mic.
     */
    override suspend fun connectAndStartAudioStream(): Boolean {
        if (isRecordingAudio) return true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "RECORD_AUDIO not granted, skipping audio")
            return false
        }

        // Require Android 10+ so we can lock AudioRecord to glasses via setPreferredDevice — never use phone mic
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.w(TAG, "Glasses audio requires Android 10 (API 29) or later to lock to glasses mic. Will NOT use phone mic.")
            return false
        }

        val glassesDevice = getConnectedGlassesHfpDevice()
        if (glassesDevice == null) {
            Log.e(TAG, "Glasses mic not found. Ensure Ray-Ban Meta glasses are paired and connected as audio device. Will NOT use phone mic.")
            return false
        }
        Log.d(TAG, "Found glasses HFP device: ${glassesDevice.name} (${glassesDevice.address})")

        // Register receiver BEFORE startBluetoothSco to avoid missing the SCO broadcast
        var scoCont: kotlin.coroutines.Continuation<Unit>? = null
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED) {
                    val state = intent.getIntExtra(
                        AudioManager.EXTRA_SCO_AUDIO_STATE,
                        AudioManager.SCO_AUDIO_STATE_DISCONNECTED
                    )
                    if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                        try {
                            context.unregisterReceiver(this)
                        } catch (_: Exception) { /* already unregistered */ }
                        scoReceiver = null
                        Log.d(TAG, "Bluetooth SCO connected — glasses mic active (${glassesDevice.name})")
                        scoCont?.resume(Unit)
                    }
                }
            }
        }
        scoReceiver = receiver
        val filter = IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true
            Log.d(TAG, "Bluetooth SCO started, waiting for connection...")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Bluetooth SCO: ${e.message}")
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
            scoReceiver = null
            return false
        }

        val scoConnected = withTimeoutOrNull(SCO_CONNECT_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                scoCont = cont
                cont.invokeOnCancellation {
                    try {
                        context.unregisterReceiver(receiver)
                    } catch (_: Exception) { /* already unregistered */ }
                    scoReceiver = null
                }
            }
        }

        if (scoConnected == null) {
            Log.e(TAG, "Bluetooth SCO connection timed out — glasses mic not available. Pair glasses and ensure they are connected.")
            try {
                scoReceiver?.let { context.unregisterReceiver(it) }
                scoReceiver = null
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                audioManager.mode = AudioManager.MODE_NORMAL
            } catch (_: Exception) {}
            return false
        }

        if (!audioManager.isBluetoothScoOn) {
            Log.e(TAG, "SCO reported connected but isBluetoothScoOn is false — aborting to avoid phone mic")
            try {
                audioManager.stopBluetoothSco()
                audioManager.mode = AudioManager.MODE_NORMAL
            } catch (_: Exception) {}
            return false
        }

        // Retry resolving glasses input device — on some devices it appears in getDevices() after a short delay
        val glassesAddress = glassesDevice.address
        var glassesInput: AudioDeviceInfo? = null
        for (attempt in 1..GLASSES_INPUT_RETRY_ATTEMPTS) {
            val inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            glassesInput = inputDevices.firstOrNull { info ->
                val isBt = info.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    info.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                isBt && info.address.equals(glassesAddress, ignoreCase = true)
            }
            if (glassesInput != null) {
                Log.d(TAG, "Glasses input device found on attempt $attempt (no phone mic)")
                break
            }
            if (attempt < GLASSES_INPUT_RETRY_ATTEMPTS) {
                delay(GLASSES_INPUT_RETRY_DELAY_MS)
            }
        }
        if (glassesInput == null) {
            Log.e(TAG, "Glasses not found in audio inputs after $GLASSES_INPUT_RETRY_ATTEMPTS attempts — aborting to avoid phone mic")
            try {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                audioManager.mode = AudioManager.MODE_NORMAL
            } catch (_: Exception) {}
            return false
        }

        val bufferSize = AudioRecord.getMinBufferSize(HFP_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
            Log.e(TAG, "Invalid audio buffer size for 8kHz HFP")
            return false
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                HFP_SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            // Lock to glasses only — we already resolved glassesInput in the retry loop (API 29+ only)
            val set = audioRecord!!.setPreferredDevice(glassesInput)
            if (!set) {
                Log.e(TAG, "setPreferredDevice failed — aborting to avoid phone mic")
                audioRecord?.release()
                audioRecord = null
                return false
            }
            Log.d(TAG, "AudioRecord locked to glasses input (no phone mic)")

            isRecordingAudio = true
            audioRecord?.startRecording()

            audioThread = Thread({
                val buffer = ByteArray(bufferSize)
                var frameCount = 0L
                var lastLogTime = System.currentTimeMillis()
                var firstFrameLogged = false
                while (isRecordingAudio) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        if (!firstFrameLogged) {
                            firstFrameLogged = true
                            Log.d(TAG, "First audio frame read from glasses mic — feeding wake word engine")
                        }
                        frameCount++
                        val chunk8k = buffer.copyOf(read)
                        val chunk16k = AudioResampler.upsample8kTo16k(chunk8k)
                        onAudioFrame(chunk16k)
                        val now = System.currentTimeMillis()
                        if (now - lastLogTime >= 2000) {
                            Log.d(TAG, "Audio flowing: $frameCount frames in last ${(now - lastLogTime) / 1000}s")
                            lastLogTime = now
                        }
                    }
                }
            }, "GlassesHFPAudioThread").apply { start() }

            Log.d(TAG, "Audio stream started (8kHz HFP from glasses mic -> 16kHz upsampled)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio: ${e.message}", e)
            isRecordingAudio = false
            return false
        }
        return true
    }

    override fun startAudioStream() {
        // Legacy sync entry point — use connectAndStartAudioStream() from coroutine instead
        scope.launch { connectAndStartAudioStream() }
    }

    override fun stopAudioStream() {
        isRecordingAudio = false
        try {
            scoReceiver?.let {
                try { context.unregisterReceiver(it) } catch (_: Exception) {}
            }
            scoReceiver = null
            audioThread?.join(1000)
            audioRecord?.stop()
            audioRecord?.release()
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio: ${e.message}")
        }
        audioRecord = null
        audioThread = null
        Log.d(TAG, "Audio stream stopped")
    }

    /**
     * Converts Meta DAT VideoFrame (NV21 YUV in ByteBuffer) to Bitmap for ML Kit.
     */
    private fun videoFrameToBitmap(frame: VideoFrame): Bitmap? {
        val buffer = frame.buffer ?: return null
        val width = frame.width
        val height = frame.height
        if (width <= 0 || height <= 0) return null
        val size = buffer.remaining()
        val expectedNv21Size = width * height * 3 / 2
        if (size < expectedNv21Size) return null
        val bytes = ByteArray(size)
        buffer.duplicate().get(bytes)
        return try {
            val yuvImage = YuvImage(bytes, ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 95, out)
            BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
        } catch (e: Exception) {
            Log.e(TAG, "VideoFrame to Bitmap failed: ${e.message}")
            null
        }
    }

    // --- Video (DAT StreamSession — glasses camera only, no phone camera) ---
    // Permission is gated by GlassesSessionService (checkCameraPermissionResult) before this is called.

    override fun startVideoStream(streamContext: Context?) {
        if (streamSession != null) return

        val ctx = streamContext ?: context
        val isActivity = ctx is Activity
        Log.d(TAG, "Starting video stream (DAT — glasses camera only, context is ${if (isActivity) "Activity" else "Service"})")
        try {
            val session = Wearables.startStreamSession(
                context = ctx,
                deviceSelector = AutoDeviceSelector(),
                streamConfiguration = StreamConfiguration(
                    videoQuality = VideoQuality.HIGH,
                    frameRate = 24
                )
            )
            streamSession = session

            videoCollectorJob = scope.launch(Dispatchers.Default) {
                var lastProcessTime = 0L
                var firstFrameLogged = false
                val streamStartTime = System.currentTimeMillis()
                val fastPhaseMs = 5000L  // First 5 seconds: faster throttle for quicker QR detection
                val throttleFastMs = 40L
                val throttleNormalMs = 80L
                session.videoStream.collect { frame: VideoFrame ->
                    try {
                        if (!firstFrameLogged) {
                            firstFrameLogged = true
                            Log.d(TAG, "First video frame received from glasses — stream active (${frame.width}x${frame.height})")
                            onFirstVideoFrameReceived?.invoke()
                        }
                        val now = System.currentTimeMillis()
                        val throttleMs = if (now - streamStartTime < fastPhaseMs) throttleFastMs else throttleNormalMs
                        if (now - lastProcessTime < throttleMs) return@collect
                        lastProcessTime = now
                        val buffer = frame.buffer
                        val width = frame.width
                        val height = frame.height
                        if (buffer != null && width > 0 && height > 0) {
                            val size = buffer.remaining()
                            val expectedNv21Size = width * height * 3 / 2
                            if (size >= expectedNv21Size) {
                                if (onVideoFrameNv21 != null) {
                                    val bytes = ByteArray(size)
                                    buffer.duplicate().get(bytes)
                                    onVideoFrameNv21(bytes, width, height, 0)
                                } else {
                                    val bitmap = videoFrameToBitmap(frame)
                                    if (bitmap != null) {
                                        onVideoFrame(bitmap)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing video frame: ${e.message}")
                    }
                }
            }

            stateCollectorJob = scope.launch {
                session.state.collect { state: StreamSessionState ->
                    Log.d(TAG, "Stream state: $state")
                    if (state == StreamSessionState.STOPPED) {
                        Log.w(TAG, "Stream session stopped by device")
                        onVideoStreamStopped?.invoke()
                    }
                }
            }

            Log.d(TAG, "Video stream started via DAT SDK (glasses camera only)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start video stream: ${e.message}", e)
        }
    }

    override fun stopVideoStream() {
        videoCollectorJob?.cancel()
        stateCollectorJob?.cancel()
        videoCollectorJob = null
        stateCollectorJob = null
        try {
            streamSession?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing stream session: ${e.message}")
        }
        streamSession = null
        Log.d(TAG, "Video stream stopped")
    }

    override fun destroy() {
        stopAudioStream()
        stopVideoStream()
        Log.d(TAG, "RealMetaGlassesManager destroyed")
    }
}
