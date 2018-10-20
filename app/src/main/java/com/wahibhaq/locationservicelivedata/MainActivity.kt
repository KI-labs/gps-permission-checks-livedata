package com.wahibhaq.locationservicelivedata

import android.Manifest
import android.app.NotificationManager
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

    private lateinit var locationServiceListener: LocationServiceListener

    private lateinit var notificationsUtil: NotificationsUtil

    private lateinit var viewModel: MainViewModel

    private var alertDialog: AlertDialog? = null

    private var shouldEnableGpsClick = false

    private var shouldEnablePermissionClick = false

    private val gpsObserver = Observer<GpsStatus> { status ->
        status?.let { handleGpsCheckUI(status) }
    }

    private val permissionObserver = Observer<PermissionStatus> { status ->
        status?.let {
            handlePermissionCheckUI(status)
            when (it) {
                is PermissionStatus.Granted -> handleGpsCheckDialog()
                is PermissionStatus.Denied -> showLocationPermissionNeededDialog()
            }
        }
    }

    private val permissionHandler = object : PermissionHandler() {
        override fun onGranted() {
            Timber.i("Activity: %s", R.string.permission_status_granted)
            handleGpsCheckDialog()
            handlePermissionCheckUI(PermissionStatus.Granted())
        }

        override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) {
            Timber.w("Activity: %s", R.string.permission_status_denied)
            handlePermissionCheckUI(PermissionStatus.Denied())
        }

        override fun onJustBlocked(
                context: Context?,
                justBlockedList: ArrayList<String>?,
                deniedPermissions: ArrayList<String>?
        ) {
            Timber.w("Activity: %s", R.string.permission_status_blocked)
            handlePermissionCheckUI(PermissionStatus.Blocked())
        }
    }

    /**
     *  Using current value of [GpsStatusListener] livedata as default
     */
    private fun handleGpsCheckDialog(status: GpsStatus = viewModel.gpsStatusLiveData.value as GpsStatus) {
        when (status) {
            is GpsStatus.Enabled -> hideGpsNotEnabledDialog()
            is GpsStatus.Disabled -> showGpsNotEnabledDialog()
        }
    }

    private fun handleGpsCheckUI(status: GpsStatus) {
        when (status) {
            is GpsStatus.Enabled -> {
                shouldEnableGpsClick = false
                gpsStatusDisplay.apply {
                    text = status.message
                    setTextColor(Color.BLUE)
                }

                handleGpsCheckDialog(GpsStatus.Enabled())
            }

            is GpsStatus.Disabled -> {
                shouldEnableGpsClick = true
                gpsStatusDisplay.apply {
                    text = status.message
                    setTextColor(Color.RED)
                }
            }
        }

        updateButtonEnableStatus()
    }

    private fun handlePermissionCheckUI(status: PermissionStatus) {
        when (status) {
            is PermissionStatus.Granted -> {
                shouldEnablePermissionClick = false
                permissionStatusDisplay.apply {
                    text = getString(R.string.permission_status_granted)
                    setTextColor(Color.BLUE)
                }
            }

            is PermissionStatus.Denied -> {
                shouldEnablePermissionClick = true
                permissionStatusDisplay.apply {
                    text = getString(R.string.permission_status_denied)
                    setTextColor(Color.RED)
                }
            }

            is PermissionStatus.Blocked -> {
                shouldEnablePermissionClick = true
                permissionStatusDisplay.apply {
                    text = getString(R.string.permission_status_blocked)
                    setTextColor(Color.RED)
                }
            }
        }

        updateButtonEnableStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        locationServiceListener = LocationServiceListener(
                this, Intent(applicationContext, LocationService::class.java))

        notificationsUtil = NotificationsUtil(this,
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)

        setupUI()
        subscribeToGpsListener()
        subscribeToLocationPermissionListener()
    }

    private fun subscribeToGpsListener() = viewModel.gpsStatusLiveData
            .observe(this, gpsObserver)

    private fun subscribeToLocationPermissionListener() =
            viewModel.locationPermissionStatusLiveData.observe(this, permissionObserver)

    /**
     * Using 3rd party lib *Permissions* for showing dialogs and handling response
     */
    private fun handlePermissionAndGpsCheck() = Permissions.check(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            R.string.permission_required_body,
            Permissions.Options()
                    .setSettingsDialogTitle(getString(R.string.permission_required_title))
                    .setSettingsDialogMessage(getString(R.string.permission_blocked_body))
                    .setRationaleDialogTitle(getString(R.string.permission_required_title)),
            permissionHandler)

    private fun setupUI() {
        btnInitTracking.setOnClickListener {
            //Only if there is a need and it's valid to start tracking
            if (!isTrackingRunning && !isServiceRunning) startTracking()
            else stopTracking() //To toggle state
        }

        /**
         * This is to simulate how user is alerted via notifications when activity start is detected
         * in background by [LocationService]
         */
        btnInitSimulatingDetection.setOnClickListener {
            Handler().apply {
                postDelayed({ locationServiceListener.subscribeToLocationUpdates() }, 3000)
            }
        }

        gpsStatusDisplay.setOnClickListener { handleGpsCheckDialog() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionStatusDisplay.visibility = View.VISIBLE
            permissionStatusDisplay.setOnClickListener { handlePermissionAndGpsCheck() }
        }
    }

    private fun updateButtonEnableStatus() {
        gpsStatusDisplay.isEnabled = shouldEnableGpsClick
        permissionStatusDisplay.isEnabled = shouldEnablePermissionClick

        if (btnInitTracking.text == getString(R.string.button_text_end))
            btnInitTracking.isEnabled = true //Button is always clickable to allow user to end tracking
        else
            btnInitTracking.isEnabled = shouldEnableGpsClick.not()
                    && shouldEnablePermissionClick.not()

        btnInitSimulatingDetection.isEnabled = btnInitTracking.isEnabled.not()
    }

    private fun startTracking() {
        Timber.i("Tracking start triggered from Button on Activity")
        locationServiceListener.subscribeToLocationUpdates()
        btnInitTracking.text = getString(R.string.button_text_end)
    }

    private fun stopTracking() {
        Timber.i("Tracking stop triggered from Activity")
        locationServiceListener.unsubscribeFromLocationUpdates()
        notificationsUtil.cancelAlertNotification()
        btnInitTracking.text = getString(R.string.button_text_start)
        updateButtonEnableStatus() //To make button disable again if needed
    }

    override fun onResume() {
        super.onResume()
        if (isServiceRunning) btnInitTracking.text = getString(R.string.button_text_end)
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
