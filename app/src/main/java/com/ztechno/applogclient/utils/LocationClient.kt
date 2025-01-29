package com.ztechno.applogclient.utils

import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun getLocationUpdates(interval: Long): Flow<Location>
    fun getProvider(): FusedLocationProviderClient
    fun setGpsAccuracy(priority: Int)
    class LocationException(message: String): Exception()
}