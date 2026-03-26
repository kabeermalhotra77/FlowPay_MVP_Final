package com.flowpay.app.glasses

sealed class GlassesSessionState {

    object Idle : GlassesSessionState()

    object Connecting : GlassesSessionState()

    object Connected : GlassesSessionState()

    /** Audio stream open, Porcupine listening for wake word. */
    object Listening : GlassesSessionState()

    /** Wake word detected, camera stream active, ML Kit scanning frames. */
    object Scanning : GlassesSessionState()

    /** QR detected, phone wake-up sequence in progress. */
    data class PaymentReady(val rawQR: String) : GlassesSessionState()

    data class Error(val message: String) : GlassesSessionState()

    fun isActive(): Boolean = this !is Idle && this !is Error
}
