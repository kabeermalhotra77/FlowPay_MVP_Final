package com.flowpay.app.managers

import android.content.Context
import android.content.SharedPreferences
import com.flowpay.app.constants.AppConstants

/**
 * Manages form state persistence for the PayContactDialog
 * Ensures form data is preserved during permission flows and app lifecycle events
 */
class FormStateManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        AppConstants.PREFS_NAME, 
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_PHONE_NUMBER = "form_phone_number"
        private const val KEY_AMOUNT = "form_amount"
        private const val KEY_CONTACT_NAME = "form_contact_name"
        private const val KEY_IS_FORM_ACTIVE = "form_is_active"
        private const val KEY_FORM_TIMESTAMP = "form_timestamp"
        
        // Form state expires after 10 minutes
        private const val FORM_STATE_TIMEOUT = 10 * 60 * 1000L
    }
    
    /**
     * Form data class to hold all form state
     */
    data class FormData(
        val phoneNumber: String = "",
        val amount: String = "",
        val contactName: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Save form state
     */
    fun saveFormState(formData: FormData) {
        prefs.edit().apply {
            putString(KEY_PHONE_NUMBER, formData.phoneNumber)
            putString(KEY_AMOUNT, formData.amount)
            putString(KEY_CONTACT_NAME, formData.contactName)
            putBoolean(KEY_IS_FORM_ACTIVE, true)
            putLong(KEY_FORM_TIMESTAMP, formData.timestamp)
            apply()
        }
    }
    
    /**
     * Load form state if it exists and is not expired
     */
    fun loadFormState(): FormData? {
        val isActive = prefs.getBoolean(KEY_IS_FORM_ACTIVE, false)
        val timestamp = prefs.getLong(KEY_FORM_TIMESTAMP, 0L)
        
        // Check if form state is expired
        if (isActive && (System.currentTimeMillis() - timestamp) > FORM_STATE_TIMEOUT) {
            clearFormState()
            return null
        }
        
        return if (isActive) {
            FormData(
                phoneNumber = prefs.getString(KEY_PHONE_NUMBER, "") ?: "",
                amount = prefs.getString(KEY_AMOUNT, "") ?: "",
                contactName = prefs.getString(KEY_CONTACT_NAME, null),
                timestamp = timestamp
            )
        } else {
            null
        }
    }
    
    /**
     * Clear form state
     */
    fun clearFormState() {
        prefs.edit().apply {
            remove(KEY_PHONE_NUMBER)
            remove(KEY_AMOUNT)
            remove(KEY_CONTACT_NAME)
            putBoolean(KEY_IS_FORM_ACTIVE, false)
            remove(KEY_FORM_TIMESTAMP)
            apply()
        }
    }
    
    /**
     * Check if form state exists and is valid
     */
    fun hasValidFormState(): Boolean {
        val isActive = prefs.getBoolean(KEY_IS_FORM_ACTIVE, false)
        val timestamp = prefs.getLong(KEY_FORM_TIMESTAMP, 0L)
        
        return isActive && (System.currentTimeMillis() - timestamp) <= FORM_STATE_TIMEOUT
    }
    
    /**
     * Mark form as submitted (clear state)
     */
    fun markFormSubmitted() {
        clearFormState()
    }
    
    /**
     * Update specific form field
     */
    fun updatePhoneNumber(phoneNumber: String) {
        val currentState = loadFormState() ?: FormData()
        saveFormState(currentState.copy(phoneNumber = phoneNumber))
    }
    
    fun updateAmount(amount: String) {
        val currentState = loadFormState() ?: FormData()
        saveFormState(currentState.copy(amount = amount))
    }
    
    fun updateContactName(contactName: String?) {
        val currentState = loadFormState() ?: FormData()
        saveFormState(currentState.copy(contactName = contactName))
    }
}
