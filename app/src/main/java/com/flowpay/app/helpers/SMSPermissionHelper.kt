package com.flowpay.app.helpers

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Formerly managed READ_SMS / RECEIVE_SMS runtime permissions.
 * After migration to NotificationListenerService, this helper manages
 * notification listener access instead — no dangerous permissions required.
 */
object SMSPermissionHelper {

    // Kept for source compatibility at any remaining call sites
    const val SMS_PERMISSION_CODE = 101

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        )
        return flat?.contains(context.packageName) == true
    }

    fun openNotificationListenerSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    /**
     * Checks notification listener access and opens settings if not granted.
     * Returns true if access is already enabled, false if settings were opened.
     */
    fun checkAndRequestPermissions(context: Context): Boolean {
        return if (isNotificationListenerEnabled(context)) {
            true
        } else {
            openNotificationListenerSettings(context)
            false
        }
    }
}
