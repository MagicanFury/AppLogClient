package com.ztechno.applogclient.loc

import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun isIntervalChanged(interval: Long): Boolean
    fun getLocationUpdates(interval: Long, onClose: () -> Unit): Flow<Location>
    fun getProvider(): FusedLocationProviderClient
    fun setGpsAccuracy(priority: Int)
    class LocationException(message: String): Exception()
}