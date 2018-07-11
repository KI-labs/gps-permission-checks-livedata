package com.wahibhaq.locationservicelivedata

import android.Manifest
import android.app.NotificationManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
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
        status?.let {
            handleGpsCheck()
        }
    }

    private val permissionHandler = object : PermissionHandler() {
        override fun onGranted() {
            shouldEnablePermissionClick = false
            Timber.i("Activity: %s", R.string.permission_status_granted)
            permissionStatusDisplay.text = getString(R.string.permission_status_granted)
            permissionStatusDisplay.setTextColor(Color.BLUE)
            handleGpsCheck()
        }

        override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) {
            shouldEnablePermissionClick = true
            Timber.w("Activity: %s", R.string.permission_status_denied)
            permissionStatusDisplay.text = getString(R.string.permission_status_denied)
            permissionStatusDisplay.setTextColor(Color.RED)
            updateButtonEnableStatus()
        }

        override fun onJustBlocked(
                context: Context?,
                justBlockedList: ArrayList<String>?,
                deniedPermissions: ArrayList<String>?
        ) {
            shouldEnablePermissionClick = true
            Timber.w("Activity: %s", R.string.permission_status_blocked)
            permissionStatusDisplay.text = getString(R.string.permission_status_blocked)
            permissionStatusDisplay.setTextColor(Color.RED)
            updateButtonEnableStatus()
        }
    }

    private fun handleGpsCheck() {
        val status = viewModel.gpsStatusLiveData.value //using current value of livedata
        when (status) {
            is GpsStatus.GpsIsEnabled -> {
                shouldEnableGpsClick = false
                gpsStatusDisplay.text = getString(status.message)
                gpsStatusDisplay.setTextColor(Color.BLUE)
            }

            is GpsStatus.GpsIsDisabled -> {
                shouldEnableGpsClick = true
                gpsStatusDisplay.text = getString(status.message)
                gpsStatusDisplay.setTextColor(Color.RED)
                showGpsNotEnabledDialog()
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
        notificationsUtil.cancelAlertNotification() //to clear if there were any notifications
        subscribeToGpsListener()
        setupUI()
    }

    private fun subscribeToGpsListener() =
    //Subscribe to gps status updates until activity is not destroyed
            viewModel.gpsStatusLiveData.observeForever(gpsObserver)


    private fun unsubscribeToGpsListener() {
        if (viewModel.gpsStatusLiveData.hasObservers()) {
            Timber.i("Removing LiveData Observer and LifeCycleOwner")
            viewModel.gpsStatusLiveData.removeObserver(gpsObserver)
        }
    }

    override fun onStart() {
        super.onStart()
        handlePermissionAndGpsCheck()
    }

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

        btnInitSimulatingDetection.setOnClickListener {
            Handler().apply {
                postDelayed({
                    locationServiceListener.subscribeToLocationUpdates()
                }, 3000)
            }
        }

        gpsStatusDisplay.setOnClickListener {
            handleGpsCheck()
        }

        permissionStatusDisplay.setOnClickListener {
            handlePermissionAndGpsCheck()
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

    override fun onDestroy() {
        unsubscribeToGpsListener()
        super.onDestroy()
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
}
