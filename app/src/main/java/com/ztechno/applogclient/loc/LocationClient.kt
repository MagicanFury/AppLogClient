package com.ztechno.applogclient.loc

import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.ztechno.applogclient.utils.ALatLng
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun getClosestUserLocDistance(): Double
    fun isCloseToUserLocations(currLatLng: ALatLng, userLocations: List<ALatLng>, thresholdDist: Double = 50.0): Boolean
    fun getLocationUpdates(interval: Long, onClose: () -> Unit): Flow<Location>
    fun getProvider(): FusedLocationProviderClient
    fun setGpsAccuracy(priority: Int)
    class LocationException(message: String): Exception()
}