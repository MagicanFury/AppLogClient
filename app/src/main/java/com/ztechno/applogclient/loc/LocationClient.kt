package com.ztechno.applogclient.loc

import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.ztechno.applogclient.utils.ALatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun isAtUserLocUsingWifi(): Boolean
    fun getUserLocUsingWifi(): Location?
    fun fetchLocationSmart(serviceScope: CoroutineScope, callback: ((loc: Location?) -> Unit)? = null)
    fun getClosestUserLocDistance(location: Location?): Double
    fun isCloseToUserLocations(location: Location, thresholdDist: Double = 30.0): Boolean
    fun isCloseToUserLocations(currLatLng: ALatLng, thresholdDist: Double = 30.0): Boolean
    fun getLocationUpdates(interval: Long, onClose: () -> Unit): Flow<Location>
    fun getProvider(): FusedLocationProviderClient
    fun setGpsAccuracy(priority: Int)
    class LocationException(message: String): Exception()
}