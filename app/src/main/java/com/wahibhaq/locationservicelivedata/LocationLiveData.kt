package com.wahibhaq.locationservicelivedata

import android.arch.lifecycle.LiveData
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest


class LocationLiveData(
    private val context: Context, private val fusedLocationClient:
    FusedLocationProviderClient, private val locationRequest: LocationRequest
) : LiveData<LocationResult>() {

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
            postValue(LocationResult.LocationData(locationResult.locations))
        }
    }

    override fun onInactive() {
        super.onInactive()
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (unlikely: SecurityException) {
            postValue(
                LocationResult.PermissionLost(
                    error = "Lost location permission. " +
                            "Could not remove updates. $unlikely"
                )
            )
        }
    }

    override fun onActive() {
        super.onActive()
        if (!AppUtil.isLocationEnabled(context)) {
            postValue(LocationResult.GpsNotEnabled(error = "GPS Not Enabled"))
        } else {
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback,
                    Looper.myLooper()
                )
            } catch (unlikely: SecurityException) {
                postValue(
                    LocationResult.PermissionLost(
                        error = "Lost location permission. " +
                                "Could not request updates. $unlikely"
                    )
                )
            }
        }
    }
}

sealed class LocationResult {

    data class GpsNotEnabled(val error: String) : LocationResult()

    data class PermissionMissing(val error: String) : LocationResult()

    data class PermissionLost(val error: String) : LocationResult()

    data class LocationData(val values: List<Location>) : LocationResult()
}
