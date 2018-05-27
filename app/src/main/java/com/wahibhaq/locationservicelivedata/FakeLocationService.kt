package com.wahibhaq.locationservicelivedata

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.text.format.DateUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import timber.log.Timber


class FakeLocationService : LifecycleService() {

    private lateinit var notificationsUtil: NotificationsUtil

    private lateinit var context: Context

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var locationRequest: LocationRequest

    //TODO Implement App Permission check
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val locationLiveData = LocationLiveData(context, fusedLocationClient, locationRequest)
        locationLiveData.observe(
            this, Observer { locationResult ->
                when (locationResult) {
                    is LocationResult.GpsNotEnabled -> {
                        isServiceRunning = false
                        Timber.e(locationResult.error)
                        showGpsNotEnabledNotification()
                    }
                    is LocationResult.LocationData -> {
                        isServiceRunning = true
                        Timber.i("Tracking service started successfully")
                        showInProgressNotification()
                    }
                }

            }
        )

        return Service.START_STICKY
    }

    private fun showGpsNotEnabledNotification() {
        // Show the notification with an intent to take to enable location
        val resultIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        notificationsUtil.createAlertNotification(
            "GPS Not Enabled!", "Enable GPS otherwise location tracking won't work",
            pendingIntent
        )
    }

    private fun showInProgressNotification() {
        Intent(this, MainActivity::class.java)
            .let { PendingIntent.getActivity(this, 0, it, 0) }
            .let { pendingIntent ->
                notificationsUtil.createOngoingNotification(
                    this,
                    "Location Tracking", "Status: In Progress", pendingIntent
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

    companion object {
        var isServiceRunning: Boolean = false
    }
}