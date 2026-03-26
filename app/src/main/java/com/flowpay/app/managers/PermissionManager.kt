package com.flowpay.app.managers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.flowpay.app.constants.PermissionConstants

class PermissionManager(private val activity: Activity) {
    
    companion object {
        private const val TAG = "PermissionManager"
        
        fun canDrawOverlays(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
    }
    
    /**
     * Checks if all required permissions are granted
     */
    fun checkAllPermissions(): Boolean {
        return PermissionConstants.REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Convenience helpers for feature-based permission groups
     */

    /**
     * Phone permissions: CALL_PHONE + READ_PHONE_STATE (and any other critical phone-related ones)
     */
    fun hasPhonePermissions(): Boolean {
        return isPermissionGranted(Manifest.permission.CALL_PHONE) &&
                isPermissionGranted(Manifest.permission.READ_PHONE_STATE)
    }

    fun requestPhonePermissions() {
        val phonePermissions = listOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        )

        val permissionsNeeded = phonePermissions.filterNot { isPermissionGranted(it) }

        if (permissionsNeeded.isNotEmpty()) {
            Log.d(TAG, "Requesting phone permissions: ${permissionsNeeded.joinToString()}")
            ActivityCompat.requestPermissions(
                activity,
                permissionsNeeded.toTypedArray(),
                PermissionConstants.PERMISSIONS_REQUEST_CODE
            )
        } else {
            Log.d(TAG, "Phone permissions already granted")
        }
    }

    /**
     * Camera permission: CAMERA
     */
    fun hasCameraPermission(): Boolean {
        return isPermissionGranted(Manifest.permission.CAMERA)
    }

    fun requestCameraPermission() {
        if (!hasCameraPermission()) {
            Log.d(TAG, "Requesting camera permission")
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.CAMERA),
                PermissionConstants.CAMERA_PERMISSION_REQ_CODE
            )
        } else {
            Log.d(TAG, "Camera permission already granted")
        }
    }
    
    /**
     * Checks if overlay permission is granted
     */
    fun checkOverlayPermission(): Boolean {
        return canDrawOverlays(activity)
    }

    /**
     * Alias for readability in some call sites
     */
    fun hasOverlayPermission(): Boolean = checkOverlayPermission()
    
    /**
     * Requests all required permissions using native Android dialogs
     */
    fun requestRequiredPermissions() {
        val permissionsNeeded = mutableListOf<String>()
        
        PermissionConstants.REQUIRED_PERMISSIONS.forEach { permission ->
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission)
            }
        }
        
        if (permissionsNeeded.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: ${permissionsNeeded.joinToString()}")
            // Use native Android permission request - no custom dialog
            ActivityCompat.requestPermissions(
                activity,
                permissionsNeeded.toTypedArray(),
                PermissionConstants.PERMISSIONS_REQUEST_CODE
            )
        } else {
            Log.d(TAG, "All permissions already granted")
        }
    }
    
    
    /**
     * Requests overlay permission
     */
    fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !canDrawOverlays(activity)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivityForResult(intent, PermissionConstants.OVERLAY_PERMISSION_REQ_CODE)
        }
    }
    
    /**
     * Checks if a specific permission is granted
     */
    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    
    /**
     * Gets the list of missing permissions
     */
    fun getMissingPermissions(): List<String> {
        return PermissionConstants.REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    
    /**
     * Handles permission request results
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        when (requestCode) {
            PermissionConstants.PERMISSIONS_REQUEST_CODE -> {
                val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                
                if (allPermissionsGranted) {
                    Log.d(TAG, "All permissions granted")
                    return true
                } else {
                    Log.w(TAG, "Some permissions denied")
                    val deniedPermissions = permissions.filterIndexed { index, _ ->
                        grantResults[index] != PackageManager.PERMISSION_GRANTED
                    }
                    Log.w(TAG, "Denied permissions: ${deniedPermissions.joinToString()}")
                    return false
                }
            }
            PermissionConstants.OVERLAY_PERMISSION_REQ_CODE -> {
                val overlayGranted = checkOverlayPermission()
                if (overlayGranted) {
                    Log.d(TAG, "Overlay permission granted")
                    return true
                } else {
                    Log.w(TAG, "Overlay permission denied")
                    return false
                }
            }
            PermissionConstants.SMS_PERMISSION_REQUEST_CODE -> {
                val allSMSPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                
                if (allSMSPermissionsGranted) {
                    Log.d(TAG, "SMS permissions granted")
                    return true
                } else {
                    Log.w(TAG, "SMS permissions denied")
                    val deniedPermissions = permissions.filterIndexed { index, _ ->
                        grantResults[index] != PackageManager.PERMISSION_GRANTED
                    }
                    Log.w(TAG, "Denied SMS permissions: ${deniedPermissions.joinToString()}")
                    return false
                }
            }
            PermissionConstants.GLASSES_PERMISSION_REQUEST_CODE -> {
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                if (allGranted) {
                    Log.d(TAG, "Glasses permissions granted")
                    return true
                } else {
                    Log.w(TAG, "Some glasses permissions denied")
                    return false
                }
            }
        }
        return false
    }
    
    /**
     * Gets permission status summary
     */
    fun getPermissionStatusSummary(): String {
        val granted = PermissionConstants.REQUIRED_PERMISSIONS.count { isPermissionGranted(it) }
        val total = PermissionConstants.REQUIRED_PERMISSIONS.size
        val overlayGranted = checkOverlayPermission()
        
        return "Permissions: $granted/$total granted, Overlay: ${if (overlayGranted) "Yes" else "No"}"
    }
    
    
    
    /**
     * Checks if overlay permission is available for services
     * This replaces inline checks in USSDOverlayService and UssdSetupOverlayService
     */
    fun canDrawOverlays(): Boolean {
        return canDrawOverlays(activity)
    }
    
    /**
     * Checks if SMS permissions are granted
     */
    fun checkSMSPermissions(): Boolean {
        return isPermissionGranted(Manifest.permission.RECEIVE_SMS) && 
               isPermissionGranted(Manifest.permission.READ_SMS)
    }

    /**
     * Alias with consistent naming
     */
    fun hasSmsPermissions(): Boolean = checkSMSPermissions()
    
    /**
     * Requests SMS permissions specifically
     */
    fun requestSMSPermissions() {
        val smsPermissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        
        val permissionsNeeded = smsPermissions.filter { permission ->
            !isPermissionGranted(permission)
        }
        
        if (permissionsNeeded.isNotEmpty()) {
            Log.d(TAG, "Requesting SMS permissions: ${permissionsNeeded.joinToString()}")
            ActivityCompat.requestPermissions(
                activity,
                permissionsNeeded.toTypedArray(),
                PermissionConstants.SMS_PERMISSION_REQUEST_CODE
            )
        } else {
            Log.d(TAG, "SMS permissions already granted")
        }
    }
    
    /**
     * Gets SMS permission status
     */
    fun getSMSPermissionStatus(): String {
        val receiveSMS = isPermissionGranted(Manifest.permission.RECEIVE_SMS)
        val readSMS = isPermissionGranted(Manifest.permission.READ_SMS)
        
        return "SMS Permissions - Receive: ${if (receiveSMS) "✅" else "❌"}, Read: ${if (readSMS) "✅" else "❌"}"
    }
    
    /**
     * Checks if SMS permissions are critical for current functionality
     */
    fun areSMSPermissionsCritical(): Boolean {
        // SMS permissions are critical for payment detection
        return true
    }
    
    /**
     * Checks if contact permission is granted
     */
    fun hasContactPermission(): Boolean {
        return isPermissionGranted(Manifest.permission.READ_CONTACTS)
    }
    
    /**
     * Requests contact permission
     */
    fun requestContactPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_CONTACTS),
            PermissionConstants.CONTACTS_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Checks if all glasses-specific permissions are granted
     */
    fun checkGlassesPermissions(): Boolean {
        return PermissionConstants.GLASSES_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Requests glasses-specific permissions (BLE, location, microphone)
     */
    fun requestGlassesPermissions() {
        val needed = PermissionConstants.GLASSES_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            Log.d(TAG, "Requesting glasses permissions: ${needed.joinToString()}")
            ActivityCompat.requestPermissions(
                activity,
                needed.toTypedArray(),
                PermissionConstants.GLASSES_PERMISSION_REQUEST_CODE
            )
        } else {
            Log.d(TAG, "All glasses permissions already granted")
        }
    }
}
