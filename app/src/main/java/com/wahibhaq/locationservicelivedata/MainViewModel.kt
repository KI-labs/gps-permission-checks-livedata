package com.wahibhaq.locationservicelivedata

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData


class MainViewModel(application: Application) : AndroidViewModel(application) {

    val gpsStatusLiveData: LiveData<GpsStatus> = GpsStatusListener(application)
}