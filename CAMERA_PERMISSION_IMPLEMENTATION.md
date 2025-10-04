# Camera Permission Implementation

## Overview
This implementation ensures that camera permission is only requested when the user actually tries to scan a QR code, not during the setup screen or app initialization.

## Changes Made

### 1. QRScannerActivity.kt
- **Modified onCreate()**: Instead of immediately checking permissions, now shows a camera permission request screen
- **Added showCameraPermissionRequestScreen()**: Displays a user-friendly screen explaining why camera permission is needed
- **Added showCameraPermissionExplanationDialog()**: Shows a dialog explaining the camera permission requirement
- **Added requestCameraPermission()**: Specifically requests only camera permission
- **Updated handlePermissionResult()**: Handles camera permission result and then checks other permissions

### 2. MainActivityHelper.kt
- **Updated startQRScanning()**: Now checks camera permission first before opening QR scanner
- **Logic Flow**: 
  1. Check if camera permission is granted
  2. If not granted, open QR scanner to request permission
  3. If granted, check other permissions
  4. If all permissions granted, open QR scanner

### 3. PermissionConstants.kt
- **Removed CAMERA from REQUIRED_PERMISSIONS**: Camera permission is no longer requested during app initialization
- **Kept CAMERA in OPTIONAL_PERMISSIONS**: Camera permission is still available but only requested when needed

## Permission Flow

### Before (Old Flow)
1. App starts → Setup screen
2. Setup screen requests ALL permissions including camera
3. User grants camera permission during setup
4. User clicks "Scan QR Code" → Camera starts immediately

### After (New Flow)
1. App starts → Setup screen
2. Setup screen requests only essential permissions (no camera)
3. User completes setup
4. User clicks "Scan QR Code" → Camera permission request appears
5. User grants camera permission → Camera starts

## Benefits

1. **Better User Experience**: Users understand why camera permission is needed
2. **Privacy Friendly**: Camera permission only requested when actually needed
3. **Setup Simplification**: Setup process is faster without unnecessary permissions
4. **Clear Intent**: Permission request is contextual to the action being performed

## Testing

Use the provided test script:
```bash
./test_camera_permission_flow.sh
```

This script will:
1. Build and install the app
2. Clear app data to start fresh
3. Check that no camera permission is requested during setup
4. Provide manual test steps for the complete flow

## Files Modified

1. `app/src/main/java/com/flowpay/app/features/qr_scanner/presentation/QRScannerActivity.kt`
2. `app/src/main/java/com/flowpay/app/helpers/MainActivityHelper.kt`
3. `app/src/main/java/com/flowpay/app/constants/PermissionConstants.kt`

## Implementation Status

✅ **Completed**: Camera permission is now only requested when user tries to scan QR codes
✅ **Completed**: Setup screen no longer requests camera permission
✅ **Completed**: Permission flow is contextual and user-friendly
✅ **Completed**: Test script provided for verification
