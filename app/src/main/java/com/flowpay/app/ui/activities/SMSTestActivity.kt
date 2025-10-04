package com.flowpay.app.ui.activities

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.flowpay.app.R
import com.flowpay.app.helpers.DebugHelper

/**
 * Test activity for SMS functionality
 * This activity provides buttons to test different SMS scenarios
 */
class SMSTestActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_test)
        
        setupTestButtons()
    }
    
    private fun setupTestButtons() {
        // Test basic SMS
        findViewById<Button>(R.id.btnTestBasicSMS).setOnClickListener {
            DebugHelper.sendTestSMS(this)
            Toast.makeText(this, "Basic SMS test sent", Toast.LENGTH_SHORT).show()
        }
        
        // Test HDFC SMS
        findViewById<Button>(R.id.btnTestHDFCSMS).setOnClickListener {
            DebugHelper.sendTestSMSWithBankFormat(this, "HDFC")
            Toast.makeText(this, "HDFC SMS test sent", Toast.LENGTH_SHORT).show()
        }
        
        // Test SBI SMS
        findViewById<Button>(R.id.btnTestSBISMS).setOnClickListener {
            DebugHelper.sendTestSMSWithBankFormat(this, "SBI")
            Toast.makeText(this, "SBI SMS test sent", Toast.LENGTH_SHORT).show()
        }
        
        // Test ICICI SMS
        findViewById<Button>(R.id.btnTestICICISMS).setOnClickListener {
            DebugHelper.sendTestSMSWithBankFormat(this, "ICICI")
            Toast.makeText(this, "ICICI SMS test sent", Toast.LENGTH_SHORT).show()
        }
        
        // Test Paytm SMS
        findViewById<Button>(R.id.btnTestPaytmSMS).setOnClickListener {
            DebugHelper.sendTestSMSWithBankFormat(this, "PAYTM")
            Toast.makeText(this, "Paytm SMS test sent", Toast.LENGTH_SHORT).show()
        }
        
        // Test failed transaction
        findViewById<Button>(R.id.btnTestFailedSMS).setOnClickListener {
            DebugHelper.sendTestFailedSMS(this)
            Toast.makeText(this, "Failed SMS test sent", Toast.LENGTH_SHORT).show()
        }
        
        // Test pending transaction
        findViewById<Button>(R.id.btnTestPendingSMS).setOnClickListener {
            DebugHelper.sendTestPendingSMS(this)
            Toast.makeText(this, "Pending SMS test sent", Toast.LENGTH_SHORT).show()
        }
        
        // Test all formats
        findViewById<Button>(R.id.btnTestAllSMS).setOnClickListener {
            DebugHelper.testAllSMSFormats(this)
            Toast.makeText(this, "All SMS formats test started", Toast.LENGTH_SHORT).show()
        }
    }
}


