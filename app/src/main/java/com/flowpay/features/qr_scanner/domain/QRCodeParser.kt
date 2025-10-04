package com.flowpay.features.qr_scanner.domain

import android.net.Uri

object QRCodeParser {
    
    fun parseUPIQRCode(qrCode: String): UPIData {
        return try {
            val uri = Uri.parse(qrCode)
            
            UPIData(
                vpa = uri.getQueryParameter("pa") ?: "",
                payeeName = uri.getQueryParameter("pn"),
                amount = uri.getQueryParameter("am"),
                transactionNote = uri.getQueryParameter("tn")
            )
        } catch (e: Exception) {
            UPIData(
                vpa = "",
                payeeName = null,
                amount = null,
                transactionNote = null
            )
        }
    }
    
    fun isValidUPIQRCode(qrCode: String): Boolean {
        return qrCode.contains("upi://", ignoreCase = true)
    }
}

data class UPIData(
    val vpa: String,
    val payeeName: String?,
    val amount: String?,
    val transactionNote: String?
)
