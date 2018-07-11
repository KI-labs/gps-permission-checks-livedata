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
import com.google.android.gms.location.*
import timber.log.Timber

/**
 * Ideally this Service should be started/stopped based on [ActivityTransitionEvent] received in
 * Custom Broadcast Receiver.
 */
class LocationService : LifecycleService() {

    private lateinit var notificationsUtil: NotificationsUtil

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var locationRequest: LocationRequest

    private var gpsIsEnabled = false

    private var permissionIsGranted = false

    private lateinit var gpsAndPermissionStatusLiveData: LiveData<Pair<PermissionStatus, GpsStatus>>

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
            //Decide how to use/store location coordinates
            Timber.d("New Coordinates Received: %s", locationResult.locations.toString())
        }
    }

    private val pairObserver = Observer<Pair<PermissionStatus, GpsStatus>> { pair ->
        pair?.let {
            Timber.i("Pairobserver received with %s and %s", pair.first, pair.second)
            handlePermissionStatus(pair.first)
            handleGpsStatus(pair.second)
            stopServiceIfNeeded()
        }
    }

    private fun handlePermissionStatus(status: PermissionStatus) {
        when (status) {
            is PermissionStatus.Granted -> {
                Timber.i("Service - Permission: %s", status.message)
                permissionIsGranted = true
                registerForLocationTracking()
            }

            is PermissionStatus.Denied -> {
                Timber.w("Service - Permission: %s", status.message)
                permissionIsGranted = false
                showPermissionIsMissingNotification()
            }
        }
    }

    private fun handleGpsStatus(status: GpsStatus) {
        when (status) {
            is GpsStatus.GpsIsEnabled -> {
                Timber.i("Service - GPS: %s", status.message)
                gpsIsEnabled = true
                registerForLocationTracking()
            }

            is GpsStatus.GpsIsDisabled -> {
                Timber.w("Service - GPS: %s", status.message)
                gpsIsEnabled = false
                showGpsIsDisabledNotification()
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

        gpsAndPermissionStatusLiveData = PermissionStatusListener(applicationContext)
                .combineLatestWith(GpsStatusListener(applicationContext))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.i("Tracking service getting started")
        showOnGoingNotification()
        startObservingGpsAndPermissionStatus()
        //Mainly because we want Service to restart if user revokes permission and to notify him
        return Service.START_STICKY
    }

    private fun startObservingGpsAndPermissionStatus() = gpsAndPermissionStatusLiveData
            .observe(this, pairObserver)

    private fun eitherPermissionOrGpsIsDisabled() = gpsIsEnabled.not() || permissionIsGranted.not()

    /**
     * We only start listening when Gps and Location Permission are enabled
     */
    private fun registerForLocationTracking() {
        if (permissionIsGranted && gpsIsEnabled) {
            Timber.i("Registering location update listener")
            isTrackingRunning = try {
                fusedLocationClient.requestLocationUpdates(
                        locationRequest, locationCallback,
                        Looper.myLooper())
                true
            } catch (unlikely: SecurityException) {
                Timber.e("Error when registerLocationUpdates()")
                error("Error when registerLocationUpdates()")
            }
        }
    }

    private fun unregisterFromLocationTracking() {
        Timber.i("Unregistering location update listener")
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (unlikely: SecurityException) {
            Timber.e("Error when unregisterLocationUpdated()")
            error("Error when unregisterLocationUpdated()")
        }
    }

    private fun showPermissionIsMissingNotification() {
        // Clicking notification will taker user to enable location setting screen
        val resultIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null))
        val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        notificationsUtil.createAlertNotification(ALERT_PERMISSION_NOTIFICATION_ID,
                getString(R.string.permission_required_title),
                getString(R.string.permission_required_body),
                pendingIntent)
    }

    private fun showGpsIsDisabledNotification() {
        // Clicking notification will taker user to enable location setting screen
        val resultIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        notificationsUtil.createAlertNotification(
                ALERT_GPS_NOTIFICATION_ID,
                getString(R.string.gps_required_title),
                getString(R.string.gps_required_body),
                pendingIntent)
    }

    private fun showOnGoingNotification() {
        Timber.i("showing showing ongoing notification")
        notificationsUtil.cancelAlertNotification() //remove existing alert notifs if any
        isServiceRunning = true
        Intent(this, MainActivity::class.java)
                .let { PendingIntent.getActivity(this, 0, it, 0) }
                .let { pendingIntent ->
                    notificationsUtil.createOngoingNotification(this,
                            getString(R.string.notif_location_tracking_title), getString(R.string.notif_in_progress),
                            pendingIntent)
                }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("Service is destroyed now")
        isTrackingRunning = false
        isServiceRunning = false

        //Only attempt to stop tracking when you know startTracking() was already called
        if (eitherPermissionOrGpsIsDisabled().not()) unregisterFromLocationTracking()
    }

    /**
     * This is to handle case when GPS or Permission wasn't enabled but start drive was detected.
     * Once Notifications are shown, there's no need to continue tracking because app won't be able
     * to receive location coordinates.
     */
    private fun stopServiceIfNeeded() {
        if (eitherPermissionOrGpsIsDisabled()) {
            //Maybe you would like to store current drive before killing the service
            stopSelf()
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