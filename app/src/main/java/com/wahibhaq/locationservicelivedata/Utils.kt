package com.wahibhaq.locationservicelivedata

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer

//https://medium.com/@BladeCoder/architecture-components-pitfalls-part-1-9300dd969808
fun <T> LiveData<T>.reObserve(owner: LifecycleOwner, observer: Observer<T>) {
    removeObserver(observer)
    observe(owner, observer)
}