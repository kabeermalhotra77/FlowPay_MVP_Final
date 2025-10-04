# FlowPay Overlay Implementation

## Overview
This implementation provides a beautiful frosted glass overlay system for FlowPay that appears during UPI calls to protect users from fraud. The overlay features a modern design with smooth animations and audio muting capabilities.

## Features

### 🎨 Frosted Glass Design
- **95% white opacity** with blur effect for readability
- **Centered positioning** that adapts to any background
- **Smooth animations** with bounce entrance and fade exit
- **Modern card design** with rounded corners and subtle shadows

### 🔊 Audio Muting
- **Complete audio muting** during UPI calls for security
- **No extra permissions** required for audio muting
- **Automatic restoration** of audio after call ends
- **Microphone muting** to prevent accidental audio capture

### 📱 User Experience
- **Large, bold fonts** (20-30% bigger than standard)
- **Progress animation** with percentage display (35 seconds)
- **Step-by-step status** updates during transfer
- **Transaction details** display with masked phone numbers
- **Security indicators** and muted audio warnings

## File Structure

```
app/src/main/
├── java/com/example/upifraudprevention/
│   ├── CallOverlayService.kt          # Main overlay service
│   ├── FlowPayApp.kt                  # Composable UI
│   └── TestOverlayActivity.kt         # Test activity
├── res/layout/
│   └── call_overlay_flowpay.xml       # Overlay layout
└── res/drawable/
    ├── frosted_glass_background.xml   # Main card background
    ├── white_progress_track.xml       # Progress bar track
    ├── dialer_style_progress.xml      # Progress bar style
    ├── white_card_bg.xml              # Details card background
    ├── muted_pill_bg.xml              # Audio muted indicator
    ├── success_badge_bg.xml           # Security badge
    ├── ic_check_circle.xml            # Check icon
    └── ic_volume_off.xml              # Volume off icon
```

## Usage

### Basic Implementation
```kotlin
// Show overlay
CallOverlayService.showOverlay(context, "9876543210", "100")

// Hide overlay
CallOverlayService.hideOverlay(context)
```

### Integration with MainActivity
The overlay integrates seamlessly with the existing MainActivity and automatically:
1. **Detects UPI calls** using PhoneStateListener
2. **Mutes audio** when call connects
3. **Shows overlay** with transaction details
4. **Restores audio** when call ends
5. **Shows PIN alert** after call completion

### Test the Overlay
Use the `TestOverlayActivity` to test the overlay functionality:
1. Grant overlay permission when prompted
2. Enter phone number and amount
3. Tap "Test Overlay" to see the frosted glass design
4. Overlay auto-hides after 10 seconds

## Design Details

### Frosted Glass Effect
- **Base layer**: 95% white (`#F2FFFFFF`)
- **Gradient overlay**: Subtle depth effect
- **Border**: 1dp white with 33% opacity
- **Corner radius**: 28dp for modern look

### Typography
- **FlowPay logo**: 24sp, bold
- **Status text**: 28sp, bold (main status)
- **Step text**: 16sp, medium (current step)
- **Amount**: 24sp, bold (transaction amount)
- **Progress**: 12sp, bold (percentage)

### Colors
- **Primary text**: `#1A1A1A` (dark gray)
- **Secondary text**: `#424242` (medium gray)
- **Muted text**: `#757575` (light gray)
- **Success green**: `#4CAF50`
- **Error red**: `#FF6B6B`

## Permissions Required

```xml
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

## Service Declaration

```xml
<service
    android:name="com.example.upifraudprevention.CallOverlayService"
    android:enabled="true"
    android:exported="false" />
```

## Animation Timeline

1. **0-5 seconds**: Connecting to bank
2. **5-10 seconds**: Verifying account details
3. **10-15 seconds**: Processing transfer request
4. **15-20 seconds**: Authenticating transaction
5. **20-25 seconds**: Confirming with server
6. **25-30 seconds**: Finalizing payment
7. **30-35 seconds**: Transfer complete

## Security Features

- **Audio muting** prevents eavesdropping
- **Microphone muting** prevents accidental recording
- **Phone number masking** shows only last 4 digits
- **Secure transfer badge** indicates encrypted connection
- **PIN entry alert** after call completion

## Troubleshooting

### Overlay Not Showing
1. Check `SYSTEM_ALERT_WINDOW` permission
2. Verify service is properly declared in manifest
3. Check logs for overlay service errors

### Audio Not Muting
1. Ensure `MODIFY_AUDIO_SETTINGS` permission
2. Check audio manager initialization
3. Verify call state detection

### Layout Issues
1. Check drawable resources are properly created
2. Verify layout constraints
3. Test on different screen sizes

## Future Enhancements

- **Custom animations** for different transfer states
- **Sound effects** for status changes
- **Haptic feedback** for important events
- **Accessibility improvements** for screen readers
- **Dark mode support** with theme switching

## Performance Notes

- **Lightweight service** with minimal memory footprint
- **Efficient animations** using ValueAnimator
- **Proper cleanup** on service destruction
- **Memory leak prevention** with proper view management

This implementation provides a production-ready overlay system that enhances security while maintaining an excellent user experience.
