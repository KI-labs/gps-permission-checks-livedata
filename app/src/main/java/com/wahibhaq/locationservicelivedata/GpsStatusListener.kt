package com.wahibhaq.locationservicelivedata

import android.app.Application
import android.arch.lifecycle.LiveData
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager

/**
 * Listens to Gps (location service) which is highly important for tracking to work and then
 * responds with appropriate state specified in {@link GpsStatus}
 */
//TODO see if can be converted to object
class GpsStatusListener(
    private val application: Application
) : LiveData<GpsStatus>() {

    private var gpsSwitchStateReceiver: BroadcastReceiver? = null

    override fun onInactive() {
        super.onInactive()
        unregisterReceiver()
    }

    private fun unregisterReceiver() {
        gpsSwitchStateReceiver?.let {
            application.unregisterReceiver(gpsSwitchStateReceiver)
        }
    }

    override fun onActive() {
        super.onActive()
        registerReceiver()
        checkGpsAndReact() //Need to explicitly call for the first time check
    }

    private fun checkGpsAndReact() {
        if (AppUtil.isLocationEnabled(application)) {
            postValue(GpsStatus.GpsIsEnabled("Gps Is Enabled"))
        } else {
            postValue(GpsStatus.GpsIsDisabled("GPS Is Disabled"))
        }
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
        application.registerReceiver(
            gpsSwitchStateReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        )
    }

}

sealed class GpsStatus {
    data class GpsIsDisabled(val message: String) : GpsStatus()
    data class GpsIsEnabled(val message: String) : GpsStatus()
}
