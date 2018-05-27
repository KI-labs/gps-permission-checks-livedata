package com.wahibhaq.locationservicelivedata

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var fakeLocationListener: FakeLocationListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fakeLocationListener = FakeLocationListener(
            applicationContext,
            Intent(applicationContext, FakeLocationService::class.java)
        )

        btnInitTracking.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) fakeLocationListener.subscribeToLocationUpdates()
            else fakeLocationListener.unsubscribeFromLocationUpdates()
        }
    }

    override fun onResume() {
        super.onResume()
        when(FakeLocationService.isServiceRunning) {
            true -> {
                btnInitTracking.text = btnInitTracking.textOn;
                btnInitTracking.toggle()
            }
            false -> btnInitTracking.text = btnInitTracking.textOff
        }

    }



}
