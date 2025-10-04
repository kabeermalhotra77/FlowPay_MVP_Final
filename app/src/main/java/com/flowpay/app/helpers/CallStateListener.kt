package com.flowpay.app.helpers

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log

class CallStateListener(private val context: Context) : PhoneStateListener() {
    
    companion object {
        private const val TAG = "CallStateListener"
    }
    
    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
        super.onCallStateChanged(state, phoneNumber)
        
        when (state) {
            TelephonyManager.CALL_STATE_IDLE -> {
                // Call ended or no call
                Log.d(TAG, "Call state: IDLE - Call ended")
                
                // Restore audio if it was muted
                if (AudioStateManager.isCallAudioMuted()) {
                    Log.d(TAG, "Call ended, restoring audio...")
                    AudioStateManager.restoreCallAudio(context)
                }
            }
            
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                // Call active
                Log.d(TAG, "Call state: OFFHOOK - Call active")
            }
            
            TelephonyManager.CALL_STATE_RINGING -> {
                // Phone ringing
                Log.d(TAG, "Call state: RINGING")
            }
        }
    }
}


