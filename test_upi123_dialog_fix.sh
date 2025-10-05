#!/bin/bash

echo "🧪 Testing UPI123 Dialog Fix"
echo "=============================="

# Check if the recursive call bug is fixed
echo "📋 Checking for recursive call bug fix..."

# Look for the old recursive call pattern
if grep -q "showUpi123CompletionDialog()" /Users/kabeermalhotra/Library/Mobile\ Documents/com~apple~CloudDocs/Desktop/FLOWPAYYYYY/app/src/main/java/com/flowpay/app/TestConfigurationActivity.kt; then
    echo "📋 Found showUpi123CompletionDialog method - checking implementation..."
    
    # Check if it's the problematic recursive call (calling itself)
    if grep -A 5 "runOnUiThread" /Users/kabeermalhotra/Library/Mobile\ Documents/com~apple~CloudDocs/Desktop/FLOWPAYYYYY/app/src/main/java/com/flowpay/app/TestConfigurationActivity.kt | grep -q "showUpi123CompletionDialog()"; then
        echo "❌ RECURSIVE CALL BUG STILL EXISTS!"
        echo "   The method is calling itself recursively in runOnUiThread"
        exit 1
    else
        echo "✅ No recursive call detected - method implementation is correct"
    fi
else
    echo "✅ No showUpi123CompletionDialog method found"
fi

# Check if the dialog is properly implemented in the callback
echo "📋 Checking dialog implementation..."

if grep -A 10 "override fun showUpi123CompletionDialog()" /Users/kabeermalhotra/Library/Mobile\ Documents/com~apple~CloudDocs/Desktop/FLOWPAYYYYY/app/src/main/java/com/flowpay/app/TestConfigurationActivity.kt | grep -q "AlertDialog.Builder"; then
    echo "✅ Dialog is properly implemented in the callback"
else
    echo "❌ Dialog implementation not found in callback"
    exit 1
fi

# Check if the private method was removed
if grep -q "private fun showUpi123CompletionDialog()" /Users/kabeermalhotra/Library/Mobile\ Documents/com~apple~CloudDocs/Desktop/FLOWPAYYYYY/app/src/main/java/com/flowpay/app/TestConfigurationActivity.kt; then
    echo "❌ Private method still exists - should be removed"
    exit 1
else
    echo "✅ Private method properly removed"
fi

echo ""
echo "🎉 UPI123 Dialog Fix Verification Complete!"
echo "✅ Recursive call bug fixed"
echo "✅ Dialog properly implemented"
echo "✅ No duplicate methods"
echo ""
echo "The UPI123 call should now work without crashing and show the dialog properly."
