package com.wahibhaq.locationservicelivedata

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.arch.lifecycle.LifecycleService
import android.content.Context
import android.content.Intent
import android.os.IBinder
import timber.log.Timber


class FakeLocationService : LifecycleService() {
    lateinit var notificationsUtil: NotificationsUtil

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.i("Tracking service started")
        isServiceRunning = true

        Intent(this, MainActivity::class.java)
            .let { PendingIntent.getActivity(this, 0, it, 0) }
            .let { pendingIntent ->
                notificationsUtil.createForegroundNotification(
                    this,
                    "Location Tracking", "Status: In Progress", pendingIntent
                )
            }
        return Service.START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        notificationsUtil = NotificationsUtil(
            applicationContext,
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                    NotificationManager
        )
        //other init comes here
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        throw UnsupportedOperationException("Not yet implemented")
    }

    companion object {
        var isServiceRunning: Boolean = false
    }
}