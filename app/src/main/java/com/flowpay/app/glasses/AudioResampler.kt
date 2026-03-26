package com.flowpay.app.glasses

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Upsamples 8 kHz mono 16-bit PCM audio to 16 kHz mono 16-bit PCM
 * using linear interpolation. Required because Bluetooth HFP delivers
 * 8 kHz audio from the glasses mic but Porcupine expects 16 kHz.
 */
object AudioResampler {

    /**
     * @param input raw PCM bytes at 8 kHz, 16-bit little-endian mono
     * @return raw PCM bytes at 16 kHz, 16-bit little-endian mono (2x the sample count)
     */
    fun upsample8kTo16k(input: ByteArray): ByteArray {
        val shortBuf = ShortArray(input.size / 2)
        ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuf)

        val outputShorts = ShortArray(shortBuf.size * 2)
        for (i in shortBuf.indices) {
            val current = shortBuf[i].toInt()
            val next = if (i + 1 < shortBuf.size) shortBuf[i + 1].toInt() else current
            outputShorts[i * 2] = current.toShort()
            outputShorts[i * 2 + 1] = ((current + next) / 2).toShort()
        }

        val outputBytes = ByteArray(outputShorts.size * 2)
        ByteBuffer.wrap(outputBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(outputShorts)
        return outputBytes
    }
}
