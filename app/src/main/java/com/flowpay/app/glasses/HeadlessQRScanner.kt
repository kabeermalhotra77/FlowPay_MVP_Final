package com.flowpay.app.glasses

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import com.flowpay.app.features.qr_scanner.domain.QRCodeParser
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.ByteArrayOutputStream

/**
 * Processes raw video frames (NV21) through ML Kit barcode scanning
 * without requiring CameraX or a preview surface.
 *
 * When a valid UPI QR is detected, passes the **raw QR string** to the callback.
 * The actual VPA extraction, clipboard copy, and USSD dialing happen inside the
 * existing [QRScannerActivity.processQRCode()] pipeline.
 */
class HeadlessQRScanner(
    private val onQRDetected: (String) -> Unit
) {
    companion object {
        private const val TAG = "HeadlessQRScanner"
        private const val DUPLICATE_THRESHOLD_MS = 3000L
        /** Minimum length of the longer side for reliable QR detection from glasses (higher = better at distance). */
        private const val MIN_QR_SIDE = 1920
    }

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAllPotentialBarcodes()
            .build()
    )

    private var lastDetectedCode: String? = null
    private var lastDetectionTime = 0L
    @Volatile
    private var isProcessing = false
    @Volatile
    private var pendingBitmap: Bitmap? = null

    @Volatile
    private var hasLoggedScaledDimensions = false

    /**
     * Feed a raw NV21 frame from the phone camera fallback.
     * Small frames are scaled up to MIN_QR_SIDE on the longer side so ML Kit can detect QRs reliably.
     */
    fun processFrame(data: ByteArray, width: Int, height: Int, rotation: Int) {
        if (isProcessing) return
        isProcessing = true
        val maxSide = maxOf(width, height)
        if (maxSide >= MIN_QR_SIDE) {
            val inputImage = InputImage.fromByteArray(
                data, width, height, rotation,
                InputImage.IMAGE_FORMAT_NV21
            )
            scanImage(inputImage, null, width, height)
        } else {
            val bitmap = nv21ToBitmap(data, width, height)
            if (bitmap != null) {
                val scaled = scaleBitmapForQR(bitmap)
                if (scaled !== bitmap) bitmap.recycle()
                if (!hasLoggedScaledDimensions) {
                    hasLoggedScaledDimensions = true
                    Log.d(TAG, "Frame ${width}x${height} scaled to ${scaled.width}x${scaled.height} for QR detection")
                }
                scanImage(InputImage.fromBitmap(scaled, 0), scaled, scaled.width, scaled.height)
            } else {
                val inputImage = InputImage.fromByteArray(
                    data, width, height, rotation,
                    InputImage.IMAGE_FORMAT_NV21
                )
                scanImage(inputImage, null, width, height)
            }
        }
    }

    /** Convert NV21 YUV bytes to Bitmap (same approach as RealMetaGlassesManager.videoFrameToBitmap). */
    private fun nv21ToBitmap(data: ByteArray, width: Int, height: Int): Bitmap? {
        if (width <= 0 || height <= 0) return null
        val expectedNv21Size = width * height * 3 / 2
        if (data.size < expectedNv21Size) return null
        return try {
            val yuvImage = YuvImage(data, ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 95, out)
            BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
        } catch (e: Exception) {
            Log.e(TAG, "NV21 to Bitmap failed: ${e.message}")
            null
        }
    }

    /**
     * Feed a Bitmap from the DAT SDK's [VideoFrame].
     * When the scanner is busy, the latest frame is queued and processed when the current scan completes.
     */
    fun processBitmap(bitmap: Bitmap) {
        val toProcess: Bitmap?
        synchronized(this) {
            if (isProcessing) {
                pendingBitmap?.recycle()
                pendingBitmap = bitmap
                return
            }
            isProcessing = true
            toProcess = bitmap
        }
        val scaled = scaleBitmapForQR(bitmap)
        val inputImage = InputImage.fromBitmap(scaled, 0)
        if (scaled !== bitmap) bitmap.recycle()
        scanImage(inputImage, scaled, scaled.width, scaled.height)
    }

    /** Scale up small bitmaps so ML Kit has enough pixels for QR detection. */
    private fun scaleBitmapForQR(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val maxSide = maxOf(w, h)
        if (maxSide >= MIN_QR_SIDE) return bitmap
        val scale = MIN_QR_SIDE.toFloat() / maxSide
        val newW = (w * scale).toInt().coerceAtLeast(1)
        val newH = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun scanImage(inputImage: InputImage, bitmapToRecycle: Bitmap?, imageWidth: Int = 0, imageHeight: Int = 0) {
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val rawValue = barcodes.firstOrNull()?.rawValue
                if (rawValue != null) {
                    val now = System.currentTimeMillis()
                    if (rawValue == lastDetectedCode &&
                        (now - lastDetectionTime) < DUPLICATE_THRESHOLD_MS
                    ) {
                        Log.d(TAG, "Duplicate QR within threshold, ignoring")
                        return@addOnSuccessListener
                    }
                    if (!QRCodeParser.isValidUPIQRCode(rawValue)) {
                        Log.d(TAG, "Non-UPI barcode ignored (detection works): ${rawValue.take(50)}...")
                        return@addOnSuccessListener
                    }
                    lastDetectedCode = rawValue
                    lastDetectionTime = now
                    Log.d(TAG, "Valid UPI QR detected: ${rawValue.take(80)}...")
                    onQRDetected(rawValue)
                } else if (imageWidth > 0 && imageHeight > 0 && barcodes.isEmpty()) {
                    Log.v(TAG, "ML Kit: no barcode in ${imageWidth}x${imageHeight} frame")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Barcode scanning failed: ${e.message}")
            }
            .addOnCompleteListener {
                bitmapToRecycle?.recycle()
                val next = synchronized(this) {
                    isProcessing = false
                    pendingBitmap.also { pendingBitmap = null }
                }
                if (next != null) {
                    isProcessing = true
                    val scaled = scaleBitmapForQR(next)
                    val img = InputImage.fromBitmap(scaled, 0)
                    if (scaled !== next) next.recycle()
                    scanImage(img, scaled, scaled.width, scaled.height)
                }
            }
    }

    fun destroy() {
        scanner.close()
        pendingBitmap?.recycle()
        pendingBitmap = null
        Log.d(TAG, "HeadlessQRScanner destroyed")
    }
}
