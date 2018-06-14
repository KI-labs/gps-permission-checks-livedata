package com.wahibhaq.locationservicelivedata

import android.Manifest
import android.arch.lifecycle.LiveData
import android.content.Context
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import java.util.*

/**
 * Listens to Location Runtime Permissions which comes under the category of "Dangerous" and
 * then responds with appropriate state specified in {@link PermissionStatus}
 */
class PermissionStatusListener(private val context: Context) : LiveData<PermissionStatus>() {

    override fun onActive() {
        super.onActive()
        handleRuntimePermissionCheck()
    }

    private val permissionHandler = object : PermissionHandler() {
        override fun onGranted() =
                postValue(PermissionStatus.Granted("Location permission is already granted"))

        override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) =
                postValue(PermissionStatus.Denied("Waiting for location permission to be granted"))

        override fun onJustBlocked(
                context: Context?,
                justBlockedList: ArrayList<String>?,
                deniedPermissions: ArrayList<String>?
        ) = postValue(PermissionStatus.Blocked("Waiting for location permission to be unblocked"))
    }

    private fun handleRuntimePermissionCheck() = Permissions.check(context,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            R.string.permission_required_body,
            Permissions.Options()
                    .setSettingsDialogTitle(context.getString(R.string.permission_required_title))
                    .setSettingsDialogMessage(context.getString(R.string.permission_blocked_body))
                    .setRationaleDialogTitle(context.getString(R.string.permission_required_title)),
            permissionHandler)
}

sealed class PermissionStatus {
    data class Granted(val message: String) : PermissionStatus()
    data class Denied(val message: String) : PermissionStatus()
    data class Blocked(val message: String) : PermissionStatus()
}