// =====================================
// 2. PermissionManager.kt
// =====================================
package com.flowpay.app.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {
    
    companion object {
        const val PERMISSION_REQUEST_CODE = 1001
        
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.VIBRATE
        )
        
        val PERMISSION_DESCRIPTIONS = mapOf(
            Manifest.permission.CALL_PHONE to "Make UPI 123 payment calls",
            Manifest.permission.READ_PHONE_STATE to "Monitor call states for payment protection",
            Manifest.permission.RECEIVE_SMS to "Receive payment confirmation messages",
            Manifest.permission.READ_SMS to "Read payment-related messages",
            Manifest.permission.READ_CONTACTS to "Select contacts for easy transfers",
            Manifest.permission.CAMERA to "Scan QR codes for payments",
            Manifest.permission.VIBRATE to "Provide haptic feedback"
        )
    }
    
    fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == 
               PackageManager.PERMISSION_GRANTED
    }
    
    fun checkAllPermissions(): Map<String, Boolean> {
        return REQUIRED_PERMISSIONS.associate { permission ->
            permission to checkPermission(permission)
        }
    }
    
    fun getMissingPermissions(): List<String> {
        return REQUIRED_PERMISSIONS.filter { !checkPermission(it) }
    }
    
    fun requestPermissions(activity: Activity, permissions: Array<String>) {
        ActivityCompat.requestPermissions(
            activity,
            permissions,
            PERMISSION_REQUEST_CODE
        )
    }
    
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
    
    /**
     * Checks if contact permission is granted
     */
    fun hasContactPermission(): Boolean {
        return checkPermission(Manifest.permission.READ_CONTACTS)
    }
    
    /**
     * Requests contact permission
     */
    fun requestContactPermission(activity: Activity) {
        requestPermissions(activity, arrayOf(Manifest.permission.READ_CONTACTS))
    }
}
