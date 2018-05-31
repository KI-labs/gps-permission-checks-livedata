package com.wahibhaq.locationservicelivedata

import android.arch.lifecycle.Observer
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

//TODO Inject locationServiceListener and use that to start and stop
class MainActivity : AppCompatActivity() {

    private lateinit var locationServiceListener: LocationServiceListener

    private lateinit var localGpsStatus: GpsStatus

    private lateinit var localPermissionStatus: PermissionStatus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationServiceListener = LocationServiceListener(
            applicationContext,
            Intent(applicationContext, LocationService::class.java)
        )

        observeAndDisplayGpsStatus()
        observeAndDisplayPermissionStatus()

        btnInitTracking.setOnClickListener {
            if (!LocationService.isTrackingRunning && !LocationService.isServiceRunning) {

                if (localGpsStatus is GpsStatus.GpsIsEnabled) {

                    when (localPermissionStatus) {

                        is PermissionStatus.Granted -> startDrive()

                        is PermissionStatus.Denied -> {
                            AppUtil.showEnablePermissionDialog(this@MainActivity,
                                DialogInterface.OnClickListener { _, _ ->
                                    endDrive()
                                })
                        }

                        is PermissionStatus.Blocked -> {
                            AppUtil.showEnablePermissionDialog(this@MainActivity,
                                DialogInterface.OnClickListener { _, _ ->
                                    endDrive()
                                })
                        }
                    }
                } else {
                    AppUtil.showEnableGpsDialog(this)
                }
            } else {
                endDrive()
            }
        }

    }

    private fun startDrive() {
        locationServiceListener.subscribeToLocationUpdates()
        btnInitTracking.text = getString(R.string.button_text_end)
    }

    private fun endDrive() {
        locationServiceListener.unsubscribeFromLocationUpdates()
        btnInitTracking.text = getString(R.string.button_text_start)
    }

    private fun observeAndDisplayGpsStatus() {
        GpsStatusListener(this.application).observe(this,
            Observer { status ->

                localGpsStatus = status!!
                when (status) {
                    is GpsStatus.GpsIsEnabled -> {
                        gpsStatusDisplay.text = String.format(
                            getString(
                                R.string
                                    .gps_status_label
                            ), status.message
                        )
                    }

                    is GpsStatus.GpsIsDisabled -> {
                        gpsStatusDisplay.text = String.format(
                            getString(
                                R.string
                                    .gps_status_label
                            ), status.message
                        )
                    }
                }
            })
    }

    private fun observeAndDisplayPermissionStatus() {
        PermissionStatusListener((this.application)).observe(this,
            Observer { status ->
                localPermissionStatus = status!!
                when (status) {
                    is PermissionStatus.Granted -> {
                        permissionStatusDisplay.text = String.format(
                            getString(
                                R.string
                                    .permission_status_label
                            ), status.message
                        )
                    }

                    is PermissionStatus.Denied -> {
                        permissionStatusDisplay.text = String.format(
                            getString(
                                R.string
                                    .permission_status_label
                            ), status.message
                        )
                    }

                    is PermissionStatus.Blocked -> {
                        permissionStatusDisplay.text = String.format(
                            getString(
                                R.string
                                    .permission_status_label
                            ), status.message
                        )
                    }
                }
            })
    }

    override fun onResume() {
        super.onResume()
        when (LocationService.isServiceRunning) {
            true -> btnInitTracking.text = getString(R.string.button_text_end)
            false -> btnInitTracking.text = getString(R.string.button_text_start)
        }

    }
}
