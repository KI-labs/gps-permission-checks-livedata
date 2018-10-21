package com.wahibhaq.locationservicelivedata

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import com.wahibhaq.locationservicelivedata.LocationService.Companion.isServiceRunning
import com.wahibhaq.locationservicelivedata.LocationService.Companion.isTrackingRunning
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel

    private var alertDialog: AlertDialog? = null

    private val gpsObserver = Observer<GpsStatus> { status ->
        status?.let {
            updateGpsCheckUI(status)
        }
    }

    private val permissionObserver = Observer<PermissionStatus> { status ->
        status?.let {
            updatePermissionCheckUI(status)
            when (status) {
                is PermissionStatus.Granted -> handleGpsAlertDialog()
                is PermissionStatus.Denied -> showLocationPermissionNeededDialog()
            }
        }
    }

    private val permissionHandler = object : PermissionHandler() {
        override fun onGranted() {
            Timber.i("Activity: %s", R.string.permission_status_granted)
            updatePermissionCheckUI(PermissionStatus.Granted())
            handleGpsAlertDialog()
        }

        override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) {
            Timber.w("Activity: %s", R.string.permission_status_denied)
            updatePermissionCheckUI(PermissionStatus.Denied())
        }

        override fun onJustBlocked(
                context: Context?,
                justBlockedList: ArrayList<String>?,
                deniedPermissions: ArrayList<String>?
        ) {
            Timber.w("Activity: %s", R.string.permission_status_blocked)
            updatePermissionCheckUI(PermissionStatus.Blocked())
        }
    }

    private fun updateGpsCheckUI(status: GpsStatus) {
        when (status) {
            is GpsStatus.Enabled -> {
                gpsStatusDisplay.isEnabled = false
                gpsStatusDisplay.apply {
                    text = getString(status.message)
                    setTextColor(Color.BLUE)
                }

                handleGpsAlertDialog(GpsStatus.Enabled())
            }

            is GpsStatus.Disabled -> {
                gpsStatusDisplay.isEnabled = true
                gpsStatusDisplay.apply {
                    text = getString(status.message).plus(getString(R.string.click_to_retry))
                    setTextColor(Color.RED)
                }
            }
        }

        toggleButtonClickableState()
    }

    private fun updatePermissionCheckUI(status: PermissionStatus) {
        when (status) {
            is PermissionStatus.Granted -> {
                permissionStatusDisplay.isEnabled = false
                permissionStatusDisplay.apply {
                    text = getString(status.message)
                    setTextColor(Color.BLUE)
                }
            }

            is PermissionStatus.Denied -> {
                permissionStatusDisplay.isEnabled = true
                permissionStatusDisplay.apply {
                    text = getString(status.message.plus(R.string.click_to_retry))
                    setTextColor(Color.RED)
                }
            }

            is PermissionStatus.Blocked -> {
                permissionStatusDisplay.isEnabled = true
                permissionStatusDisplay.apply {
                    text = getString(status.message.plus(R.string.click_to_retry))
                    setTextColor(Color.RED)
                }
            }
        }

        toggleButtonClickableState()
    }

    private fun isTrackingRunningAlready() = isTrackingRunning && isServiceRunning

    private fun setupUI() {

        //Start Tracking Only if there is a need and it's valid to start tracking
        if (isTrackingRunningAlready()) btnControlTracking.text = getString(R.string.button_text_end)
        btnControlTracking.setOnClickListener {
            if (isTrackingRunningAlready().not())
                startTracking()
            else
                stopTracking()
        }

        /**
         * This is to simulate how user is alerted via notifications when activity start is detected
         * in background by [LocationService]
         */
        btnSimulateNotification.setOnClickListener {
            Handler().apply {
                postDelayed({ viewModel.startLocationTracking() }, 3000)
            }
        }

        gpsStatusDisplay.setOnClickListener { handleGpsAlertDialog() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionStatusDisplay.visibility = View.VISIBLE
            permissionStatusDisplay.setOnClickListener { showLocationPermissionNeededDialog() }
        }
    }

    private fun toggleButtonClickableState() {
        btnControlTracking.isEnabled = gpsStatusDisplay.isEnabled.not() && permissionStatusDisplay.isEnabled.not()
        btnSimulateNotification.isEnabled = btnControlTracking.isEnabled.not()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        subscribeToGpsListener()
        subscribeToLocationPermissionListener()
    }

    private fun subscribeToGpsListener() = viewModel.gpsStatusLiveData
            .observe(this, gpsObserver)

    private fun subscribeToLocationPermissionListener() =
            viewModel.locationPermissionStatusLiveData.observe(this, permissionObserver)

    private fun startTracking() {
        Timber.i("Tracking start triggered from Button on Activity")
        viewModel.startLocationTracking()
        btnControlTracking.text = getString(R.string.button_text_end)
    }

    private fun stopTracking() {
        Timber.i("Tracking stop triggered from Activity")
        viewModel.stopLocationTracking()
        btnControlTracking.text = getString(R.string.button_text_start)
    }

    override fun onResume() {
        super.onResume()
        setupUI()
    }

    /**
     *  Using current value of [GpsStatusListener] livedata as default
     */
    private fun handleGpsAlertDialog(status: GpsStatus = viewModel.gpsStatusLiveData.value as GpsStatus) {
        when (status) {
            is GpsStatus.Enabled -> hideGpsNotEnabledDialog()
            is GpsStatus.Disabled -> showGpsNotEnabledDialog()
        }
    }

    private fun showGpsNotEnabledDialog() {
        if (alertDialog?.isShowing == true) {
            return // null or already being shown
        }

        alertDialog = AlertDialog.Builder(this)
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

    private fun hideGpsNotEnabledDialog() {
        if (alertDialog?.isShowing == true) alertDialog?.dismiss()
    }

    private fun showLocationPermissionNeededDialog() {
        if (alertDialog?.isShowing == true) {
            return // null or dialog already being shown
        }

        alertDialog = AlertDialog.Builder(this)
                .setTitle(R.string.permission_required_title)
                .setMessage(R.string.permission_required_body)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    //Using 3rd party lib *Permissions* for showing dialogs and handling response
                    Permissions.check(this,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            null,
                            permissionHandler)
                }
                .setCancelable(false) //to disable outside click for cancel
                .create()

        alertDialog?.apply {
            show()
        }
    }
}
