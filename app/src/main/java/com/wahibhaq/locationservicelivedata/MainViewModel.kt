package com.wahibhaq.locationservicelivedata

import android.app.Application
import android.arch.lifecycle.AndroidViewModel


class MainViewModel(application: Application) : AndroidViewModel(application) {

    val gpsStatusLiveData = GpsStatusListener(application)
}