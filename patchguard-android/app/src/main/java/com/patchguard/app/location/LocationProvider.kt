package com.patchguard.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationProvider(context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location

    private val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS)
        .setMinUpdateIntervalMillis(MIN_INTERVAL_MS)
        .build()

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            _location.value = result.lastLocation
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    fun stop() {
        client.removeLocationUpdates(callback)
        _location.value = null
    }

    companion object {
        private const val INTERVAL_MS = 1000L
        private const val MIN_INTERVAL_MS = 500L
    }
}
