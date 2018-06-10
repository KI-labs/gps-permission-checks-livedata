package com.wahibhaq.locationservicelivedata

import android.arch.lifecycle.Observer
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import com.wahibhaq.locationservicelivedata.LocationService.Companion.isServiceRunning
import com.wahibhaq.locationservicelivedata.LocationService.Companion.isTrackingRunning
import kotlinx.android.synthetic.main.activity_main.*


//TODO Inject locationServiceListener and use that to start and stop
class MainActivity : AppCompatActivity() {

    private lateinit var locationServiceListener: LocationServiceListener

    private var triggerSourceIsButton = false

    private lateinit var localGpsStatus: GpsStatus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationServiceListener = LocationServiceListener(
                applicationContext,
                Intent(applicationContext, LocationService::class.java)
        )

        observeOnGpsStatus()
        setupButtonAndUI()
    }

    private fun setupButtonAndUI() {
        btnInitTracking.setOnClickListener {
            triggerSourceIsButton = true

            //Only if there is a need and it's valid to start tracking
            if (!isTrackingRunning && !isServiceRunning)
                checkGpsAndThenPermission()
            else
                stopTracking() //To toggle state
        }

        permissionStatusDisplay.text = getString(R.string.permission_status_undefined)
        gpsStatusDisplay.text = getString(R.string.gps_status_undefined)
    }

    private fun startTracking() {
        locationServiceListener.subscribeToLocationUpdates()
        btnInitTracking.text = getString(R.string.button_text_end)
    }

    private fun stopTracking() {
        locationServiceListener.unsubscribeFromLocationUpdates()
        btnInitTracking.text = getString(R.string.button_text_start)
    }

    private fun observeOnGpsStatus() = GpsStatusListener(this.application).observe(this,
            Observer { status ->
                localGpsStatus = status!!
                checkGpsAndThenPermission()
            })

    /**
     * First checks GPS and if Enabled then checks for Runtime Permissions
     */
    private fun checkGpsAndThenPermission(): Any = when (localGpsStatus) {
        is GpsStatus.GpsIsEnabled -> {
            gpsStatusDisplay.text = (localGpsStatus as GpsStatus.GpsIsEnabled).message
            gpsStatusDisplay.setTextColor(Color.GREEN)

            observeAndDisplayPermissionStatus()
        }

        is GpsStatus.GpsIsDisabled -> {
            gpsStatusDisplay.text = (localGpsStatus as GpsStatus.GpsIsDisabled).message
            gpsStatusDisplay.setTextColor(Color.RED)

            showGpsNotEnabledDialog()
        }
    }

    private fun observeAndDisplayPermissionStatus(): Any =
            PermissionStatusListener((this.application)).observe(this,
                    Observer { status ->
                        when (status) {
                            is PermissionStatus.Granted -> {
                                permissionStatusDisplay.text = status.message
                                permissionStatusDisplay.setTextColor(Color.GREEN)

                                if (triggerSourceIsButton) startTracking()
                            }

                            is PermissionStatus.Denied -> {
                                permissionStatusDisplay.text = status.message
                                permissionStatusDisplay.setTextColor(Color.RED)
                            }

                            is PermissionStatus.Blocked -> {
                                permissionStatusDisplay.text = status.message
                                permissionStatusDisplay.setTextColor(Color.RED)
                            }
                        }
                    })

    override fun onResume() {
        super.onResume()
        when (isServiceRunning) {
            true -> btnInitTracking.text = getString(R.string.button_text_end)
            false -> btnInitTracking.text = getString(R.string.button_text_start)
        }
    }

    private fun showGpsNotEnabledDialog() = AlertDialog.Builder(this)
            .setTitle(R.string.gps_required)
            .setMessage(R.string.dialog_message_gps_disabled)
            .setPositiveButton(R.string.action_settings) { _, _ ->
                // Open the app's settings.
                val intent = Intent().apply {
                    action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
                }
                startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
}
