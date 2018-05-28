package com.wahibhaq.locationservicelivedata

import android.arch.lifecycle.LiveData
import android.content.Context
import android.content.IntentSender
import android.location.LocationManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class LocationLiveData(
    context: Context, private val fusedLocationClient:
    FusedLocationProviderClient, locationRequest: LocationRequest
) : LiveData<CustomLocationResult>() {



    private val locationSetting =
        LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
    private val client: SettingsClient = LocationServices.getSettingsClient(context)


//    private val gnssCallback = @RequiresApi(Build.VERSION_CODES.N)
//object : GnssStatus.Callback() {
//        override fun onSatelliteStatusChanged(status: GnssStatus?) {
//            super.onSatelliteStatusChanged(status)
//            Timber.i("GPS Status Changed")
//        }
//
//        override fun onStarted() {
//            super.onStarted()
//            Timber.i("GPS Status Started")
//            registerForLocationUpdates()
//        }
//
//        override fun onStopped() {
//            super.onStopped()
//            Timber.i("GPS Status Stopped")
//        }
//    }

//    private val gpsListener = GpsStatus.Listener { event ->
//        if (event == GpsStatus.GPS_EVENT_FIRST_FIX) {
////            showMessageDialog("GPS fixed")
//        }
//    }



    override fun onInactive() {
        super.onInactive()

    }


    override fun onActive() {
        super.onActive()

//        registerGnss()

        //TODO still not sure if it will keep listening or not
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(locationSetting
            .build())

        task.addOnSuccessListener {
            postValue(CustomLocationResult.GpsIsEnabled("Gps is Enabled"))
        }

        task.addOnFailureListener {exception ->
            if (exception is ResolvableApiException){
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
//                    exception.startResolutionForResult(this@MainActivity,
//                        REQUEST_CHECK_SETTINGS)


//                    context.sendBroadcast(Intent(CUSTOM_INTENT))
                    postValue(CustomLocationResult.GpsNotEnabled( "GPS Not Enabled"))

                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }

//        if (!AppUtil.isLocationEnabled(context)) {
//            postValue(CustomLocationResult.GpsNotEnabled(error = "GPS Not Enabled"))
//        } else {
//            try {
//                fusedLocationClient.requestLocationUpdates(
//                    locationRequest, locationCallback,
//                    Looper.myLooper()
//                )
//            } catch (unlikely: SecurityException) {
//                postValue(
//                    CustomLocationResult.PermissionLost(
//                        error = "Lost location permission. " +
//                                "Could not request updates. $unlikely"
//                    )
//                )
//            }
//        }
    }



//    private fun registerGnss() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//
//            if (ContextCompat.checkSelfPermission(
//                    context,
//                    android.Manifest.permission.ACCESS_FINE_LOCATION
//                )
//                == PackageManager.PERMISSION_GRANTED) {
//
//                locationManager.registerGnssStatusCallback(gnssCallback)
//            } else {
//
//            }
//        }
//    }

}

sealed class CustomLocationResult {

    data class GpsNotEnabled(val message: String) : CustomLocationResult()

    data class GpsIsEnabled(val message: String) : CustomLocationResult()

    data class PermissionMissing(val message: String) : CustomLocationResult()

    data class PermissionLost(val message: String) : CustomLocationResult()
}
