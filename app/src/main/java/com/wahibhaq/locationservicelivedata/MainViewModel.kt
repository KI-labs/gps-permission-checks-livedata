package com.wahibhaq.locationservicelivedata

import android.Manifest
import android.app.Application
import android.arch.lifecycle.AndroidViewModel


class MainViewModel(application: Application) : AndroidViewModel(application) {

    val gpsStatusLiveData = GpsStatusListener(application)

    val locationPermissionStatusLiveData = PermissionStatusListener(application,
            Manifest.permission.ACCESS_FINE_LOCATION)
}