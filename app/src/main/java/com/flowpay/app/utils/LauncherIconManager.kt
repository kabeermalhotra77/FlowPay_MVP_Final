package com.flowpay.app.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object LauncherIconManager {
    private const val THEME_BLUE = "blue"
    private const val THEME_RED = "red"

    private const val BLUE_ALIAS = "com.flowpay.app.MainActivityBlueLauncher"
    private const val RED_ALIAS = "com.flowpay.app.MainActivityRedLauncher"

    fun applyForAccentTheme(context: Context, accentTheme: String) {
        val pm = context.packageManager ?: return
        val appContext = context.applicationContext

        val enableRed = accentTheme == THEME_RED
        setEnabled(pm, appContext, BLUE_ALIAS, enabled = !enableRed)
        setEnabled(pm, appContext, RED_ALIAS, enabled = enableRed)
    }

    private fun setEnabled(
        pm: PackageManager,
        context: Context,
        componentClassName: String,
        enabled: Boolean
    ) {
        val component = ComponentName(context, componentClassName)
        val desiredState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        val currentState = pm.getComponentEnabledSetting(component)
        if (currentState == desiredState) return

        pm.setComponentEnabledSetting(
            component,
            desiredState,
            PackageManager.DONT_KILL_APP
        )
    }
}

