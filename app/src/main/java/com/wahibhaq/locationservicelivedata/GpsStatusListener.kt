package com.wahibhaq.locationservicelivedata

import android.arch.lifecycle.LiveData
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.provider.Settings

/**
 * Listens to Gps (location service) which is highly important for tracking to work and then
 * responds with appropriate state specified in {@link GpsStatus}
 */
class GpsStatusListener(private val context: Context) : LiveData<GpsStatus>() {

    init {
        checkGpsAndReact() //Need to explicitly call for the first time check
    }

    private var gpsSwitchStateReceiver: BroadcastReceiver? = null

    override fun onInactive() {
        super.onInactive()
        unregisterReceiver()
    }

    private fun unregisterReceiver() {
        gpsSwitchStateReceiver?.let {
            context.unregisterReceiver(gpsSwitchStateReceiver)
        }
    }

    override fun onActive() {
        super.onActive()
        registerReceiver()
    }

    private fun checkGpsAndReact() = if (isLocationEnabled()) {
        postValue(GpsStatus.GpsIsEnabled("GPS is Enabled"))
    } else {
        postValue(GpsStatus.GpsIsDisabled("GPS is Disabled"))
    }

    private fun isLocationEnabled(): Boolean {
        val locationMode: Int
        try {
            locationMode = Settings.Secure.getInt(context.contentResolver,
                    Settings.Secure.LOCATION_MODE)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
            return false
        }
        return locationMode != Settings.Secure.LOCATION_MODE_OFF
    }

    /**
     * Following broadcast receiver is to listen the Location button toggle state in Android.
     */
    private fun registerReceiver() {
        if (gpsSwitchStateReceiver == null) {
            gpsSwitchStateReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action.matches("android.location.PROVIDERS_CHANGED".toRegex())) {
                        checkGpsAndReact()
                    }
                }
            }
        }
        context.registerReceiver(gpsSwitchStateReceiver,
                IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
    }

}

sealed class GpsStatus {
    data class GpsIsDisabled(val message: String) : GpsStatus()
    data class GpsIsEnabled(val message: String) : GpsStatus()
}
