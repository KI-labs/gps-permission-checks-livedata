package com.wahibhaq.locationservicelivedata

import android.arch.lifecycle.LiveData
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager


class GpsStatusListener(
    private val context: Context
) : LiveData<CustomLocationResult>() {

    override fun onInactive() {
        super.onInactive()
        context.unregisterReceiver(gpsSwitchStateReceiver)
    }


    override fun onActive() {
        super.onActive()
        context.registerReceiver(
            gpsSwitchStateReceiver, IntentFilter(
                LocationManager
                    .PROVIDERS_CHANGED_ACTION
            )
        )
        checkGpsAndReact() //Need to explicitly call for the first time check
    }

    private fun checkGpsAndReact() {
        if (AppUtil.isLocationEnabled(context)) {
            postValue(CustomLocationResult.GpsIsEnabled("Gps Is Enabled"))
        } else {
            postValue(CustomLocationResult.GpsIsDisabled("GPS Is Disabled"))
        }
    }

    /**
     * Following broadcast receiver is to listen the Location button toggle state in Android.
     */
    private val gpsSwitchStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action.matches("android.location.PROVIDERS_CHANGED".toRegex())) {
                checkGpsAndReact()
            }
        }
    }

}

sealed class CustomLocationResult {
    data class GpsIsDisabled(val message: String) : CustomLocationResult()

    data class GpsIsEnabled(val message: String) : CustomLocationResult()
}
