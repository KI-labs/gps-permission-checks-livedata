package com.wahibhaq.locationservicelivedata

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.support.v7.app.AlertDialog


object AppUtil {
    fun isLocationEnabled(context: Context): Boolean {
        val locationMode: Int
        try {
            locationMode = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCATION_MODE
            )
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
            return false
        }
        return locationMode != Settings.Secure.LOCATION_MODE_OFF
    }

    fun showPermissionsPermanentlyDeniedDialog(context: Context, listener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(context)
            .setTitle(R.string.permissions_required)
            .setMessage(R.string.dialog_message_permanently_denied_permissions)
            .setPositiveButton(R.string.action_settings) { _, _ ->
                // Open the app's settings.
                val intent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel, listener)
            .show()
    }

    fun showGPSNotEnabledDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle(R.string.gps_required)
            .setMessage(R.string.dialog_message_gps_disabled)
            .setPositiveButton(R.string.action_settings) { _, _ ->
                // Open the app's settings.
                val intent = Intent().apply {
                    action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
                }
                context.startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}