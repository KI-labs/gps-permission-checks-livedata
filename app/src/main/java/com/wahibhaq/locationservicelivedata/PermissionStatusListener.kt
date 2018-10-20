package com.wahibhaq.locationservicelivedata

import android.arch.lifecycle.LiveData
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat

/**
 * Listens to Runtime Permission Status of provided [permissionToListen] which comes under the
 * category of "Dangerous" and then responds with appropriate state specified in {@link PermissionStatus}
 */
class PermissionStatusListener(private val context: Context,
                               private val permissionToListen: String) : LiveData<PermissionStatus>() {

    override fun onActive() = handlePermissionCheck()

    private fun handlePermissionCheck() {
        val isPermissionGranted = ActivityCompat.checkSelfPermission(context,
                permissionToListen) == PackageManager.PERMISSION_GRANTED

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