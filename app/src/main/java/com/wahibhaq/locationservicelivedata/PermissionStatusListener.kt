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
                postValue(PermissionStatus.Granted("Permission already granted"))

        override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) =
                postValue(PermissionStatus.Denied("Waiting for permission to be granted"))

        override fun onJustBlocked(
                context: Context?,
                justBlockedList: ArrayList<String>?,
                deniedPermissions: ArrayList<String>?
        ) = postValue(PermissionStatus.Blocked("Waiting for permission to be unblocked"))
    }

    private fun handleRuntimePermissionCheck() = Permissions.check(context,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            R.string.dialog_message_denied_permissions,
            Permissions.Options()
                    .setSettingsDialogTitle(context.getString(R.string.permission_required))
                    .setSettingsDialogMessage(context.getString(R.string.dialog_message_blocked_permission))
                    .setRationaleDialogTitle(context.getString(R.string.permission_required)),
            permissionHandler)
}

sealed class PermissionStatus {
    data class Granted(val message: String) : PermissionStatus()
    data class Denied(val message: String) : PermissionStatus()
    data class Blocked(val message: String) : PermissionStatus()
}