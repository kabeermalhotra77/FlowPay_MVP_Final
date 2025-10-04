package com.flowpay.app.features.qr_scanner.domain

import android.net.Uri
import android.util.Log
import com.flowpay.app.data.UPIData

object QRCodeParser {
    
    fun parseUPIQRCode(qrCode: String): UPIData {
        return try {
            Log.d("QRCodeParser", "Parsing QR code: ${qrCode.take(100)}...")
            
            // Check if it's a UPI QR code
            if (qrCode.contains("upi://", ignoreCase = true)) {
                Log.d("QRCodeParser", "Detected UPI QR code format")
                val uri = Uri.parse(qrCode)
                
                val vpa = uri.getQueryParameter("pa") ?: ""
                val payeeName = uri.getQueryParameter("pn")
                val amount = uri.getQueryParameter("am")
                val transactionNote = uri.getQueryParameter("tn")
                
                Log.d("QRCodeParser", "Extracted - VPA: $vpa, Payee: $payeeName, Amount: $amount")
                
                UPIData(
                    vpa = vpa,
                    payeeName = payeeName ?: "",
                    amount = amount ?: "",
                    transactionNote = transactionNote ?: "",
                    currency = "INR"
                )
            } else {
                Log.d("QRCodeParser", "Non-UPI QR code, trying to extract VPA")
                
                // For non-UPI QR codes, try to extract VPA from the text
                // Look for patterns like "upi://pay?pa=..." or just VPA patterns
                val vpaPattern = Regex("pa=([^&]+)")
                val vpaMatch = vpaPattern.find(qrCode)
                val vpa = vpaMatch?.groupValues?.get(1) ?: ""
                
                Log.d("QRCodeParser", "VPA from pa= pattern: $vpa")
                
                // If no VPA found, treat the entire QR code as potential VPA if it contains @
                val finalVpa = if (vpa.isEmpty() && qrCode.contains("@")) {
                    val trimmed = qrCode.trim()
                    Log.d("QRCodeParser", "Using entire QR code as VPA: $trimmed")
                    trimmed
                } else {
                    vpa
                }
                
                Log.d("QRCodeParser", "Final VPA: $finalVpa")
                
                UPIData(
                    vpa = finalVpa,
                    payeeName = "",
                    amount = "",
                    transactionNote = "",
                    currency = "INR"
                )
            }
        } catch (e: Exception) {
            Log.e("QRCodeParser", "Error parsing QR code: ${e.message}", e)
            
            // If parsing fails, try to extract VPA from the raw text
            val vpaPattern = Regex("([a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+)")
            val vpaMatch = vpaPattern.find(qrCode)
            val vpa = vpaMatch?.value ?: ""
            
            Log.d("QRCodeParser", "Fallback VPA extraction: $vpa")
            
            UPIData(
                vpa = vpa,
                payeeName = "",
                amount = "",
                transactionNote = "",
                currency = "INR"
            )
        }
    }
    
    fun isValidUPIQRCode(qrCode: String): Boolean {
        return qrCode.contains("upi://", ignoreCase = true) || 
               qrCode.contains("@") // Basic VPA pattern check
    }
}

