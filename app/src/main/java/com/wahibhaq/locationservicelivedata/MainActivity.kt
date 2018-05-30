package com.wahibhaq.locationservicelivedata

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private lateinit var locationServiceListener: LocationServiceListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationServiceListener = LocationServiceListener(
            applicationContext,
            Intent(applicationContext, LocationService::class.java)
        )

        btnInitTracking.setOnClickListener {
            if (AppUtil.isLocationEnabled(this)) {
                if (!LocationService.isTrackingRunning) {
                    handleRuntimePermission()
                } else if (LocationService.isTrackingRunning) {
                    locationServiceListener.unsubscribeFromLocationUpdates()
                    btnInitTracking.text = getString(R.string.button_text_start)
                }
            } else {
                AppUtil.showGPSNotEnabledDialog(this)
            }
        }
    }

    private fun handleRuntimePermission() {
        Permissions.check(this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            R.string.dialog_message_permanently_denied_permissions,
            Permissions.Options().setSettingsDialogTitle("Warning!").setRationaleDialogTitle("Info")
                .sendDontAskAgainToSettings(true),
            object : PermissionHandler() {
                override fun onGranted() {
                    Toast.makeText(this@MainActivity, "Permission Accepted", Toast.LENGTH_SHORT).show()
                    locationServiceListener.subscribeToLocationUpdates()
                    btnInitTracking.text = getString(R.string.button_text_end)
                }

                override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) {
                    Toast.makeText(this@MainActivity, "Permission Denied", Toast.LENGTH_SHORT).show()
                }

                override fun onJustBlocked(
                    context: Context?,
                    justBlockedList: ArrayList<String>?,
                    deniedPermissions: ArrayList<String>?
                ) {
                    Toast.makeText(this@MainActivity, "Permission Permanently Denied", Toast.LENGTH_SHORT)
                        .show()
                    AppUtil.showPermissionsPermanentlyDeniedDialog(this@MainActivity,
                        DialogInterface.OnClickListener { _, _ ->
                            locationServiceListener.unsubscribeFromLocationUpdates()
                        })
                }
            })
    }

    override fun onResume() {
        super.onResume()
        when (LocationService.isTrackingRunning) {
            true -> btnInitTracking.text = getString(R.string.button_text_end)
            false -> btnInitTracking.text = getString(R.string.button_text_start)
        }

    }
}
