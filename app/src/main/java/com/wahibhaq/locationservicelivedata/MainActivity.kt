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
import timber.log.Timber



//TODO Inject locationServiceListener and use that to start and stop
class MainActivity : AppCompatActivity() {

    private lateinit var locationServiceListener: LocationServiceListener

    private var triggerSourceIsButton = false

    private lateinit var localGpsStatus: GpsStatus

    private val gpsObserver = Observer<GpsStatus> { status ->
        localGpsStatus = status!!
        checkGpsAndThenPermission()
    }

    private val permissionObserver = Observer<PermissionStatus> { status ->
        when (status) {
            is PermissionStatus.Granted -> {
                Timber.i("Permission granted in Activity")
                permissionStatusDisplay.text = status.message
                permissionStatusDisplay.setTextColor(Color.BLUE)

                if (triggerSourceIsButton) startTracking()
            }

            is PermissionStatus.Denied -> {
                Timber.i("Permission denied in Activity")
                permissionStatusDisplay.text = status.message
                permissionStatusDisplay.setTextColor(Color.RED)
            }

            is PermissionStatus.Blocked -> {
                Timber.i("Permission blocked in Activity")
                permissionStatusDisplay.text = status.message
                permissionStatusDisplay.setTextColor(Color.RED)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationServiceListener = LocationServiceListener(
                applicationContext,
                Intent(applicationContext, LocationService::class.java))

        setupButtonAndUI()
    }

    override fun onStart() {
        super.onStart()
        observeOnGpsStatus()
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
        Timber.i("Tracking start triggered from Activity")
        btnInitTracking.text = getString(R.string.button_text_end)
        locationServiceListener.subscribeToLocationUpdates()
    }

    private fun stopTracking() {
        Timber.i("Tracking stop triggered from Activity")
        btnInitTracking.text = getString(R.string.button_text_start)
        locationServiceListener.unsubscribeFromLocationUpdates()
    }

    private fun observeOnGpsStatus() = GpsStatusListener(this)
            .reObserve(this, gpsObserver)

    /**
     * First checks GPS and if Enabled then checks for Runtime Permissions
     */
    private fun checkGpsAndThenPermission(): Any = when (localGpsStatus) {
        is GpsStatus.GpsIsEnabled -> {
            gpsStatusDisplay.text = (localGpsStatus as GpsStatus.GpsIsEnabled).message
            gpsStatusDisplay.setTextColor(Color.BLUE)
            observeAndDisplayPermissionStatus()
        }

        is GpsStatus.GpsIsDisabled -> {
            observeAndDisplayPermissionStatus()
            gpsStatusDisplay.text = (localGpsStatus as GpsStatus.GpsIsDisabled).message
            gpsStatusDisplay.setTextColor(Color.RED)
            showGpsNotEnabledDialog()
        }
    }

    private fun observeAndDisplayPermissionStatus(): Any =
            PermissionStatusListener((this)).reObserve(this, permissionObserver)

    override fun onResume() {
        super.onResume()
        triggerSourceIsButton = false
        if (isServiceRunning) btnInitTracking.text = getString(R.string.button_text_end)
    }

    private fun showGpsNotEnabledDialog() = AlertDialog.Builder(this)
            .setTitle(R.string.gps_required_title)
            .setMessage(R.string.gps_required_body)
            .setPositiveButton(R.string.action_settings) { _, _ ->
                // Open app's settings.
                val intent = Intent().apply {
                    action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
                }
                startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
}
