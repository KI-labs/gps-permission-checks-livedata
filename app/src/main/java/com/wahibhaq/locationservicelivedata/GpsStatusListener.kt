package com.wahibhaq.locationservicelivedata

import android.arch.lifecycle.LiveData
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.provider.Settings
import android.provider.Settings.Secure.*
import timber.log.Timber

/**
 * Listens to Gps (location service) which is highly important for tracking to work and then
 * responds with appropriate state specified in {@link GpsStatus}
 */
class GpsStatusListener(private val context: Context) : LiveData<GpsStatus>() {
    private val locationProviderRegex: Regex

    init {
        checkGpsAndReact() //Need to explicitly call for the first time check
        locationProviderRegex = "android.location.PROVIDERS_CHANGED".toRegex()
    }

    private val gpsSwitchStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) =
                intent.action.matches(locationProviderRegex).let { checkGpsAndReact() }
    }

    override fun onInactive() = unregisterReceiver()

    override fun onActive() {
        registerReceiver()
    }

    private fun checkGpsAndReact() = if (isLocationEnabled()) {
        postValue(GpsStatus.GpsIsEnabled("GPS is Enabled"))
    } else {
        postValue(GpsStatus.GpsIsDisabled("GPS is Disabled"))
    }

    private fun isLocationEnabled() = try {
        getInt(context.contentResolver, LOCATION_MODE) != LOCATION_MODE_OFF
    } catch (e: Settings.SettingNotFoundException) {
        Timber.e(e)
        false
    }

    /**
     * Broadcast receiver to listen the Location button toggle state in Android.
     */
    private fun registerReceiver() = context.registerReceiver(gpsSwitchStateReceiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))

    private fun unregisterReceiver() = gpsSwitchStateReceiver.let {
        context.unregisterReceiver(gpsSwitchStateReceiver)
    }
}

sealed class GpsStatus {
    data class GpsIsDisabled(val message: String) : GpsStatus()
    data class GpsIsEnabled(val message: String) : GpsStatus()
}
