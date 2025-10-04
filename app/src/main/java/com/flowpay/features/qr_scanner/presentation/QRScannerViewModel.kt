package com.flowpay.features.qr_scanner.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowpay.features.qr_scanner.domain.PaymentFlowManager
import com.flowpay.features.qr_scanner.domain.QRCodeParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QRScannerState(
    val isScanning: Boolean = true,
    val showInstructions: Boolean = false,
    val showSuccess: Boolean = false,
    val showProcessing: Boolean = false,
    val vpaAddress: String = "",
    val error: String? = null
)

class QRScannerViewModel : ViewModel() {
    
    private val _state = MutableStateFlow(QRScannerState())
    val state: StateFlow<QRScannerState> = _state.asStateFlow()
    
    private val paymentFlowManager = PaymentFlowManager()
    private var cameraManager: com.flowpay.features.qr_scanner.domain.CameraManager? = null
    
    init {
        // Reset state when ViewModel is created
        resetToInitialState()
    }
    
    fun processQRCode(qrCode: String, context: Context) {
        viewModelScope.launch {
            try {
                // Validate QR code format
                if (!QRCodeParser.isValidUPIQRCode(qrCode)) {
                    _state.value = _state.value.copy(
                        error = "Invalid UPI QR code format"
                    )
                    return@launch
                }
                
                // Parse UPI QR code
                val upiData = QRCodeParser.parseUPIQRCode(qrCode)
                
                if (upiData.vpa.isEmpty()) {
                    _state.value = _state.value.copy(
                        error = "Invalid UPI QR code - VPA not found"
                    )
                    return@launch
                }
                
                // Copy VPA to clipboard
                paymentFlowManager.copyVPAToClipboard(context, upiData.vpa)
                
                // Update state to show processing screen
                _state.value = _state.value.copy(
                    isScanning = false,
                    showInstructions = false,
                    showSuccess = false,
                    showProcessing = true,
                    vpaAddress = upiData.vpa
                )
                
                // Dial USSD code
                paymentFlowManager.dialUSSD(context)
                    .onFailure { exception ->
                        _state.value = _state.value.copy(
                            error = "Failed to initiate USSD: ${exception.message}"
                        )
                    }
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to process QR code: ${e.message}"
                )
            }
        }
    }
    
    fun onPaymentComplete() {
        _state.value = _state.value.copy(
            showInstructions = false,
            showSuccess = true
        )
        // Reset camera processing to allow new QR code scanning
        cameraManager?.resetProcessing()
    }
    
    fun cancelPayment() {
        resetToInitialState()
    }
    
    fun setCameraManager(cameraManager: com.flowpay.features.qr_scanner.domain.CameraManager) {
        this.cameraManager = cameraManager
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
    
    fun resetToInitialState() {
        _state.value = QRScannerState()
        // Reset camera processing to allow new QR code scanning
        cameraManager?.resetProcessing()
    }
    
    fun onScreenResumed() {
        // Reset state when screen is resumed to ensure clean state
        resetToInitialState()
    }
}
