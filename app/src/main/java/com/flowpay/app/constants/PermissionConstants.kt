package com.flowpay.app.constants

import android.Manifest
import android.os.Build

/**
 * Centralized permission constants to avoid duplication and ensure consistency
 */
object PermissionConstants {
    
    // Standard Android permissions required by the app
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
        Manifest.permission.CAMERA,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.VIBRATE
    )
    
    // Critical permissions that are absolutely necessary for core functionality
    val CRITICAL_PERMISSIONS = arrayOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE
    )
    
    // Optional permissions that enhance functionality but aren't critical
    val OPTIONAL_PERMISSIONS = arrayOf(
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
        Manifest.permission.CAMERA,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.VIBRATE
    )
    
    // Permission request codes - using unique values to avoid conflicts
    const val PERMISSIONS_REQUEST_CODE = 0x1000
    const val OVERLAY_PERMISSION_REQ_CODE = 0x1001
    const val CAMERA_PERMISSION_REQ_CODE = 0x1002
    const val SMS_PERMISSION_REQUEST_CODE = 0x1003
    const val CONTACTS_PERMISSION_REQUEST_CODE = 0x1004
    const val GLASSES_PERMISSION_REQUEST_CODE = 0x1005
    
    // Glasses-specific permissions.
    // RECORD_AUDIO is required by Android 14+ to start a foreground service with
    // foregroundServiceType="microphone", even though audio is captured from the
    // glasses Bluetooth mic — not the phone mic. Location is NOT required.
    val GLASSES_PERMISSIONS: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.RECORD_AUDIO
        )
    } else {
        arrayOf(
            Manifest.permission.RECORD_AUDIO
        )
    }

    // Permission descriptions for user-friendly explanations
    val PERMISSION_DESCRIPTIONS = mapOf(
        Manifest.permission.CALL_PHONE to "Make phone calls for USSD payments",
        Manifest.permission.READ_PHONE_STATE to "Monitor call states for payment protection",
        Manifest.permission.MODIFY_AUDIO_SETTINGS to "Control audio during calls for better experience",
        Manifest.permission.CAMERA to "Scan QR codes (phone camera for Tap to scan; glasses camera via Meta when connected)",
        Manifest.permission.RECEIVE_SMS to "Receive payment confirmation messages",
        Manifest.permission.READ_SMS to "Read payment-related messages",
        Manifest.permission.READ_CONTACTS to "Select contacts for easy transfers",
        Manifest.permission.VIBRATE to "Provide haptic feedback for notifications",
        Manifest.permission.RECORD_AUDIO to "Glasses microphone for wake word only (not phone mic)"
    )

    /** User-facing description for glasses camera (managed by Meta; not an Android permission). Meta AI may not show a separate permission dialog. */
    const val GLASSES_CAMERA_DESCRIPTION = "Glasses camera for QR scanning only (not phone camera). Meta AI runs the permission flow (Allow once / Allow always) when you tap Open Meta AI."
    /** Guidance when glasses camera stream fails. */
    const val GLASSES_CAMERA_GRANT_STEPS = "If the camera stream doesn't start: tap Open Meta AI — Meta AI will show Allow once or Allow always for the glasses camera. Then return and tap the card to retry."
    /** Shown when stream stopped without receiving frames but camera permission is already granted. */
    const val GLASSES_STREAM_STOPPED_UNEXPECTEDLY = "Camera stream stopped unexpectedly. Tap to retry."
    /** Shorter hint for the glasses card when camera may not be allowed yet. */
    const val GLASSES_CAMERA_HINT = "Glasses camera: Tap Open Meta AI to allow camera (Allow once / Allow always). Then say flow pay or tap to retry."
    
    // Permission categories for better organization
    enum class PermissionCategory {
        PHONE,
        AUDIO,
        CAMERA,
        MESSAGING,
        CONTACTS,
        SYSTEM
    }
    
    val PERMISSION_CATEGORIES = mapOf(
        Manifest.permission.CALL_PHONE to PermissionCategory.PHONE,
        Manifest.permission.READ_PHONE_STATE to PermissionCategory.PHONE,
        Manifest.permission.MODIFY_AUDIO_SETTINGS to PermissionCategory.AUDIO,
        Manifest.permission.RECORD_AUDIO to PermissionCategory.AUDIO,
        Manifest.permission.CAMERA to PermissionCategory.CAMERA,
        Manifest.permission.RECEIVE_SMS to PermissionCategory.MESSAGING,
        Manifest.permission.READ_SMS to PermissionCategory.MESSAGING,
        Manifest.permission.READ_CONTACTS to PermissionCategory.CONTACTS,
        Manifest.permission.VIBRATE to PermissionCategory.SYSTEM
    )
}
