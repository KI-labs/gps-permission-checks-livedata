package com.wahibhaq.locationservicelivedata


interface LocationListener {
    fun subscribeToLocationUpdates()

    fun unsubscribeFromLocationUpdates()
}