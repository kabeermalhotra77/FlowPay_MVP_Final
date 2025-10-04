// Test file to verify name extraction patterns
// This is a temporary test file to verify the regex patterns work correctly

fun main() {
    val testSMSMessages = listOf(
        "Debited Rs.100.00 to JOHN DOE via UPI. Ref: 1234567890",
        "Amount Rs.500.00 paid to ABC STORE via UPI",
        "UPI payment to MERCHANT NAME via UPI",
        "Money transferred to RECIPIENT NAME via UPI",
        "Transferred to SENDER NAME via UPI",
        "Payment to TEST MERCHANT of Rs.250.00",
        "sent to JOHN DOE via UPI",
        "paid to ABC STORE via UPI",
        "to MERCHANT NAME via UPI",
        "to 9876543210 via UPI", // This should extract phone number
        "received from SENDER NAME via UPI",
        "credited from ABC COMPANY via UPI"
    )
    
    val recipientPatterns = listOf(
        "(?:sent|paid|transferred)\\s+to\\s+([A-Z][A-Z\\s\\.]+?)(?:\\s+(?:via|@|on|for|UPI|Ref)|\\.|,|;|$)",
        "to\\s+([A-Z][A-Z\\s\\.]+?)\\s+(?:via|@)",
        "to\\s+(?:merchant|M/s\\.?|Mr\\.?|Mrs\\.?|Ms\\.?)\\s*([A-Z][A-Z\\s\\.]+?)(?:\\s+(?:via|@|on|for)|\\.|,|;|$)",
        "to\\s+([a-zA-Z][a-zA-Z0-9\\s]+?)@",
        "([A-Z][A-Z\\s\\.]+?)\\s*[-–]\\s*(?:Rs|INR|₹)",
        "to\\s+(\\d{10})(?:\\s|\\.|,|;|$)",
        "Payment\\s+to\\s+([A-Z][A-Z\\s\\.]+?)\\s+(?:of|for)\\s+(?:Rs|INR|₹)",
        "Debited\\s+Rs\\.?\\s*[0-9,]+\\s+to\\s+([A-Z][A-Z\\s\\.]+?)(?:\\s+(?:via|@|on|for)|\\.|,|;|$)",
        "Amount\\s+Rs\\.?\\s*[0-9,]+\\s+paid\\s+to\\s+([A-Z][A-Z\\s\\.]+?)(?:\\s+(?:via|@|on|for)|\\.|,|;|$)",
        "UPI\\s+payment\\s+to\\s+([A-Z][A-Z\\s\\.]+?)(?:\\s+(?:via|@|on|for)|\\.|,|;|$)",
        "Money\\s+transferred\\s+to\\s+([A-Z][A-Z\\s\\.]+?)(?:\\s+(?:via|@|on|for)|\\.|,|;|$)",
        "Transferred\\s+to\\s+([A-Z][A-Z\\s\\.]+?)(?:\\s+(?:via|@|on|for)|\\.|,|;|$)"
    )
    
    println("Testing Name Extraction Patterns")
    println("================================")
    
    testSMSMessages.forEach { sms ->
        println("\nSMS: $sms")
        var found = false
        
        recipientPatterns.forEachIndexed { index, pattern ->
            try {
                val regex = Regex(pattern, setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                val match = regex.find(sms)
                
                if (match != null && match.groups.size > 1) {
                    val extracted = match.groups[1]?.value?.trim()
                    if (!extracted.isNullOrEmpty()) {
                        println("  Pattern ${index + 1}: '$extracted'")
                        found = true
                    }
                }
            } catch (e: Exception) {
                println("  Pattern ${index + 1}: Error - ${e.message}")
            }
        }
        
        if (!found) {
            println("  No match found")
        }
    }
}


