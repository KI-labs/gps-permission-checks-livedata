package com.wahibhaq.locationservicelivedata

import android.arch.lifecycle.Observer
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
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
                    observeAndReactToPermissionsCheck()
                } else if (LocationService.isTrackingRunning) {
                    locationServiceListener.unsubscribeFromLocationUpdates()
                    btnInitTracking.text = getString(R.string.button_text_start)
                }
            } else {
                AppUtil.showGPSNotEnabledDialog(this)
            }
        }

    }

    private fun observeAndReactToPermissionsCheck() {
        PermissionStatusListener(this.application)
            .observe(this, Observer { permissionState ->
                when (permissionState) {
                    is PermissionStatus.Granted -> {
                        if (!LocationService.isTrackingRunning) {
                            Toast.makeText(
                                this@MainActivity, "Permission Already Granted", Toast
                                    .LENGTH_SHORT
                            ).show()
                            locationServiceListener.subscribeToLocationUpdates()
                            btnInitTracking.text = getString(R.string.button_text_end)
                        }
                    }

                    is PermissionStatus.Denied -> {
                        //End of Drive. Maybe you would like to do something with coordinates

                        Toast.makeText(this@MainActivity, "Permission Denied", Toast.LENGTH_SHORT)
                            .show()
                    }

                    is PermissionStatus.Blocked -> {
                        //End of Drive. Maybe you would like to do something with coordinates

                        Toast.makeText(
                            this@MainActivity,
                            "Permission Permanently Denied",
                            Toast.LENGTH_SHORT
                        )
                            .show()

                        AppUtil.showPermissionsPermanentlyDeniedDialog(this@MainActivity,
                            DialogInterface.OnClickListener { _, _ ->
                                locationServiceListener.unsubscribeFromLocationUpdates()
                            })
                    }
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
