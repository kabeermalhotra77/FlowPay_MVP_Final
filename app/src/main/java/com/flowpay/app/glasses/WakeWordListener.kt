package com.flowpay.app.glasses

import android.content.Context
import android.util.Log
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException

/**
 * Offline wake-word detection using Picovoice Porcupine.
 *
 * Expects the Android-targeted .ppn model at assets/flowpay-wake-word_android.ppn.
 * If the model is missing or the access key is not set, falls back to a no-op
 * so the rest of the pipeline can still be tested.
 * Audio is fed from [RealMetaGlassesManager] (glasses mic only).
 */
class WakeWordListener(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit
) {
    companion object {
        private const val TAG = "WakeWordListener"

        // Picovoice Console access key
        private const val ACCESS_KEY = "faB+zAB8jkqWoC28aP2jp8sUMnz9+xOJVeFYFq+kG/kgd/EL/9tf1g=="
        private const val KEYWORD_ASSET = "flowpay-wake-word_android.ppn"
    }

    private var porcupine: Porcupine? = null
    private var isStarted = false

    fun start() {
        if (isStarted) return
        try {
            porcupine = Porcupine.Builder()
                .setAccessKey(ACCESS_KEY)
                .setKeywordPath(KEYWORD_ASSET)
                .setSensitivity(0.85f)  // High sensitivity for glasses mic; try saying "flow pay" with slight pause if needed
                .build(context)
            isStarted = true
            Log.d(TAG, "Porcupine initialized — listening for wake word")
        } catch (e: PorcupineException) {
            Log.e(TAG, "Porcupine init failed (missing .ppn or bad key): ${e.message}", e)
            Log.w(TAG, "Wake word disabled — check flowpay-wake-word_android.ppn in assets and Picovoice access key. Use 'Tap to scan' instead.")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error initializing Porcupine: ${e.message}", e)
        }
    }

    /**
     * Feed a raw PCM audio frame (16-bit, 16 kHz, mono) into the Porcupine engine.
     * Called on the audio thread from [RealMetaGlassesManager].
     */
    private var processFrameCount = 0L
    private var lastProcessLogTime = 0L
    private var firstFrameLogged = false

    fun processAudioFrame(pcmData: ByteArray) {
        val engine = porcupine ?: return
        try {
            processFrameCount++
            if (!firstFrameLogged) {
                firstFrameLogged = true
                Log.d(TAG, "First audio frame reached Porcupine — wake word engine active (sampleRate=${engine.sampleRate}, frameLength=${engine.frameLength})")
            }
            val now = System.currentTimeMillis()
            if (now - lastProcessLogTime >= 2000) {
                Log.d(TAG, "Porcupine processing: $processFrameCount frames (listening for flowpay)")
                lastProcessLogTime = now
            }
            val frameLength = engine.frameLength
            val shortBuffer = ShortArray(pcmData.size / 2)
            java.nio.ByteBuffer.wrap(pcmData)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()
                .get(shortBuffer)

            var offset = 0
            while (offset + frameLength <= shortBuffer.size) {
                val frame = shortBuffer.copyOfRange(offset, offset + frameLength)
                val keywordIndex = engine.process(frame)
                if (keywordIndex >= 0) {
                    Log.d(TAG, "Wake word detected!")
                    onWakeWordDetected()
                    return
                }
                offset += frameLength
            }
        } catch (e: PorcupineException) {
            Log.e(TAG, "Porcupine processing error: ${e.message}")
        }
    }

    fun stop() {
        isStarted = false
        Log.d(TAG, "Wake word listener stopped")
    }

    fun destroy() {
        stop()
        try {
            porcupine?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying Porcupine: ${e.message}")
        }
        porcupine = null
        Log.d(TAG, "Wake word listener destroyed")
    }
}
