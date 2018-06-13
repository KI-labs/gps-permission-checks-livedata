package com.wahibhaq.locationservicelivedata

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.support.v4.app.ActivityCompat
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
        Timber.i("Tracking service getting started")
        checkGpsAndThenPermission()
        return Service.START_STICKY
    }

    private fun checkLocationPermission() {
        val isPermissionGranted = ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (isPermissionGranted) {
            Timber.i("Permission Granted in Service")
            notificationsUtil.cancelAlertNotification()
            startTracking()
        } else {
            Timber.w("Permission Denied in Service")
            stopTracking()
            showPermissionIsMissingNotification()
            stopSelf()
        }
    }

    private fun showPermissionIsMissingNotification() {
        // Clicking notification will taker user to enable location setting screen
        val resultIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null))
        val pendingIntent = PendingIntent.getActivity(
                context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        notificationsUtil.createAlertNotification(
                R.string.permission_required,
                R.string.dialog_message_denied_permissions,
                pendingIntent)
    }

    private fun checkGpsAndThenPermission() = GpsStatusListener(this.application).observe(
            this, Observer { gpsState ->
        when (gpsState) {
            is GpsStatus.GpsIsDisabled -> {
                Timber.w(gpsState.message)
                stopTracking()
                showOnGoingNotification(R.string.notif_gps_waiting_body)
                showGpsIsDisabledNotification()
            }

            is GpsStatus.GpsIsEnabled -> {
                Timber.i(gpsState.message)
                notificationsUtil.cancelAlertNotification()
                checkLocationPermission()
            }
        }
    })

    private fun startTracking() {
        showOnGoingNotification(R.string.notification_in_progress)
        registerLocationUpdates() //We only start listening when Gps and Location Permission is enabled
    }

    private fun stopTracking() {
        //saveTrackingResults() //Maybe store coordinates at this point
        unregisterLocationUpdates()
    }

    private fun showGpsIsDisabledNotification() {
        // Clicking notification will taker user to enable location setting screen
        val resultIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        val pendingIntent = PendingIntent.getActivity(
                context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        notificationsUtil.createAlertNotification(
                R.string.notif_gps_not_enabled_title,
                R.string.notif_gps_not_enabled_body,
                pendingIntent)
    }

    private fun showOnGoingNotification(message: Int) {
        Timber.i("showing ongoing notification")
        isServiceRunning = true
        Intent(this, MainActivity::class.java)
                .let { PendingIntent.getActivity(this, 0, it, 0) }
                .let { pendingIntent ->
                    notificationsUtil.createOngoingNotification(this,
                            R.string.notif_ongoing_title, message, pendingIntent)
                }
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        notificationsUtil = NotificationsUtil(context,
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)

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
        Timber.i("Service is destroyed now")
        stopTracking()
        super.onDestroy()
    }

    private fun unregisterLocationUpdates() {
        Timber.i("Unregistering location update listener")
        isTrackingRunning = false
        isServiceRunning = false
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (unlikely: SecurityException) {
            Timber.e("Error when unregisterLocationUpdated()")
        }
    }

    private fun registerLocationUpdates() {
        Timber.i("Registering location update listener")
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