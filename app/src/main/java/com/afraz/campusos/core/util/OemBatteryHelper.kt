package com.afraz.campusos.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object OemBatteryHelper {
    private val supportedManufacturers = setOf("xiaomi", "oppo", "vivo", "realme", "oneplus", "iqoo", "poco")

    fun shouldShow(context: Context): Boolean {
        if (Build.MANUFACTURER.lowercase() !in supportedManufacturers) return false
        val manager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return !manager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIntent(context: Context): Intent = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}")
    )
}
