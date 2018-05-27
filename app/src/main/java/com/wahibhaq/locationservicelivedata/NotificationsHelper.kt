package com.wahibhaq.locationservicelivedata

import android.app.*
import android.content.Context
import android.os.Build
import android.support.v4.app.NotificationCompat

const val FOREGROUND_NOTIFICATION_ID = 100
val NOTIFICATION_CHANNEL_NAME = "All"
const val NOTIFICATION_CHANNEL_ID = "com.wahibhaq.locationservicelivedata"

class NotificationsUtil(
    val context: Context,
    val notificationManager: NotificationManager
) {

    fun createForegroundNotification(
        service: Service,
        title: String,
        text: String,
        pendingIntent: PendingIntent
    ) {
        service.startForeground(
            FOREGROUND_NOTIFICATION_ID,
            getOngoingNotification(title, text, pendingIntent)
        )
    }

    private fun getOngoingNotification(
        title: String, text: String,
        pendingIntent: PendingIntent? = null
    ): Notification {

        createNotificationChannel()
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setOngoing(true)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_MIN
            )
                .let { channel ->
                    notificationManager.createNotificationChannel(channel)
                }
        }
    }
}