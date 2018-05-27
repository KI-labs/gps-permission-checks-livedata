package com.wahibhaq.locationservicelivedata

import android.content.Context
import android.provider.Settings


object AppUtil {
    fun isLocationEnabled(context: Context): Boolean {
        val locationMode: Int
        try {
            locationMode = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCATION_MODE
            )
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
            return false
        }
        return locationMode != Settings.Secure.LOCATION_MODE_OFF
    }
}