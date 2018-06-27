package com.wahibhaq.locationservicelivedata

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.net.Uri
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

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var locationRequest: LocationRequest

    private var gpsIsEnabled = false

    private var permissionIsGranted = false

    private lateinit var gpsAndPermissionStatusLiveData: LiveData<Pair<GpsStatus, PermissionStatus>>

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
            //Decide how to use location coordinates
            Timber.d("New Coordinates Received: %s", locationResult.locations.toString())
        }
    }

    private val pairObserver = Observer<Pair<GpsStatus, PermissionStatus>> { pair ->
        pair?.let {
            Timber.i("Pairobserver received with %s and %s", pair.first, pair.second)
            handleGpsStatus(pair.first)
            handlePermissionStatus(pair.second)
        }
    }

    private fun handleGpsStatus(status: GpsStatus) {
        when (status) {
            is GpsStatus.GpsIsEnabled -> {
                Timber.w(status.message)
                gpsIsEnabled = true
                notificationsUtil.cancelAlertNotification()
            }

            is GpsStatus.GpsIsDisabled -> {
                Timber.w(status.message)
                gpsIsEnabled = false
                stopTracking()
                showOnGoingNotification(R.string.notif_gps_waiting_body)
                showGpsIsDisabledNotification()
            }
        }
    }

    private fun handlePermissionStatus(status: PermissionStatus) {
        when (status) {
            is PermissionStatus.Granted -> {
                Timber.i("Service: %s", status.message)
                permissionIsGranted = true
                startTracking()
            }

            is PermissionStatus.Denied -> {
                Timber.w("Service: %s", status.message)
                permissionIsGranted = false
                stopTracking()
                showPermissionIsMissingNotification()
                stopSelf()
            }

            is PermissionStatus.Blocked -> {
                Timber.w("Service: %s", status.message)
                permissionIsGranted = false
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        notificationsUtil = NotificationsUtil(applicationContext,
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = 5 * DateUtils.SECOND_IN_MILLIS
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }

        gpsAndPermissionStatusLiveData = GpsStatusListener(applicationContext)
                .zip(PermissionStatusListener(applicationContext, isForService = true))

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.i("Tracking service getting started")
        startObservingGpsAndPermissionStatus()

        //Mainly because we want Service to restart if user revokes permission and to notify him
        return Service.START_STICKY
    }

    private fun showPermissionIsMissingNotification() {
        // Clicking notification will taker user to enable location setting screen
        val resultIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null))
        val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        notificationsUtil.createAlertNotification(
                R.string.permission_required_title,
                R.string.permission_required_body,
                pendingIntent)
    }

    private fun startObservingGpsAndPermissionStatus() = gpsAndPermissionStatusLiveData
            .reObserve(this, pairObserver)

    private fun startTracking() {
        if (permissionIsGranted && gpsIsEnabled) {
            showOnGoingNotification(R.string.notif_in_progress)
            registerLocationUpdates() //We only start listening when Gps and Location Permission is enabled
        }
    }

    private fun stopTracking() {
        //saveTrackingResults() //Maybe store coordinates at this point
        unregisterLocationUpdates()
    }

    private fun showGpsIsDisabledNotification() {
        // Clicking notification will taker user to enable location setting screen
        val resultIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        notificationsUtil.createAlertNotification(
                R.string.gps_required_title,
                R.string.gps_required_body,
                pendingIntent)
    }

    private fun showOnGoingNotification(message: Int) {
        Timber.i("showing ongoing notification")
        isServiceRunning = true
        Intent(this, MainActivity::class.java)
                .let { PendingIntent.getActivity(this, 0, it, 0) }
                .let { pendingIntent ->
                    notificationsUtil.createOngoingNotification(this,
                            R.string.notif_location_tracking_title, message, pendingIntent)
                }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onDestroy() {
        Timber.i("Service is destroyed now")
        stopTracking()
        unsubscribeToLiveData()
        super.onDestroy()
    }

    private fun unsubscribeToLiveData() {
        if (gpsAndPermissionStatusLiveData.hasObservers()) {
            Timber.i("Removing LiveData Observer and LifeCycleOwner")
            gpsAndPermissionStatusLiveData.removeObserver(pairObserver)
        }
    }

    private fun unregisterLocationUpdates() {
        Timber.i("Unregistering location update listener")
        isTrackingRunning = false
        isServiceRunning = false
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (unlikely: SecurityException) {
            Timber.e("Error when unregisterLocationUpdated()")
            error("Error when unregisterLocationUpdated()")
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
            error("Error when registerLocationUpdates()")
        }
    }

    companion object {
        //Refers to when this service is running and foreground notification is being displayed
        var isServiceRunning: Boolean = false
            private set

        //Refers to when app is listening to location updates
        var isTrackingRunning: Boolean = false
            private set
    }
}