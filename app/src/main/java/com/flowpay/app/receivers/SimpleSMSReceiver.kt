package com.flowpay.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Retained as an empty stub after migration to FlowPayNotificationListener.
 * All SMS/transaction detection is now handled via NotificationListenerService.
 * This class is no longer registered anywhere and performs no function.
 */
@Deprecated("Replaced by FlowPayNotificationListener. Safe to delete.")
class SimpleSMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // No-op — SMS detection migrated to FlowPayNotificationListener
    }
}
