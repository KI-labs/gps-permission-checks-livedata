package com.wahibhaq.locationservicelivedata

import android.Manifest
import android.arch.lifecycle.LiveData
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat

/**
 * Listens to Location Runtime Permissions which comes under the category of "Dangerous" and
 * then responds with appropriate state specified in {@link PermissionStatus}
 */
class PermissionStatusListener(private val context: Context) : LiveData<PermissionStatus>() {

    override fun onActive() = handlePermissionCheck()

    private fun handlePermissionCheck() {
        val isPermissionGranted = ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (isPermissionGranted)
            postValue(PermissionStatus.Granted(context.getString(R.string.permission_status_granted)))
        else
            postValue(PermissionStatus.Denied(context.getString(R.string.permission_status_denied_show_notif)))
    }
}

sealed class PermissionStatus {
    data class Granted(val message: String = "") : PermissionStatus()
    data class Denied(val message: String = "") : PermissionStatus()
    data class Blocked(val message: String = "") : PermissionStatus()
}