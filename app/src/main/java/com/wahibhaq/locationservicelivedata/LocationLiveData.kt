package com.wahibhaq.locationservicelivedata

import android.arch.lifecycle.LiveData
import android.content.*
import android.location.LocationManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task


class LocationLiveData(
    private val context: Context, locationRequest: LocationRequest
) : LiveData<CustomLocationResult>() {

    private val locationSetting =
        LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
    private val client: SettingsClient = LocationServices.getSettingsClient(context)


    override fun onInactive() {
        super.onInactive()
        context.unregisterReceiver(GpsSwitchStateReceiver)
    }


    override fun onActive() {
        super.onActive()
        context.registerReceiver(
            GpsSwitchStateReceiver, IntentFilter(
                LocationManager
                    .PROVIDERS_CHANGED_ACTION
            )
        )
        checkGpsForFirstFlow()
    }

    private fun checkGpsForFirstFlow() {
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(
            locationSetting.build()
        )

        task.addOnSuccessListener {
            postValue(CustomLocationResult.GpsIsEnabled("Gps Ss Enabled"))
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    postValue(CustomLocationResult.GpsIsDisabled("GPS Is Disabled"))

                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    /**
     * Following broadcast receiver is to listen the Location button toggle state in Android.
     */
    private val GpsSwitchStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            if (intent.action.matches("android.location.PROVIDERS_CHANGED".toRegex())) {
                // Make an action or refresh an already managed state.
                if (AppUtil.isLocationEnabled(context)) {
                    postValue(CustomLocationResult.GpsIsEnabled("Gps Is Enabled"))
                } else {
                    postValue(CustomLocationResult.GpsIsDisabled("GPS Is Disabled"))
                }
            }
        }
    }

}

sealed class CustomLocationResult {

    data class GpsIsDisabled(val message: String) : CustomLocationResult()

    data class GpsIsEnabled(val message: String) : CustomLocationResult()

    data class PermissionMissing(val message: String) : CustomLocationResult()
}
