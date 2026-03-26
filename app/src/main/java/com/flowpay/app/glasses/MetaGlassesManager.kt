package com.flowpay.app.glasses

import android.content.Context

/**
 * Abstraction over the glasses hardware connection.
 * [RealMetaGlassesManager] uses only the glasses mic (Bluetooth HFP) and glasses camera (DAT);
 * the phone mic and phone camera are never used for the glasses flow.
 */
interface MetaGlassesManager {
    /**
     * Connects and starts the audio stream. For real glasses, waits for Bluetooth SCO.
     * @return true if audio stream started successfully, false if glasses mic unavailable
     */
    suspend fun connectAndStartAudioStream(): Boolean

    fun startAudioStream()
    fun stopAudioStream()
    /** Start video stream. Pass an Activity context when available so Meta AI can run the permission/stream flow; otherwise uses the service context. */
    fun startVideoStream(streamContext: Context? = null)
    fun stopVideoStream()
    fun destroy()
}
