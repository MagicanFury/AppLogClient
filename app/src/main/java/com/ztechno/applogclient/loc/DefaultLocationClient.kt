package com.ztechno.applogclient.loc

import android.os.Build
import android.content.Context
import android.location.Location
import android.annotation.SuppressLint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.utils.ALatLng
import com.ztechno.applogclient.utils.ZGps
import com.ztechno.applogclient.utils.hasLocationPermission
import com.ztechno.applogclient.utils.ZLog
import com.ztechno.applogclient.utils.ZTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlin.math.min

class DefaultLocationClient(
    private val locationService: LocationService,
    private val context: Context,
    private val client: FusedLocationProviderClient,
    private var gpsPriority: Int
): LocationClient {
    
    private var distFromHome: Double = Double.MAX_VALUE
    
    override fun isAtUserLocUsingWifi(): Boolean {
        val ssidWifi = locationService.connectionTicker.lastConnection?.ssid
        if (ssidWifi != null) {
            val found = locationService.userLocations.filter { it.wifi != null }.find { it.wifi == ssidWifi }
            if (found != null) {
                distFromHome = 0.0
                return true
            }
        }
        return false
    }
    
    override fun getUserLocUsingWifi(): Location? {
        val ssidWifi = locationService.connectionTicker.lastConnection?.ssid
        if (ssidWifi != null) {
            val found = locationService.userLocations.filter { it.wifi != null }.find { it.wifi == ssidWifi }
            if (found != null) {
                val fakeLocation = Location("").apply {
                    latitude = found.lat
                    longitude = found.lng
                    accuracy = 6.9f
                    time = ZTime.msSince1970()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        isMock = true
                    }
                }
                distFromHome = 0.0
//                ZLog.write("Mocking location becauseuser location detected with ssid: ${found.wifi}\n\t fakeLocation: $fakeLocation")
                return fakeLocation
            }
        }
        return null
    }
    
    @SuppressLint("MissingPermission")
    override fun fetchLocationSmart(serviceScope: CoroutineScope, callback: ((loc: Location?) -> Unit)?) {
        val fakeLocation = getUserLocUsingWifi()
        if (fakeLocation != null) {
            callback?.invoke(fakeLocation)
            return
        }
        
        serviceScope.launch {
            if (!context.hasLocationPermission()) {
                ZLog.write("[DefaultLocationClient] fetchLocation has no permission! :(")
                callback?.invoke(null)
                return@launch
            }
            
            client.getCurrentLocation(gpsPriority, object : CancellationToken() {
                override fun onCanceledRequested(arg: OnTokenCanceledListener) = CancellationTokenSource().token
                override fun isCancellationRequested() = false
            }).addOnSuccessListener { location ->
                if (location == null) {
                    callback?.invoke(null)
                } else {
                    callback?.invoke(location)
                }
            }.addOnCanceledListener {
                ZLog.info("[DefaultLocationClient]", "fetchLocation canceled!")
                callback?.invoke(null)
            }.addOnFailureListener {
                ZLog.error(it)
                callback?.invoke(null)
            }
        }
    }
    
    override fun getClosestUserLocDistance(location: Location?): Double {
        if (location != null) {
            isCloseToUserLocations(location)
        }
        return distFromHome
    }
    
    override fun isCloseToUserLocations(location: Location, thresholdDist: Double): Boolean {
        return isCloseToUserLocations(ALatLng(location.latitude, location.longitude), thresholdDist)
    }
    
    override fun isCloseToUserLocations(currLatLng: ALatLng, thresholdDist: Double): Boolean {
        var minDist: Double = Double.MAX_VALUE
        var isCloseToUserLoc = false
        locationService.userLocations.map {
            val latLng = ALatLng(it.lat, it.lng)
            val dist = ZGps.distancePrecise(currLatLng, latLng)
            minDist = min(minDist, dist)
            if (dist <= thresholdDist) {
                isCloseToUserLoc = true
            }
            distFromHome = minDist
        }
        return isCloseToUserLoc
    }

    override fun getProvider(): FusedLocationProviderClient {
        return client
    }
    
    override fun setGpsAccuracy(priority: Int) {
        gpsPriority = priority
    }
    
    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long, onClose: () -> Unit): Flow<Location> {
        ZLog.write("[DefaultLocationClient] getLocationUpdates(interval=${"%.2f".format((interval / 1000f))}s)")
        return callbackFlow {
//            if(!context.hasLocationPermission()) {
//                throw LocationClient.LocationException("Missing location permission")
//            }
//            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
//            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
//            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
//            if (!isGpsEnabled && !isNetworkEnabled) {
////                throw LocationClient.LocationException("GPS is disabled")
//                ZLog.error("GPS is disabled")
//            }
//
//            val builder = LocationRequest.Builder(gpsPriority, interval).apply {
//                setIntervalMillis(interval)
//                setMinUpdateIntervalMillis(interval)
//            }
//            val request = builder.build()
//
//            val locationCallback = object : LocationCallback() {
//                @RequiresApi(Build.VERSION_CODES.O)
//                override fun onLocationResult(result: LocationResult) {
//                    super.onLocationResult(result)
//                    if (gpsPriority != request.priority) {
//                        request.priority = gpsPriority
//                    }
//                    result.locations.lastOrNull()?.let { location ->
//                        launch { send(location) }
//                    }
//                }
//            }
//
//            client.requestLocationUpdates(
//                request,
//                locationCallback,
//                Looper.getMainLooper()
//            )
//            awaitClose {
//                ZLog.write("Calling client.removeLocationUpdates(locationCallback)")
//                client.removeLocationUpdates(locationCallback).addOnCompleteListener {
//                    onClose.invoke()
//                }
//            }
        }
    }
}