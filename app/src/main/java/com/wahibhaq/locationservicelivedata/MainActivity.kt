package com.wahibhaq.locationservicelivedata

import android.app.NotificationManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
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

    private lateinit var notificationsUtil: NotificationsUtil

    private lateinit var viewModel: MainViewModel

    private var shouldEnableGpsClick = false

    private var shouldEnablePermissionClick = false

    private lateinit var localGpsStatus: GpsStatus

    private val pairObserver = Observer<Pair<GpsStatus, PermissionStatus>> { pair ->
        pair?.let {
            Timber.i("Pairobserver received with : " + pair.first + " and " + pair.second)
            localGpsStatus = pair.first
            handleGpsStatus(pair.first)
            handlePermissionStatus(pair.second)
        }
    }

    private fun handleGpsStatus(status: GpsStatus) {
        when (status) {
            is GpsStatus.GpsIsEnabled -> {
                shouldEnableGpsClick = false
                gpsStatusDisplay.text = status.message
                gpsStatusDisplay.setTextColor(Color.BLUE)
            }

            is GpsStatus.GpsIsDisabled -> {
                shouldEnableGpsClick = true
                gpsStatusDisplay.text = status.message
                gpsStatusDisplay.setTextColor(Color.RED)
                showGpsNotEnabledDialog()
            }
        }
    }

    private fun handlePermissionStatus(status: PermissionStatus) {
        when (status) {
            is PermissionStatus.Granted -> {
                shouldEnablePermissionClick = false
                Timber.i("Permission granted in Activity")
                permissionStatusDisplay.text = status.message
                permissionStatusDisplay.setTextColor(Color.BLUE)
            }

            is PermissionStatus.Denied -> {
                shouldEnablePermissionClick = true
                Timber.i("Permission denied in Activity")
                permissionStatusDisplay.text = status.message
                permissionStatusDisplay.setTextColor(Color.RED)
            }

            is PermissionStatus.Blocked -> {
                shouldEnablePermissionClick = true
                Timber.i("Permission blocked in Activity")
                permissionStatusDisplay.text = status.message
                permissionStatusDisplay.setTextColor(Color.RED)
            }
        }

        btnInitTracking.isEnabled = shouldEnableGpsClick.not() && shouldEnablePermissionClick.not()
        gpsStatusDisplay.isEnabled = shouldEnableGpsClick
        permissionStatusDisplay.isEnabled = shouldEnablePermissionClick
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationServiceListener = LocationServiceListener(
                applicationContext,
                Intent(applicationContext, LocationService::class.java))

        notificationsUtil = NotificationsUtil(applicationContext,
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)

        setupButtonAndUI()
        notificationsUtil.cancelAlertNotification() //to clear if there were any notifications

        subscribeViewModel()
    }

    private fun subscribeViewModel() {
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        //Subscribe to updates from GPS and Runtime Permission in parallel
        viewModel.getStatusResponse().observeForever(pairObserver)
    }

    private fun setupButtonAndUI() {
        permissionStatusDisplay.text = getString(R.string.permission_status_undefined)
        gpsStatusDisplay.text = getString(R.string.gps_status_undefined)

        btnInitTracking.setOnClickListener {
            //Only if there is a need and it's valid to start tracking
            if (!isTrackingRunning && !isServiceRunning) startTracking()
            else stopTracking() //To toggle state
        }

        gpsStatusDisplay.setOnClickListener {
            handleGpsStatus(localGpsStatus)
        }

        permissionStatusDisplay.setOnClickListener {
            viewModel.getPermissionCheck().observe(this, Observer { state ->
                handlePermissionStatus(state!!)
            })
        }
    }

    private fun startTracking() {
        Timber.i("Tracking start triggered from Button on Activity")
        btnInitTracking.text = getString(R.string.button_text_end)
        locationServiceListener.subscribeToLocationUpdates()
    }

    private fun stopTracking() {
        Timber.i("Tracking stop triggered from Activity")
        btnInitTracking.text = getString(R.string.button_text_start)
        locationServiceListener.unsubscribeFromLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (isServiceRunning) btnInitTracking.text = getString(R.string.button_text_end)
    }

    override fun onDestroy() {
        super.onDestroy()
        unSubscribeLiveData()
    }

    private fun unSubscribeLiveData() {
        if (viewModel.getStatusResponse().hasObservers()) {
            Timber.d("LiveData Observer getting removed in onDestroy()")
            viewModel.getStatusResponse().removeObservers(this)
        }
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
