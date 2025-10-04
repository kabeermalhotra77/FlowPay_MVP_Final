package com.flowpay.app.managers

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.flowpay.app.R

/**
 * Manages transaction-related dialogs based on call outcomes
 */
class TransactionDialogManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TransactionDialogManager"
    }
    
    /**
     * Show dialog when user cancels the transaction by ending the call early
     */
    fun showTransactionCancelled() {
        Log.d(TAG, "Showing transaction cancelled dialog")
        
        try {
            AlertDialog.Builder(context)
                .setTitle("Transaction Cancelled")
                .setMessage("You cancelled the transaction by ending the call early. No payment was processed.")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    Log.d(TAG, "Transaction cancelled dialog dismissed")
                }
                .setCancelable(false)
                .create()
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show transaction cancelled dialog: ${e.message}")
            // Fallback to toast if dialog fails
            Toast.makeText(context, "Transaction cancelled", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Show dialog when user cancels the transaction using the terminate button
     */
    fun showTransactionCancelledByUser() {
        Log.d(TAG, "Showing transaction cancelled by user dialog")
        
        try {
            AlertDialog.Builder(context)
                .setTitle("Transaction Cancelled")
                .setMessage("The transaction was cancelled by the user.")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    Log.d(TAG, "Transaction cancelled by user dialog dismissed")
                }
                .setCancelable(false)
                .create()
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show transaction cancelled by user dialog: ${e.message}")
            // Fallback to toast if dialog fails
            Toast.makeText(context, "The transaction was cancelled by the user", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Show dialog when transaction completes successfully
     */
    fun showTransactionCompleted() {
        Log.d(TAG, "Showing transaction completed dialog")
        
        try {
            // Create custom dialog with FlowPay design
            val dialogBuilder = AlertDialog.Builder(context)
            val inflater = LayoutInflater.from(context)
            val dialogView = inflater.inflate(R.layout.dialog_transaction_success, null)
            
            // Setup dialog appearance
            val dialog = dialogBuilder
                .setView(dialogView)
                .setCancelable(false)
                .create()
            
            // Make dialog background transparent and rounded
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            // Setup success icon
            val successIcon = dialogView.findViewById<ImageView>(R.id.iv_success_icon)
            successIcon?.setImageResource(R.drawable.ic_success_check)
            successIcon?.setColorFilter(context.getColor(R.color.flowpay_green))
            
            // Setup title
            val titleText = dialogView.findViewById<TextView>(R.id.tv_success_title)
            titleText?.text = "Payment Successful"
            
            // Setup message
            val messageText = dialogView.findViewById<TextView>(R.id.tv_success_message)
            messageText?.text = "Your payment has been processed successfully! You will receive a confirmation SMS shortly."
            
            // Setup done button
            val doneButton = dialogView.findViewById<Button>(R.id.btn_done)
            doneButton?.setOnClickListener {
                dialog.dismiss()
                Log.d(TAG, "Transaction completed dialog dismissed")
            }
            
            dialog.show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show transaction completed dialog: ${e.message}")
            // Fallback to basic dialog
            try {
                AlertDialog.Builder(context)
                    .setTitle("Transaction Successful")
                    .setMessage("Your payment has been processed successfully! You will receive a confirmation SMS shortly.")
                    .setPositiveButton("Great!") { dialog, _ ->
                        dialog.dismiss()
                        Log.d(TAG, "Transaction completed dialog dismissed")
                    }
                    .setCancelable(false)
                    .create()
                    .show()
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to show fallback dialog: ${e2.message}")
                // Final fallback to toast
                Toast.makeText(context, "Transaction completed successfully!", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * Show dialog when transaction fails
     */
    fun showTransactionFailed() {
        Log.d(TAG, "Showing transaction failed dialog")
        
        try {
            AlertDialog.Builder(context)
                .setTitle("Transaction Failed")
                .setMessage("The call ended unexpectedly. This could be due to network issues or the recipient's phone being busy. Please try again.")
                .setPositiveButton("Retry") { dialog, _ ->
                    dialog.dismiss()
                    Log.d(TAG, "Transaction failed dialog dismissed - user chose to retry")
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    Log.d(TAG, "Transaction failed dialog dismissed - user chose to cancel")
                }
                .setCancelable(false)
                .create()
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show transaction failed dialog: ${e.message}")
            // Fallback to toast if dialog fails
            Toast.makeText(context, "Transaction failed. Please try again.", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Show dialog when system terminates the call
     */
    fun showSystemTerminated() {
        Log.d(TAG, "Showing system terminated dialog")
        
        try {
            AlertDialog.Builder(context)
                .setTitle("Call Terminated")
                .setMessage("The call was terminated by the system. This could be due to network issues or service unavailability. Please try again later.")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    Log.d(TAG, "System terminated dialog dismissed")
                }
                .setCancelable(false)
                .create()
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show system terminated dialog: ${e.message}")
            // Fallback to toast if dialog fails
            Toast.makeText(context, "Call terminated by system. Please try again later.", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Show a custom dialog with specific title and message
     */
    fun showCustomDialog(title: String, message: String, positiveButton: String = "OK") {
        Log.d(TAG, "Showing custom dialog: $title")
        
        try {
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButton) { dialog, _ ->
                    dialog.dismiss()
                    Log.d(TAG, "Custom dialog dismissed: $title")
                }
                .setCancelable(false)
                .create()
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show custom dialog: ${e.message}")
            // Fallback to toast if dialog fails
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
