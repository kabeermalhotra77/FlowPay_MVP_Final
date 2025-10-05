# Camera Permission Persistence Fix

## Problem
The camera permission dialog was appearing every time the user opened the QR scanner, even after they had already granted the permission. This created a poor user experience where users had to dismiss the dialog repeatedly.

## Root Cause
The QRScannerActivity was always calling `showCameraPermissionRequestScreen()` in the `onCreate()` method, regardless of whether camera permission was already granted.

## Solution
Modified the QRScannerActivity to check for existing camera permission before showing the permission request dialog.

## Changes Made

### QRScannerActivity.kt
**Before:**
```kotlin
// Show permission request screen instead of immediately checking permissions
Log.d("QRScanner", "Showing camera permission request screen...")
showCameraPermissionRequestScreen()
```

**After:**
```kotlin
// Check camera permission first before showing request screen
Log.d("QRScanner", "Checking camera permission...")
if (permissionManager.isPermissionGranted(Manifest.permission.CAMERA)) {
    Log.d("QRScanner", "Camera permission already granted, starting camera")
    checkPermissionsAndStartCamera()
} else {
    Log.d("QRScanner", "Camera permission not granted, showing request screen...")
    showCameraPermissionRequestScreen()
}
```

## New Behavior

### First Time (Permission Not Granted)
1. User clicks "Scan QR Code"
2. QRScannerActivity opens
3. Camera permission dialog appears
4. User grants permission
5. Camera starts

### Subsequent Times (Permission Already Granted)
1. User clicks "Scan QR Code"
2. QRScannerActivity opens
3. **No dialog appears** - camera starts directly
4. User can scan QR codes immediately

## Benefits

1. **Better User Experience**: No repeated permission dialogs
2. **Faster Access**: QR scanner opens immediately after first permission grant
3. **Consistent Behavior**: Follows standard Android permission patterns
4. **Reduced Friction**: Users don't have to dismiss dialogs repeatedly

## Testing

Use the provided test script:
```bash
./test_camera_permission_persistence.sh
```

### Manual Test Steps:
1. Clear app data and start fresh
2. Complete setup (no camera permission requested)
3. Click "Scan QR Code" - dialog should appear
4. Grant camera permission
5. Close QR scanner, go back to main screen
6. Click "Scan QR Code" again - **NO dialog should appear**
7. Repeat step 6 multiple times - dialog should never appear again

## Files Modified

1. `app/src/main/java/com/flowpay/app/features/qr_scanner/presentation/QRScannerActivity.kt`

## Implementation Status

✅ **Completed**: Camera permission dialog now only appears once
✅ **Completed**: After permission is granted, QR scanner opens directly
✅ **Completed**: No repeated permission dialogs on subsequent opens
✅ **Completed**: Test script provided for verification

## Related Issues

This fix addresses the user's concern about the camera permission dialog appearing every time they open the QR scanner. The dialog will now only appear once, and after the user grants permission, the QR scanner will open directly without any dialogs.

