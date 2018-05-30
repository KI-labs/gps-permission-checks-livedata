package com.wahibhaq.locationservicelivedata

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.os.Build
import timber.log.Timber


class LocationServiceListener(
    private val context: Context,
    private val serviceIntent: Intent
) : LocationListener {

    @TargetApi(Build.VERSION_CODES.O)
    override fun subscribeToLocationUpdates() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Timber.d("Running on Android O")
            context.startForegroundService(serviceIntent)
        } else {
            Timber.d("Running on Android N or lower")
            context.startService(serviceIntent)
        }
    }

    override fun unsubscribeFromLocationUpdates() {
        context.stopService(serviceIntent)
    }
}