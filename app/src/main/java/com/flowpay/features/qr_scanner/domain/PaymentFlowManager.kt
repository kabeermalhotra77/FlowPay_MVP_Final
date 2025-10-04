package com.flowpay.features.qr_scanner.domain

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

class PaymentFlowManager {
    
    fun copyVPAToClipboard(context: Context, vpa: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("UPI VPA", vpa)
        clipboard.setPrimaryClip(clip)
        
        Toast.makeText(context, "VPA copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    
    fun dialUSSD(context: Context): Result<Unit> {
        return try {
            val ussdCode = "*99*1*3#"
            val encodedHash = Uri.encode("#")
            val ussd = ussdCode.replace("#", encodedHash)
            
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$ussd")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
