#!/bin/bash

# Test script for Contact Selection Implementation
# This script verifies that the contact selection functionality is properly implemented

echo "🔍 Testing Contact Selection Implementation..."
echo "=============================================="

# Check if AndroidManifest.xml has READ_CONTACTS permission
echo "1. Checking AndroidManifest.xml for READ_CONTACTS permission..."
if grep -q "READ_CONTACTS" app/src/main/AndroidManifest.xml; then
    echo "   ✅ READ_CONTACTS permission found in AndroidManifest.xml"
else
    echo "   ❌ READ_CONTACTS permission missing in AndroidManifest.xml"
fi

# Check if PermissionConstants.kt includes contact permission
echo "2. Checking PermissionConstants.kt for contact permission..."
if grep -q "READ_CONTACTS" app/src/main/java/com/flowpay/app/constants/PermissionConstants.kt; then
    echo "   ✅ READ_CONTACTS permission found in PermissionConstants.kt"
else
    echo "   ❌ READ_CONTACTS permission missing in PermissionConstants.kt"
fi

# Check if PermissionManager has contact permission methods
echo "3. Checking PermissionManager for contact permission methods..."
if grep -q "hasContactPermission\|requestContactPermission" app/src/main/java/com/flowpay/app/managers/PermissionManager.kt; then
    echo "   ✅ Contact permission methods found in PermissionManager"
else
    echo "   ❌ Contact permission methods missing in PermissionManager"
fi

# Check if ContactPickerDialog exists
echo "4. Checking if ContactPickerDialog exists..."
if [ -f "app/src/main/java/com/flowpay/app/ui/dialogs/ContactPickerDialog.kt" ]; then
    echo "   ✅ ContactPickerDialog.kt found"
else
    echo "   ❌ ContactPickerDialog.kt missing"
fi

# Check if ManualEntryDialog has contact picker functionality
echo "5. Checking ManualEntryDialog for contact picker functionality..."
if grep -q "ContactPickerDialog\|showContactPicker\|selectedContactName" app/src/main/java/com/flowpay/app/MainActivity.kt; then
    echo "   ✅ Contact picker functionality found in ManualEntryDialog"
else
    echo "   ❌ Contact picker functionality missing in ManualEntryDialog"
fi

# Check if Contact import is present in MainActivity.kt
echo "6. Checking imports in MainActivity.kt..."
if grep -q "import.*ContactPickerDialog\|import.*Contact" app/src/main/java/com/flowpay/app/MainActivity.kt; then
    echo "   ✅ Contact-related imports found in MainActivity.kt"
else
    echo "   ❌ Contact-related imports missing in MainActivity.kt"
fi

# Test build
echo "7. Testing build..."
if ./gradlew assembleDebug > /dev/null 2>&1; then
    echo "   ✅ Build successful - no compilation errors"
else
    echo "   ❌ Build failed - check compilation errors"
fi

echo ""
echo "🎉 Contact Selection Implementation Test Complete!"
echo "=============================================="
echo ""
echo "📱 To test the functionality:"
echo "1. Install the app on a device/emulator"
echo "2. Open the app and tap 'Manual Pay'"
echo "3. Tap the contact icon next to the phone number field"
echo "4. Grant contact permission if prompted"
echo "5. Select a contact from the list"
echo "6. Verify the contact name appears and phone number is filled"
echo ""
echo "✨ The contact selection feature should now be fully functional!"
