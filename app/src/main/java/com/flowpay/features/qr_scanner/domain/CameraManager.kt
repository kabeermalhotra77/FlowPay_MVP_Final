package com.flowpay.features.qr_scanner.domain

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class CameraManager {
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var isAnalyzing = false
    private var isProcessing = false
    
    fun initializeCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onQrCodeDetected: (String) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                setupCamera(context, lifecycleOwner, previewView, onQrCodeDetected)
            } catch (e: Exception) {
                Log.e("CameraManager", "Camera initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    private fun setupCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onQrCodeDetected: (String) -> Unit
    ) {
        val cameraProvider = this.cameraProvider ?: return
        
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        
        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(
                    Executors.newSingleThreadExecutor()
                ) { imageProxy ->
                    if (!isAnalyzing && !isProcessing) {
                        processImageProxy(imageProxy) { result ->
                            if (!isAnalyzing && !isProcessing) {
                                isAnalyzing = true
                                isProcessing = true
                                onQrCodeDetected(result)
                            }
                        }
                    } else {
                        imageProxy.close()
                    }
                }
            }
        
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalyzer
            )
        } catch (e: Exception) {
            Log.e("CameraManager", "Camera binding failed", e)
        }
    }
    
    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(
        imageProxy: ImageProxy,
        onQrCodeDetected: (String) -> Unit
    ) {
        imageProxy.image?.let { image ->
            val inputImage = InputImage.fromMediaImage(
                image,
                imageProxy.imageInfo.rotationDegrees
            )
            
            val scanner = BarcodeScanning.getClient()
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull { barcode ->
                        barcode.valueType == Barcode.TYPE_TEXT ||
                        barcode.valueType == Barcode.TYPE_URL
                    }?.rawValue?.let { value ->
                        if (value.contains("upi://", ignoreCase = true)) {
                            onQrCodeDetected(value)
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } ?: imageProxy.close()
    }
    
    fun stopCamera() {
        cameraProvider?.unbindAll()
        isAnalyzing = false
        isProcessing = false
    }
    
    fun resetAnalysis() {
        isAnalyzing = false
        // Don't reset isProcessing here - let it stay true to prevent multiple detections
    }
    
    fun resetProcessing() {
        isProcessing = false
        isAnalyzing = false
    }
}
