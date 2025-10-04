package com.flowpay.app.helpers

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.flowpay.app.TestConfigurationActivity

/**
 * Helper class containing all business logic for SetupActivity
 * This separates the UI concerns from the business logic
 */
class SetupHelper(
    private val context: Context,
    private val uiCallback: UICallback
) {
    companion object {
        private const val TAG = "SetupHelper"
    }

    /**
     * Interface for UI callbacks
     */
    interface UICallback {
        fun showToast(message: String)
        fun navigateToTestConfiguration()
    }

    /**
     * Data class for validation results
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String
    )

    /**
     * Data class for setup data
     */
    data class SetupData(
        val mobileNumber: String,
        val selectedBank: String,
        val selectedPrimarySim: String,
        val isDualSimEnabled: Boolean,
        val selectedSecondarySim: String
    )

    /**
     * Get available banks
     */
    fun getBanks(): List<Pair<String, String>> {
        return listOf(
            "sbi" to "State Bank of India",
            "hdfc" to "HDFC Bank",
            "icici" to "ICICI Bank",
            "axis" to "Axis Bank",
            "kotak" to "Kotak Mahindra Bank",
            "pnb" to "Punjab National Bank",
            "bob" to "Bank of Baroda",
            "yes" to "Yes Bank",
            "idbi" to "IDBI Bank",
            "canara" to "Canara Bank"
        )
    }

    /**
     * Get available SIM carriers
     */
    fun getSimCarriers(): List<Pair<String, String>> {
        return listOf(
            "jio" to "Jio",
            "airtel" to "Airtel",
            "vodafone" to "Vodafone",
            "bsnl" to "BSNL"
        )
    }

    /**
     * Get secondary SIM options (excluding primary SIM)
     */
    fun getSecondarySimOptions(primarySim: String): List<Pair<String, String>> {
        val allCarriers = getSimCarriers()
        return if (primarySim.isNotEmpty()) {
            allCarriers.filter { it.first != primarySim }
        } else {
            allCarriers
        }
    }

    /**
     * Validate mobile number
     */
    fun validateMobileNumber(mobileNumber: String): ValidationResult {
        if (mobileNumber.isEmpty() || mobileNumber.length != 10) {
            return ValidationResult(false, "Please enter a valid 10-digit mobile number")
        }
        
        // Check if phone number starts with valid digits (6-9 for Indian mobile numbers)
        if (!mobileNumber.matches(Regex("^[6-9][0-9]{9}$"))) {
            return ValidationResult(false, "Please enter a valid Indian mobile number (starting with 6-9)")
        }
        
        return ValidationResult(true, "")
    }

    /**
     * Validate bank selection
     */
    fun validateBankSelection(selectedBank: String): ValidationResult {
        if (selectedBank.isEmpty()) {
            return ValidationResult(false, "Please select your bank")
        }
        return ValidationResult(true, "")
    }

    /**
     * Validate primary SIM selection
     */
    fun validatePrimarySimSelection(selectedPrimarySim: String): ValidationResult {
        if (selectedPrimarySim.isEmpty()) {
            return ValidationResult(false, "Please select your primary SIM")
        }
        return ValidationResult(true, "")
    }

    /**
     * Validate secondary SIM selection (only if dual SIM is enabled)
     */
    fun validateSecondarySimSelection(
        isDualSimEnabled: Boolean,
        selectedSecondarySim: String
    ): ValidationResult {
        if (isDualSimEnabled && selectedSecondarySim.isEmpty()) {
            return ValidationResult(false, "Please select your secondary SIM")
        }
        return ValidationResult(true, "")
    }

    /**
     * Validate complete form
     */
    fun validateForm(setupData: SetupData): ValidationResult {
        // Validate mobile number
        val mobileValidation = validateMobileNumber(setupData.mobileNumber)
        if (!mobileValidation.isValid) {
            return mobileValidation
        }

        // Validate bank selection
        val bankValidation = validateBankSelection(setupData.selectedBank)
        if (!bankValidation.isValid) {
            return bankValidation
        }

        // Validate primary SIM selection
        val primarySimValidation = validatePrimarySimSelection(setupData.selectedPrimarySim)
        if (!primarySimValidation.isValid) {
            return primarySimValidation
        }

        // Validate secondary SIM selection
        val secondarySimValidation = validateSecondarySimSelection(
            setupData.isDualSimEnabled,
            setupData.selectedSecondarySim
        )
        if (!secondarySimValidation.isValid) {
            return secondarySimValidation
        }

        return ValidationResult(true, "")
    }

    /**
     * Save setup data to SharedPreferences
     */
    fun saveSetupData(setupData: SetupData) {
        val sharedPreferences = context.getSharedPreferences("FlowPayPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putBoolean("setup_completed", true)
            .putString("mobile_number", setupData.mobileNumber)
            .putString("selected_bank", setupData.selectedBank)
            .putString("selected_primary_sim", setupData.selectedPrimarySim)
            .putBoolean("is_dual_sim_enabled", setupData.isDualSimEnabled)
            .putString("selected_secondary_sim", setupData.selectedSecondarySim)
            .apply()
    }

    /**
     * Complete setup process
     */
    fun completeSetup(setupData: SetupData) {
        val validationResult = validateForm(setupData)
        
        if (validationResult.isValid) {
            // Save setup data
            saveSetupData(setupData)
            
            // Show success message
            uiCallback.showToast("Setup completed successfully!")
            
            // Navigate to test configuration
            uiCallback.navigateToTestConfiguration()
        } else {
            // Show validation error
            uiCallback.showToast(validationResult.errorMessage)
        }
    }

    /**
     * Load existing setup data
     */
    fun loadSetupData(): SetupData? {
        val sharedPreferences = context.getSharedPreferences("FlowPayPrefs", Context.MODE_PRIVATE)
        
        val mobileNumber = sharedPreferences.getString("mobile_number", "")
        val selectedBank = sharedPreferences.getString("selected_bank", "")
        val selectedPrimarySim = sharedPreferences.getString("selected_primary_sim", "")
        val isDualSimEnabled = sharedPreferences.getBoolean("is_dual_sim_enabled", false)
        val selectedSecondarySim = sharedPreferences.getString("selected_secondary_sim", "")
        
        // Only return data if setup was completed
        val isSetupCompleted = sharedPreferences.getBoolean("setup_completed", false)
        if (!isSetupCompleted) {
            return null
        }
        
        return SetupData(
            mobileNumber = mobileNumber ?: "",
            selectedBank = selectedBank ?: "",
            selectedPrimarySim = selectedPrimarySim ?: "",
            isDualSimEnabled = isDualSimEnabled,
            selectedSecondarySim = selectedSecondarySim ?: ""
        )
    }

    /**
     * Check if setup is already completed
     */
    fun isSetupCompleted(): Boolean {
        val sharedPreferences = context.getSharedPreferences("FlowPayPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("setup_completed", false)
    }

    /**
     * Reset setup data
     */
    fun resetSetupData() {
        val sharedPreferences = context.getSharedPreferences("FlowPayPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putBoolean("setup_completed", false)
            .remove("mobile_number")
            .remove("selected_bank")
            .remove("selected_primary_sim")
            .remove("is_dual_sim_enabled")
            .remove("selected_secondary_sim")
            .apply()
    }

    /**
     * Format mobile number input (remove non-digits, handle leading zeros)
     */
    fun formatMobileNumberInput(input: String): String {
        // More strict validation - only allow digits, no leading zeros
        val filtered = input.filter { char -> char.isDigit() }
        val validated = if (filtered.length > 1 && filtered.startsWith("0")) {
            filtered.drop(1) // Remove leading zero
        } else {
            filtered
        }.take(10)
        return validated
    }

    /**
     * Check if dual SIM is valid (primary and secondary are different)
     */
    fun isDualSimValid(primarySim: String, secondarySim: String): Boolean {
        return primarySim.isNotEmpty() && secondarySim.isNotEmpty() && primarySim != secondarySim
    }

    /**
     * Get setup completion message
     */
    fun getSetupCompletionMessage(): String {
        return "Setup completed successfully!"
    }
}
