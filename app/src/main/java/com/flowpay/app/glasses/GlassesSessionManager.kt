package com.flowpay.app.glasses

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.flowpay.app.services.GlassesSessionService
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionError
import com.meta.wearable.dat.core.types.PermissionStatus
import com.meta.wearable.dat.core.types.RegistrationState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers


/**
 * Result of checking DAT camera permission before starting the stream.
 * Service uses this to gate stream start: only start when [Granted].
 */
sealed class CheckCameraResult {
    data object Granted : CheckCameraResult()
    data object Denied : CheckCameraResult()
    data class Error(val reason: String) : CheckCameraResult()
}

/**
 * User-facing messages for Meta DAT [PermissionError] so the app can show specific failure reasons.
 */
fun permissionErrorToUserMessage(permissionError: PermissionError): String = when (permissionError) {
    PermissionError.NO_DEVICE -> "No glasses discovered or registered."
    PermissionError.NO_DEVICE_WITH_CONNECTION -> "Glasses are powered off or disconnected."
    PermissionError.META_AI_NOT_INSTALLED -> "Meta AI app is not installed."
    PermissionError.CONNECTION_ERROR -> "Connection error with glasses."
    PermissionError.REQUEST_IN_PROGRESS -> "A permission request is already in progress."
    PermissionError.REQUEST_TIMEOUT -> "Permission request timed out."
    PermissionError.INTERNAL_ERROR -> "An unexpected error occurred. Try again."
}

/**
 * Best user-facing message for a permission check/request failure.
 * If [throwable] wraps a [PermissionError] (e.g. as cause), returns [permissionErrorToUserMessage]; else uses message or generic.
 */
fun userMessageForPermissionFailure(throwable: Throwable?): String {
    if (throwable == null) return "Could not check camera permission."
    val permissionError = throwable.cause as? PermissionError
    return permissionError?.let { permissionErrorToUserMessage(it) }
        ?: throwable.message
        ?: "Could not complete camera permission request."
}

/**
 * Singleton entry point for the glasses integration.
 * Handles DAT registration and service lifecycle. The service checks camera permission before starting the stream and only starts when Granted.
 * Use [hasCameraPermission] / [requestCameraPermission] to guide users to grant camera in Meta AI when stream fails.
 */
object GlassesSessionManager {

    private const val TAG = "GlassesSessionManager"

    val state: StateFlow<GlassesSessionState>
        get() = GlassesSessionService.state

    /**
     * Check if the app is registered with a Meta glasses device via DAT.
     */
    fun isRegistered(): Boolean {
        return try {
            val regState = runBlocking { Wearables.registrationState.first() }
            regState is RegistrationState.Registered
        } catch (e: Exception) {
            Log.w(TAG, "Could not check registration: ${e.message}")
            false
        }
    }

    /**
     * Launch the one-time DAT registration flow (deep-links to Meta AI app).
     */
    fun startRegistration(activity: Activity) {
        try {
            Wearables.startRegistration(activity)
            Log.d(TAG, "DAT registration flow launched")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch registration: ${e.message}", e)
        }
    }

    /**
     * Suspend check of DAT camera permission. Use this before starting the video stream.
     * Returns [CheckCameraResult.Granted] only when the user has granted camera in Meta AI; [Denied] or [Error] otherwise.
     */
    suspend fun checkCameraPermissionResult(): CheckCameraResult {
        return withContext(Dispatchers.Default) {
            try {
                val result = runBlocking { Wearables.checkPermissionStatus(Permission.CAMERA) }
                when (val status = result.getOrNull()) {
                    PermissionStatus.Granted -> CheckCameraResult.Granted
                    PermissionStatus.Denied -> CheckCameraResult.Denied
                    else -> CheckCameraResult.Error(
                        result.exceptionOrNull()?.let { userMessageForPermissionFailure(it) }
                            ?: "Could not check camera permission."
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not check DAT camera permission: ${e.message}")
                CheckCameraResult.Error(userMessageForPermissionFailure(e))
            }
        }
    }

    /**
     * Check if DAT camera permission is granted on the glasses.
     * Glasses camera only; not the phone camera. Meta AI may not show a dedicated permission screen — see [PermissionConstants.GLASSES_CAMERA_GRANT_STEPS].
     */
    fun hasCameraPermission(): Boolean {
        return try {
            val result = runBlocking { Wearables.checkPermissionStatus(Permission.CAMERA) }
            result.getOrNull() == PermissionStatus.Granted
        } catch (e: Exception) {
            Log.w(TAG, "Could not check DAT camera permission: ${e.message}")
            false
        }
    }
    /**
     * Open Meta AI app so the user can grant camera for FlowPay in Glasses → App permissions.
     */
    fun requestCameraPermission(activity: Activity) {
        openMetaAIApp(activity)
    }
    /**
     * Try to open the Meta AI app (Stella / Meta View / Ray-Ban).
     */
    fun openMetaAIApp(activity: Activity) {
        val packageNames = listOf("com.facebook.stella", "com.meta.view", "com.rayban.stories", "com.meta.rayban")
        for (pkg in packageNames) {
            try {
                val intent = activity.packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    activity.startActivity(intent)
                    Log.d(TAG, "Opened Meta AI app: $pkg")
                    return
                }
            } catch (_: Exception) { }
        }
        Log.w(TAG, "Meta AI app not found; user can open it manually to grant camera for FlowPay")
    }
    fun startSession(context: Context) {
        Log.d(TAG, "Starting glasses session")
        val intent = Intent(context, GlassesSessionService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopSession(context: Context) {
        Log.d(TAG, "Stopping glasses session")
        GlassesSessionService.instance?.stopSession()
            ?: context.stopService(Intent(context, GlassesSessionService::class.java))
    }

    /**
     * Manually trigger QR scan when wake word doesn't respond.
     */
    fun triggerScan() {
        GlassesSessionService.instance?.triggerScan()
    }
}
