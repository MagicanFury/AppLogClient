package com.plcoding.backgroundlocationtracking.utils

import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun getLocationUpdates(interval: Long): Flow<Location>
    fun getProvider(): FusedLocationProviderClient
    class LocationException(message: String): Exception()
}