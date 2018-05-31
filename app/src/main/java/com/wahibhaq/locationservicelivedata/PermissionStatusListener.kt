package com.wahibhaq.locationservicelivedata

import android.Manifest
import android.app.Application
import android.arch.lifecycle.LiveData
import android.content.Context
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import java.util.*

/**
 * Listens to Location Runtime Permissions which comes under the category of "Dangerous" and
 * then responds with appropriate state specified in {@link PermissionStatus}
 */
class PermissionStatusListener(private val application: Application) : LiveData<PermissionStatus>() {

    override fun onActive() {
        super.onActive()
        handleRuntimePermission()
    }

    private fun handleRuntimePermission() {
        Permissions.check(application,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            R.string.dialog_message_permanently_denied_permissions,
            Permissions.Options().setSettingsDialogTitle("Warning!").setRationaleDialogTitle("Info")
                .sendDontAskAgainToSettings(true),
            object : PermissionHandler() {
                override fun onGranted() {
                    postValue(PermissionStatus.Granted("Permission Granted"))
                }

                override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) {
                    postValue(PermissionStatus.Denied("Waiting for permissions to be granted"))
                }

                override fun onJustBlocked(
                    context: Context?,
                    justBlockedList: ArrayList<String>?,
                    deniedPermissions: ArrayList<String>?
                ) {
                    postValue(PermissionStatus.Blocked("Waiting for permissions to be unblocked"))
                }
            })
    }

}

sealed class PermissionStatus {
    data class Granted(val message: String) : PermissionStatus()
    data class Denied(val message: String) : PermissionStatus()
    data class Blocked(val message: String) : PermissionStatus()
}