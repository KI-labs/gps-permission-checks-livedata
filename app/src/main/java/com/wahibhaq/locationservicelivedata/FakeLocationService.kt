package com.wahibhaq.locationservicelivedata

import android.Manifest
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
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import timber.log.Timber
import java.util.*


class FakeLocationService : LifecycleService() {

    private lateinit var notificationsUtil: NotificationsUtil

    private lateinit var context: Context

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var locationRequest: LocationRequest

    private lateinit var notificationState: NotificationState

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
            //Decide how to use location coordinates
        }
    }

    //TODO Implement App Permission check
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val locationLiveData = GpsStatusListener(context)
        locationLiveData.observe(
            this, Observer { locationResult ->
                when (locationResult) {
                    is CustomLocationResult.GpsIsDisabled -> {
                        Timber.e(locationResult.message)
                        unregisterLocationUpdates()

                        //Only override the message if it was not displaying any permission warnings
                        if (notificationState == NotificationState.InProgress()) {
                            showOnGoingNotification("Listening to Location...")
                        }

                        showGpsIsDisabledNotification()
                        notificationState = NotificationState.WaitingForGps()
                    }

                    is CustomLocationResult.GpsIsEnabled -> {
                        Timber.i(locationResult.message)
                        showOnGoingNotification("In Progress")
                        registerLocationUpdates() //We only start listening when Gps and
                        // Permissions are there
                        notificationsUtil.cancelAlertNotification()
                        notificationState = NotificationState.InProgress()
                    }
                }

            }
        )

        return Service.START_STICKY
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

        handleRuntimePermission()
    }
    
    private fun handleRuntimePermission() {
        Permissions.check(this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            R.string.dialog_message_permanently_denied_permissions,
            Permissions.Options().setSettingsDialogTitle("Warning!").setRationaleDialogTitle("Info")
                .sendDontAskAgainToSettings(true),
            object : PermissionHandler() {
                override fun onGranted() {
                    showOnGoingNotification("In Progress")
                    registerLocationUpdates()
                    notificationState = NotificationState.InProgress()
                }

                override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) {
                    unregisterLocationUpdates()
                    showOnGoingNotification("Waiting for permissions to be granted")
                    notificationState = NotificationState.WaitingForEnablingPermission()
                }

                override fun onJustBlocked(
                    context: Context?,
                    justBlockedList: ArrayList<String>?,
                    deniedPermissions: ArrayList<String>?
                ) {
                    unregisterLocationUpdates()
                    showOnGoingNotification("Waiting for permissions to be unblocked")
                    notificationState = NotificationState.WaitingForUnblockingPermission()
                }
            })
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
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (unlikely: SecurityException) {
            Timber.e("Error when unregisterLocationUpdated()")
        }
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
        var isTrackingRunning: Boolean = false
    }
}

sealed class NotificationState {
    class InProgress : NotificationState()
    class WaitingForGps : NotificationState()
    class WaitingForEnablingPermission : NotificationState()
    class WaitingForUnblockingPermission : NotificationState()
}