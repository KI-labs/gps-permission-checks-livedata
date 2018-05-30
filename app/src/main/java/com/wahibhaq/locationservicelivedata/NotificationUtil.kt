package com.wahibhaq.locationservicelivedata

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.os.Build
import android.support.v4.app.NotificationCompat

const val ONGOING_NOTIFICATION_ID = 100
const val ALERT_NOTIFICATION_ID = 200
const val NOTIFICATION_CHANNEL_NAME = "All"
const val NOTIFICATION_CHANNEL_ONGOING_ID = "com.wahibhaq.locationservicelivedata.ongoing"
const val NOTIFICATION_CHANNEL_ALERTS_ID = "com.wahibhaq.locationservicelivedata.alerts"

class NotificationsUtil(
    private val context: Context,
    private val notificationManager: NotificationManager
) {

    fun createOngoingNotification(
        service: Service,
        title: String,
        text: String,
        pendingIntent: PendingIntent?
    ) {
        createOngoingNotificationChannel()
        service.startForeground(
            ONGOING_NOTIFICATION_ID,
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ONGOING_ID)
                .setContentTitle(title)
                .setOngoing(true)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .build()
        )
    }

    fun createAlertNotification(
        title: String, text: String,
        pendingIntent: PendingIntent? = null
    ) {
        createAlertsNotificationChannel()
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ALERTS_ID)
            .setContentTitle(title)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun createOngoingNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                NOTIFICATION_CHANNEL_ONGOING_ID, NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_MIN
            )
                .let { channel ->
                    notificationManager.createNotificationChannel(channel)
                }
        }
    }

    private fun createAlertsNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                NOTIFICATION_CHANNEL_ALERTS_ID, NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
                .let { channel ->
                    notificationManager.createNotificationChannel(channel)
                }
        }
    }

    fun cancelAlertNotification() {
        notificationManager.cancel(ALERT_NOTIFICATION_ID)
    }
}