package com.flowpay.app.helpers

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object SMSPermissionHelper {
    const val SMS_PERMISSION_CODE = 101
    
    fun checkAndRequestPermissions(activity: Activity): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
        
        
        // Add CALL_PHONE for making and ending calls
        permissions.add(Manifest.permission.CALL_PHONE)
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }
        
        return if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                SMS_PERMISSION_CODE
            )
            false
        } else {
            true
        }
    }
}
