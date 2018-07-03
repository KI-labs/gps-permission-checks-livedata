package com.wahibhaq.locationservicelivedata.di

import android.content.Context
import android.content.Intent
import com.google.android.gms.location.LocationServices
import com.wahibhaq.locationservicelivedata.LocationServiceListener
import dagger.Module
import dagger.Provides

@Module
class MainActivityModule {

    @Provides
    fun providesIntent(context: Context) = Intent(context, LocationServices::class.java)

    @Provides
    fun providesLocationService(context: Context,intent : Intent) = LocationServiceListener(context,intent)
}