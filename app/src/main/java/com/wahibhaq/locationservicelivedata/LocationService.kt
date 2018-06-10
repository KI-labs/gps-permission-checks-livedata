package com.wahibhaq.locationservicelivedata

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.format.DateUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import timber.log.Timber


class LocationService : LifecycleService() {

    private lateinit var notificationsUtil: NotificationsUtil

    private lateinit var context: Context

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var locationRequest: LocationRequest

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
            //Decide how to use location coordinates
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        observeAndReactToPermissionsCheck()
        observeAndReactToGpsCheck()
        return Service.START_STICKY
    }

    private fun observeAndReactToPermissionsCheck() = PermissionStatusListener(this.application)
            .observe(this, Observer { permissionState ->
                when (permissionState) {
                    is PermissionStatus.Granted -> {
                        startTracking()
                        Timber.i(permissionState.message)
                    }

                    is PermissionStatus.Denied -> {
                        //End of Tracking. Maybe you would like to save coordinates

                        Timber.w(permissionState.message)
                        stopTracking()
                    }

                    is PermissionStatus.Blocked -> {
                        //End of Tracking. Maybe you would like to save coordinates

                        Timber.w(permissionState.message)
                        stopTracking()
                    }
                }
            })

    private fun stopTracking() {
        unregisterLocationUpdates()
        stopSelf()
    }

    private fun observeAndReactToGpsCheck() = GpsStatusListener(this.application).observe(
            this, Observer { gpsState ->
        when (gpsState) {
            is GpsStatus.GpsIsDisabled -> {
                Timber.e(gpsState.message)
                unregisterLocationUpdates()

                showOnGoingNotification("Waiting for GPS to be enabled")
                showGpsIsDisabledNotification()
            }

            is GpsStatus.GpsIsEnabled -> {
                Timber.i(gpsState.message)
                startTracking()
                notificationsUtil.cancelAlertNotification()
            }
        }
    })

    private fun startTracking() {
        showOnGoingNotification("Location Tracking is in Progress...")
        registerLocationUpdates() //We only start listening when Gps and Location Permission is enabled
    }

    private fun showGpsIsDisabledNotification() {
        // Clicking notification will taker user to enable location setting screen
        val resultIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        val pendingIntent = PendingIntent.getActivity(
                context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        notificationsUtil.createAlertNotification(
                "GPS Not Enabled!", "Enable GPS otherwise location tracking won't work",
                pendingIntent
        )
    }

    private fun showOnGoingNotification(message: String) {
        isServiceRunning = true
        Intent(this, MainActivity::class.java)
                .let { PendingIntent.getActivity(this, 0, it, 0) }
                .let { pendingIntent ->
                    notificationsUtil.createOngoingNotification(
                            this,
                            "Location Tracking", message, pendingIntent
                    )
                }
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        notificationsUtil = NotificationsUtil(
                context, context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        )

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = 5 * DateUtils.SECOND_IN_MILLIS
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }

    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onDestroy() {
        unregisterLocationUpdates()
        super.onDestroy()
    }

    private fun unregisterLocationUpdates() {
        isTrackingRunning = false
        isServiceRunning = false
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (unlikely: SecurityException) {
            Timber.e("Error when unregisterLocationUpdated()")
        }
        notificationsUtil.cancelAlertNotification()
    }

    private fun registerLocationUpdates() {
        isTrackingRunning = true
        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback,
                    Looper.myLooper()
            )
        } catch (unlikely: SecurityException) {
            Timber.e("Error when registerLocationUpdates()")

        }
    }

    companion object {
        //Refers to when this service is running and foreground notification is being displayed
        var isServiceRunning: Boolean = false
        //Refers to when app is listening to location updates
        var isTrackingRunning: Boolean = false
    }
}