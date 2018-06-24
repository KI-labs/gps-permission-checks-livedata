package com.wahibhaq.locationservicelivedata

import android.app.Application
import android.arch.lifecycle.AndroidViewModel


class MainViewModel(application: Application) : AndroidViewModel(application) {

    fun getStatusResponse() =
            GpsStatusListener(getApplication()).zip(getPermissionCheck())

    fun getPermissionCheck() = PermissionStatusListener(getApplication())
}