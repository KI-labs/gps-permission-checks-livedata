package com.wahibhaq.locationservicelivedata

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.arch.lifecycle.AndroidViewModel
import android.content.Context
import android.content.Intent


class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val locationServiceListener = LocationServiceListener(application, Intent(application,
            LocationService::class.java))

    private val notificationsUtil = NotificationsUtil(application,
            application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)

    val gpsStatusLiveData = GpsStatusListener(application)

    val locationPermissionStatusLiveData = PermissionStatusListener(application,
            Manifest.permission.ACCESS_FINE_LOCATION)

    fun startLocationTracking() = locationServiceListener.subscribeToLocationUpdates()

    fun stopLocationTracking() {
        locationServiceListener.unsubscribeFromLocationUpdates()
        notificationsUtil.cancelAlertNotification()
    }

}