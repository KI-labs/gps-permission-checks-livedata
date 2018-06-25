package com.wahibhaq.locationservicelivedata

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData


class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var gpsAndPermissionStatusLiveData: LiveData<Pair<GpsStatus, PermissionStatus>>

    init {
        gpsAndPermissionStatusLiveData = GpsStatusListener(getApplication()).zip(getPermissionCheck())
    }

    fun getStatusResponse() = gpsAndPermissionStatusLiveData

    fun getPermissionCheck() = PermissionStatusListener(getApplication())
}